package com.arslan.customanimator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.arslan.customanimator.data.TerminalPreset
import com.arslan.customanimator.data.TerminalTileConfig
import com.arslan.customanimator.utils.CommandSuggestions
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalPresetManager
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.TerminalTileSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TerminalScreenContent(
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState(),
    command: TextFieldValue,
    onCommandChange: (TextFieldValue) -> Unit,
    history: List<String>,
    onHistoryChange: (List<String>) -> Unit,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val presetManager = remember { TerminalPresetManager(context) }

    LaunchedEffect(isActive) {
        if (!isActive) focusManager.clearFocus(force = true)
    }

    var output by rememberSaveable { mutableStateOf("") }
    var exitCode by rememberSaveable { mutableStateOf<Int?>(null) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var presets by remember { mutableStateOf(presetManager.getAllPresets()) }

    // null = dialog closed, a preset with a blank id = "add new"
    var editingPreset by remember { mutableStateOf<TerminalPreset?>(null) }

    var tilePreset by remember { mutableStateOf<TerminalPreset?>(null) }

    // Shown once; accepting is remembered so it never interrupts again.
    var showRiskDialog by remember {
        mutableStateOf(!com.arslan.customanimator.utils.SettingsManager.hasAcceptedTerminalRisk(context))
    }

    val canRun = hasShizukuPermission && !isRunning

    val run: (String) -> Unit = { raw ->
        val trimmed = raw.trim()
        if (trimmed.isNotEmpty() && canRun) {
            onHistoryChange((listOf(trimmed) + history.filterNot { it == trimmed }).take(50))
            isRunning = true
            exitCode = null
            output = "$ $trimmed"
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", trimmed))
                }
                output = if (result.output.isBlank()) {
                    "$ $trimmed"
                } else {
                    "$ $trimmed\n${result.output}"
                }
                exitCode = result.exitCode
                isRunning = false
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.terminal_disclaimer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!hasShizukuPermission) {
            item {
                WarningCard(
                    message = stringResource(R.string.terminal_needs_shizuku),
                    actionLabel = stringResource(R.string.grant_shizuku_permission),
                    onAction = { ShizukuHelper.requestShizukuPermission(context) }
                )
            }
        }

        item {
            CommandField(
                value = command,
                onValueChange = onCommandChange,
                enabled = hasShizukuPermission,
                isScreenActive = isActive,
                history = history,
                onHistorySelected = { selected ->
                    onCommandChange(
                        TextFieldValue(
                            text = selected,
                            selection = TextRange(selected.length)
                        )
                    )
                    focusManager.clearFocus()
                    run(selected)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            run(command.text)
                        },
                        enabled = canRun && command.text.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.terminal_run)
                        )
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.terminal_presets),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { editingPreset = TerminalPreset(id = "", name = "", command = command.text.trim()) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.terminal_save_current))
                }
            }
        }

        if (presets.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.terminal_no_presets),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(presets, key = { it.id }) { preset ->
                PresetCard(
                    preset = preset,
                    canRun = canRun,
                    onRun = { run(preset.command) },
                    onEdit = { editingPreset = preset },
                    onConfigureTile = { tilePreset = preset },
                    onDelete = {
                        presetManager.deletePreset(preset.id)
                        presets = presetManager.getAllPresets()
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.terminal_output),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRunning) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        exitCode?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.terminal_exit_code, it),
                                fontSize = 12.sp,
                                color = if (it == 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
                if (output.isNotEmpty()) {
                    TextButton(onClick = { output = ""; exitCode = null }) {
                        Text(stringResource(R.string.terminal_clear_output))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SelectionContainer {
                    Text(
                        text = output.ifEmpty { stringResource(R.string.terminal_output_empty) },
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    editingPreset?.let { preset ->
        PresetDialog(
            preset = preset,
            isScreenActive = isActive,
            onDismiss = { editingPreset = null },
            onConfirm = { name, cmd ->
                if (preset.id.isEmpty()) {
                    presetManager.savePreset(name, cmd)
                } else {
                    presetManager.updatePreset(preset.id, name, cmd)
                }
                presets = presetManager.getAllPresets()
                editingPreset = null
            }
        )
    }

    if (showRiskDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.risk_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.risk_dialog_terminal),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    com.arslan.customanimator.utils.SettingsManager.markTerminalRiskAccepted(context)
                    showRiskDialog = false
                }) {
                    Text(stringResource(R.string.risk_dialog_accept))
                }
            }
        )
    }

    tilePreset?.let { preset ->
        TileConfigDialog(
            preset = preset,
            freeSlot = remember(preset.id, presets) {
                presetManager.firstFreeSlot(excludingPresetId = preset.id)
            },
            onDismiss = { tilePreset = null },
            onSave = { config ->
                presetManager.setTileConfig(preset.id, config)
                presets = presetManager.getAllPresets()
                tilePreset = null
            },
            onSaveAndAdd = { config ->
                presetManager.setTileConfig(preset.id, config)
                presets = presetManager.getAllPresets()
                TerminalTileSlots.requestAddTile(
                    context = context,
                    slot = config.slot,
                    label = config.label.ifBlank { preset.name },
                    iconKey = config.iconKey
                )
                tilePreset = null
            }
        )
    }
}

@Composable
private fun CommandField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    isScreenActive: Boolean = true,
    history: List<String> = emptyList(),
    onHistorySelected: (String) -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val cursor = value.selection.end

    var installedPackages by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        installedPackages = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getInstalledPackages(0).map { it.packageName }.sorted()
            }.getOrDefault(emptyList())
        }
    }

    val suggestions = remember(value.text, cursor, installedPackages) {
        CommandSuggestions.suggest(value.text, cursor, installedPackages).take(60)
    }

    var focused by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val listState = rememberLazyListState()

    LaunchedEffect(value.text, cursor) {
        showHistory = false
    }
    LaunchedEffect(showHistory, suggestions.firstOrNull()?.token, history.firstOrNull()) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }

    val mounted = enabled && focused && isScreenActive
    val fieldWidth = with(density) { fieldSize.width.toDp() }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldSize = it }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused && !focused) showHistory = false
                    focused = it.isFocused
                },
            enabled = enabled,
            label = { Text(stringResource(R.string.terminal_command_hint)) },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            maxLines = 4,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = {
                            focusRequester.requestFocus()
                            showHistory = !showHistory
                        }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = stringResource(R.string.terminal_history)
                            )
                        }
                    }
                    if (value.text.isNotEmpty()) {
                        IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.terminal_clear_command)
                            )
                        }
                    }
                    trailingIcon?.invoke()
                }
            }
        )

        if (mounted) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, fieldSize.height),
                onDismissRequest = { },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    modifier = Modifier
                        .width(fieldWidth)
                        .heightIn(max = 360.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp
                ) {
                    LazyColumn(state = listState) {
                        if (showHistory && history.isNotEmpty()) {
                            items(history, key = { it }) { command ->
                                HistoryRow(
                                    command = command,
                                    onClick = { onHistorySelected(command) }
                                )
                            }
                        } else {
                            items(suggestions, key = { it.token }) { suggestion ->
                                SuggestionRow(
                                    suggestion = suggestion,
                                    onClick = {
                                        val (text, newCursor) = CommandSuggestions.apply(
                                            value.text,
                                            cursor,
                                            suggestion.token
                                        )
                                        onValueChange(
                                            TextFieldValue(
                                                text = text,
                                                selection = TextRange(newCursor)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: CommandSuggestions.Suggestion,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = suggestion.token,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (suggestion.description.isNotEmpty()) {
            Text(
                text = suggestion.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryRow(
    command: String,
    onClick: () -> Unit
) {
    Text(
        text = command,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun PresetCard(
    preset: TerminalPreset,
    canRun: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onConfigureTile: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    preset.tile?.let { tile ->
                        Spacer(Modifier.width(6.dp))
                        TileBadge(iconKey = tile.iconKey)
                    }
                }
                Text(
                    text = preset.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRun, enabled = canRun) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.terminal_run)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.terminal_preset_options)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal_edit_preset)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal_tile_menu)) },
                        leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onConfigureTile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.terminal_delete_preset)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetDialog(
    preset: TerminalPreset,
    isScreenActive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var command by remember {
        mutableStateOf(
            TextFieldValue(preset.command, TextRange(preset.command.length))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (preset.id.isEmpty()) R.string.terminal_add_preset else R.string.terminal_edit_preset
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.terminal_preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CommandField(
                    value = command,
                    onValueChange = { command = it },
                    enabled = true,
                    isScreenActive = isScreenActive
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), command.text.trim()) },
                enabled = name.isNotBlank() && command.text.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TileBadge(iconKey: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.terminal_tile_badge),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun TileConfigDialog(
    preset: TerminalPreset,
    freeSlot: Int?,
    onDismiss: () -> Unit,
    onSave: (TerminalTileConfig?) -> Unit,
    onSaveAndAdd: (TerminalTileConfig) -> Unit
) {
    val existing = preset.tile

    var enabled by remember { mutableStateOf(existing != null) }
    var label by remember { mutableStateOf(existing?.label?.ifBlank { preset.name } ?: preset.name) }
    var iconKey by remember {
        mutableStateOf(TerminalTileIcons.canonicalKey(existing?.iconKey ?: TerminalTileIcons.DEFAULT_KEY))
    }
    var showToast by remember { mutableStateOf(existing?.showToast ?: true) }
    var collapsePanel by remember { mutableStateOf(existing?.collapsePanel ?: true) }
    var showIconPicker by remember { mutableStateOf(false) }

    val slot = existing?.slot ?: freeSlot
    val trimmedLabel = label.trim()
    val buildConfig: () -> TerminalTileConfig? = {
        if (enabled && slot != null) {
            TerminalTileConfig(
                slot = slot,
                label = trimmedLabel,
                iconKey = iconKey,
                showToast = showToast,
                collapsePanel = collapsePanel
            )
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_tile_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TileToggleRow(
                    title = stringResource(R.string.terminal_tile_enable),
                    description = stringResource(R.string.terminal_tile_enable_description),
                    checked = enabled,
                    enabled = slot != null,
                    onCheckedChange = { enabled = it }
                )

                if (slot == null) {
                    Text(
                        text = stringResource(
                            R.string.terminal_tile_slots_full,
                            TerminalPresetManager.MAX_TILE_SLOTS
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (enabled && slot != null) {
                    Spacer(Modifier.height(4.dp))
                    TilePreview(iconKey = iconKey, label = trimmedLabel.ifEmpty { preset.name })

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.terminal_tile_label)) },
                        supportingText = { Text(stringResource(R.string.terminal_tile_label_helper)) },
                        isError = trimmedLabel.isEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showIconPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.terminal_tile_icon),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = stringResource(R.string.terminal_tile_icon_change),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    TileToggleRow(
                        title = stringResource(R.string.terminal_tile_toast),
                        description = stringResource(R.string.terminal_tile_toast_description),
                        checked = showToast,
                        onCheckedChange = { showToast = it }
                    )
                    TileToggleRow(
                        title = stringResource(R.string.terminal_tile_collapse),
                        description = stringResource(R.string.terminal_tile_collapse_description),
                        checked = collapsePanel,
                        onCheckedChange = { collapsePanel = it }
                    )

                    Spacer(Modifier.height(8.dp))
                    if (!TerminalTileSlots.canRequestAdd()) {
                        Text(
                            text = stringResource(R.string.terminal_tile_add_manual_hint),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedButton(
                            onClick = { buildConfig()?.let(onSaveAndAdd) },
                            enabled = trimmedLabel.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.terminal_tile_add))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(buildConfig()) },
                enabled = !enabled || (slot != null && trimmedLabel.isNotEmpty())
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

@Composable
private fun TilePreview(iconKey: String, label: String) {
    Column {
        Text(
            text = stringResource(R.string.terminal_tile_preview),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TileIconPickerDialog(
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalisedQuery = query.trim().lowercase().replace(' ', '_')
    val matches = remember(normalisedQuery) {
        if (normalisedQuery.isEmpty()) {
            emptyList()
        } else {
            TerminalTileIcons.keys.filter { it.contains(normalisedQuery) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_tile_icon_picker_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.terminal_tile_icon_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                if (normalisedQuery.isNotEmpty() && matches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.terminal_tile_icon_none),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 56.dp),
                        modifier = Modifier.heightIn(min = 200.dp, max = 340.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (normalisedQuery.isEmpty()) {
                            TerminalTileIcons.categories.forEach { category ->
                                item(
                                    key = "header-${category.titleRes}",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Text(
                                        text = stringResource(category.titleRes),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                gridItems(category.keys, key = { it }) { key ->
                                    TileIconCell(
                                        iconKey = key,
                                        selected = key == selectedKey,
                                        onClick = { onSelect(key) }
                                    )
                                }
                            }
                        } else {
                            gridItems(matches, key = { it }) { key ->
                                TileIconCell(
                                    iconKey = key,
                                    selected = key == selectedKey,
                                    onClick = { onSelect(key) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TileIconCell(iconKey: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(14.dp)
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
            contentDescription = iconKey.replace('_', ' '),
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TileToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
