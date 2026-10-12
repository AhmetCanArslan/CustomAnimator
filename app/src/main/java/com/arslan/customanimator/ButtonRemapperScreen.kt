package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ButtonAction
import com.arslan.customanimator.utils.ButtonActions
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonRemapperManager.PowerLongPress
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.LocalWriteSecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val customAction = powerLongPress.action
    val customLabel by produceState<String?>(null, customAction) {
        value = customAction?.let { withContext(Dispatchers.IO) { ButtonActions.label(context, it) } }
    }
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

    ButtonActionPicker(
        step = pickerStep,
        current = customAction,
        onStepChange = { pickerStep = it },
        onPicked = { action ->
            pickerStep = ButtonActionPickerStep.HIDDEN
            applyChoice(LongPressChoice(PowerLongPress.ASSISTANT, action)) {
                ButtonRemapperManager.setCustomLongPress(context, contentResolver, action)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.button_remapper),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    onCustomClick = { pickerStep = ButtonActionPickerStep.ACTIONS }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
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
