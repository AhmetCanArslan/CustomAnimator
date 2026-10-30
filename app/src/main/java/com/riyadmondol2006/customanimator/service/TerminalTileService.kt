package com.riyadmondol2006.customanimator.service

import android.service.quicksettings.Tile
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.ShizukuHelper
import com.riyadmondol2006.customanimator.utils.TerminalPresetManager
import com.riyadmondol2006.customanimator.utils.TerminalTileIcons

abstract class TerminalTileService : BaseTileService() {

    protected abstract val slot: Int

    override suspend fun loadSpec(): TileSpec {
        val preset = TerminalPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile ?: return unavailableSpec(
            R.string.terminal_tile_unassigned,
            TileIcon.Res(TerminalTileIcons.resFor(TerminalTileIcons.DEFAULT_KEY))
        )
        val ready = ShizukuHelper.hasShizukuPermission()
        return TileSpec(
            label = config.label.ifBlank { preset.name },
            icon = TileIcon.Res(TerminalTileIcons.resFor(config.iconKey)),
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE,
            subtitle = if (ready) null else getString(R.string.terminal_tile_subtitle_no_shizuku)
        )
    }

    override fun onClick() {
        super.onClick()
        runUnlockedTileAction {
            val preset = TerminalPresetManager(this).getPresetForSlot(slot)
            val config = preset?.tile
                ?: return@runUnlockedTileAction toast(R.string.terminal_tile_unassigned_message)
            if (!ShizukuHelper.hasShizukuPermission()) {
                return@runUnlockedTileAction toast(R.string.terminal_tile_needs_shizuku)
            }
            if (config.collapsePanel) {
                collapseShade()
            }
            val result = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("sh", "-c", preset.command)
            )
            if (!config.showToast) {
                return@runUnlockedTileAction
            }
            val label = config.label.ifBlank { preset.name }
            if (result.exitCode == 0) {
                toast(R.string.terminal_tile_toast_success, label)
            } else {
                toast(R.string.terminal_tile_toast_failed, label, result.exitCode)
            }
        }
    }
}

class TerminalTileService1 : TerminalTileService() {
    override val slot = 0
}

class TerminalTileService2 : TerminalTileService() {
    override val slot = 1
}

class TerminalTileService3 : TerminalTileService() {
    override val slot = 2
}

class TerminalTileService4 : TerminalTileService() {
    override val slot = 3
}

class TerminalTileService5 : TerminalTileService() {
    override val slot = 4
}
