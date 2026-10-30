package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.service.PowerButtonService
import com.arslan.customanimator.ui.components.SliderRow
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonTrigger
import com.arslan.customanimator.utils.ButtonRemapperManager.PowerLongPress
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.LocalWriteSecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.utils.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapperScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    hasWriteSecureSettings: Boolean = LocalWriteSecureSettings.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val actionsEnabled = hasShizukuPermission || hasWriteSecureSettings

    val readLongPress = {
        LongPressChoice(
            behavior = ButtonRemapperManager.getPowerLongPress(contentResolver),
            action = ButtonRemapperManager.getCustomLongPress(context, contentResolver)
        )
    }
    var powerLongPress by remember { mutableStateOf(readLongPress()) }
    var serviceActions by remember {
        mutableStateOf(ButtonTrigger.service.associateWith { ButtonRemapperManager.getAction(context, it) })
    }
    var volumeScreenOff by remember { mutableStateOf(ButtonRemapperManager.isVolumeScreenOff(context)) }
    var pickerTrigger by remember { mutableStateOf(ButtonTrigger.POWER_LONG_PRESS) }
    var doublePressWindowMs by remember { mutableIntStateOf(ButtonRemapperManager.getDoublePressWindowMs(context)) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    val defaultPowerLongPress = remember { ButtonRemapperManager.getDefaultPowerLongPress() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                powerLongPress = readLongPress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun applyChoice(choice: LongPressChoice, write: () -> Boolean) {
        val previous = powerLongPress
        if (choice == previous) return
        powerLongPress = choice
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { write() }
            if (success) {
                maybeShowInterstitial(context)
            } else {
                powerLongPress = previous
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun syncService(write: () -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            write()
            PowerButtonService.sync(context)
        }
    }

    fun applyServiceAction(trigger: ButtonTrigger, action: ShortcutEntry?) {
        if (action == serviceActions[trigger]) return
        serviceActions = serviceActions + (trigger to action)
        if (action != null) requestNotificationPermission()
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                ButtonRemapperManager.setAction(context, trigger, action)
                PowerButtonService.sync(context)
            }
            maybeShowInterstitial(context)
        }
    }

    fun applyPickedAction(action: ShortcutEntry) {
        val trigger = pickerTrigger
        if (trigger != ButtonTrigger.POWER_LONG_PRESS) {
            applyServiceAction(trigger, action)
            return
        }
        applyChoice(LongPressChoice(PowerLongPress.ASSISTANT, action)) {
            ButtonRemapperManager.setCustomLongPress(context, contentResolver, action)
        }
    }

    val onActionPicked by rememberUpdatedState<(ShortcutEntry) -> Unit> { applyPickedAction(it) }
    val actionPicker = remember { ShortcutsState(context, coroutineScope) { onActionPicked(it) } }
    val openSource = rememberShortcutSources(actionPicker, hasShizukuPermission)
    val pickAction: (ButtonTrigger, ShortcutEntry?) -> Unit = { trigger, current ->
        pickerTrigger = trigger
        actionPicker.startActionPicker(withSources = true, selected = current)
    }

    LaunchedEffect(hasShizukuPermission) {
        actionPicker.load(hasShizukuPermission)
    }

    ShortcutStepHost(state = actionPicker, hasShizukuPermission = hasShizukuPermission, onSource = openSource)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.button_remapper),
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!ButtonRemapperManager.isSupported()) {
                item { UnsupportedCard(stringResource(R.string.button_remapper_unsupported)) }
                return@LazyColumn
            }

            if (!actionsEnabled) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
                gap(SECTION_GAP)
            }

            item {
                InfoCard(
                    dismissKey = "button_remapper_info",
                    texts = listOf(
                        stringResource(R.string.button_remapper_info),
                        stringResource(R.string.button_remapper_oem_note)
                    )
                )
            }
            gap(SECTION_GAP)

            sectionTitle(R.string.button_remapper_power_long_press)
            cardRows({
                PowerLongPressOptions(
                    choice = powerLongPress,
                    deviceDefault = defaultPowerLongPress,
                    enabled = actionsEnabled,
                    customEnabled = hasShizukuPermission,
                    onSelected = { behavior ->
                        applyChoice(LongPressChoice(behavior, null)) {
                            ButtonRemapperManager.setPowerLongPress(context, contentResolver, behavior)
                        }
                    },
                    onCustomClick = { pickAction(ButtonTrigger.POWER_LONG_PRESS, powerLongPress.action) }
                )
            })
            gap(SECTION_GAP)

            sectionTitle(R.string.button_remapper_power_double_press)
            cardRows({
                PowerDoublePressOptions(
                    action = serviceActions[ButtonTrigger.POWER_DOUBLE_PRESS],
                    enabled = hasShizukuPermission,
                    onDefault = { applyServiceAction(ButtonTrigger.POWER_DOUBLE_PRESS, null) },
                    onCustomClick = {
                        pickAction(ButtonTrigger.POWER_DOUBLE_PRESS, serviceActions[ButtonTrigger.POWER_DOUBLE_PRESS])
                    }
                )
            })
            gap(SECTION_GAP)

            sectionTitle(R.string.button_remapper_volume_buttons)
            VolumeKey.entries.forEach { key ->
                cardRows(
                    { VolumeKeyHeader(key = key, enabled = hasShizukuPermission) },
                    {
                        GestureRow(
                            title = stringResource(R.string.button_gesture_double_press),
                            hint = stringResource(R.string.button_gesture_double_hint),
                            action = serviceActions[key.doublePress],
                            enabled = hasShizukuPermission,
                            onPick = { pickAction(key.doublePress, serviceActions[key.doublePress]) },
                            onClear = { applyServiceAction(key.doublePress, null) }
                        )
                    },
                    {
                        GestureRow(
                            title = stringResource(R.string.button_gesture_long_press),
                            hint = stringResource(R.string.button_gesture_long_hint),
                            action = serviceActions[key.longPress],
                            enabled = hasShizukuPermission,
                            onPick = { pickAction(key.longPress, serviceActions[key.longPress]) },
                            onClear = { applyServiceAction(key.longPress, null) }
                        )
                    }
                )
                gap(GROUP_GAP)
            }
            cardRows({
                GestureRow(
                    title = stringResource(R.string.button_remapper_volume_combo),
                    hint = stringResource(R.string.button_gesture_combo_hint),
                    action = serviceActions[ButtonTrigger.VOLUME_COMBO],
                    enabled = hasShizukuPermission,
                    onPick = { pickAction(ButtonTrigger.VOLUME_COMBO, serviceActions[ButtonTrigger.VOLUME_COMBO]) },
                    onClear = { applyServiceAction(ButtonTrigger.VOLUME_COMBO, null) },
                    leading = { IconBadge(icon = Icons.Filled.SwapVert, size = 40.dp) }
                )
            })
            gap(SECTION_GAP)

            sectionTitle(R.string.settings)
            val screenOffRow: @Composable () -> Unit = {
                ToggleRow(
                    icon = Icons.Filled.Bedtime,
                    title = stringResource(R.string.button_remapper_volume_screen_off),
                    description = stringResource(R.string.button_remapper_volume_screen_off_desc),
                    checked = volumeScreenOff,
                    enabled = hasShizukuPermission,
                    onCheckedChange = { enabled ->
                        volumeScreenOff = enabled
                        syncService { ButtonRemapperManager.setVolumeScreenOff(context, enabled) }
                    }
                )
            }
            val windowRow: @Composable () -> Unit = {
                DoublePressWindowSlider(
                    windowMs = doublePressWindowMs,
                    enabled = hasShizukuPermission,
                    onWindowChange = { windowMs ->
                        doublePressWindowMs = windowMs
                        syncService { ButtonRemapperManager.setDoublePressWindowMs(context, windowMs) }
                    }
                )
            }
            if (DOUBLE_PRESS_TRIGGERS.any { serviceActions[it] != null }) {
                cardRows(screenOffRow, windowRow)
            } else {
                cardRows(screenOffRow)
            }
            gap(SECTION_GAP)
        }
    }
}

@Composable
private fun PowerLongPressOptions(
    choice: LongPressChoice,
    deviceDefault: PowerLongPress?,
    enabled: Boolean,
    customEnabled: Boolean,
    onSelected: (PowerLongPress?) -> Unit,
    onCustomClick: () -> Unit
) {
    val customAction = choice.action
    val customSelected = customAction != null
    RemapOptions {
        RemapOptionRow(
            selected = !customSelected && choice.behavior == null,
            label = stringResource(R.string.button_remapper_system_default),
            description = if (deviceDefault == null) {
                stringResource(R.string.button_remapper_system_default_desc)
            } else {
                stringResource(
                    R.string.button_remapper_system_default_known,
                    stringResource(deviceDefault.labelRes())
                )
            },
            enabled = enabled,
            onClick = { onSelected(null) }
        )
        PowerLongPress.entries.forEach { behavior ->
            RemapOptionRow(
                selected = !customSelected && choice.behavior == behavior,
                label = stringResource(behavior.labelRes()),
                description = stringResource(behavior.descriptionRes()),
                enabled = enabled,
                onClick = { onSelected(behavior) }
            )
        }
        RemapOptionRow(
            selected = customSelected,
            label = stringResource(R.string.button_remapper_custom),
            description = if (customAction != null) {
                stringResource(R.string.button_remapper_custom_selected, customAction.label)
            } else {
                stringResource(R.string.button_remapper_custom_desc)
            },
            enabled = customEnabled,
            onClick = onCustomClick
        )
    }
}

@Composable
private fun PowerDoublePressOptions(
    action: ShortcutEntry?,
    enabled: Boolean,
    onDefault: () -> Unit,
    onCustomClick: () -> Unit
) {
    RemapOptions {
        RemapOptionRow(
            selected = action == null,
            label = stringResource(R.string.button_remapper_system_default),
            description = stringResource(R.string.button_remapper_system_default_desc),
            enabled = enabled,
            onClick = onDefault
        )
        RemapOptionRow(
            selected = action != null,
            label = stringResource(R.string.button_remapper_custom),
            description = if (action != null) {
                stringResource(R.string.button_remapper_double_press_selected, action.label)
            } else {
                stringResource(R.string.button_remapper_double_press_desc)
            },
            enabled = enabled,
            onClick = onCustomClick
        )
    }
}

@Composable
private fun DoublePressWindowSlider(windowMs: Int, enabled: Boolean, onWindowChange: (Int) -> Unit) {
    val resources = LocalResources.current
    SliderRow(
        title = stringResource(R.string.button_remapper_double_press_window),
        description = stringResource(R.string.button_remapper_double_press_window_desc),
        value = windowMs.toFloat(),
        min = ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MIN_MS.toFloat(),
        max = ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MAX_MS.toFloat(),
        steps = DOUBLE_PRESS_WINDOW_STEPS,
        enabled = enabled,
        display = { resources.getString(R.string.button_remapper_double_press_window_value, it.roundToInt()) },
        onChange = { onWindowChange(it.roundToInt()) }
    )
}

@Composable
private fun VolumeKeyHeader(key: VolumeKey, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = key.icon, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(key.titleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.button_remapper_volume_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GestureRow(
    title: String,
    hint: String,
    action: ShortcutEntry?,
    enabled: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onPick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (action == null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ActionChip(entry = action)
            }
        }
        if (action == null) {
            FilledTonalIconButton(onClick = onPick, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.button_remapper_custom_pick))
            }
        } else {
            IconButton(onClick = onClear, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
            }
        }
    }
}

@Composable
private fun RemapOptions(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), content = content)
}

@Composable
private fun RemapOptionRow(
    selected: Boolean,
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val DOUBLE_PRESS_WINDOW_STEPS =
    (ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MAX_MS - ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MIN_MS) /
        ButtonRemapperManager.DOUBLE_PRESS_WINDOW_STEP_MS - 1

private const val DISABLED_ALPHA = 0.5f
private val SECTION_GAP = 24.dp
private val GROUP_GAP = 12.dp

private enum class VolumeKey(
    val titleRes: Int,
    val icon: ImageVector,
    val doublePress: ButtonTrigger,
    val longPress: ButtonTrigger
) {
    UP(
        R.string.button_remapper_volume_up,
        Icons.AutoMirrored.Filled.VolumeUp,
        ButtonTrigger.VOLUME_UP_DOUBLE,
        ButtonTrigger.VOLUME_UP_LONG
    ),
    DOWN(
        R.string.button_remapper_volume_down,
        Icons.AutoMirrored.Filled.VolumeDown,
        ButtonTrigger.VOLUME_DOWN_DOUBLE,
        ButtonTrigger.VOLUME_DOWN_LONG
    )
}

private fun LazyListScope.gap(height: Dp) {
    item { Spacer(modifier = Modifier.height(height)) }
}

private val DOUBLE_PRESS_TRIGGERS =
    listOf(ButtonTrigger.POWER_DOUBLE_PRESS, ButtonTrigger.VOLUME_UP_DOUBLE, ButtonTrigger.VOLUME_DOWN_DOUBLE)

private data class LongPressChoice(val behavior: PowerLongPress?, val action: ShortcutEntry?)

private fun PowerLongPress.labelRes(): Int = when (this) {
    PowerLongPress.POWER_MENU -> R.string.button_remapper_power_menu
    PowerLongPress.ASSISTANT -> R.string.button_remapper_assistant
    PowerLongPress.VOICE_ASSIST -> R.string.button_remapper_voice_assist
    PowerLongPress.NOTHING -> R.string.button_remapper_nothing
    PowerLongPress.SHUT_DOWN_CONFIRM -> R.string.button_remapper_shut_down_confirm
    PowerLongPress.SHUT_DOWN -> R.string.button_remapper_shut_down
}

private fun PowerLongPress.descriptionRes(): Int = when (this) {
    PowerLongPress.POWER_MENU -> R.string.button_remapper_power_menu_desc
    PowerLongPress.ASSISTANT -> R.string.button_remapper_assistant_desc
    PowerLongPress.VOICE_ASSIST -> R.string.button_remapper_voice_assist_desc
    PowerLongPress.NOTHING -> R.string.button_remapper_nothing_desc
    PowerLongPress.SHUT_DOWN_CONFIRM -> R.string.button_remapper_shut_down_confirm_desc
    PowerLongPress.SHUT_DOWN -> R.string.button_remapper_shut_down_desc
}
