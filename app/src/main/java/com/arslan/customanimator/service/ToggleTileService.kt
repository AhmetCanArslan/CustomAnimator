package com.arslan.customanimator.service

import com.arslan.customanimator.R
import com.arslan.customanimator.data.ToggleTile
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.ToggleTileManager

abstract class ToggleTileService : BaseTileService() {

    protected abstract val slot: Int

    override suspend fun loadSpec(): TileSpec {
        val manager = ToggleTileManager(this)
        val tile = manager.getTileForSlot(slot) ?: return unassignedSpec()
        val ready = ShizukuHelper.awaitShizukuPermission(BINDER_WAIT_MS)
        val active = if (ready) manager.resolveState(tile) else manager.getStoredState(tile.id)
        return specFor(tile, active, ready)
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            val manager = ToggleTileManager(this)
            val tile = manager.getTileForSlot(slot)
                ?: return@runTileAction toast(R.string.qs_tile_unassigned_message)
            if (!ShizukuHelper.awaitShizukuPermission(BINDER_WAIT_MS)) {
                return@runTileAction toast(R.string.qs_tile_needs_shizuku)
            }
            if (tile.collapsePanel) {
                collapseShade()
            }
            val target = !manager.resolveState(tile)
            val success = manager.apply(tile, target)
            if (tile.showToast) {
                toast(toastFor(success, target), tile.label)
            }
        }
    }

    private fun unassignedSpec(): TileSpec = unavailableSpec(
        R.string.qs_tile_unassigned,
        TileIcon.Res(TerminalTileIcons.resFor(TerminalTileIcons.DEFAULT_KEY))
    )

    private fun specFor(tile: ToggleTile, active: Boolean, ready: Boolean) = TileSpec(
        label = tile.label,
        icon = TileIcon.Res(TerminalTileIcons.resFor(tile.iconKey)),
        state = stateOf(ready, active),
        subtitle = getString(subtitleFor(active, ready))
    )

    private fun subtitleFor(active: Boolean, ready: Boolean): Int = when {
        !ready -> R.string.qs_tile_subtitle_no_shizuku
        active -> R.string.qs_tile_subtitle_on
        else -> R.string.qs_tile_subtitle_off
    }

    private fun toastFor(success: Boolean, active: Boolean): Int = when {
        !success -> R.string.qs_tile_toast_failed
        active -> R.string.qs_tile_toast_on
        else -> R.string.qs_tile_toast_off
    }

    private companion object {
        const val BINDER_WAIT_MS = 3000L
    }
}

class ToggleTileService1 : ToggleTileService() {
    override val slot = 0
}

class ToggleTileService2 : ToggleTileService() {
    override val slot = 1
}

class ToggleTileService3 : ToggleTileService() {
    override val slot = 2
}

class ToggleTileService4 : ToggleTileService() {
    override val slot = 3
}

class ToggleTileService5 : ToggleTileService() {
    override val slot = 4
}

class ToggleTileService6 : ToggleTileService() {
    override val slot = 5
}

class ToggleTileService7 : ToggleTileService() {
    override val slot = 6
}

class ToggleTileService8 : ToggleTileService() {
    override val slot = 7
}

class ToggleTileService9 : ToggleTileService() {
    override val slot = 8
}

class ToggleTileService10 : ToggleTileService() {
    override val slot = 9
}

class ToggleTileService11 : ToggleTileService() {
    override val slot = 10
}

class ToggleTileService12 : ToggleTileService() {
    override val slot = 11
}
