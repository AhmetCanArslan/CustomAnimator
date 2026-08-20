package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.service.FpsOverlayService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.FpsOverlayManager
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ToolsScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    hasWriteSecureSettings: Boolean,
    onNavigateToGraphicsApiOverride: () -> Unit,
    onNavigateToScreenshotActions: () -> Unit,
    onNavigateToSoundTile: () -> Unit,
    onNavigateToWifiPasswords: () -> Unit,
    onNavigateToHotspotManager: () -> Unit,
    onNavigateToAlarmRevealer: () -> Unit,
    onNavigateToCarrierName: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    var canWriteSystemSettings by remember { mutableStateOf(ShizukuHelper.canWriteSystemSettings(context)) }
    var fancyImeDisabled by remember { mutableStateOf(DeveloperOptionsManager.isFancyImeAnimationsDisabled(contentResolver)) }
    var clockSecondsEnabled by remember { mutableStateOf(DeveloperOptionsManager.isClockSecondsEnabled(contentResolver)) }
    var rotationSuggestionsDisabled by remember { mutableStateOf(DeveloperOptionsManager.isRotationSuggestionsDisabled(contentResolver)) }
    var highVolumeWarningDisabled by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningDisabled(context)) }
    var highVolumeWarningPending by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningPendingRestart(context)) }
    var fpsMeterEnabled by remember { mutableStateOf(FpsOverlayManager.isActive(context)) }
    var isRotationLocked by remember { mutableStateOf(!DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)) }
    var userRotation by remember { mutableStateOf(DeveloperOptionsManager.getUserRotation(contentResolver)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                canWriteSystemSettings = ShizukuHelper.canWriteSystemSettings(context)
                fancyImeDisabled = DeveloperOptionsManager.isFancyImeAnimationsDisabled(contentResolver)
                clockSecondsEnabled = DeveloperOptionsManager.isClockSecondsEnabled(contentResolver)
                rotationSuggestionsDisabled = DeveloperOptionsManager.isRotationSuggestionsDisabled(contentResolver)
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

            item {
                DevSectionTitle(stringResource(R.string.tools_utilities))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        NavigationRow(
                            icon = Icons.Filled.Screenshot,
                            title = stringResource(R.string.screenshot_actions),
                            description = stringResource(R.string.screenshot_actions_desc),
                            onClick = onNavigateToScreenshotActions
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.VolumeUp,
                            title = stringResource(R.string.sound_tile),
                            description = stringResource(R.string.sound_tile_desc),
                            onClick = onNavigateToSoundTile
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
                            icon = Icons.Filled.ScreenRotation,
                            title = stringResource(R.string.disable_rotation_suggestions),
                            description = stringResource(R.string.disable_rotation_suggestions_desc),
                            checked = rotationSuggestionsDisabled,
                            enabled = secureToggleEnabled,
                            onCheckedChange = { newValue ->
                                applyToggle(
                                    newValue,
                                    { rotationSuggestionsDisabled = it },
                                    { DeveloperOptionsManager.setRotationSuggestions(context, contentResolver, newValue) }
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
                            icon = Icons.Filled.WifiTethering,
                            title = stringResource(R.string.hotspot_manager),
                            description = stringResource(R.string.hotspot_manager_desc),
                            onClick = onNavigateToHotspotManager
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
}
