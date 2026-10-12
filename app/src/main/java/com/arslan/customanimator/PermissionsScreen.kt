package com.arslan.customanimator

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.ContentCopy
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.theme.MonoBody
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.notify.data.RuleType
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.screenshot.ScreenshotPermissions
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.ShizukuState
import com.arslan.customanimator.utils.LocalShizukuAvailable

private val GrantedGreen: Color
    @Composable
    get() = LocalExtendedColors.current.success

enum class AppPermission {
    SHIZUKU,
    SECURE_SETTINGS,
    NOTIFICATION_ACCESS,
    POST_NOTIFICATIONS,
    OVERLAY,
    BATTERY_UNRESTRICTED,
    IMAGES
}

data class PermissionEntry(
    val permission: AppPermission,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val optional: Boolean = false
)

fun isPermissionGranted(context: Context, permission: AppPermission): Boolean = when (permission) {
    AppPermission.SHIZUKU -> ShizukuHelper.hasShizukuPermission()

    AppPermission.SECURE_SETTINGS ->
        ShizukuHelper.hasWriteSecureSettingsPermission(context)

    AppPermission.NOTIFICATION_ACCESS ->
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    AppPermission.POST_NOTIFICATIONS ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    AppPermission.OVERLAY -> Settings.canDrawOverlays(context)

    AppPermission.BATTERY_UNRESTRICTED ->
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    AppPermission.IMAGES -> ScreenshotPermissions.hasImagesPermission(context)
}

private val NOTIFY_BASE_REQUIRED = listOf(
    AppPermission.NOTIFICATION_ACCESS,
    AppPermission.POST_NOTIFICATIONS,
    AppPermission.BATTERY_UNRESTRICTED
)

fun notifyRequiredPermissions(context: Context): List<AppPermission> {
    val actionTypes = RulesManager(context).getRules()
        .filter { it.isEnabled }
        .flatMap { it.actions }
        .map { it.type }
        .toSet()

    val extras = buildList {
        if (RuleType.FLASH_SCREEN in actionTypes) add(AppPermission.OVERLAY)
        if (RuleType.AOD in actionTypes) add(AppPermission.SECURE_SETTINGS)
    }

    return NOTIFY_BASE_REQUIRED + extras
}

val SCREENSHOT_PERMISSIONS = listOf(
    AppPermission.IMAGES,
    AppPermission.POST_NOTIFICATIONS,
    AppPermission.OVERLAY
)

private val SYSTEM_PERMISSIONS = listOf(AppPermission.SHIZUKU, AppPermission.SECURE_SETTINGS)

private val NOTIFICATION_PERMISSIONS = listOf(
    AppPermission.NOTIFICATION_ACCESS,
    AppPermission.POST_NOTIFICATIONS
)

private val OTHER_PERMISSIONS = listOf(
    AppPermission.OVERLAY,
    AppPermission.BATTERY_UNRESTRICTED,
    AppPermission.IMAGES
)

private val PERMISSION_ICONS = mapOf(
    AppPermission.SHIZUKU to Icons.Filled.Terminal,
    AppPermission.SECURE_SETTINGS to Icons.Filled.Security,
    AppPermission.NOTIFICATION_ACCESS to Icons.Filled.NotificationsActive,
    AppPermission.POST_NOTIFICATIONS to Icons.Filled.Notifications,
    AppPermission.OVERLAY to Icons.Filled.Layers,
    AppPermission.BATTERY_UNRESTRICTED to Icons.Filled.BatteryFull,
    AppPermission.IMAGES to Icons.Filled.Image
)

private val PERMISSION_TITLES = mapOf(
    AppPermission.SHIZUKU to R.string.permission_shizuku,
    AppPermission.SECURE_SETTINGS to R.string.pn_permission_secure_settings,
    AppPermission.NOTIFICATION_ACCESS to R.string.pn_permission_notification_access,
    AppPermission.POST_NOTIFICATIONS to R.string.pn_permission_post_notifications,
    AppPermission.OVERLAY to R.string.pn_permission_overlay_title,
    AppPermission.BATTERY_UNRESTRICTED to R.string.pn_permission_run_background,
    AppPermission.IMAGES to R.string.screenshot_perm_images
)

private val PERMISSION_DESCRIPTIONS = mapOf(
    AppPermission.SHIZUKU to R.string.permission_shizuku_desc,
    AppPermission.SECURE_SETTINGS to R.string.permission_secure_settings_desc,
    AppPermission.NOTIFICATION_ACCESS to R.string.pn_permission_notification_access_desc,
    AppPermission.POST_NOTIFICATIONS to R.string.pn_permission_post_notifications_desc,
    AppPermission.OVERLAY to R.string.permission_overlay_desc,
    AppPermission.BATTERY_UNRESTRICTED to R.string.pn_permission_run_background_desc,
    AppPermission.IMAGES to R.string.permission_images_desc
)

private fun descriptionOf(permission: AppPermission, granted: Boolean, isShizukuAvailable: Boolean): Int = when {
    permission != AppPermission.SHIZUKU -> PERMISSION_DESCRIPTIONS.getValue(permission)
    !isShizukuAvailable -> R.string.permission_shizuku_unavailable
    granted -> R.string.permission_shizuku_ready
    else -> R.string.permission_shizuku_desc
}

@Composable
fun permissionEntries(
    permissions: List<AppPermission>,
    optional: Set<AppPermission> = emptySet(),
    isShizukuAvailable: Boolean = LocalShizukuAvailable.current
): List<PermissionEntry> {
    val context = LocalContext.current
    return permissions.map { permission ->
        val granted = isPermissionGranted(context, permission)
        PermissionEntry(
            permission = permission,
            icon = PERMISSION_ICONS.getValue(permission),
            title = stringResource(PERMISSION_TITLES.getValue(permission)),
            description = stringResource(descriptionOf(permission, granted, isShizukuAvailable)),
            granted = granted,
            optional = permission in optional
        )
    }
}

@Composable
fun rememberPermissionRefresh(): MutableIntState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val refresh = remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh.intValue++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return refresh
}

@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (!isPermissionGranted(context, AppPermission.POST_NOTIFICATIONS)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun requestShizuku(context: Context, isShizukuAvailable: Boolean) {
    if (isShizukuAvailable) {
        ShizukuState.requestPermission(context)
    } else {
        Toast.makeText(context, R.string.permission_shizuku_unavailable, Toast.LENGTH_SHORT).show()
    }
}

private fun grantSecureSettings(context: Context): Boolean {
    val granted = ShizukuHelper.hasShizukuPermission() && ShizukuHelper.grantWriteSecureSettingsPermission(context)
    if (granted) {
        ShizukuState.refresh()
        Toast.makeText(context, R.string.action_succeeded, Toast.LENGTH_SHORT).show()
    }
    return granted
}

private fun settingsIntentOf(context: Context, permission: AppPermission): Intent {
    if (permission == AppPermission.NOTIFICATION_ACCESS) {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    val action = if (permission == AppPermission.OVERLAY) {
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    } else {
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    }
    return Intent(action, Uri.parse("package:${context.packageName}"))
}

@Composable
fun rememberPermissionRequester(
    onChanged: () -> Unit,
    isShizukuAvailable: Boolean = LocalShizukuAvailable.current
): (AppPermission) -> Unit {
    val context = LocalContext.current
    var showAdbDialog by remember { mutableStateOf(false) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onChanged() }

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onChanged() }

    val imagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onChanged()
        if (!granted) {
            runCatching { context.startActivity(ScreenshotPermissions.appSettingsIntent(context)) }
        }
    }

    if (showAdbDialog) {
        AdbGrantDialog(
            onDismiss = { showAdbDialog = false },
            onConfirm = {
                showAdbDialog = false
                onChanged()
            }
        )
    }

    return { permission ->
        when (permission) {
            AppPermission.SHIZUKU -> requestShizuku(context, isShizukuAvailable)

            AppPermission.SECURE_SETTINGS -> {
                if (grantSecureSettings(context)) onChanged() else showAdbDialog = true
            }

            AppPermission.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AppPermission.IMAGES -> imagesLauncher.launch(ScreenshotPermissions.imagesPermission)

            AppPermission.NOTIFICATION_ACCESS,
            AppPermission.OVERLAY,
            AppPermission.BATTERY_UNRESTRICTED ->
                settingsLauncher.launch(settingsIntentOf(context, permission))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean = LocalShizukuAvailable.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val refresh = rememberPermissionRefresh()
    val onGrant = rememberPermissionRequester(
        onChanged = { refresh.intValue++ },
        isShizukuAvailable = isShizukuAvailable
    )

    val notifyRequired = notifyRequiredPermissions(context)
    val systemEntries = key(refresh.intValue) {
        permissionEntries(SYSTEM_PERMISSIONS, setOf(AppPermission.SHIZUKU), isShizukuAvailable)
    }
    val notificationItems = key(refresh.intValue) { permissionEntries(NOTIFICATION_PERMISSIONS) }
    val otherItems = key(refresh.intValue) {
        permissionEntries(
            OTHER_PERMISSIONS,
            setOfNotNull(AppPermission.IMAGES, AppPermission.OVERLAY.takeIf { it !in notifyRequired })
        )
    }
    val allEntries = systemEntries + notificationItems + otherItems
    val grantedCount = allEntries.count { it.granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.permissions_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                PermissionsSummary(granted = grantedCount, total = allEntries.size)
            }
            val secureSettingsGranted = systemEntries
                .firstOrNull { it.permission == AppPermission.SECURE_SETTINGS }
                ?.granted == true
            if (!secureSettingsGranted) {
                item {
                    SecureSettingsSetupCard(
                        isShizukuAvailable = isShizukuAvailable,
                        onGrantViaShizuku = { onGrant(AppPermission.SECURE_SETTINGS) }
                    )
                }
            }
            item {
                PermissionSection(
                    title = stringResource(R.string.permissions_section_system),
                    entries = systemEntries,
                    onGrant = onGrant
                )
            }
            item {
                PermissionSection(
                    title = stringResource(R.string.permissions_section_notifications),
                    entries = notificationItems,
                    onGrant = onGrant
                )
            }
            item {
                PermissionSection(
                    title = stringResource(R.string.permissions_section_other),
                    entries = otherItems,
                    onGrant = onGrant
                )
            }
            item {
                Text(
                    text = stringResource(R.string.permissions_footer_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionsSummary(granted: Int, total: Int) {
    val allGranted = granted == total
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (allGranted) GrantedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permissions_summary, granted, total),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (allGranted) {
                        stringResource(R.string.permissions_summary_all_desc)
                    } else {
                        stringResource(R.string.permissions_summary_partial_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionSection(
    title: String,
    entries: List<PermissionEntry>,
    onGrant: (AppPermission) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, entry ->
                    PermissionRow(entry = entry, onGrant = { onGrant(entry.permission) })
                    if (index != entries.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionRow(entry: PermissionEntry, onGrant: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val stacked = maxWidth < 340.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !entry.granted, onClick = onGrant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (entry.granted) {
                        GrantedGreen.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = if (entry.granted) GrantedGreen else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = entry.title, style = MaterialTheme.typography.titleSmall)
                if (entry.optional) {
                    Text(
                        text = stringResource(R.string.permission_optional),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (entry.granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.pn_permission_granted),
                tint = GrantedGreen,
                modifier = Modifier.size(22.dp)
            )
        } else if (!stacked) {
            TextButton(onClick = onGrant) {
                Text(stringResource(R.string.grant))
            }
        }
    }
    if (stacked && !entry.granted) {
        TextButton(onClick = onGrant, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.grant))
        }
    }
    }
    }
}

@Composable
private fun AdbGrantDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.pn_grant_secure_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.pn_grant_secure_settings_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.pn_grant_secure_settings_adb_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AdbCommandBox()
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.pn_got_it)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun SecureSettingsSetupCard(
    isShizukuAvailable: Boolean,
    onGrantViaShizuku: () -> Unit
) {
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = Icons.Filled.Security,
                    size = 40.dp,
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.pn_grant_secure_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(R.string.permissions_secure_settings_setup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isShizukuAvailable) {
                Button(
                    onClick = onGrantViaShizuku,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.grant))
                }
            }
            Text(
                text = stringResource(R.string.pn_grant_secure_settings_adb_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            AdbCommandBox(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AdbCommandBox(
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val adbCommand =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = AppShapes.field,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SelectionContainer {
                Text(
                    text = adbCommand,
                    style = MonoBody,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("ADB Command", adbCommand)
                    )
                    Toast.makeText(
                        context,
                        resources.getString(R.string.pn_command_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.pn_copy))
            }
        }
    }
}
