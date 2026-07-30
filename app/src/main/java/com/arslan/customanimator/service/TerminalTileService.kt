package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalPresetManager
import com.arslan.customanimator.utils.TerminalTileIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class TerminalTileService : TileService() {

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

        val preset = TerminalPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile
        if (preset == null || config == null) {
            toast(getString(R.string.terminal_tile_unassigned_message))
            refreshTile()
            return
        }

        if (!ShizukuHelper.hasShizukuPermission()) {
            toast(getString(R.string.terminal_tile_needs_shizuku))
            refreshTile()
            return
        }

        val label = config.label.ifBlank { preset.name }
        setTileState(Tile.STATE_ACTIVE)

        backgroundScope.launch {
            if (config.collapsePanel) {
                collapseQuickSettings()
            }

            val result = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("sh", "-c", preset.command)
            )

            if (config.showToast) {
                val message = if (result.exitCode == 0) {
                    getString(R.string.terminal_tile_toast_success, label)
                } else {
                    getString(R.string.terminal_tile_toast_failed, label, result.exitCode)
                }
                toast(message)
            }
            setTileState(Tile.STATE_INACTIVE)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val preset = TerminalPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile

        if (preset == null || config == null) {
            tile.label = getString(R.string.terminal_tile_unassigned)
            tile.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(TerminalTileIcons.DEFAULT_KEY))
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = null
            }
        } else {
            tile.label = config.label.ifBlank { preset.name }
            tile.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(config.iconKey))
            val ready = ShizukuHelper.hasShizukuPermission()
            tile.state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (ready) null else getString(R.string.terminal_tile_subtitle_no_shizuku)
            }
        }
        tile.updateTile()
    }

    private fun setTileState(state: Int) {
        mainHandler.post {
            qsTile?.let {
                it.state = state
                it.updateTile()
            }
        }
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
