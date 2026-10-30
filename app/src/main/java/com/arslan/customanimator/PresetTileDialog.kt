package com.arslan.customanimator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.PresetTileConfig
import com.arslan.customanimator.ui.components.TileSetupDialog
import com.arslan.customanimator.ui.components.TileSetupInitial
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
    val slot = existing?.slot ?: freeSlot
    val toConfig: (com.arslan.customanimator.ui.components.TileSetupDraft) -> PresetTileConfig? = { draft ->
        slot?.let {
            PresetTileConfig(
                slot = it,
                label = draft.label,
                showToast = draft.showToast,
                collapsePanel = draft.collapsePanel
            )
        }
    }

    TileSetupDialog(
        tileName = presetName,
        initial = TileSetupInitial(
            enabled = existing != null,
            label = existing?.label.orEmpty(),
            numberText = numberText,
            showToast = existing?.showToast ?: true,
            collapsePanel = existing?.collapsePanel ?: true
        ),
        slot = slot,
        maxSlots = PresetTileJson.MAX_TILE_SLOTS,
        canRequestAdd = canRequestAdd,
        onDismiss = onDismiss,
        onSave = { draft -> onSave(draft?.let(toConfig)) },
        onSaveAndAdd = { draft -> toConfig(draft)?.let(onSaveAndAdd) }
    )
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
