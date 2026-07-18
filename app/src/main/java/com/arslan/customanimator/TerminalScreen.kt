package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.TerminalPreset
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalPresetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TerminalScreenContent(
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val presetManager = remember { TerminalPresetManager(context) }

    var command by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var exitCode by rememberSaveable { mutableStateOf<Int?>(null) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var presets by remember { mutableStateOf(presetManager.getAllPresets()) }

    // null = dialog closed, a preset with a blank id = "add new"
    var editingPreset by remember { mutableStateOf<TerminalPreset?>(null) }

    val canRun = hasShizukuPermission && !isRunning

    val run: (String) -> Unit = { raw ->
        val trimmed = raw.trim()
        if (trimmed.isNotEmpty() && canRun) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasShizukuPermission,
                label = { Text(stringResource(R.string.terminal_command_hint)) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = { run(command) }, enabled = canRun && command.isNotBlank()) {
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
                    onClick = { editingPreset = TerminalPreset(id = "", name = "", command = command.trim()) }
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
}

@Composable
private fun PresetCard(
    preset: TerminalPreset,
    canRun: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var command by remember { mutableStateOf(preset.command) }

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
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(R.string.terminal_command_hint)) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), command.trim()) },
                enabled = name.isNotBlank() && command.isNotBlank()
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
