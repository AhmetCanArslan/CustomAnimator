package com.arslan.customanimator

import androidx.lifecycle.compose.LocalLifecycleOwner
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.LocalWriteSecureSettings

@Composable
fun ToolsScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    hasWriteSecureSettings: Boolean = LocalWriteSecureSettings.current,
    onNavigateToGraphicsApiOverride: () -> Unit,
    onNavigateToHwuiTweaks: () -> Unit,
    onNavigateToAppThreading: () -> Unit,
    onNavigateToRefreshRate: () -> Unit,
    onNavigateToQsTiles: () -> Unit,
    onNavigateToScreenshotActions: () -> Unit,
    onNavigateToWifiPasswords: () -> Unit,
    onNavigateToHotspotManager: () -> Unit,
    onNavigateToAlarmRevealer: () -> Unit,
    onNavigateToCarrierName: () -> Unit,
    onNavigateToOtpCopier: () -> Unit,
    onNavigateToStatusBarIcons: () -> Unit,
    onNavigateToSystemColors: () -> Unit,
    onNavigateToButtonRemapper: () -> Unit,
    onNavigateToDebloater: () -> Unit,
    onNavigateToCompileBooster: () -> Unit,
    onNavigateToShortcuts: () -> Unit,
    onNavigateToIconChanger: () -> Unit,
    onNavigateToApkExtractor: () -> Unit,
    onNavigateToApkInstaller: () -> Unit,
    onNavigateToActivityManager: () -> Unit,
    onNavigateToAutoActions: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    var canWriteSystemSettings by remember { mutableStateOf(ShizukuHelper.canWriteSystemSettings(context)) }
    var fancyImeDisabled by remember { mutableStateOf(DeveloperOptionsManager.isFancyImeAnimationsDisabled(contentResolver)) }
    var clockSecondsEnabled by remember { mutableStateOf(DeveloperOptionsManager.isClockSecondsEnabled(contentResolver)) }
    var rotationSuggestionsDisabled by remember { mutableStateOf(DeveloperOptionsManager.isRotationSuggestionsDisabled(contentResolver)) }
    var highVolumeWarningDisabled by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningDisabled(context)) }
    var highVolumeWarningPending by remember { mutableStateOf(DeveloperOptionsManager.isHighVolumeWarningPendingRestart(context)) }
    var isRotationLocked by remember { mutableStateOf(!DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)) }
    var userRotation by remember { mutableStateOf(DeveloperOptionsManager.getUserRotation(contentResolver)) }

    val lifecycleOwner = LocalLifecycleOwner.current
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
                isRotationLocked = !DeveloperOptionsManager.isAutoRotationEnabled(contentResolver)
                userRotation = DeveloperOptionsManager.getUserRotation(contentResolver)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val secureToggleEnabled = hasShizukuPermission || hasWriteSecureSettings
    val systemToggleEnabled = hasShizukuPermission || canWriteSystemSettings

    val applyToggle: (Boolean, (Boolean) -> Unit, () -> Boolean) -> Unit = { newValue, setState, action ->
        setState(newValue)
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            if (success) {
                maybeShowInterstitial(context)
            } else {
                setState(!newValue)
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    Column {
                        SetupNudgeCard(
                            message = stringResource(R.string.developer_needs_shizuku),
                            onOpenSetup = openSetup
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            cardSection(
                R.string.tools_section_apps,
                {
                    NavigationRow(
                        icon = Icons.Filled.DeleteSweep,
                        title = stringResource(R.string.debloater),
                        description = stringResource(R.string.debloater_desc),
                        onClick = onNavigateToDebloater
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Layers,
                        title = stringResource(R.string.activity_manager),
                        description = stringResource(R.string.activity_manager_desc),
                        onClick = onNavigateToActivityManager
                    )
                },
                {
                    CompileBoosterToolRow(onClick = onNavigateToCompileBooster)
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.PlaylistRemove,
                        title = stringResource(R.string.auto_actions),
                        description = stringResource(R.string.more_auto_actions_desc),
                        onClick = onNavigateToAutoActions
                    )
                }
            )

            cardSection(
                R.string.tools_section_apk,
                {
                    NavigationRow(
                        icon = Icons.Filled.InstallMobile,
                        title = stringResource(R.string.apk_installer),
                        description = stringResource(R.string.apk_installer_desc),
                        onClick = onNavigateToApkInstaller
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Unarchive,
                        title = stringResource(R.string.apk_extractor),
                        description = stringResource(R.string.apk_extractor_desc),
                        onClick = onNavigateToApkExtractor
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.AppShortcut,
                        title = stringResource(R.string.icon_changer),
                        description = stringResource(R.string.icon_changer_desc),
                        onClick = onNavigateToIconChanger
                    )
                }
            )

            cardSection(
                R.string.tools_section_automation,
                {
                    NavigationRow(
                        icon = Icons.AutoMirrored.Filled.AddToHomeScreen,
                        title = stringResource(R.string.shortcuts),
                        description = stringResource(R.string.shortcuts_desc),
                        onClick = onNavigateToShortcuts
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.PowerSettingsNew,
                        title = stringResource(R.string.button_remapper),
                        description = stringResource(R.string.button_remapper_desc),
                        onClick = onNavigateToButtonRemapper
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Screenshot,
                        title = stringResource(R.string.screenshot_actions),
                        description = stringResource(R.string.screenshot_actions_desc),
                        onClick = onNavigateToScreenshotActions
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Password,
                        title = stringResource(R.string.otp_copier),
                        description = stringResource(R.string.otp_copier_desc),
                        onClick = onNavigateToOtpCopier
                    )
                },
                {
                    SaveToDeviceRow()
                }
            )

            cardSection(
                R.string.tools_section_system_ui,
                {
                    NavigationRow(
                        icon = Icons.Filled.Widgets,
                        title = stringResource(R.string.qs_tiles),
                        description = stringResource(R.string.qs_tiles_desc),
                        onClick = onNavigateToQsTiles
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.WebAsset,
                        title = stringResource(R.string.status_bar_icons),
                        description = stringResource(R.string.status_bar_icons_desc),
                        onClick = onNavigateToStatusBarIcons
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.system_colors),
                        description = stringResource(R.string.system_colors_desc),
                        onClick = onNavigateToSystemColors
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.SignalCellularAlt,
                        title = stringResource(R.string.carrier_name),
                        description = stringResource(R.string.carrier_name_desc),
                        onClick = onNavigateToCarrierName
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Alarm,
                        title = stringResource(R.string.alarm_revealer),
                        description = stringResource(R.string.alarm_revealer_desc),
                        onClick = onNavigateToAlarmRevealer
                    )
                }
            )

            cardSection(
                R.string.tools_section_performance,
                {
                    NavigationRow(
                        icon = Icons.Filled.MonitorHeart,
                        title = stringResource(R.string.refresh_rate),
                        description = stringResource(R.string.refresh_rate_desc),
                        onClick = onNavigateToRefreshRate
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.VideogameAsset,
                        title = stringResource(R.string.graphics_api_override),
                        description = stringResource(R.string.graphics_api_override_desc),
                        onClick = onNavigateToGraphicsApiOverride
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Brush,
                        title = stringResource(R.string.hwui_tweaks),
                        description = stringResource(R.string.hwui_tweaks_desc),
                        onClick = onNavigateToHwuiTweaks
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.app_threading),
                        description = stringResource(R.string.app_threading_desc),
                        onClick = onNavigateToAppThreading
                    )
                }
            )

            cardSection(
                R.string.tools_section_connectivity,
                {
                    NavigationRow(
                        icon = Icons.Filled.Wifi,
                        title = stringResource(R.string.wifi_password_manager),
                        description = stringResource(R.string.wifi_password_manager_desc),
                        onClick = onNavigateToWifiPasswords
                    )
                },
                {
                    NavigationRow(
                        icon = Icons.Filled.WifiTethering,
                        title = stringResource(R.string.hotspot_manager),
                        description = stringResource(R.string.hotspot_manager_desc),
                        onClick = onNavigateToHotspotManager
                    )
                }
            )

            cardSection(
                R.string.developer_tweaks,
                {
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
                },
                {
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
                },
                {
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
                        InfoNote(text = stringResource(R.string.show_clock_seconds_samsung_note), dismissKey = "clock_seconds_samsung")
                    }
                },
                {
                    ToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
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
                        InfoNote(text = stringResource(R.string.remove_high_volume_warning_note), dismissKey = "high_volume_warning")
                    }
                },
                {
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
                                        Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                },
                {
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
                                    resources.getString(if (success) R.string.action_succeeded else R.string.action_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                {
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
                                        resources.getString(R.string.action_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            )

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
