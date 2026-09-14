package com.arslan.customanimator

import android.content.res.Resources
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.BatteryHealthManager
import com.arslan.customanimator.utils.BatteryHealthSnapshot
import com.arslan.customanimator.utils.BatteryUsageManager
import com.arslan.customanimator.utils.BatteryUsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val BATTERY_HEALTH_REFRESH_MS = 2000L
private const val BATTERY_USAGE_REFRESH_MS = 60000L

private class BatteryMetric(
    val labelRes: Int,
    val source: (BatteryHealthSnapshot) -> String?,
    val format: (Resources, String) -> String?
)

private val STATUS_LABELS = mapOf(
    2 to R.string.bh_status_charging,
    3 to R.string.bh_status_discharging,
    4 to R.string.bh_status_not_charging,
    5 to R.string.bh_status_full
)

private val HEALTH_LABELS = mapOf(
    2 to R.string.bh_health_good,
    3 to R.string.bh_health_overheat,
    4 to R.string.bh_health_dead,
    5 to R.string.bh_health_over_voltage,
    6 to R.string.bh_health_failure,
    7 to R.string.bh_health_cold
)

private val THERMAL_STATUS_LABELS = mapOf(
    0 to R.string.bh_thermal_none,
    1 to R.string.bh_thermal_light,
    2 to R.string.bh_thermal_moderate,
    3 to R.string.bh_thermal_severe,
    4 to R.string.bh_thermal_critical,
    5 to R.string.bh_thermal_emergency,
    6 to R.string.bh_thermal_shutdown
)

private val POWER_SOURCES = linkedMapOf(
    "AC powered" to R.string.bh_power_ac,
    "USB powered" to R.string.bh_power_usb,
    "Wireless powered" to R.string.bh_power_wireless,
    "Dock powered" to R.string.bh_power_dock
)

private val USAGE_LABELS = linkedMapOf(
    "Battery time remaining" to R.string.bh_time_remaining,
    "Charge time remaining" to R.string.bh_charge_time_remaining,
    "Time on battery" to R.string.bh_time_on_battery,
    "Time on battery screen off" to R.string.bh_time_screen_off,
    "Time on battery screen doze" to R.string.bh_time_screen_doze,
    "Total run time" to R.string.bh_total_run_time,
    "Discharge" to R.string.bh_discharge,
    "Screen on discharge" to R.string.bh_discharge_screen_on,
    "Screen off discharge" to R.string.bh_discharge_screen_off,
    "Screen doze discharge" to R.string.bh_discharge_screen_doze,
    "Device light doze discharge" to R.string.bh_discharge_light_doze,
    "Device deep doze discharge" to R.string.bh_discharge_deep_doze,
    BatteryUsageManager.KEY_COMPUTED_DRAIN to R.string.bh_computed_drain,
    BatteryUsageManager.KEY_ACTUAL_DRAIN to R.string.bh_actual_drain,
    "Estimated battery capacity" to R.string.bh_estimated_capacity,
    "Min learned battery capacity" to R.string.bh_min_learned_capacity,
    "Max learned battery capacity" to R.string.bh_max_learned_capacity
)

private val DOZE_LABELS = linkedMapOf(
    "mState" to R.string.bh_doze_deep_state,
    "mLightState" to R.string.bh_doze_light_state,
    "mDeepEnabled" to R.string.bh_doze_deep_enabled,
    "mLightEnabled" to R.string.bh_doze_light_enabled,
    "mForceIdle" to R.string.bh_doze_force_idle,
    "mScreenOn" to R.string.bh_doze_screen_on,
    "mCharging" to R.string.bh_doze_charging
)

private fun labelFor(labels: Map<Int, Int>, resources: Resources, raw: String): String =
    resources.getString(raw.toIntOrNull()?.let { labels[it] } ?: R.string.bh_unknown)

private fun microToMilli(raw: String): String? = raw.toLongOrNull()?.let { "${it / 1000} mA" }

private fun microAhToMilliAh(raw: String): String? = raw.toLongOrNull()?.let { "${it / 1000} mAh" }

private fun formatMah(value: Double): String = String.format(Locale.getDefault(), "%.1f mAh", value)

private val BATTERY_METRICS = listOf(
    BatteryMetric(R.string.bh_level, { it.service["level"] }) { _, raw -> "$raw%" },
    BatteryMetric(R.string.bh_status, { it.service["status"] }) { res, raw -> labelFor(STATUS_LABELS, res, raw) },
    BatteryMetric(R.string.bh_condition, { it.service["health"] }) { res, raw -> labelFor(HEALTH_LABELS, res, raw) },
    BatteryMetric(R.string.bh_power_source, { s -> POWER_SOURCES.keys.firstOrNull { s.service[it] == "true" } ?: "" }) { res, raw ->
        res.getString(POWER_SOURCES[raw] ?: R.string.bh_power_battery)
    },
    BatteryMetric(R.string.bh_temperature, { it.service["temperature"] }) { _, raw ->
        raw.toFloatOrNull()?.let { String.format(Locale.getDefault(), "%.1f °C", it / 10f) }
    },
    BatteryMetric(R.string.bh_voltage, { it.service["voltage"] }) { _, raw ->
        raw.toFloatOrNull()?.let { String.format(Locale.getDefault(), "%.3f V", it / 1000f) }
    },
    BatteryMetric(R.string.bh_current, { it.supply["CURRENT_NOW"] }) { _, raw -> microToMilli(raw) },
    BatteryMetric(R.string.bh_capacity_full, { it.supply["CHARGE_FULL"] }) { _, raw -> microAhToMilliAh(raw) },
    BatteryMetric(R.string.bh_capacity_design, { it.supply["CHARGE_FULL_DESIGN"] }) { _, raw -> microAhToMilliAh(raw) },
    BatteryMetric(R.string.bh_charge_counter, { it.service["Charge counter"] }) { _, raw -> microAhToMilliAh(raw) },
    BatteryMetric(R.string.bh_cycles, { it.supply["CYCLE_COUNT"] }) { _, raw -> raw },
    BatteryMetric(R.string.bh_max_charging_power, { s -> BatteryHealthManager.maxChargingWatts(s)?.toString() }) { _, raw ->
        raw.toDoubleOrNull()?.let { String.format(Locale.getDefault(), "%.1f W", it) }
    },
    BatteryMetric(R.string.bh_technology, { it.service["technology"] }) { _, raw -> raw },
    BatteryMetric(R.string.bh_thermal_status, { it.thermalStatus?.toString() }) { res, raw ->
        labelFor(THERMAL_STATUS_LABELS, res, raw)
    }
)

private fun overviewRows(resources: Resources, snapshot: BatteryHealthSnapshot): List<Pair<String, String>> =
    BATTERY_METRICS.mapNotNull { metric ->
        metric.source(snapshot)
            ?.let { raw -> metric.format(resources, raw) }
            ?.let { value -> resources.getString(metric.labelRes) to value }
    }

private fun labeledRows(resources: Resources, labels: Map<String, Int>, values: Map<String, String>): List<Pair<String, String>> =
    labels.mapNotNull { (key, res) -> values[key]?.let { resources.getString(res) to it } }

private fun prettifyKey(key: String): String =
    key.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHealthScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    var snapshot by remember { mutableStateOf<BatteryHealthSnapshot?>(null) }
    var usage by remember { mutableStateOf<BatteryUsageSnapshot?>(null) }

    LaunchedEffect(hasShizukuPermission) {
        if (!hasShizukuPermission) return@LaunchedEffect
        launch {
            while (true) {
                snapshot = withContext(Dispatchers.IO) { BatteryHealthManager.readSnapshot() }
                delay(BATTERY_HEALTH_REFRESH_MS)
            }
        }
        while (true) {
            usage = withContext(Dispatchers.IO) { BatteryUsageManager.readUsage(context) }
            delay(BATTERY_USAGE_REFRESH_MS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.bh_title),
                        style = MaterialTheme.typography.headlineSmall,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.bh_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            val current = snapshot
            if (hasShizukuPermission && current == null) {
                item { BatteryLoadingIndicator() }
            }

            if (current != null && current.isEmpty) {
                item {
                    Text(
                        text = stringResource(R.string.bh_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (current != null && !current.isEmpty) {
                item { BatteryHealthHero(current) }
                metricsItem(R.string.bh_section_overview) { overviewRows(resources, current) }
            }

            usage?.let { batteryUsageItems(resources, it) }

            if (current != null && !current.isEmpty) {
                batteryDetailItems(resources, current)
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun LazyListScope.metricsItem(titleRes: Int, rows: () -> List<Pair<String, String>>) {
    val resolved = rows()
    if (resolved.isEmpty()) return
    item { BatteryMetricsCard(title = stringResource(titleRes), rows = resolved) }
}

private fun LazyListScope.batteryUsageItems(resources: Resources, usage: BatteryUsageSnapshot) {
    metricsItem(R.string.bh_section_usage) { labeledRows(resources, USAGE_LABELS, usage.summary) }
    metricsItem(R.string.bh_section_components) { usage.components.map { (name, value) -> prettifyKey(name) to formatMah(value) } }
    metricsItem(R.string.bh_section_apps) { usage.apps.map { (name, value) -> name to formatMah(value) } }
    metricsItem(R.string.bh_section_app_wakelocks) { usage.appWakelocks }
    metricsItem(R.string.bh_section_kernel_wakelocks) { usage.kernelWakelocks }
}

private fun LazyListScope.batteryDetailItems(resources: Resources, snapshot: BatteryHealthSnapshot) {
    metricsItem(R.string.bh_section_thermal) {
        snapshot.temperatures.map { (name, value) -> name to String.format(Locale.getDefault(), "%.1f °C", value) }
    }
    metricsItem(R.string.bh_section_doze) { labeledRows(resources, DOZE_LABELS, snapshot.doze) }
    metricsItem(R.string.bh_section_health_hal) { snapshot.healthHal.toList() }
    metricsItem(R.string.bh_section_service) { snapshot.service.toList() }
    snapshot.supplies.forEach { (name, values) ->
        item {
            BatteryMetricsCard(
                title = stringResource(R.string.bh_section_supply_named, name),
                rows = values.map { (key, value) -> prettifyKey(key) to value }
            )
        }
    }
}

@Composable
private fun BatteryLoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BatteryHealthHero(snapshot: BatteryHealthSnapshot) {
    val percent = BatteryHealthManager.healthPercent(snapshot)
    val full = BatteryHealthManager.fullCapacityMicroAh(snapshot)
    val design = BatteryHealthManager.designCapacityMicroAh(snapshot)
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.bh_health_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = percent?.let { "$it%" } ?: "—",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (full != null && design != null) {
                    stringResource(R.string.bh_health_detail, full / 1000, design / 1000)
                } else {
                    stringResource(R.string.bh_health_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BatteryMetricsCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            rows.forEach { (label, value) -> BatteryMetricRow(label, value) }
        }
    }
}

@Composable
private fun BatteryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
