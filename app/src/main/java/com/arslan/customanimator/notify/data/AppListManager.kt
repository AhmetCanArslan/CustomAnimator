package com.arslan.customanimator.notify.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.AppIcon
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppItem(
    val name: String,
    val packageName: String
)

object AppListManager {
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

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
            val ownApp = AppItem(
                name = ctx.applicationInfo.loadLabel(ctx.packageManager).toString(),
                packageName = ctx.packageName
            )
            val otherApps = InstalledAppsProvider.getLaunchableApps(ctx).map { AppItem(it.label, it.packageName) }
            _installedApps.value = (otherApps + ownApp).sortedBy { it.name.lowercase() }
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
        AppIcon(packageName = app.packageName, modifier = Modifier.size(24.dp))
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
                        }, shapes = IconButtonDefaults.shapes()) {
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
                    LoadingIndicator()
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
                                modifier = Modifier.size(24.dp),
                                shapes = IconButtonDefaults.shapes()
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
