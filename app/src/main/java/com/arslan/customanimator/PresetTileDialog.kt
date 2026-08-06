package com.arslan.customanimator

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.PresetTileConfig
import com.arslan.customanimator.utils.PresetTileJson

@Composable
fun PresetTileDialog(
    presetName: String,
    numberText: String,
    existing: PresetTileConfig?,
    freeSlot: Int?,
    canRequestAdd: Boolean,
    onDismiss: () -> Unit,
    onSave: (PresetTileConfig?) -> Unit,
    onSaveAndAdd: (PresetTileConfig) -> Unit
) {
    var enabled by remember { mutableStateOf(existing != null) }
    var label by remember { mutableStateOf(existing?.label?.ifBlank { presetName } ?: presetName) }
    var showToast by remember { mutableStateOf(existing?.showToast ?: true) }
    var collapsePanel by remember { mutableStateOf(existing?.collapsePanel ?: true) }

    val slot = existing?.slot ?: freeSlot
    val trimmedLabel = label.trim()
    val buildConfig: () -> PresetTileConfig? = {
        if (enabled && slot != null) {
            PresetTileConfig(
                slot = slot,
                label = trimmedLabel,
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
                PresetTileToggleRow(
                    title = stringResource(R.string.terminal_tile_enable),
                    description = stringResource(R.string.preset_tile_enable_description),
                    checked = enabled,
                    enabled = slot != null,
                    onCheckedChange = { enabled = it }
                )

                if (slot == null) {
                    Text(
                        text = stringResource(
                            R.string.terminal_tile_slots_full,
                            PresetTileJson.MAX_TILE_SLOTS
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (enabled && slot != null) {
                    Spacer(Modifier.height(4.dp))
                    PresetTilePreview(
                        numberText = numberText,
                        label = trimmedLabel.ifEmpty { presetName }
                    )

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
                    Text(
                        text = stringResource(R.string.preset_tile_number_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))
                    PresetTileToggleRow(
                        title = stringResource(R.string.terminal_tile_toast),
                        description = stringResource(R.string.preset_tile_toast_description),
                        checked = showToast,
                        onCheckedChange = { showToast = it }
                    )
                    PresetTileToggleRow(
                        title = stringResource(R.string.terminal_tile_collapse),
                        description = stringResource(R.string.preset_tile_collapse_description),
                        checked = collapsePanel,
                        onCheckedChange = { collapsePanel = it }
                    )

                    Spacer(Modifier.height(8.dp))
                    if (!canRequestAdd) {
                        Text(
                            text = stringResource(R.string.terminal_tile_add_manual_hint),
                            style = MaterialTheme.typography.bodySmall,
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
}

@Composable
private fun PresetTilePreview(numberText: String, label: String) {
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
            PresetTileNumber(numberText = numberText, size = 26.dp)
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
fun PresetTileNumber(
    numberText: String,
    size: androidx.compose.ui.unit.Dp = 26.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            text = numberText,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PresetTileBadge(numberText: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.terminal_tile_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = numberText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun PresetTileToggleRow(
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
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
