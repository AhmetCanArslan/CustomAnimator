package com.arslan.customanimator.service

import android.service.quicksettings.Tile
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TileNumberIcon
import com.arslan.customanimator.utils.WidthPresetManager

abstract class WidthTileService : BaseTileService() {

    protected abstract val slot: Int

    override suspend fun loadSpec(): TileSpec {
        val preset = WidthPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile ?: return unavailableSpec(
            R.string.terminal_tile_unassigned,
            TileIcon.Number(PLACEHOLDER_TEXT)
        )
        val ready = canApply()
        return TileSpec(
            label = config.label.ifBlank { preset.name },
            icon = TileIcon.Number(TileNumberIcon.widthText(preset.widthDp)),
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE,
            subtitle = if (ready) null else getString(R.string.preset_tile_subtitle_no_permission)
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            val preset = WidthPresetManager(this).getPresetForSlot(slot)
            val config = preset?.tile
                ?: return@runTileAction toast(R.string.terminal_tile_unassigned_message)
            if (!canApply()) {
                return@runTileAction toast(R.string.preset_tile_needs_permission)
            }
            if (config.collapsePanel) {
                collapseShade()
            }
            val result = SettingsManager.setSmallestWidth(contentResolver, this, preset.widthDp)
            if (config.showToast) {
                toast(toastFor(result), config.label.ifBlank { preset.name })
            }
        }
    }

    private fun toastFor(result: SettingsManager.SmallestWidthResult): Int = when {
        !result.success -> R.string.preset_tile_toast_failed
        result.usedWriteSecureFallback -> R.string.preset_tile_toast_applied_unverified
        else -> R.string.preset_tile_toast_applied
    }

    private fun canApply(): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(this)

    private companion object {
        const val PLACEHOLDER_TEXT = "--"
    }
}

class WidthTileService1 : WidthTileService() {
    override val slot = 0
}

class WidthTileService2 : WidthTileService() {
    override val slot = 1
}

class WidthTileService3 : WidthTileService() {
    override val slot = 2
}

class WidthTileService4 : WidthTileService() {
    override val slot = 3
}

class WidthTileService5 : WidthTileService() {
    override val slot = 4
}
