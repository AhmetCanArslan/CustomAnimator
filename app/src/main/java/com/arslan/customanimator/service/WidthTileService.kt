package com.arslan.customanimator.service

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TileNumberIcon
import com.arslan.customanimator.utils.WidthPresetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class WidthTileService : TileService() {

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

        val preset = WidthPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile
        if (preset == null || config == null) {
            toast(getString(R.string.terminal_tile_unassigned_message))
            refreshTile()
            return
        }

        if (!canApply()) {
            toast(getString(R.string.preset_tile_needs_permission))
            refreshTile()
            return
        }

        val label = config.label.ifBlank { preset.name }
        backgroundScope.launch {
            if (config.collapsePanel) {
                collapseQuickSettings()
            }

            val result = SettingsManager.setSmallestWidth(contentResolver, this@WidthTileService, preset.widthDp)

            if (config.showToast) {
                val message = when {
                    !result.success -> getString(R.string.preset_tile_toast_failed, label)
                    result.usedWriteSecureFallback ->
                        getString(R.string.preset_tile_toast_applied_unverified, label)
                    else -> getString(R.string.preset_tile_toast_applied, label)
                }
                toast(message)
            }
            refreshTileAsync()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val preset = WidthPresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile

        if (preset == null || config == null) {
            tile.label = getString(R.string.terminal_tile_unassigned)
            tile.icon = TileNumberIcon.create("--")
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = null
            }
        } else {
            tile.label = config.label.ifBlank { preset.name }
            tile.icon = TileNumberIcon.create(TileNumberIcon.widthText(preset.widthDp))
            val ready = canApply()
            tile.state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (ready) {
                    null
                } else {
                    getString(R.string.preset_tile_subtitle_no_permission)
                }
            }
        }
        tile.updateTile()
    }

    private fun canApply(): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(this)

    private fun refreshTileAsync() {
        mainHandler.post { refreshTile() }
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
