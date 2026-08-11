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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.UsageAccessHelper

private val GrantedGreen: Color
    @Composable
    get() = LocalExtendedColors.current.success

enum class AppPermission {
    SHIZUKU,
    SECURE_SETTINGS,
    USAGE_ACCESS,
    NOTIFICATION_ACCESS,
    POST_NOTIFICATIONS,
    CAMERA_FLASH,
    OVERLAY,
    BATTERY_UNRESTRICTED
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

    AppPermission.USAGE_ACCESS -> UsageAccessHelper.hasUsageAccess(context)

    AppPermission.NOTIFICATION_ACCESS ->
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    AppPermission.POST_NOTIFICATIONS ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    AppPermission.CAMERA_FLASH ->
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    AppPermission.OVERLAY -> Settings.canDrawOverlays(context)

    AppPermission.BATTERY_UNRESTRICTED ->
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
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
        if (RuleType.FLASH in actionTypes) add(AppPermission.CAMERA_FLASH)
        if (RuleType.FLASH_SCREEN in actionTypes) add(AppPermission.OVERLAY)
        if (RuleType.AOD in actionTypes) add(AppPermission.SECURE_SETTINGS)
    }

    return NOTIFY_BASE_REQUIRED + extras
}

fun hasAllNotifyPermissions(context: Context): Boolean =
    notifyRequiredPermissions(context).all { isPermissionGranted(context, it) }

@Composable
private fun systemAccessEntries(context: Context, isShizukuAvailable: Boolean): List<PermissionEntry> {
    val shizukuGranted = isPermissionGranted(context, AppPermission.SHIZUKU)
    return listOf(
        PermissionEntry(
            permission = AppPermission.SHIZUKU,
            icon = Icons.Filled.Terminal,
            title = stringResource(R.string.permission_shizuku),
            description = when {
                !isShizukuAvailable -> stringResource(R.string.permission_shizuku_unavailable)
                shizukuGranted -> stringResource(R.string.permission_shizuku_ready)
                else -> stringResource(R.string.permission_shizuku_desc)
            },
            granted = shizukuGranted,
            optional = true
        ),
        PermissionEntry(
            permission = AppPermission.SECURE_SETTINGS,
            icon = Icons.Filled.Security,
            title = stringResource(R.string.pn_permission_secure_settings),
            description = stringResource(R.string.permission_secure_settings_desc),
            granted = isPermissionGranted(context, AppPermission.SECURE_SETTINGS)
        ),
        PermissionEntry(
            permission = AppPermission.USAGE_ACCESS,
            icon = Icons.Filled.QueryStats,
            title = stringResource(R.string.permission_usage_access),
            description = stringResource(R.string.permission_usage_access_desc),
            granted = isPermissionGranted(context, AppPermission.USAGE_ACCESS)
        )
    )
}

@Composable
private fun notificationEntries(context: Context): List<PermissionEntry> = listOf(
    PermissionEntry(
        permission = AppPermission.NOTIFICATION_ACCESS,
        icon = Icons.Filled.NotificationsActive,
        title = stringResource(R.string.pn_permission_notification_access),
        description = stringResource(R.string.pn_permission_notification_access_desc),
        granted = isPermissionGranted(context, AppPermission.NOTIFICATION_ACCESS)
    ),
    PermissionEntry(
        permission = AppPermission.POST_NOTIFICATIONS,
        icon = Icons.Filled.Notifications,
        title = stringResource(R.string.pn_permission_post_notifications),
        description = stringResource(R.string.pn_permission_post_notifications_desc),
        granted = isPermissionGranted(context, AppPermission.POST_NOTIFICATIONS)
    )
)

@Composable
private fun hardwareEntries(context: Context): List<PermissionEntry> {
    val required = notifyRequiredPermissions(context)
    return listOf(
        PermissionEntry(
            permission = AppPermission.CAMERA_FLASH,
            icon = Icons.Filled.CameraAlt,
            title = stringResource(R.string.pn_permission_camera_flash),
            description = stringResource(R.string.pn_permission_camera_flash_desc),
            granted = isPermissionGranted(context, AppPermission.CAMERA_FLASH),
            optional = AppPermission.CAMERA_FLASH !in required
        ),
        PermissionEntry(
            permission = AppPermission.OVERLAY,
            icon = Icons.Filled.Layers,
            title = stringResource(R.string.pn_permission_overlay_title),
            description = stringResource(R.string.permission_overlay_desc),
            granted = isPermissionGranted(context, AppPermission.OVERLAY),
            optional = AppPermission.OVERLAY !in required
        ),
        PermissionEntry(
            permission = AppPermission.BATTERY_UNRESTRICTED,
            icon = Icons.Filled.BatteryFull,
            title = stringResource(R.string.pn_permission_run_background),
            description = stringResource(R.string.pn_permission_run_background_desc),
            granted = isPermissionGranted(context, AppPermission.BATTERY_UNRESTRICTED)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAdbDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshKey++ }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }

    val systemEntries = key(refreshKey) { systemAccessEntries(context, isShizukuAvailable) }
    val notificationItems = key(refreshKey) { notificationEntries(context) }
    val hardwareItems = key(refreshKey) { hardwareEntries(context) }
    val allEntries = systemEntries + notificationItems + hardwareItems
    val grantedCount = allEntries.count { it.granted }

    val onGrant: (AppPermission) -> Unit = { permission ->
        when (permission) {
            AppPermission.SHIZUKU -> {
                if (isShizukuAvailable) {
                    ShizukuHelper.requestShizukuPermission(context)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.permission_shizuku_unavailable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            AppPermission.SECURE_SETTINGS -> {
                val grantedViaShizuku = ShizukuHelper.hasShizukuPermission() &&
                    ShizukuHelper.grantWriteSecureSettingsPermission(context)
                if (grantedViaShizuku) {
                    refreshKey++
                    Toast.makeText(
                        context,
                        context.getString(R.string.action_succeeded),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showAdbDialog = true
                }
            }

            AppPermission.USAGE_ACCESS -> UsageAccessHelper.openUsageAccessSettings(context)

            AppPermission.NOTIFICATION_ACCESS ->
                settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

            AppPermission.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AppPermission.CAMERA_FLASH ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)

            AppPermission.OVERLAY -> settingsLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )

            AppPermission.BATTERY_UNRESTRICTED -> settingsLauncher.launch(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

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
        },
        bottomBar = { BannerAdView() }
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
                    title = stringResource(R.string.permissions_section_hardware),
                    entries = hardwareItems,
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

    if (showAdbDialog) {
        AdbGrantDialog(
            onDismiss = { showAdbDialog = false },
            onConfirm = {
                showAdbDialog = false
                refreshKey++
            }
        )
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
private fun PermissionSection(
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
                        context.getString(R.string.pn_command_copied),
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
