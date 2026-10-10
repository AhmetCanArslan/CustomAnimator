package com.arslan.customanimator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ButtonAction
import com.arslan.customanimator.utils.ButtonActions
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.ToggleTilePresets

internal enum class ButtonActionPickerStep { HIDDEN, ACTIONS, APPS, COMMAND }

@Composable
internal fun ButtonActionPicker(
    step: ButtonActionPickerStep,
    current: ButtonAction?,
    onStepChange: (ButtonActionPickerStep) -> Unit,
    onPicked: (ButtonAction) -> Unit
) {
    val dismiss = { onStepChange(ButtonActionPickerStep.HIDDEN) }

    when (step) {
        ButtonActionPickerStep.HIDDEN -> Unit
        ButtonActionPickerStep.ACTIONS -> ButtonActionSheet(
            current = current,
            onStepChange = onStepChange,
            onPicked = onPicked,
            onDismiss = dismiss
        )
        ButtonActionPickerStep.APPS -> AppPickerSheet(
            onPick = { app -> onPicked(ButtonAction(ButtonAction.Type.APP, app.packageName)) },
            onDismiss = dismiss
        )
        ButtonActionPickerStep.COMMAND -> ButtonCommandDialog(
            initial = current?.takeIf { it.type == ButtonAction.Type.COMMAND }?.value.orEmpty(),
            onSave = { command -> onPicked(ButtonAction(ButtonAction.Type.COMMAND, command)) },
            onDismiss = dismiss
        )
    }
}

@Composable
private fun ButtonActionSheet(
    current: ButtonAction?,
    onStepChange: (ButtonActionPickerStep) -> Unit,
    onPicked: (ButtonAction) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(
        title = stringResource(R.string.button_remapper_custom_pick),
        subtitle = null,
        onDismiss = onDismiss
    ) {
        item {
            PickerRow(
                title = stringResource(R.string.shortcut_tile_app),
                subtitle = stringResource(R.string.shortcut_tile_app_desc),
                selected = current?.type == ButtonAction.Type.APP,
                onClick = { onStepChange(ButtonActionPickerStep.APPS) }
            ) {
                IconBadge(icon = Icons.Filled.Apps, size = 40.dp)
            }
        }
        item {
            PickerRow(
                title = stringResource(R.string.shortcut_tile_command),
                subtitle = stringResource(R.string.shortcut_tile_command_desc),
                selected = current?.type == ButtonAction.Type.COMMAND,
                onClick = { onStepChange(ButtonActionPickerStep.COMMAND) }
            ) {
                IconBadge(icon = Icons.Filled.Terminal, size = 40.dp)
            }
        }
        item { SectionHeader(title = stringResource(R.string.button_action_quick)) }
        items(ButtonActions.Quick.entries, key = { it.key }) { quick ->
            val action = ButtonAction(ButtonAction.Type.QUICK, quick.key)
            PickerRow(
                title = stringResource(quick.nameRes),
                subtitle = "",
                selected = current == action,
                onClick = { onPicked(action) }
            ) {
                IconBadge(icon = quick.icon(), size = 40.dp)
            }
        }
        ToggleTilePresets.categories.forEach { category ->
            item { SectionHeader(title = stringResource(category.titleRes)) }
            items(category.presets, key = { it.key }) { preset ->
                val action = ButtonAction(ButtonAction.Type.TOGGLE, preset.key)
                PickerRow(
                    title = stringResource(preset.nameRes),
                    subtitle = stringResource(preset.descriptionRes),
                    selected = current == action,
                    onClick = { onPicked(action) }
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(TerminalTileIcons.resFor(preset.iconKey)),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonCommandDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var command by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shortcut_tile_command)) },
        text = {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.shortcut_command_field)) },
                supportingText = { Text(stringResource(R.string.shortcut_tile_command_desc)) },
                shape = AppShapes.field
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(command.trim()) }, enabled = command.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun ButtonActions.Quick.icon(): ImageVector = when (this) {
    ButtonActions.Quick.FLASHLIGHT -> Icons.Filled.FlashlightOn
    ButtonActions.Quick.SCREENSHOT -> Icons.Filled.Screenshot
    ButtonActions.Quick.PLAY_PAUSE -> Icons.Filled.PlayArrow
    ButtonActions.Quick.NEXT_TRACK -> Icons.Filled.SkipNext
    ButtonActions.Quick.PREVIOUS_TRACK -> Icons.Filled.SkipPrevious
    ButtonActions.Quick.VOLUME_PANEL -> Icons.AutoMirrored.Filled.VolumeUp
    ButtonActions.Quick.NOTIFICATIONS -> Icons.Filled.Notifications
    ButtonActions.Quick.QUICK_SETTINGS -> Icons.Filled.Tune
    ButtonActions.Quick.RECENT_APPS -> Icons.Filled.ViewCarousel
    ButtonActions.Quick.FREE_MEMORY -> Icons.Filled.Memory
    ButtonActions.Quick.RESTART_SYSTEM_UI -> Icons.Filled.RestartAlt
}
