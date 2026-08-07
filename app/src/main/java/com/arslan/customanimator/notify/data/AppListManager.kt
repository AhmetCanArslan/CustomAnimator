package com.arslan.customanimator.notify.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

object AppListManager {
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val iconIndex = ConcurrentHashMap<String, ImageBitmap>()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var receiverRegistered = false

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh(context)
        }
    }

    fun initialize(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        registerPackageChangeReceiverIfNeeded(ctx)
        if (_installedApps.value.isNotEmpty()) return
        refreshInstalledApps(ctx)
    }

    fun refresh(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        appContext = ctx
        registerPackageChangeReceiverIfNeeded(ctx)
        refreshInstalledApps(ctx)
    }

    private fun refreshInstalledApps(ctx: Context) {
        applicationScope.launch {
            val pm = ctx.packageManager
            val iconDir = File(ctx.cacheDir, ICON_CACHE_DIR).also { it.mkdirs() }

            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val packages = try {
                pm.queryIntentActivities(launcherIntent, 0)
                    .mapNotNull { it.activityInfo?.applicationInfo }
                    .distinctBy { it.packageName }
            } catch (_: Exception) { emptyList() }

            val updateTimes: Map<String, Long> = packages.mapNotNull { appInfo ->
                try {
                    appInfo.packageName to pm.getPackageInfo(appInfo.packageName, 0).lastUpdateTime
                } catch (_: Exception) { null }
            }.toMap()

            val phase1Apps = packages.map { appInfo ->
                val icon = loadIconFromDisk(iconDir, appInfo.packageName)
                if (icon != null) iconIndex[appInfo.packageName] = icon
                AppItem(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = icon
                )
            }.sortedBy { it.name.lowercase() }

            val packageSet = phase1Apps.mapTo(HashSet(phase1Apps.size)) { it.packageName }
            for (knownPkg in iconIndex.keys) {
                if (knownPkg !in packageSet) iconIndex.remove(knownPkg)
            }

            _installedApps.value = phase1Apps

            val mutableApps = ArrayList(phase1Apps)
            var changed = false

            for (i in mutableApps.indices) {
                val app = mutableApps[i]
                val iconFile = File(iconDir, iconFileName(app.packageName))
                val updateTime = updateTimes[app.packageName] ?: 0L
                if (iconFile.exists() && iconFile.lastModified() >= updateTime && app.icon != null) continue

                val bitmap = try {
                    drawableToBitmap(pm.getApplicationIcon(app.packageName))
                } catch (_: Exception) { null } ?: continue

                saveIconToDisk(bitmap, iconFile)
                val img = bitmap.asImageBitmap()
                iconIndex[app.packageName] = img
                mutableApps[i] = app.copy(icon = img)
                changed = true
            }

            if (changed) _installedApps.value = ArrayList(mutableApps)
        }
    }

    private fun registerPackageChangeReceiverIfNeeded(ctx: Context) {
        if (receiverRegistered) return

        synchronized(this) {
            if (receiverRegistered) return

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(packageChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                ctx.registerReceiver(packageChangeReceiver, filter)
            }

            receiverRegistered = true
        }
    }

    fun getIconForPackage(packageName: String): ImageBitmap? {
        iconIndex[packageName]?.let { return it }

        val ctx = appContext ?: return null
        val iconDir = File(ctx.cacheDir, ICON_CACHE_DIR)
        val iconFile = File(iconDir, iconFileName(packageName))

        if (iconFile.exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(iconFile.absolutePath)
                if (bmp != null) {
                    val img = bmp.asImageBitmap()
                    iconIndex[packageName] = img
                    return img
                }
            } catch (_: Exception) {}
        }

        return try {
            val bitmap = drawableToBitmap(ctx.packageManager.getApplicationIcon(packageName))
            iconDir.mkdirs()
            saveIconToDisk(bitmap, iconFile)
            val img = bitmap.asImageBitmap()
            iconIndex[packageName] = img
            img
        } catch (_: Exception) { null }
    }

    private fun loadIconFromDisk(iconDir: File, packageName: String): ImageBitmap? = try {
        val file = File(iconDir, iconFileName(packageName))
        if (!file.exists()) null
        else BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    } catch (_: Exception) { null }

    private fun saveIconToDisk(bitmap: Bitmap, file: File) {
        try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {}
    }

    private fun iconFileName(packageName: String) = "${packageName.replace('/', '_')}.png"

    private const val ICON_CACHE_DIR = "app_icons"
    private const val ICON_SIZE_PX = 48

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val rawBitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        return if (rawBitmap.width > ICON_SIZE_PX || rawBitmap.height > ICON_SIZE_PX) {
            Bitmap.createScaledBitmap(rawBitmap, ICON_SIZE_PX, ICON_SIZE_PX, true)
        } else {
            rawBitmap
        }
    }
}

@Composable
fun AppRow(
    app: AppItem,
    actionIcon: ImageVector? = null,
    actionIconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(40.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionIcon != null) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AppSelectionTable(
    installedApps: List<AppItem>,
    selectedApps: List<AppItem>,
    onSelectedAppsChanged: (List<AppItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = installedApps.isEmpty()
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    val selectedPackages = remember(selectedApps) {
        selectedApps.map { it.packageName }.toHashSet()
    }
    val unselectedApps = remember(installedApps, selectedPackages) {
        installedApps.filter { it.packageName !in selectedPackages }
    }

    val filteredUnselectedApps = remember(unselectedApps, searchQuery) {
        if (searchQuery.isBlank()) unselectedApps
        else unselectedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredSelectedApps = remember(selectedApps, searchQuery) {
        if (searchQuery.isBlank()) selectedApps
        else selectedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.pn_target_apps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.pn_search_apps)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.pn_cd_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(stringResource(R.string.pn_available), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredUnselectedApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                AppRow(
                                    app = app,
                                    actionIcon = Icons.Default.Add,
                                    onClick = { onSelectedAppsChanged(selectedApps + app) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    VerticalDivider()
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.pn_selected), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            IconButton(
                                onClick = { onSelectedAppsChanged(unselectedApps) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.pn_cd_swap_apps),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredSelectedApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                AppRow(
                                    app = app,
                                    actionIcon = Icons.Default.Check,
                                    onClick = { onSelectedAppsChanged(selectedApps - app) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
