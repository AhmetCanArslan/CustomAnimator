package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    onNavigateToAutoForceStop: () -> Unit,
    onNavigateToAutoPermissionDisabler: () -> Unit,
    onNavigateToGraphicsApiOverride: () -> Unit
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    var adbEnabled by remember { mutableStateOf(DeveloperOptionsManager.isAdbEnabled(contentResolver)) }
    var adbWifiEnabled by remember { mutableStateOf(DeveloperOptionsManager.isAdbWifiEnabled(contentResolver)) }
    var dontKeepActivities by remember { mutableStateOf(DeveloperOptionsManager.isAlwaysFinishActivitiesEnabled(contentResolver)) }
    var limitBackgroundProcesses by remember { mutableStateOf(DeveloperOptionsManager.isBackgroundProcessLimitEnabled(contentResolver)) }

    var isBusy by remember { mutableStateOf(false) }
    var showClearCachesConfirm by remember { mutableStateOf(false) }
    var showCloseAppsConfirm by remember { mutableStateOf(false) }
    var showCompileAllConfirm by remember { mutableStateOf(false) }

    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()

    val actionsEnabled = hasShizukuPermission && !isBusy

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless; service still runs without a visible notification if denied */ }

    val startCompileAll: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        CompileBoosterService.start(context)
    }

    val runAction: (() -> Boolean, Int) -> Unit = { action, successMessageRes ->
        isBusy = true
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            isBusy = false
            Toast.makeText(
                context,
                context.getString(if (success) successMessageRes else R.string.action_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.developer),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.developer_needs_shizuku),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.developer_toggles))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        ToggleRow(
                            title = stringResource(R.string.usb_debugging),
                            description = stringResource(R.string.usb_debugging_desc),
                            checked = adbEnabled,
                            onCheckedChange = { newValue ->
                                val previous = adbEnabled
                                adbEnabled = newValue
                                if (!DeveloperOptionsManager.setAdbEnabled(context, contentResolver, newValue)) {
                                    adbEnabled = previous
                                    Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ToggleRow(
                                title = stringResource(R.string.wireless_debugging),
                                description = stringResource(R.string.wireless_debugging_desc),
                                checked = adbWifiEnabled,
                                onCheckedChange = { newValue ->
                                    val previous = adbWifiEnabled
                                    adbWifiEnabled = newValue
                                    if (!DeveloperOptionsManager.setAdbWifiEnabled(context, contentResolver, newValue)) {
                                        adbWifiEnabled = previous
                                        Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            title = stringResource(R.string.dont_keep_activities),
                            description = stringResource(R.string.dont_keep_activities_desc),
                            checked = dontKeepActivities,
                            onCheckedChange = { newValue ->
                                val previous = dontKeepActivities
                                dontKeepActivities = newValue
                                if (!DeveloperOptionsManager.setAlwaysFinishActivities(context, contentResolver, newValue)) {
                                    dontKeepActivities = previous
                                    Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            title = stringResource(R.string.limit_background_processes),
                            description = stringResource(R.string.limit_background_processes_desc),
                            checked = limitBackgroundProcesses,
                            onCheckedChange = { newValue ->
                                val previous = limitBackgroundProcesses
                                limitBackgroundProcesses = newValue
                                if (!DeveloperOptionsManager.setBackgroundProcessLimit(context, contentResolver, newValue)) {
                                    limitBackgroundProcesses = previous
                                    Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.developer_quick_actions))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        QuickActionRow(
                            icon = Icons.Filled.CleaningServices,
                            title = stringResource(R.string.clear_all_app_caches),
                            description = stringResource(R.string.clear_all_app_caches_desc),
                            enabled = actionsEnabled,
                            onClick = { showClearCachesConfirm = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        QuickActionRow(
                            icon = Icons.Filled.StopCircle,
                            title = stringResource(R.string.close_background_apps),
                            description = stringResource(R.string.close_background_apps_desc),
                            enabled = actionsEnabled,
                            onClick = { showCloseAppsConfirm = true }
                        )
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.auto_force_stop))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        NavigationRow(
                            icon = Icons.Filled.PlaylistRemove,
                            title = stringResource(R.string.auto_force_stop),
                            description = stringResource(R.string.auto_force_stop_desc),
                            onClick = onNavigateToAutoForceStop
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Shield,
                            title = stringResource(R.string.auto_permission_disabler),
                            description = stringResource(R.string.auto_permission_disabler_desc),
                            onClick = onNavigateToAutoPermissionDisabler
                        )
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.developer_performance))
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        CompileBoosterRow(
                            isRunning = compileProgress.isRunning,
                            percent = compileProgress.percent,
                            enabled = hasShizukuPermission,
                            onClick = { showCompileAllConfirm = true },
                            onCancel = { CompileBoosterService.stop(context) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.VideogameAsset,
                            title = stringResource(R.string.graphics_api_override),
                            description = stringResource(R.string.graphics_api_override_desc),
                            onClick = onNavigateToGraphicsApiOverride
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearCachesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCachesConfirm = false },
            title = { Text(stringResource(R.string.clear_caches_confirm_title)) },
            text = { Text(stringResource(R.string.clear_caches_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showClearCachesConfirm = false
                    runAction({ DeveloperOptionsManager.clearAllAppCaches() }, R.string.action_succeeded)
                }) {
                    Text(stringResource(R.string.clear_all_app_caches))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearCachesConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCloseAppsConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseAppsConfirm = false },
            title = { Text(stringResource(R.string.close_apps_confirm_title)) },
            text = { Text(stringResource(R.string.close_apps_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showCloseAppsConfirm = false
                    isBusy = true
                    coroutineScope.launch {
                        val closedCount = withContext(Dispatchers.IO) {
                            InstalledAppsProvider.getLaunchableApps(context)
                                .count { DeveloperOptionsManager.forceStopApp(it.packageName) }
                        }
                        isBusy = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.apps_closed_count, closedCount),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text(stringResource(R.string.close_background_apps))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCloseAppsConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCompileAllConfirm) {
        AlertDialog(
            onDismissRequest = { showCompileAllConfirm = false },
            title = { Text(stringResource(R.string.compile_all_apps_confirm_title)) },
            text = { Text(stringResource(R.string.compile_all_apps_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showCompileAllConfirm = false
                    startCompileAll()
                }) {
                    Text(stringResource(R.string.compile_all_apps))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCompileAllConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DevSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onClick, enabled = enabled) {
            Text(text = if (!enabled) stringResource(R.string.working) else stringResource(R.string.apply_settings), fontSize = 12.sp)
        }
    }
}

@Composable
private fun CompileBoosterRow(
    isRunning: Boolean,
    percent: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.compile_booster), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (isRunning) {
                    stringResource(R.string.compile_booster_progress, percent)
                } else {
                    stringResource(R.string.compile_booster_desc)
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isRunning) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.cancel), fontSize = 12.sp)
            }
        } else {
            Button(onClick = onClick, enabled = enabled) {
                Text(stringResource(R.string.compile_all_apps), fontSize = 12.sp)
            }
        }
    }
}
