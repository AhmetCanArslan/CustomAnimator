package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.service.PowerButtonService
import com.arslan.customanimator.ui.components.SliderRow
import com.arslan.customanimator.utils.ButtonAction
import com.arslan.customanimator.utils.ButtonActions
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonRemapperManager.PowerLongPress
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.LocalWriteSecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.AppCard

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
    var pickerStep by remember { mutableStateOf(ButtonActionPickerStep.HIDDEN) }
    var pickerTarget by remember { mutableStateOf(PressTarget.LONG_PRESS) }
    val customAction = powerLongPress.action
    val customLabel by produceState<String?>(null, customAction) {
        value = customAction?.let { withContext(Dispatchers.IO) { ButtonActions.label(context, it) } }
    }
    var doublePressAction by remember { mutableStateOf(ButtonRemapperManager.getDoublePressAction(context)) }
    val doublePressLabel by produceState<String?>(null, doublePressAction) {
        value = doublePressAction?.let { withContext(Dispatchers.IO) { ButtonActions.label(context, it) } }
    }
    var volumeComboAction by remember { mutableStateOf(ButtonRemapperManager.getVolumeComboAction(context)) }
    val volumeComboLabel by produceState<String?>(null, volumeComboAction) {
        value = volumeComboAction?.let { withContext(Dispatchers.IO) { ButtonActions.label(context, it) } }
    }
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
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun applyServiceAction(
        action: ButtonAction?,
        current: ButtonAction?,
        update: (ButtonAction?) -> Unit,
        store: (ButtonAction?) -> Unit
    ) {
        if (action == current) return
        update(action)
        if (action != null) requestNotificationPermission()
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                store(action)
                PowerButtonService.sync(context)
            }
            maybeShowInterstitial(context)
        }
    }

    fun applyDoublePress(action: ButtonAction?) =
        applyServiceAction(action, doublePressAction, { doublePressAction = it }) {
            ButtonRemapperManager.setDoublePressAction(context, it)
        }

    fun applyVolumeCombo(action: ButtonAction?) =
        applyServiceAction(action, volumeComboAction, { volumeComboAction = it }) {
            ButtonRemapperManager.setVolumeComboAction(context, it)
        }

    ButtonActionPicker(
        step = pickerStep,
        current = when (pickerTarget) {
            PressTarget.LONG_PRESS -> customAction
            PressTarget.DOUBLE_PRESS -> doublePressAction
            PressTarget.VOLUME_COMBO -> volumeComboAction
        },
        onStepChange = { pickerStep = it },
        onPicked = { action ->
            pickerStep = ButtonActionPickerStep.HIDDEN
            when (pickerTarget) {
                PressTarget.LONG_PRESS -> applyChoice(LongPressChoice(PowerLongPress.ASSISTANT, action)) {
                    ButtonRemapperManager.setCustomLongPress(context, contentResolver, action)
                }
                PressTarget.DOUBLE_PRESS -> applyDoublePress(action)
                PressTarget.VOLUME_COMBO -> applyVolumeCombo(action)
            }
        }
    )

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            item {
                DevSectionTitle(stringResource(R.string.button_remapper_power_long_press))
            }

            item {
                PowerLongPressCard(
                    choice = powerLongPress,
                    deviceDefault = defaultPowerLongPress,
                    customLabel = customLabel,
                    enabled = actionsEnabled,
                    customEnabled = hasShizukuPermission,
                    onSelected = { behavior ->
                        applyChoice(LongPressChoice(behavior, null)) {
                            ButtonRemapperManager.setPowerLongPress(context, contentResolver, behavior)
                        }
                    },
                    onCustomClick = {
                        pickerTarget = PressTarget.LONG_PRESS
                        pickerStep = ButtonActionPickerStep.ACTIONS
                    }
                )
            }

            item {
                DevSectionTitle(stringResource(R.string.button_remapper_power_double_press))
            }

            item {
                PowerDoublePressCard(
                    action = doublePressAction,
                    actionLabel = doublePressLabel,
                    windowMs = doublePressWindowMs,
                    enabled = hasShizukuPermission,
                    onDefault = { applyDoublePress(null) },
                    onWindowChange = { windowMs ->
                        doublePressWindowMs = windowMs
                        coroutineScope.launch(Dispatchers.IO) {
                            ButtonRemapperManager.setDoublePressWindowMs(context, windowMs)
                            PowerButtonService.sync(context)
                        }
                    },
                    onCustomClick = {
                        pickerTarget = PressTarget.DOUBLE_PRESS
                        pickerStep = ButtonActionPickerStep.ACTIONS
                    }
                )
            }

            item {
                DevSectionTitle(stringResource(R.string.button_remapper_volume_combo))
            }

            item {
                ServiceActionCard(
                    action = volumeComboAction,
                    actionLabel = volumeComboLabel,
                    selectedRes = R.string.button_remapper_volume_combo_selected,
                    descriptionRes = R.string.button_remapper_volume_combo_desc,
                    enabled = hasShizukuPermission,
                    onDefault = { applyVolumeCombo(null) },
                    onCustomClick = {
                        pickerTarget = PressTarget.VOLUME_COMBO
                        pickerStep = ButtonActionPickerStep.ACTIONS
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PowerLongPressCard(
    choice: LongPressChoice,
    deviceDefault: PowerLongPress?,
    customLabel: String?,
    enabled: Boolean,
    customEnabled: Boolean,
    onSelected: (PowerLongPress?) -> Unit,
    onCustomClick: () -> Unit
) {
    val customSelected = choice.action != null
    RemapOptionsCard {
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
            description = if (customSelected && customLabel != null) {
                stringResource(R.string.button_remapper_custom_selected, customLabel)
            } else {
                stringResource(R.string.button_remapper_custom_desc)
            },
            enabled = customEnabled,
            onClick = onCustomClick
        )
    }
}

@Composable
private fun PowerDoublePressCard(
    action: ButtonAction?,
    actionLabel: String?,
    windowMs: Int,
    enabled: Boolean,
    onDefault: () -> Unit,
    onWindowChange: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    val resources = LocalResources.current
    ServiceActionCard(
        action = action,
        actionLabel = actionLabel,
        selectedRes = R.string.button_remapper_double_press_selected,
        descriptionRes = R.string.button_remapper_double_press_desc,
        enabled = enabled,
        onDefault = onDefault,
        onCustomClick = onCustomClick
    ) {
        if (action != null) {
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
    }
}

@Composable
private fun ServiceActionCard(
    action: ButtonAction?,
    actionLabel: String?,
    selectedRes: Int,
    descriptionRes: Int,
    enabled: Boolean,
    onDefault: () -> Unit,
    onCustomClick: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    RemapOptionsCard {
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
            description = if (action != null && actionLabel != null) {
                stringResource(selectedRes, actionLabel)
            } else {
                stringResource(descriptionRes)
            },
            enabled = enabled,
            onClick = onCustomClick
        )
        extra()
    }
}

@Composable
private fun RemapOptionsCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(contentPadding = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), content = content)
    }
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

@Composable
internal fun UnsupportedCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private const val DOUBLE_PRESS_WINDOW_STEPS =
    (ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MAX_MS - ButtonRemapperManager.DOUBLE_PRESS_WINDOW_MIN_MS) /
        ButtonRemapperManager.DOUBLE_PRESS_WINDOW_STEP_MS - 1

private enum class PressTarget { LONG_PRESS, DOUBLE_PRESS, VOLUME_COMBO }

private data class LongPressChoice(val behavior: PowerLongPress?, val action: ButtonAction?)

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
