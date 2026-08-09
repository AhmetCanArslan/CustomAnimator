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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatTextdirectionRToL
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.utils.CloseAppsExclusionManager
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.service.FpsOverlayService
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.FpsOverlayManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class QuickAction { CLEAR_CACHES, CLOSE_APPS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    hasWriteSecureSettings: Boolean,
    onNavigateToAutoForceStop: () -> Unit,
    onNavigateToAutoPermissionDisabler: () -> Unit,
    onNavigateToGraphicsApiOverride: () -> Unit,
    onNavigateToCloseAppsExclusions: () -> Unit,
    onNavigateToWifiPasswords: () -> Unit,
    onNavigateToAlarmRevealer: () -> Unit,
    onNavigateToCarrierName: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    var adbEnabled by remember { mutableStateOf(DeveloperOptionsManager.isAdbEnabled(contentResolver)) }
    var adbWifiEnabled by remember { mutableStateOf(DeveloperOptionsManager.isAdbWifiEnabled(contentResolver)) }
    var dontKeepActivities by remember { mutableStateOf(DeveloperOptionsManager.isAlwaysFinishActivitiesEnabled(contentResolver)) }
    var limitBackgroundProcesses by remember { mutableStateOf(DeveloperOptionsManager.isBackgroundProcessLimitEnabled(contentResolver)) }

    var canWriteSystemSettings by remember { mutableStateOf(ShizukuHelper.canWriteSystemSettings(context)) }
    var showTouches by remember { mutableStateOf(DeveloperOptionsManager.isShowTouchesEnabled(contentResolver)) }
    var pointerLocation by remember { mutableStateOf(DeveloperOptionsManager.isPointerLocationEnabled(contentResolver)) }
    var forceRtl by remember { mutableStateOf(DeveloperOptionsManager.isForceRtlEnabled(contentResolver)) }
    var layoutBounds by remember { mutableStateOf(false) }
    var gpuProfiling by remember { mutableStateOf(false) }
    var sensorsOff by remember { mutableStateOf(false) }

    var fancyImeDisabled by remember { mutableStateOf(DeveloperOptionsManager.isFancyImeAnimationsDisabled(contentResolver)) }
    var clockSecondsEnabled by remember { mutableStateOf(DeveloperOptionsManager.isClockSecondsEnabled(contentResolver)) }
    var highVolumeWarningDisabled by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningDisabled(context)) }
    var highVolumeWarningPending by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningPendingRestart(context)) }
    var fpsMeterEnabled by remember {
        mutableStateOf(FpsOverlayManager.isActive(context))
    }
    var isRotationLocked by remember { mutableStateOf(!DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)) }
    var userRotation by remember { mutableStateOf(DeveloperOptionsManager.getUserRotation(contentResolver)) }

    var runningAction by remember { mutableStateOf<QuickAction?>(null) }
    val isBusy = runningAction != null
    var showClearCachesConfirm by remember { mutableStateOf(false) }
    var showCloseAppsConfirm by remember { mutableStateOf(false) }
    var showCompileAllConfirm by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                adbEnabled = DeveloperOptionsManager.isAdbEnabled(contentResolver)
                adbWifiEnabled = DeveloperOptionsManager.isAdbWifiEnabled(contentResolver)
                dontKeepActivities = DeveloperOptionsManager.isAlwaysFinishActivitiesEnabled(contentResolver)
                limitBackgroundProcesses = DeveloperOptionsManager.isBackgroundProcessLimitEnabled(contentResolver)
                canWriteSystemSettings = ShizukuHelper.canWriteSystemSettings(context)
                showTouches = DeveloperOptionsManager.isShowTouchesEnabled(contentResolver)
                pointerLocation = DeveloperOptionsManager.isPointerLocationEnabled(contentResolver)
                forceRtl = DeveloperOptionsManager.isForceRtlEnabled(contentResolver)
                fancyImeDisabled = DeveloperOptionsManager.isFancyImeAnimationsDisabled(contentResolver)
                clockSecondsEnabled = DeveloperOptionsManager.isClockSecondsEnabled(contentResolver)
                DeveloperOptionsManager.reapplyHighVolumeWarning(context)
                highVolumeWarningDisabled = DeveloperOptionsManager.isHighVolumeWarningDisabled(context)
                highVolumeWarningPending = DeveloperOptionsManager.isHighVolumeWarningPendingRestart(context)
                fpsMeterEnabled = FpsOverlayManager.isActive(context)
                isRotationLocked = !DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)
                userRotation = DeveloperOptionsManager.getUserRotation(contentResolver)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasShizukuPermission) {
        if (!hasShizukuPermission) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            layoutBounds = DeveloperOptionsManager.isLayoutBoundsEnabled()
            gpuProfiling = DeveloperOptionsManager.isGpuProfilingEnabled()
            sensorsOff = DeveloperOptionsManager.isSensorsOffEnabled()
        }
    }

    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()

    val actionsEnabled = hasShizukuPermission && !isBusy
    val secureToggleEnabled = hasShizukuPermission || hasWriteSecureSettings
    val systemToggleEnabled = hasShizukuPermission || canWriteSystemSettings

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val requestNotificationsIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canWriteSystemSettings = ShizukuHelper.canWriteSystemSettings(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (FpsOverlayManager.canDrawOverlay(context)) {
            FpsOverlayManager.setEnabled(context, true)
            fpsMeterEnabled = true
            requestNotificationsIfNeeded()
            FpsOverlayService.start(context)
        } else {
            fpsMeterEnabled = false
        }
    }

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

    val applyToggle: (Boolean, (Boolean) -> Unit, () -> Boolean) -> Unit = { newValue, setState, action ->
        setState(newValue)
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            if (success) {
                maybeShowInterstitial(context)
            } else {
                setState(!newValue)
                Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val runAction: (QuickAction, () -> Boolean, Int) -> Unit = { which, action, successMessageRes ->
        runningAction = which
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            runningAction = null
            Toast.makeText(
                context,
                context.getString(if (success) successMessageRes else R.string.action_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            if (!hasShizukuPermission && !canWriteSystemSettings) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.card
                    ) {
                        ActionRow(
                            icon = Icons.Filled.EditNote,
                            title = stringResource(R.string.write_settings_permission),
                            description = stringResource(R.string.write_settings_permission_desc),
                            buttonLabel = stringResource(R.string.grant),
                            enabled = true,
                            onClick = {
                                writeSettingsLauncher.launch(ShizukuHelper.writeSystemSettingsIntent(context))
                            }
                        )
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.developer_toggles))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        ToggleRow(
                            icon = Icons.Filled.Usb,
                            title = stringResource(R.string.usb_debugging),
                            description = stringResource(R.string.usb_debugging_desc),
                            checked = adbEnabled,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { adbEnabled = it },
                                    { DeveloperOptionsManager.setAdbEnabled(context, contentResolver, newValue) }
                                )
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ToggleRow(
                                icon = Icons.Filled.WifiTethering,
                                title = stringResource(R.string.wireless_debugging),
                                description = stringResource(R.string.wireless_debugging_desc),
                                checked = adbWifiEnabled,
                                enabled = secureToggleEnabled,
                                onCheckedChange = { newValue ->
                                    applyToggle(
                                        newValue,
                                        { adbWifiEnabled = it },
                                        { DeveloperOptionsManager.setAdbWifiEnabled(context, contentResolver, newValue) }
                                    )
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.LayersClear,
                            title = stringResource(R.string.dont_keep_activities),
                            description = stringResource(R.string.dont_keep_activities_desc),
                            checked = dontKeepActivities,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { dontKeepActivities = it },
                                    { DeveloperOptionsManager.setAlwaysFinishActivities(context, contentResolver, newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.Memory,
                            title = stringResource(R.string.limit_background_processes),
                            description = stringResource(R.string.limit_background_processes_desc),
                            checked = limitBackgroundProcesses,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { limitBackgroundProcesses = it },
                                    { DeveloperOptionsManager.setBackgroundProcessLimit(context, contentResolver, newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.SensorsOff,
                            title = stringResource(R.string.sensors_off),
                            description = stringResource(R.string.sensors_off_desc),
                            checked = sensorsOff,
                            enabled = hasShizukuPermission,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { sensorsOff = it },
                                    { DeveloperOptionsManager.setSensorsOff(newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.TouchApp,
                            title = stringResource(R.string.show_taps),
                            description = stringResource(R.string.show_taps_desc),
                            checked = showTouches,
                            enabled = systemToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { showTouches = it },
                                    { DeveloperOptionsManager.setShowTouches(context, contentResolver, newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.MyLocation,
                            title = stringResource(R.string.pointer_location),
                            description = stringResource(R.string.pointer_location_desc),
                            checked = pointerLocation,
                            enabled = systemToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { pointerLocation = it },
                                    { DeveloperOptionsManager.setPointerLocation(context, contentResolver, newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.BorderAll,
                            title = stringResource(R.string.show_layout_bounds),
                            description = stringResource(R.string.show_layout_bounds_desc),
                            checked = layoutBounds,
                            enabled = hasShizukuPermission,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { layoutBounds = it },
                                    { DeveloperOptionsManager.setLayoutBounds(newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.BarChart,
                            title = stringResource(R.string.gpu_profiling),
                            description = stringResource(R.string.gpu_profiling_desc),
                            checked = gpuProfiling,
                            enabled = hasShizukuPermission,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { gpuProfiling = it },
                                    { DeveloperOptionsManager.setGpuProfiling(newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.FormatTextdirectionRToL,
                            title = stringResource(R.string.force_rtl),
                            description = stringResource(R.string.force_rtl_desc),
                            checked = forceRtl,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { forceRtl = it },
                                    { DeveloperOptionsManager.setForceRtl(context, contentResolver, newValue) }
                                )
                            }
                        )
                        InfoNote(text = stringResource(R.string.developer_tiles_restart_note))
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.developer_quick_actions))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        QuickActionRow(
                            icon = Icons.Filled.CleaningServices,
                            title = stringResource(R.string.clear_all_app_caches),
                            description = stringResource(R.string.clear_all_app_caches_desc),
                            enabled = actionsEnabled,
                            isRunning = runningAction == QuickAction.CLEAR_CACHES,
                            onClick = { showClearCachesConfirm = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        QuickActionRow(
                            icon = Icons.Filled.StopCircle,
                            title = stringResource(R.string.close_background_apps),
                            description = stringResource(R.string.close_background_apps_desc),
                            enabled = actionsEnabled,
                            isRunning = runningAction == QuickAction.CLOSE_APPS,
                            onClick = { showCloseAppsConfirm = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Block,
                            title = stringResource(R.string.close_apps_exclusions),
                            description = stringResource(R.string.close_apps_exclusions_desc),
                            onClick = onNavigateToCloseAppsExclusions
                        )
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.auto_actions))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
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
                DevSectionTitle(stringResource(R.string.developer_tweaks))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        ToggleRow(
                            icon = Icons.Filled.Keyboard,
                            title = stringResource(R.string.disable_keyboard_animation),
                            description = stringResource(R.string.disable_keyboard_animation_desc),
                            checked = fancyImeDisabled,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { fancyImeDisabled = it },
                                    { DeveloperOptionsManager.setFancyImeAnimations(context, contentResolver, newValue) }
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.Schedule,
                            title = stringResource(R.string.show_clock_seconds),
                            description = stringResource(R.string.show_clock_seconds_desc),
                            checked = clockSecondsEnabled,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { clockSecondsEnabled = it },
                                    { DeveloperOptionsManager.setClockSeconds(context, contentResolver, newValue) }
                                )
                            }
                        )
                        if (DeveloperOptionsManager.isOneUi()) {
                            InfoNote(text = stringResource(R.string.show_clock_seconds_samsung_note))
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.VolumeUp,
                            title = stringResource(R.string.remove_high_volume_warning),
                            description = stringResource(R.string.remove_high_volume_warning_desc),
                            checked = highVolumeWarningDisabled,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { highVolumeWarningDisabled = it },
                                    {
                                        DeveloperOptionsManager.setHighVolumeWarningDisabled(context, contentResolver, newValue)
                                            .also { highVolumeWarningPending = DeveloperOptionsManager.isHighVolumeWarningPendingRestart(context) }
                                    }
                                )
                            }
                        )
                        if (highVolumeWarningPending) {
                            InfoNote(text = stringResource(R.string.remove_high_volume_warning_note))
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.Speed,
                            title = stringResource(R.string.fps_meter),
                            description = stringResource(R.string.fps_meter_desc),
                            checked = fpsMeterEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    if (FpsOverlayManager.canDrawOverlay(context)) {
                                        FpsOverlayManager.setEnabled(context, true)
                                        fpsMeterEnabled = true
                                        requestNotificationsIfNeeded()
                                        FpsOverlayService.start(context)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.fps_overlay_permission_needed),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        overlayPermissionLauncher.launch(
                                            FpsOverlayManager.overlayPermissionIntent(context)
                                        )
                                    }
                                } else {
                                    FpsOverlayManager.setEnabled(context, false)
                                    fpsMeterEnabled = false
                                    FpsOverlayService.stop(context)
                                }
                            }
                        )
                        if (fpsMeterEnabled) {
                            InfoNote(text = stringResource(R.string.fps_meter_hint))
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Wifi,
                            title = stringResource(R.string.wifi_password_manager),
                            description = stringResource(R.string.wifi_password_manager_desc),
                            onClick = onNavigateToWifiPasswords
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Alarm,
                            title = stringResource(R.string.alarm_revealer),
                            description = stringResource(R.string.alarm_revealer_desc),
                            onClick = onNavigateToAlarmRevealer
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.SignalCellularAlt,
                            title = stringResource(R.string.carrier_name),
                            description = stringResource(R.string.carrier_name_desc),
                            onClick = onNavigateToCarrierName
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ActionRow(
                            icon = Icons.Filled.RestartAlt,
                            title = stringResource(R.string.restart_system_ui),
                            description = stringResource(R.string.restart_system_ui_desc),
                            buttonLabel = stringResource(R.string.restart),
                            enabled = hasShizukuPermission,
                            onClick = {
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        DeveloperOptionsManager.restartSystemUi()
                                    }
                                    if (!success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.action_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.ScreenLockRotation,
                            title = stringResource(R.string.lock_screen_rotation),
                            description = stringResource(R.string.lock_screen_rotation_desc),
                            checked = isRotationLocked,
                            enabled = systemToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { isRotationLocked = it },
                                    { DeveloperOptionsManager.setAutoRotation(context, contentResolver, !newValue) }
                                )
                            }
                        )
                        if (isRotationLocked) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            RotationSelector(
                                currentRotation = userRotation,
                                enabled = systemToggleEnabled,
                                onRotationSelected = { rotation ->
                                    val previous = userRotation
                                    userRotation = rotation
                                    coroutineScope.launch {
                                        val success = withContext(Dispatchers.IO) {
                                            DeveloperOptionsManager.setUserRotation(context, contentResolver, rotation)
                                        }
                                        if (!success) {
                                            userRotation = previous
                                            Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ActionRow(
                            icon = Icons.Filled.ScreenRotation,
                            title = stringResource(R.string.reset_rotation),
                            description = stringResource(R.string.reset_rotation_desc),
                            buttonLabel = stringResource(R.string.reset),
                            enabled = systemToggleEnabled,
                            onClick = {
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        DeveloperOptionsManager.resetRotation(context, contentResolver)
                                    }
                                    isRotationLocked = !DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)
                                    userRotation = DeveloperOptionsManager.getUserRotation(contentResolver)
                                    Toast.makeText(
                                        context,
                                        context.getString(if (success) R.string.action_succeeded else R.string.action_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
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
                    runAction(
                        QuickAction.CLEAR_CACHES,
                        { DeveloperOptionsManager.clearAllAppCaches() },
                        R.string.action_succeeded
                    )
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
                    runningAction = QuickAction.CLOSE_APPS
                    coroutineScope.launch {
                        val closedCount = withContext(Dispatchers.IO) {
                            val skip = CloseAppsExclusionManager(context).getSelectedPackages() +
                                InstalledAppsProvider.getUnsafeToKillPackages(context)
                            InstalledAppsProvider.getLaunchableApps(context)
                                .filterNot { skip.contains(it.packageName) }
                                .count { DeveloperOptionsManager.forceStopApp(it.packageName) }
                        }
                        runningAction = null
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
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    isRunning: Boolean,
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
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onClick, enabled = enabled) {
            Text(
                text = if (isRunning) stringResource(R.string.working) else stringResource(R.string.apply_settings),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 340.dp
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!stacked) {
                    Button(onClick = onClick, enabled = enabled) {
                        Text(text = buttonLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            if (stacked) {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = buttonLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
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
            Text(text = stringResource(R.string.compile_booster), style = MaterialTheme.typography.titleSmall,)
            Text(
                text = if (isRunning) {
                    stringResource(R.string.compile_booster_progress, percent)
                } else {
                    stringResource(R.string.compile_booster_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isRunning) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        } else {
            Button(onClick = onClick, enabled = enabled) {
                Text(stringResource(R.string.compile_all_apps_short), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RotationSelector(
    currentRotation: Int,
    enabled: Boolean,
    onRotationSelected: (Int) -> Unit
) {
    val rotations = listOf(
        0 to stringResource(R.string.rotation_portrait),
        1 to stringResource(R.string.rotation_landscape_90),
        2 to stringResource(R.string.rotation_reverse_portrait),
        3 to stringResource(R.string.rotation_landscape_270)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.rotation_orientation).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        rotations.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { onRotationSelected(value) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentRotation == value,
                    onClick = { if (enabled) onRotationSelected(value) },
                    enabled = enabled
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
