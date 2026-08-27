package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.data.ToggleTile
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.ToggleTileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class ToggleTileService : TileService() {

    protected abstract val slot: Int

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()

        val manager = ToggleTileManager(this)
        val tile = manager.getTileForSlot(slot)
        if (tile == null) {
            toast(getString(R.string.qs_tile_unassigned_message))
            renderUnassigned()
            return
        }

        if (!ShizukuHelper.hasShizukuPermission()) {
            toast(getString(R.string.qs_tile_needs_shizuku))
            render(tile, manager.getStoredState(tile.id), false)
            return
        }

        backgroundScope.launch {
            if (tile.collapsePanel) {
                collapseQuickSettings()
            }

            val target = !manager.resolveState(tile)
            val success = manager.apply(tile, target)
            val state = manager.resolveState(tile)

            if (tile.showToast) {
                val message = if (success) {
                    getString(
                        if (state) R.string.qs_tile_toast_on else R.string.qs_tile_toast_off,
                        tile.label
                    )
                } else {
                    getString(R.string.qs_tile_toast_failed, tile.label)
                }
                toast(message)
            }

            mainHandler.post { render(tile, state, true) }
        }
    }

    private fun refreshTile() {
        val manager = ToggleTileManager(this)
        val tile = manager.getTileForSlot(slot)
        if (tile == null) {
            renderUnassigned()
            return
        }

        val ready = ShizukuHelper.hasShizukuPermission()
        if (!ready) {
            render(tile, manager.getStoredState(tile.id), false)
            return
        }

        backgroundScope.launch {
            val state = manager.resolveState(tile)
            mainHandler.post { render(tile, state, true) }
        }
    }

    private fun renderUnassigned() {
        val qs = qsTile ?: return
        qs.label = getString(R.string.qs_tile_unassigned)
        qs.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(TerminalTileIcons.DEFAULT_KEY))
        qs.state = Tile.STATE_UNAVAILABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qs.subtitle = null
        }
        qs.updateTile()
    }

    private fun render(tile: ToggleTile, active: Boolean, ready: Boolean) {
        val qs = qsTile ?: return
        qs.label = tile.label
        qs.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(tile.iconKey))
        qs.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            active -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qs.subtitle = when {
                !ready -> getString(R.string.qs_tile_subtitle_no_shizuku)
                active -> getString(R.string.qs_tile_subtitle_on)
                else -> getString(R.string.qs_tile_subtitle_off)
            }
        }
        qs.updateTile()
    }

    private fun collapseQuickSettings() {
        runCatching {
            ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "collapse"))
        }
    }

    private fun toast(message: String) {
        val appContext = applicationContext
        mainHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
