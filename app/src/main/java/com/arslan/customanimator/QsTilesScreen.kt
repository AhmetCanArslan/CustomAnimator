package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.ToggleTile
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.SettingRow
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.components.TileIconPickerDialog
import com.arslan.customanimator.utils.BuiltInTiles
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.ToggleTileManager
import com.arslan.customanimator.utils.ToggleTilePresets
import com.arslan.customanimator.utils.ToggleTileSlots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QsTilesScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { ToggleTileManager(context) }

    var tiles by remember { mutableStateOf(manager.getTiles()) }
    var editingTile by remember { mutableStateOf<ToggleTile?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val reload: () -> Unit = { tiles = manager.getTiles() }

    val activate: (ToggleTilePresets.Preset) -> Unit = { preset ->
        val added = manager.addTile(
            presetKey = preset.key,
            label = context.getString(preset.nameRes),
            onCommand = preset.onCommand,
            offCommand = preset.offCommand,
            readCommand = preset.readCommand,
            onValue = preset.onValue,
            iconKey = preset.iconKey,
            collapsePanel = true,
            showToast = true
        )
        if (added == null) {
            Toast.makeText(context, R.string.qs_tiles_no_free_slot, Toast.LENGTH_SHORT).show()
        } else {
            reload()
            if (ToggleTileSlots.canRequestAdd()) {
                ToggleTileSlots.requestAddTile(context, added.slot, added.label, added.iconKey)
            }
            maybeShowInterstitial(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.qs_tiles),
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
            item {
                AppCard {
                    Text(
                        text = stringResource(R.string.qs_tiles_intro_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.qs_tiles_intro_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    StatusPill(
                        text = stringResource(
                            R.string.qs_tiles_slots_used,
                            tiles.size,
                            ToggleTileManager.MAX_TILE_SLOTS
                        ),
                        tone = if (tiles.size < ToggleTileManager.MAX_TILE_SLOTS) {
                            StatusTone.INFO
                        } else {
                            StatusTone.WARNING
                        }
                    )
                }
            }

            if (!hasShizukuPermission) {
                item {
                    AppCard(onClick = openSetup) {
                        Text(
                            text = stringResource(R.string.qs_tiles_needs_shizuku_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.qs_tiles_needs_shizuku_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.qs_tiles_active_section),
                    subtitle = stringResource(R.string.qs_tiles_active_section_desc)
                )
            }

            if (tiles.isEmpty()) {
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.qs_tiles_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    AppCard(contentPadding = 4.dp) {
                        tiles.forEachIndexed { index, tile ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                            ActiveTileRow(
                                tile = tile,
                                onEdit = { editingTile = tile },
                                onAddToPanel = {
                                    ToggleTileSlots.requestAddTile(
                                        context,
                                        tile.slot,
                                        tile.label,
                                        tile.iconKey
                                    )
                                },
                                onRemove = {
                                    manager.removeTile(tile.id)
                                    reload()
                                }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.qs_tiles_add_custom))
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.qs_tiles_builtin_section),
                    subtitle = stringResource(R.string.qs_tiles_builtin_section_desc)
                )
            }

            item {
                AppCard(contentPadding = 4.dp) {
                    BuiltInTiles.all.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                        BuiltInTileRow(
                            entry = entry,
                            onAddToPanel = { BuiltInTiles.requestAddTile(context, entry) }
                        )
                    }
                }
            }

            if (!BuiltInTiles.canRequestAdd()) {
                item {
                    Text(
                        text = stringResource(R.string.qs_tiles_builtin_unsupported),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ToggleTilePresets.categories.forEach { category ->
                item(key = "header-${category.titleRes}") {
                    SectionHeader(title = stringResource(category.titleRes))
                }
                item(key = "cat-${category.titleRes}") {
                    AppCard(contentPadding = 4.dp) {
                        category.presets.forEachIndexed { index, preset ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                            val existing = tiles.firstOrNull { it.presetKey == preset.key }
                            PresetRow(
                                preset = preset,
                                checked = existing != null,
                                enabled = existing != null || tiles.size < ToggleTileManager.MAX_TILE_SLOTS,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        activate(preset)
                                    } else {
                                        existing?.let {
                                            manager.removeTile(it.id)
                                            reload()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    editingTile?.let { tile ->
        ToggleTileDialog(
            initial = tile,
            onDismiss = { editingTile = null },
            onConfirm = { updated ->
                manager.updateTile(updated)
                reload()
                editingTile = null
            },
            onDelete = {
                manager.removeTile(tile.id)
                reload()
                editingTile = null
            }
        )
    }

    if (showCustomDialog) {
        ToggleTileDialog(
            initial = null,
            onDismiss = { showCustomDialog = false },
            onConfirm = { draft ->
                val added = manager.addTile(
                    presetKey = ToggleTilePresets.CUSTOM_KEY,
                    label = draft.label,
                    onCommand = draft.onCommand,
                    offCommand = draft.offCommand,
                    readCommand = draft.readCommand,
                    onValue = draft.onValue,
                    iconKey = draft.iconKey,
                    collapsePanel = draft.collapsePanel,
                    showToast = draft.showToast
                )
                if (added == null) {
                    Toast.makeText(context, R.string.qs_tiles_no_free_slot, Toast.LENGTH_SHORT).show()
                } else {
                    reload()
                    if (ToggleTileSlots.canRequestAdd()) {
                        ToggleTileSlots.requestAddTile(context, added.slot, added.label, added.iconKey)
                    }
                }
                showCustomDialog = false
            },
            onDelete = null
        )
    }
}

@Composable
private fun ActiveTileRow(
    tile: ToggleTile,
    onEdit: () -> Unit,
    onAddToPanel: () -> Unit,
    onRemove: () -> Unit
) {
    SettingRow(
        title = tile.label,
        subtitle = stringResource(R.string.qs_tiles_slot_label, tile.slot + 1),
        onClick = onEdit,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ToggleTileSlots.canRequestAdd()) {
                    TextButton(onClick = onAddToPanel) {
                        Text(stringResource(R.string.qs_tiles_add_to_panel))
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.qs_tiles_remove)
                    )
                }
            }
        }
    )
}

@Composable
private fun BuiltInTileRow(
    entry: BuiltInTiles.Entry,
    onAddToPanel: () -> Unit
) {
    val canAdd = BuiltInTiles.canRequestAdd()
    SettingRow(
        title = stringResource(entry.nameRes),
        subtitle = stringResource(entry.descriptionRes),
        enabled = canAdd,
        onClick = if (canAdd) onAddToPanel else null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(entry.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canAdd) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onAddToPanel) {
                        Text(stringResource(R.string.qs_tiles_add_to_panel))
                    }
                }
            }
        }
    )
}

@Composable
private fun PresetRow(
    preset: ToggleTilePresets.Preset,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        title = stringResource(preset.nameRes),
        subtitle = stringResource(preset.descriptionRes),
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(TerminalTileIcons.resFor(preset.iconKey)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            }
        }
    )
}

@Composable
private fun ToggleTileDialog(
    initial: ToggleTile?,
    onDismiss: () -> Unit,
    onConfirm: (ToggleTile) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var onCommand by remember { mutableStateOf(initial?.onCommand ?: "") }
    var offCommand by remember { mutableStateOf(initial?.offCommand ?: "") }
    var readCommand by remember { mutableStateOf(initial?.readCommand ?: "") }
    var onValue by remember { mutableStateOf(initial?.onValue ?: "") }
    var iconKey by remember {
        mutableStateOf(TerminalTileIcons.canonicalKey(initial?.iconKey ?: TerminalTileIcons.DEFAULT_KEY))
    }
    var collapsePanel by remember { mutableStateOf(initial?.collapsePanel ?: true) }
    var showToast by remember { mutableStateOf(initial?.showToast ?: true) }
    var showIconPicker by remember { mutableStateOf(false) }

    val canSave = label.isNotBlank() && onCommand.isNotBlank() && offCommand.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.qs_tiles_dialog_new else R.string.qs_tiles_dialog_edit
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = onCommand,
                    onValueChange = { onCommand = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_on_command)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = offCommand,
                    onValueChange = { offCommand = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_off_command)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = readCommand,
                    onValueChange = { readCommand = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_read_command)) },
                    supportingText = { Text(stringResource(R.string.qs_tiles_field_read_command_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = onValue,
                    onValueChange = { onValue = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_on_value)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showIconPicker = true }) {
                        Icon(
                            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.qs_tiles_pick_icon))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.qs_tiles_collapse_panel),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = collapsePanel, onCheckedChange = { collapsePanel = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.qs_tiles_show_toast),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = showToast, onCheckedChange = { showToast = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        ToggleTile(
                            id = initial?.id ?: "",
                            presetKey = initial?.presetKey ?: ToggleTilePresets.CUSTOM_KEY,
                            label = label.trim(),
                            onCommand = onCommand.trim(),
                            offCommand = offCommand.trim(),
                            readCommand = readCommand.trim(),
                            onValue = onValue.trim(),
                            iconKey = iconKey,
                            slot = initial?.slot ?: -1,
                            collapsePanel = collapsePanel,
                            showToast = showToast
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.qs_tiles_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )

    if (showIconPicker) {
        TileIconPickerDialog(
            selectedKey = iconKey,
            onDismiss = { showIconPicker = false },
            onSelect = {
                iconKey = it
                showIconPicker = false
            }
        )
    }
}
