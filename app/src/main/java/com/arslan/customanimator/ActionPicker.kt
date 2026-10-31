package com.arslan.customanimator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ButtonActions
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.ToggleTilePresets

private const val IMAGE_MIME = "image/*"
private const val ANY_MIME = "*/*"

internal enum class ShortcutTile(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val needsShizuku: Boolean = false
) {
    APP(Icons.Filled.Apps, R.string.shortcut_tile_app, R.string.shortcut_tile_app_desc),
    ACTION(Icons.Filled.Bolt, R.string.shortcut_tile_action, R.string.shortcut_tile_action_desc),
    ACTIVITY(Icons.AutoMirrored.Filled.Launch, R.string.shortcut_tile_activity, R.string.shortcut_tile_activity_desc),
    APP_SHORTCUT(
        Icons.Filled.TouchApp,
        R.string.shortcut_tile_app_shortcut,
        R.string.shortcut_tile_app_shortcut_desc
    ),
    SCREEN(Icons.Filled.SpaceDashboard, R.string.shortcut_tile_screen, R.string.shortcut_tile_screen_desc),
    FILE(Icons.AutoMirrored.Filled.InsertDriveFile, R.string.shortcut_tile_file, R.string.shortcut_tile_file_desc),
    PHOTO(Icons.Filled.Image, R.string.shortcut_tile_photo, R.string.shortcut_tile_photo_desc),
    FOLDER(Icons.Filled.Folder, R.string.shortcut_tile_folder, R.string.shortcut_tile_folder_desc),
    LINK(Icons.Filled.Link, R.string.shortcut_tile_link, R.string.shortcut_tile_link_desc),
    COMMAND(Icons.Filled.Terminal, R.string.shortcut_tile_command, R.string.shortcut_tile_command_desc, needsShizuku = true)
}

@Composable
internal fun rememberShortcutSources(state: ShortcutsState, hasShizukuPermission: Boolean): (ShortcutTile) -> Unit {
    val openSetup = LocalOpenSetupGuide.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(state::onFilePicked)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(state::onFolderPicked)
    }
    return { tile ->
        when (tile) {
            ShortcutTile.APP -> state.startAppPicker(AppPickTarget.APP)
            ShortcutTile.ACTION -> state.startActionPicker(withSources = false)
            ShortcutTile.ACTIVITY -> state.startAppPicker(AppPickTarget.ACTIVITY)
            ShortcutTile.APP_SHORTCUT -> state.startAppPicker(AppPickTarget.APP_SHORTCUT)
            ShortcutTile.SCREEN -> state.startScreenPicker()
            ShortcutTile.FILE -> filePicker.launch(arrayOf(ANY_MIME))
            ShortcutTile.PHOTO -> filePicker.launch(arrayOf(IMAGE_MIME))
            ShortcutTile.FOLDER -> folderPicker.launch(null)
            ShortcutTile.LINK -> state.startBlank(ShortcutType.LINK)
            ShortcutTile.COMMAND -> if (hasShizukuPermission) state.startBlank(ShortcutType.COMMAND) else openSetup()
        }
    }
}

@Composable
internal fun ActionPickerSheet(
    step: ShortcutStep.PickAction,
    current: ShortcutEntry?,
    hasShizukuPermission: Boolean,
    onSource: (ShortcutTile) -> Unit,
    onPick: (ShortcutEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    PickerSheet(
        title = stringResource(R.string.button_remapper_custom_pick),
        subtitle = null,
        onDismiss = onDismiss
    ) {
        if (step.withSources) sourceItems(hasShizukuPermission, onSource)
        ButtonActions.Group.entries.forEach { group ->
            item { SectionHeader(title = stringResource(group.titleRes)) }
            items(ButtonActions.Quick.entries.filter { it.group == group }, key = { "quick_" + it.key }) { quick ->
                ActionRow(
                    title = stringResource(quick.nameRes),
                    subtitle = "",
                    iconKey = quick.iconKey,
                    selected = current?.type == ShortcutType.QUICK && current.actionKey == quick.key,
                    enabled = !quick.needsShizuku || hasShizukuPermission,
                    onClick = { onPick(ButtonActions.quickEntry(context, quick)) }
                )
            }
        }
        ToggleTilePresets.categories.forEach { category ->
            item { SectionHeader(title = stringResource(category.titleRes)) }
            items(category.presets, key = { "toggle_" + it.key }) { preset ->
                ActionRow(
                    title = stringResource(preset.nameRes),
                    subtitle = stringResource(preset.descriptionRes),
                    iconKey = preset.iconKey,
                    selected = current?.type == ShortcutType.TOGGLE && current.actionKey == preset.key,
                    enabled = hasShizukuPermission,
                    onClick = { onPick(ButtonActions.toggleEntry(context, preset)) }
                )
            }
        }
    }
}

private fun LazyListScope.sourceItems(hasShizukuPermission: Boolean, onSource: (ShortcutTile) -> Unit) {
    items(ShortcutTile.entries.filter { it != ShortcutTile.ACTION }, key = { "source_" + it.name }) { tile ->
        PickerRow(
            title = stringResource(tile.titleRes),
            subtitle = stringResource(tile.descriptionRes),
            enabled = !tile.needsShizuku || hasShizukuPermission,
            onClick = { onSource(tile) }
        ) {
            IconBadge(icon = tile.icon, size = 40.dp)
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    iconKey: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    PickerRow(title = title, subtitle = subtitle, selected = selected, enabled = enabled, onClick = onClick) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun ActionChip(entry: ShortcutEntry) {
    LabelChip(label = entry.label) { modifier -> ActionIcon(entry = entry, modifier = modifier) }
}

@Composable
internal fun LabelChip(label: String, icon: @Composable (Modifier) -> Unit) {
    Surface(
        shape = AppShapes.chip,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionIcon(entry: ShortcutEntry, modifier: Modifier) {
    val packageName = entry.packageName
    if (packageName != null) {
        AppIcon(packageName = packageName, modifier = modifier)
        return
    }
    Icon(
        painter = painterResource(ButtonActions.iconRes(entry) ?: ShortcutMaker.glyphRes(entry.type)),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
internal fun ActionValueDialog(
    entry: ShortcutEntry,
    onSave: (ShortcutEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val valueLabelRes = entry.valueLabelRes() ?: return
    var value by remember(entry.id) { mutableStateOf(entry.inputValue()) }
    val tile = if (entry.type == ShortcutType.COMMAND) ShortcutTile.COMMAND else ShortcutTile.LINK
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(tile.titleRes)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(valueLabelRes)) },
                supportingText = { Text(stringResource(tile.descriptionRes)) },
                shape = AppShapes.field
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(entry.withInput(value, value)) },
                enabled = value.isNotBlank(),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
        }
    )
}
