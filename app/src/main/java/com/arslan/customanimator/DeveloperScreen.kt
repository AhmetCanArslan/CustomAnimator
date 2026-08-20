package com.arslan.customanimator

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.FormatTextdirectionRToL
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    hasWriteSecureSettings: Boolean,
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

    val secureToggleEnabled = hasShizukuPermission || hasWriteSecureSettings
    val systemToggleEnabled = hasShizukuPermission || canWriteSystemSettings

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canWriteSystemSettings = ShizukuHelper.canWriteSystemSettings(context)
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
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
