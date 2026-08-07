package com.arslan.customanimator

import androidx.compose.foundation.BorderStroke
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.BatteryTweak
import com.arslan.customanimator.utils.BatteryTweaksManager
import com.arslan.customanimator.utils.ShizukuHelper
import kotlin.math.roundToInt

@Composable
fun BatteryScreenContent(
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val resolver = context.contentResolver
    val mgr = BatteryTweaksManager

    val canWrite = hasShizukuPermission ||
        ShizukuHelper.hasWriteSecureSettingsPermission(context)

    var refreshToken by remember { mutableIntStateOf(0) }

    val lowPower = remember(refreshToken) { mgr.getGlobalInt(resolver, mgr.KEY_LOW_POWER, 0) == 1 }
    val triggerLevel = remember(refreshToken) { mgr.getGlobalInt(resolver, mgr.KEY_LOW_POWER_TRIGGER, 0) }
    val sticky = remember(refreshToken) { mgr.getGlobalInt(resolver, mgr.KEY_LOW_POWER_STICKY, 0) == 1 }
    val stickyAuto = remember(refreshToken) {
        mgr.getGlobalInt(resolver, mgr.KEY_STICKY_AUTO_DISABLE_ENABLED, 1) == 1
    }
    val stickyLevel = remember(refreshToken) {
        mgr.getGlobalInt(resolver, mgr.KEY_STICKY_AUTO_DISABLE_LEVEL, 90)
    }
    val autoMode = remember(refreshToken) {
        mgr.getGlobalInt(resolver, mgr.KEY_AUTOMATIC_POWER_SAVE_MODE, 0)
    }
    val appStandby = remember(refreshToken) { mgr.getGlobalInt(resolver, mgr.KEY_APP_STANDBY, 1) == 1 }
    val adaptiveBattery = remember(refreshToken) {
        mgr.getGlobalInt(resolver, mgr.KEY_ADAPTIVE_BATTERY, 1) == 1
    }
    val autoRestriction = remember(refreshToken) {
        mgr.getGlobalInt(resolver, mgr.KEY_APP_AUTO_RESTRICTION, 1) == 1
    }
    val freezer = remember(refreshToken) {
        mgr.getGlobalString(resolver, mgr.KEY_CACHED_APPS_FREEZER) != "disabled"
    }
    val bleScan = remember(refreshToken) { mgr.getGlobalInt(resolver, mgr.KEY_BLE_SCAN_ALWAYS, 0) == 1 }
    val adaptiveCharging = remember(refreshToken) {
        mgr.getSecureInt(resolver, mgr.KEY_ADAPTIVE_CHARGING, 0) == 1
    }
    val chargingSounds = remember(refreshToken) {
        mgr.getSecureInt(resolver, mgr.KEY_CHARGING_SOUNDS, 1) == 1
    }
    val chargingVibration = remember(refreshToken) {
        mgr.getSecureInt(resolver, mgr.KEY_CHARGING_VIBRATION, 1) == 1
    }

    val saverRaw = remember(refreshToken) {
        mgr.getGlobalString(resolver, mgr.KEY_BATTERY_SAVER_CONSTANTS)
    }
    val dozeRaw = remember(refreshToken) {
        mgr.getGlobalString(resolver, mgr.KEY_DEVICE_IDLE_CONSTANTS)
    }
    val dozeValues = remember(dozeRaw) { mgr.parseConstants(dozeRaw) }
    val appliedSaver = remember(refreshToken) { mgr.getAppliedPreset(context, mgr.GROUP_SAVER) }
    val appliedDoze = remember(refreshToken) { mgr.getAppliedPreset(context, mgr.GROUP_DOZE) }

    val policyValues = remember(refreshToken) {
        val current = mgr.parseConstants(saverRaw)
        val merged = mutableMapOf<String, String>()
        mgr.policyTweaks.forEach { tweak ->
            merged[tweak.key] = current[tweak.key] ?: when (tweak) {
                is BatteryTweak.Toggle -> tweak.default.toString()
                is BatteryTweak.FloatRange -> tweak.default.toString()
                is BatteryTweak.Choice -> tweak.default.toString()
                is BatteryTweak.IntRange -> tweak.default.toString()
            }
        }
        merged
    }

    var advancedOpen by remember { mutableStateOf(false) }

    val policySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun afterWrite(success: Boolean) {
        refreshToken++
        if (success) maybeShowInterstitial(context)
    }

    fun writeGlobal(key: String, value: String) {
        afterWrite(mgr.putGlobal(context, resolver, key, value))
    }

    fun writeSecure(key: String, value: String) {
        afterWrite(mgr.putSecure(context, resolver, key, value))
    }

    fun writePolicy(key: String, value: String) {
        val next = policyValues.toMutableMap()
        next[key] = value
        val success = mgr.putGlobal(
            context, resolver, mgr.KEY_BATTERY_SAVER_CONSTANTS, mgr.serialiseConstants(next)
        )
        mgr.setAppliedPreset(context, mgr.GROUP_SAVER, "")
        afterWrite(success)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!canWrite) {
            item {
                SetupNudgeCard(
                    message = stringResource(R.string.bt_needs_permission),
                    onOpenSetup = openSetup
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.bt_info_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.bt_info_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (hasShizukuPermission) {
                            stringResource(R.string.bt_info_shizuku_active)
                        } else {
                            stringResource(R.string.bt_info_shizuku_missing)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasShizukuPermission) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        item { BatterySectionTitle(stringResource(R.string.bt_section_saver)) }
        item {
            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()) {
                Column {
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_low_power),
                        description = stringResource(R.string.bt_low_power_desc),
                        checked = lowPower,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_LOW_POWER, if (it) "1" else "0") }

                    HorizontalDivider()
                    BatterySliderRow(
                        title = stringResource(R.string.bt_trigger_level),
                        description = stringResource(R.string.bt_trigger_level_desc),
                        value = triggerLevel.toFloat(),
                        min = 0f,
                        max = 75f,
                        steps = 0,
                        enabled = canWrite,
                        display = { "${it.roundToInt()}%" }
                    ) { writeGlobal(mgr.KEY_LOW_POWER_TRIGGER, it.roundToInt().toString()) }

                    HorizontalDivider()
                    BatteryChoiceRow(
                        title = stringResource(R.string.bt_auto_mode),
                        description = stringResource(R.string.bt_auto_mode_desc),
                        selected = autoMode,
                        options = listOf(
                            stringResource(R.string.bt_auto_mode_percentage),
                            stringResource(R.string.bt_auto_mode_routine)
                        ),
                        enabled = canWrite
                    ) { mode ->
                        if (hasShizukuPermission) mgr.setAdaptivePowerSaver(mode == 1)
                        writeGlobal(mgr.KEY_AUTOMATIC_POWER_SAVE_MODE, mode.toString())
                    }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_sticky),
                        description = stringResource(R.string.bt_sticky_desc),
                        checked = sticky,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_LOW_POWER_STICKY, if (it) "1" else "0") }

                    if (sticky) {
                        HorizontalDivider()
                        BatteryToggleRow(
                            title = stringResource(R.string.bt_sticky_auto_disable),
                            description = stringResource(R.string.bt_sticky_auto_disable_desc),
                            checked = stickyAuto,
                            enabled = canWrite
                        ) { writeGlobal(mgr.KEY_STICKY_AUTO_DISABLE_ENABLED, if (it) "1" else "0") }
                    }

                    if (sticky && stickyAuto) {
                        HorizontalDivider()
                        BatterySliderRow(
                            title = stringResource(R.string.bt_sticky_level),
                            description = stringResource(R.string.bt_sticky_level_desc),
                            value = stickyLevel.toFloat(),
                            min = 10f,
                            max = 100f,
                            steps = 0,
                            enabled = canWrite,
                            display = { "${it.roundToInt()}%" }
                        ) { writeGlobal(mgr.KEY_STICKY_AUTO_DISABLE_LEVEL, it.roundToInt().toString()) }
                    }
                }
            }
        }

        item { BatterySectionTitle(stringResource(R.string.bt_section_presets)) }

        if (!policySupported) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.bt_requires_android10, Build.VERSION.RELEASE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (policySupported) items(mgr.saverPresets.size) { index ->
            val preset = mgr.saverPresets[index]
            val matches = if (saverRaw.isNotBlank()) {
                preset.constants.isNotEmpty() &&
                    preset.constants.all { policyValues[it.key] == it.value }
            } else {
                if (preset.constants.isEmpty()) appliedSaver == null || appliedSaver == preset.id
                else appliedSaver == preset.id
            }
            PresetRow(
                title = stringResource(preset.titleRes),
                description = stringResource(preset.descriptionRes),
                selected = matches,
                enabled = canWrite
            ) {
                val success = if (preset.constants.isEmpty()) {
                    mgr.clearGlobal(context, resolver, mgr.KEY_BATTERY_SAVER_CONSTANTS)
                } else {
                    val next = policyValues.toMutableMap()
                    next.putAll(preset.constants)
                    mgr.putGlobal(
                        context, resolver, mgr.KEY_BATTERY_SAVER_CONSTANTS,
                        mgr.serialiseConstants(next)
                    )
                }
                mgr.setAppliedPreset(context, mgr.GROUP_SAVER, preset.id)
                afterWrite(success)
            }
        }

        if (policySupported) item {
            Text(
                text = stringResource(R.string.bt_policy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (policySupported) item {
            ExpandHeader(
                title = stringResource(R.string.bt_show_advanced),
                expanded = advancedOpen
            ) { advancedOpen = !advancedOpen }
        }

        if (policySupported && advancedOpen) {
            items(mgr.policyTweaks.size) { index ->
                val tweak = mgr.policyTweaks[index]
                val raw = policyValues[tweak.key] ?: ""
                Card(
                    shape = AppShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    when (tweak) {
                        is BatteryTweak.Toggle -> BatteryToggleRow(
                            title = stringResource(tweak.titleRes),
                            description = stringResource(tweak.descriptionRes),
                            checked = raw.toBooleanStrictOrNull() ?: tweak.default,
                            enabled = canWrite
                        ) { writePolicy(tweak.key, it.toString()) }

                        is BatteryTweak.FloatRange -> BatterySliderRow(
                            title = stringResource(tweak.titleRes),
                            description = stringResource(tweak.descriptionRes),
                            value = raw.toFloatOrNull() ?: tweak.default,
                            min = tweak.min,
                            max = tweak.max,
                            steps = 8,
                            enabled = canWrite,
                            display = { String.format(java.util.Locale.US, "%.2f", it) }
                        ) {
                            writePolicy(tweak.key, String.format(java.util.Locale.US, "%.2f", it))
                        }

                        is BatteryTweak.Choice -> BatteryChoiceRow(
                            title = stringResource(tweak.titleRes),
                            description = stringResource(tweak.descriptionRes),
                            selected = raw.toIntOrNull() ?: tweak.default,
                            options = tweak.optionLabels.map { stringResource(it) },
                            enabled = canWrite
                        ) { writePolicy(tweak.key, it.toString()) }

                        is BatteryTweak.IntRange -> BatterySliderRow(
                            title = stringResource(tweak.titleRes),
                            description = stringResource(tweak.descriptionRes),
                            value = (raw.toIntOrNull() ?: tweak.default).toFloat(),
                            min = tweak.min.toFloat(),
                            max = tweak.max.toFloat(),
                            steps = 0,
                            enabled = canWrite,
                            display = { it.roundToInt().toString() }
                        ) { writePolicy(tweak.key, it.roundToInt().toString()) }
                    }
                }
            }
        }

        item { BatterySectionTitle(stringResource(R.string.bt_section_doze)) }
        items(mgr.dozePresets.size) { index ->
            val preset = mgr.dozePresets[index]
            val matches = if (dozeRaw.isNotBlank()) {
                preset.constants.isNotEmpty() &&
                    preset.constants.all { dozeValues[it.key] == it.value }
            } else {
                if (preset.constants.isEmpty()) appliedDoze == null || appliedDoze == preset.id
                else appliedDoze == preset.id
            }
            PresetRow(
                title = stringResource(preset.titleRes),
                description = stringResource(preset.descriptionRes),
                selected = matches,
                enabled = canWrite
            ) {
                val success = if (preset.constants.isEmpty()) {
                    mgr.clearGlobal(context, resolver, mgr.KEY_DEVICE_IDLE_CONSTANTS)
                } else {
                    mgr.putGlobal(
                        context, resolver, mgr.KEY_DEVICE_IDLE_CONSTANTS,
                        mgr.serialiseConstants(preset.constants)
                    )
                }
                mgr.setAppliedPreset(context, mgr.GROUP_DOZE, preset.id)
                afterWrite(success)
            }
        }

        item {
            Text(
                text = stringResource(R.string.bt_doze_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        item { BatterySectionTitle(stringResource(R.string.bt_section_background)) }
        item {
            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()) {
                Column {
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_app_standby),
                        description = stringResource(R.string.bt_app_standby_desc),
                        checked = appStandby,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_APP_STANDBY, if (it) "1" else "0") }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_adaptive_battery),
                        description = stringResource(R.string.bt_adaptive_battery_desc),
                        checked = adaptiveBattery,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_ADAPTIVE_BATTERY, if (it) "1" else "0") }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_app_auto_restriction),
                        description = stringResource(R.string.bt_app_auto_restriction_desc),
                        checked = autoRestriction,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_APP_AUTO_RESTRICTION, if (it) "1" else "0") }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_cached_freezer),
                        description = stringResource(R.string.bt_cached_freezer_desc),
                        checked = freezer,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_CACHED_APPS_FREEZER, if (it) "enabled" else "disabled") }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_ble_scan),
                        description = stringResource(R.string.bt_ble_scan_desc),
                        checked = bleScan,
                        enabled = canWrite
                    ) { writeGlobal(mgr.KEY_BLE_SCAN_ALWAYS, if (it) "1" else "0") }
                }
            }
        }

        item { BatterySectionTitle(stringResource(R.string.bt_section_charging)) }
        item {
            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()) {
                Column {
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_adaptive_charging),
                        description = stringResource(R.string.bt_adaptive_charging_desc),
                        checked = adaptiveCharging,
                        enabled = canWrite
                    ) {
                        writeSecure(mgr.KEY_ADAPTIVE_CHARGING, if (it) "1" else "0")
                    }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_charging_sounds),
                        description = stringResource(R.string.bt_charging_sounds_desc),
                        checked = chargingSounds,
                        enabled = canWrite
                    ) {
                        writeSecure(mgr.KEY_CHARGING_SOUNDS, if (it) "1" else "0")
                    }

                    HorizontalDivider()
                    BatteryToggleRow(
                        title = stringResource(R.string.bt_charging_vibration),
                        description = stringResource(R.string.bt_charging_vibration_desc),
                        checked = chargingVibration,
                        enabled = canWrite
                    ) {
                        writeSecure(mgr.KEY_CHARGING_VIBRATION, if (it) "1" else "0")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    var success = mgr.clearGlobal(context, resolver, mgr.KEY_BATTERY_SAVER_CONSTANTS)
                    success = mgr.clearGlobal(context, resolver, mgr.KEY_DEVICE_IDLE_CONSTANTS) || success
                    success = mgr.putGlobal(context, resolver, mgr.KEY_LOW_POWER, "0") || success
                    success = mgr.putGlobal(context, resolver, mgr.KEY_LOW_POWER_TRIGGER, "0") || success
                    success = mgr.putGlobal(context, resolver, mgr.KEY_APP_STANDBY, "1") || success
                    mgr.setAppliedPreset(context, mgr.GROUP_SAVER, "default")
                    mgr.setAppliedPreset(context, mgr.GROUP_DOZE, "default")
                    afterWrite(success)
                },
                enabled = canWrite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bt_reset_all))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BatterySectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun BatteryToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun BatterySliderRow(
    title: String,
    description: String,
    value: Float,
    min: Float,
    max: Float,
    steps: Int,
    enabled: Boolean,
    display: (Float) -> String,
    onChange: (Float) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value.coerceIn(min, max)) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Text(text = display(local), style = MaterialTheme.typography.labelLarge,)
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = min..max,
            steps = steps,
            enabled = enabled
        )
    }
}

@Composable
private fun BatteryChoiceRow(
    title: String,
    description: String,
    selected: Int,
    options: List<String>,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall,)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = options.getOrElse(selected) { options.firstOrNull() ?: "" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = {
                        if (index == selected) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
        )
    }
}
