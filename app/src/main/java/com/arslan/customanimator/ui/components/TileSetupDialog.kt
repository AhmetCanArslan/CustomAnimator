package com.arslan.customanimator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.TerminalTileIcons

data class TileSetupDraft(
    val label: String,
    val iconKey: String,
    val showToast: Boolean,
    val collapsePanel: Boolean
)

data class TileSetupInitial(
    val enabled: Boolean,
    val label: String,
    val iconKey: String? = null,
    val numberText: String? = null,
    val showToast: Boolean = true,
    val collapsePanel: Boolean = true
)

@Composable
fun TileSetupDialog(
    tileName: String,
    initial: TileSetupInitial,
    slot: Int?,
    maxSlots: Int,
    canRequestAdd: Boolean,
    onDismiss: () -> Unit,
    onSave: (TileSetupDraft?) -> Unit,
    onSaveAndAdd: (TileSetupDraft) -> Unit
) {
    var enabled by remember { mutableStateOf(initial.enabled) }
    var label by remember { mutableStateOf(initial.label.ifBlank { tileName }) }
    var iconKey by remember {
        mutableStateOf(TerminalTileIcons.canonicalKey(initial.iconKey ?: TerminalTileIcons.DEFAULT_KEY))
    }
    var showToast by remember { mutableStateOf(initial.showToast) }
    var collapsePanel by remember { mutableStateOf(initial.collapsePanel) }
    var showIconPicker by remember { mutableStateOf(false) }

    val trimmedLabel = label.trim()
    val draft = TileSetupDraft(trimmedLabel, iconKey, showToast, collapsePanel)
    val canSave = !enabled || (slot != null && trimmedLabel.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_tile_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TileSetupToggleRow(
                    title = stringResource(R.string.terminal_tile_enable),
                    description = stringResource(R.string.terminal_tile_enable_description),
                    checked = enabled,
                    enabled = slot != null,
                    onCheckedChange = { enabled = it }
                )

                if (slot == null) {
                    Text(
                        text = stringResource(R.string.terminal_tile_slots_full, maxSlots),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (enabled && slot != null) {
                    TileSetupBody(
                        tileName = tileName,
                        label = label,
                        onLabelChange = { label = it },
                        iconKey = if (initial.iconKey == null) null else iconKey,
                        numberText = initial.numberText,
                        onPickIcon = { showIconPicker = true },
                        showToast = showToast,
                        onShowToastChange = { showToast = it },
                        collapsePanel = collapsePanel,
                        onCollapsePanelChange = { collapsePanel = it },
                        canRequestAdd = canRequestAdd,
                        onAddToPanel = { onSaveAndAdd(draft) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (enabled) draft else null) },
                enabled = canSave
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
private fun TileSetupBody(
    tileName: String,
    label: String,
    onLabelChange: (String) -> Unit,
    iconKey: String?,
    numberText: String?,
    onPickIcon: () -> Unit,
    showToast: Boolean,
    onShowToastChange: (Boolean) -> Unit,
    collapsePanel: Boolean,
    onCollapsePanelChange: (Boolean) -> Unit,
    canRequestAdd: Boolean,
    onAddToPanel: () -> Unit
) {
    val trimmedLabel = label.trim()
    Spacer(Modifier.height(4.dp))
    TileSetupPreview(
        iconKey = iconKey,
        numberText = numberText,
        label = trimmedLabel.ifEmpty { tileName }
    )

    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = label,
        onValueChange = onLabelChange,
        label = { Text(stringResource(R.string.terminal_tile_label)) },
        supportingText = { Text(stringResource(R.string.terminal_tile_label_helper)) },
        isError = trimmedLabel.isEmpty(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))
    if (iconKey == null) {
        Text(
            text = stringResource(R.string.preset_tile_number_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        TileSetupIconButton(iconKey = iconKey, onClick = onPickIcon)
    }

    Spacer(Modifier.height(8.dp))
    TileSetupToggleRow(
        title = stringResource(R.string.terminal_tile_toast),
        description = stringResource(R.string.terminal_tile_toast_description),
        checked = showToast,
        onCheckedChange = onShowToastChange
    )
    TileSetupToggleRow(
        title = stringResource(R.string.terminal_tile_collapse),
        description = stringResource(R.string.terminal_tile_collapse_description),
        checked = collapsePanel,
        onCheckedChange = onCollapsePanelChange
    )

    Spacer(Modifier.height(8.dp))
    if (canRequestAdd) {
        OutlinedButton(
            onClick = onAddToPanel,
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
    } else {
        Text(
            text = stringResource(R.string.terminal_tile_add_manual_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TileSetupIconButton(iconKey: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun TileSetupPreview(iconKey: String?, numberText: String?, label: String) {
    Column {
        Text(
            text = stringResource(R.string.terminal_tile_preview),
            style = MaterialTheme.typography.titleSmall,
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
            TileFaceIcon(iconKey = iconKey, numberText = numberText, size = 24.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TileFaceIcon(
    iconKey: String?,
    numberText: String?,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    if (iconKey != null) {
        Icon(
            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size)
        )
    } else {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Text(
                text = numberText.orEmpty(),
                fontSize = (size.value * 0.58f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TileSetupToggleRow(
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
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
