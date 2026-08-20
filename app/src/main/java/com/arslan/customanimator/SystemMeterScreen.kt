package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.service.FpsOverlayService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.FpsOverlayManager
import com.arslan.customanimator.utils.SystemMeterMetric

private fun metricIcon(metric: SystemMeterMetric): ImageVector = when (metric) {
    SystemMeterMetric.FPS -> Icons.Filled.Speed
    SystemMeterMetric.CPU_FREQ -> Icons.Filled.Memory
    SystemMeterMetric.CPU_TEMP -> Icons.Filled.Thermostat
    SystemMeterMetric.GPU_FREQ -> Icons.Filled.VideogameAsset
    SystemMeterMetric.RAM -> Icons.Filled.Storage
    SystemMeterMetric.BATTERY_LEVEL -> Icons.Filled.BatteryFull
    SystemMeterMetric.BATTERY_TEMP -> Icons.Filled.DeviceThermostat
    SystemMeterMetric.BATTERY_CURRENT -> Icons.Filled.Bolt
}

private fun metricTitle(metric: SystemMeterMetric): Int = when (metric) {
    SystemMeterMetric.FPS -> R.string.meter_fps
    SystemMeterMetric.CPU_FREQ -> R.string.meter_cpu_freq
    SystemMeterMetric.CPU_TEMP -> R.string.meter_cpu_temp
    SystemMeterMetric.GPU_FREQ -> R.string.meter_gpu_freq
    SystemMeterMetric.RAM -> R.string.meter_ram
    SystemMeterMetric.BATTERY_LEVEL -> R.string.meter_battery_level
    SystemMeterMetric.BATTERY_TEMP -> R.string.meter_battery_temp
    SystemMeterMetric.BATTERY_CURRENT -> R.string.meter_battery_current
}

private fun metricDesc(metric: SystemMeterMetric): Int = when (metric) {
    SystemMeterMetric.FPS -> R.string.meter_fps_desc
    SystemMeterMetric.CPU_FREQ -> R.string.meter_cpu_freq_desc
    SystemMeterMetric.CPU_TEMP -> R.string.meter_cpu_temp_desc
    SystemMeterMetric.GPU_FREQ -> R.string.meter_gpu_freq_desc
    SystemMeterMetric.RAM -> R.string.meter_ram_desc
    SystemMeterMetric.BATTERY_LEVEL -> R.string.meter_battery_level_desc
    SystemMeterMetric.BATTERY_TEMP -> R.string.meter_battery_temp_desc
    SystemMeterMetric.BATTERY_CURRENT -> R.string.meter_battery_current_desc
}

@Composable
fun SystemMeterScreenContent(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current

    var meterEnabled by remember { mutableStateOf(FpsOverlayManager.isActive(context)) }
    val metricStates = remember {
        SystemMeterMetric.entries
            .map { FpsOverlayManager.isMetricEnabled(context, it) }
            .toMutableStateList()
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                meterEnabled = FpsOverlayManager.isActive(context)
                SystemMeterMetric.entries.forEachIndexed { index, metric ->
                    metricStates[index] = FpsOverlayManager.isMetricEnabled(context, metric)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    val startMeter: () -> Unit = {
        FpsOverlayManager.setEnabled(context, true)
        meterEnabled = true
        requestNotificationsIfNeeded()
        FpsOverlayService.start(context)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (FpsOverlayManager.canDrawOverlay(context)) {
            startMeter()
        } else {
            meterEnabled = false
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
            item {
                DevSectionTitle(stringResource(R.string.system_meter_overlay))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    ToggleRow(
                        icon = Icons.Filled.Visibility,
                        title = stringResource(R.string.system_meter_enable),
                        description = stringResource(R.string.system_meter_enable_desc),
                        checked = meterEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                if (metricStates.none { it }) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.system_meter_need_metric),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (FpsOverlayManager.canDrawOverlay(context)) {
                                    startMeter()
                                    maybeShowInterstitial(context)
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
                                meterEnabled = false
                                FpsOverlayService.stop(context)
                            }
                        }
                    )
                    if (meterEnabled) {
                        InfoNote(text = stringResource(R.string.fps_meter_hint))
                    }
                }
            }

            item {
                DevSectionTitle(stringResource(R.string.system_meter_metrics))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    androidx.compose.foundation.layout.Column {
                        SystemMeterMetric.entries.forEachIndexed { index, metric ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                            ToggleRow(
                                icon = metricIcon(metric),
                                title = stringResource(metricTitle(metric)),
                                description = stringResource(metricDesc(metric)),
                                checked = metricStates[index],
                                onCheckedChange = { newValue ->
                                    if (!newValue && metricStates.count { it } <= 1) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.system_meter_need_metric),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        metricStates[index] = newValue
                                        FpsOverlayManager.setMetricEnabled(context, metric, newValue)
                                        if (meterEnabled) FpsOverlayService.start(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
