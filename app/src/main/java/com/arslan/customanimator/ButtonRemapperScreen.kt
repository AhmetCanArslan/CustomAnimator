package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.service.PowerButtonService
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.SliderRow
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonRemapperManager.PowerLongPress
import com.arslan.customanimator.utils.ButtonTrigger
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.LocalWriteSecureSettings
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapperScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    hasWriteSecureSettings: Boolean = LocalWriteSecureSettings.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
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
    var pickerTrigger by remember { mutableStateOf(ButtonTrigger.POWER_LONG_PRESS) }
    var doublePressWindowMs by remember { mutableIntStateOf(ButtonRemapperManager.getDoublePressWindowMs(context)) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    var showLongPressSheet by remember { mutableStateOf(false) }
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

    if (showLongPressSheet) {
        LongPressSheet(
            choice = powerLongPress,
            deviceDefault = defaultPowerLongPress,
            enabled = actionsEnabled,
            customEnabled = hasShizukuPermission,
            onSelected = { behavior ->
                showLongPressSheet = false
                applyChoice(LongPressChoice(behavior, null)) {
                    ButtonRemapperManager.setPowerLongPress(context, contentResolver, behavior)
                }
            },
            onCustom = {
                showLongPressSheet = false
                pickAction(ButtonTrigger.POWER_LONG_PRESS, powerLongPress.action)
            },
            onDismiss = { showLongPressSheet = false }
        )
    }

    val assigned: (ButtonTrigger) -> Boolean = { trigger ->
        if (trigger == ButtonTrigger.POWER_LONG_PRESS) {
            powerLongPress.behavior != null || powerLongPress.action != null
        } else {
            serviceActions[trigger] != null
        }
    }

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

            RemapKey.entries.forEach { key ->
                val header: List<@Composable () -> Unit> = key.header?.let { header ->
                    listOf<@Composable () -> Unit>({ KeyHeader(icon = header.icon, title = stringResource(header.labelRes)) })
                }.orEmpty()
                val rows = key.gestures.map { slot ->
                    @Composable {
                        val trigger = slot.trigger
                        val isLongPress = trigger == ButtonTrigger.POWER_LONG_PRESS
                        GestureRow(
                            gesture = slot.gesture,
                            assigned = assigned(trigger),
                            enabled = if (isLongPress) actionsEnabled else hasShizukuPermission,
                            onClick = {
                                if (isLongPress) {
                                    showLongPressSheet = true
                                } else {
                                    pickAction(trigger, serviceActions[trigger])
                                }
                            },
                            onClear = {
                                if (isLongPress) {
                                    applyChoice(LongPressChoice(null, null)) {
                                        ButtonRemapperManager.setPowerLongPress(context, contentResolver, null)
                                    }
                                } else {
                                    applyServiceAction(trigger, null)
                                }
                            }
                        ) {
                            if (isLongPress) LongPressValue(powerLongPress) else ActionValue(serviceActions[trigger])
                        }
                    }
                }
                cardRows(*(header + rows).toTypedArray())
                gap(GROUP_GAP)
            }
            gap(SECTION_GAP - GROUP_GAP)

            if (MULTI_PRESS_TRIGGERS.any { serviceActions[it] != null }) {
                sectionTitle(R.string.settings)
                cardRows({
                    DoublePressWindowSlider(
                        windowMs = doublePressWindowMs,
                        enabled = hasShizukuPermission,
                        onWindowChange = { windowMs ->
                            doublePressWindowMs = windowMs
                            syncService { ButtonRemapperManager.setDoublePressWindowMs(context, windowMs) }
                        }
                    )
                })
                gap(SECTION_GAP)
            }
        }
    }
}

@Composable
private fun KeyHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 36.dp)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GestureRow(
    gesture: Gesture,
    assigned: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    value: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GestureGlyph(gesture = gesture, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(GLYPH_WIDTH))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(gesture.titleRes), style = MaterialTheme.typography.titleSmall)
            value()
        }
        if (assigned) {
            IconButton(onClick = onClear, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
            }
        } else {
            Box(modifier = Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GestureGlyph(gesture: Gesture, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier.height(MARK_SIZE),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (gesture) {
            Gesture.LONG -> Mark(width = MARK_LONG, height = MARK_SIZE, color = color)
            Gesture.COMBO -> {
                Mark(width = MARK_SIZE, height = MARK_SIZE, color = color)
                Mark(width = MARK_SIZE, height = 2.dp, color = color)
                Mark(width = MARK_SIZE, height = MARK_SIZE, color = color)
            }
            else -> repeat(gesture.presses) { Mark(width = MARK_SIZE, height = MARK_SIZE, color = color) }
        }
    }
}

@Composable
private fun Mark(width: Dp, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ActionValue(action: ShortcutEntry?) {
    if (action == null) DefaultValue() else ActionChip(entry = action)
}

@Composable
private fun LongPressValue(choice: LongPressChoice) {
    val action = choice.action
    val behavior = choice.behavior
    when {
        action != null -> ActionChip(entry = action)
        behavior != null -> LabelChip(label = stringResource(behavior.labelRes())) { modifier ->
            Icon(imageVector = behavior.icon(), contentDescription = null, modifier = modifier)
        }
        else -> DefaultValue()
    }
}

@Composable
private fun DefaultValue() {
    Text(
        text = stringResource(R.string.button_remapper_system_default),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LongPressSheet(
    choice: LongPressChoice,
    deviceDefault: PowerLongPress?,
    enabled: Boolean,
    customEnabled: Boolean,
    onSelected: (PowerLongPress?) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit
) {
    val custom = choice.action != null
    val defaultSubtitle = if (deviceDefault == null) {
        stringResource(R.string.button_remapper_system_default_desc)
    } else {
        stringResource(R.string.button_remapper_system_default_known, stringResource(deviceDefault.labelRes()))
    }
    val customSubtitle = stringResource(R.string.button_remapper_custom_desc)
    PickerSheet(
        title = stringResource(R.string.button_remapper_power_long_press),
        subtitle = null,
        onDismiss = onDismiss
    ) {
        item {
            PickerRow(
                title = stringResource(R.string.button_remapper_system_default),
                subtitle = defaultSubtitle,
                selected = !custom && choice.behavior == null,
                enabled = enabled,
                onClick = { onSelected(null) },
                leading = { IconBadge(icon = Icons.Filled.PhoneAndroid, size = 40.dp) }
            )
        }
        items(PowerLongPress.entries) { behavior ->
            PickerRow(
                title = stringResource(behavior.labelRes()),
                subtitle = "",
                selected = !custom && choice.behavior == behavior,
                enabled = enabled,
                onClick = { onSelected(behavior) },
                leading = { IconBadge(icon = behavior.icon(), size = 40.dp) }
            )
        }
        item {
            PickerRow(
                title = stringResource(R.string.button_remapper_custom),
                subtitle = customSubtitle,
                selected = custom,
                enabled = customEnabled,
                onClick = onCustom,
                leading = { IconBadge(icon = Icons.Filled.TouchApp, size = 40.dp) }
            )
        }
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

private const val DOUBLE_PRESS_WINDOW_STEPS =
    (ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MAX_MS - ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MIN_MS) /
        ButtonRemapperManager.DOUBLE_PRESS_WINDOW_STEP_MS - 1

private const val DISABLED_ALPHA = 0.5f
private val SECTION_GAP = 24.dp
private val GROUP_GAP = 12.dp
private val MARK_SIZE = 10.dp
private val GLYPH_WIDTH = 40.dp
private val MARK_LONG = 34.dp

private enum class Gesture(val titleRes: Int, val presses: Int = 0) {
    DOUBLE(R.string.button_gesture_double_press, 2),
    TRIPLE(R.string.button_gesture_triple_press, 3),
    LONG(R.string.button_gesture_long_press),
    COMBO(R.string.button_remapper_volume_combo)
}

private data class GestureSlot(val gesture: Gesture, val trigger: ButtonTrigger)

private class KeyHeaderSpec(val labelRes: Int, val icon: ImageVector)

private enum class RemapKey(val header: KeyHeaderSpec?, val gestures: List<GestureSlot>) {
    POWER(
        KeyHeaderSpec(R.string.button_remapper_key_power, Icons.Filled.PowerSettingsNew),
        listOf(
            GestureSlot(Gesture.DOUBLE, ButtonTrigger.POWER_DOUBLE_PRESS),
            GestureSlot(Gesture.TRIPLE, ButtonTrigger.POWER_TRIPLE_PRESS),
            GestureSlot(Gesture.LONG, ButtonTrigger.POWER_LONG_PRESS)
        )
    ),
    VOLUME_UP(
        KeyHeaderSpec(R.string.button_remapper_volume_up, Icons.AutoMirrored.Filled.VolumeUp),
        listOf(
            GestureSlot(Gesture.DOUBLE, ButtonTrigger.VOLUME_UP_DOUBLE),
            GestureSlot(Gesture.TRIPLE, ButtonTrigger.VOLUME_UP_TRIPLE),
            GestureSlot(Gesture.LONG, ButtonTrigger.VOLUME_UP_LONG)
        )
    ),
    VOLUME_DOWN(
        KeyHeaderSpec(R.string.button_remapper_volume_down, Icons.AutoMirrored.Filled.VolumeDown),
        listOf(
            GestureSlot(Gesture.DOUBLE, ButtonTrigger.VOLUME_DOWN_DOUBLE),
            GestureSlot(Gesture.TRIPLE, ButtonTrigger.VOLUME_DOWN_TRIPLE),
            GestureSlot(Gesture.LONG, ButtonTrigger.VOLUME_DOWN_LONG)
        )
    ),
    BOTH(
        null,
        listOf(GestureSlot(Gesture.COMBO, ButtonTrigger.VOLUME_COMBO))
    )
}

private fun LazyListScope.gap(height: Dp) {
    item { Spacer(modifier = Modifier.height(height)) }
}

private val MULTI_PRESS_TRIGGERS = ButtonTrigger.service.filter { it.presses > 0 }

private data class LongPressChoice(val behavior: PowerLongPress?, val action: ShortcutEntry?)

private fun PowerLongPress.labelRes(): Int = when (this) {
    PowerLongPress.POWER_MENU -> R.string.button_remapper_power_menu
    PowerLongPress.ASSISTANT -> R.string.button_remapper_assistant
    PowerLongPress.VOICE_ASSIST -> R.string.button_remapper_voice_assist
    PowerLongPress.NOTHING -> R.string.button_remapper_nothing
    PowerLongPress.SHUT_DOWN_CONFIRM -> R.string.button_remapper_shut_down_confirm
    PowerLongPress.SHUT_DOWN -> R.string.button_remapper_shut_down
}

private fun PowerLongPress.icon(): ImageVector = when (this) {
    PowerLongPress.POWER_MENU -> Icons.Filled.PowerSettingsNew
    PowerLongPress.ASSISTANT -> Icons.Filled.Assistant
    PowerLongPress.VOICE_ASSIST -> Icons.Filled.Mic
    PowerLongPress.NOTHING -> Icons.Filled.Block
    PowerLongPress.SHUT_DOWN_CONFIRM -> Icons.Filled.PowerOff
    PowerLongPress.SHUT_DOWN -> Icons.Filled.OfflineBolt
}
