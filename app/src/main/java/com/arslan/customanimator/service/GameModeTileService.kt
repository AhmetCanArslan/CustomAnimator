package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.GameModeController
import com.arslan.customanimator.utils.GameModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GameModeTileService : TileService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        if (!GameModeController.canApply(this)) {
            toast(getString(R.string.preset_tile_needs_permission))
            refreshTile()
            return
        }

        val activate = !GameModeController.isActive(this)
        if (activate && GameModeManager(this).getSelectedPackages().isEmpty()) {
            toast(getString(R.string.game_mode_select_games))
            refreshTile()
            return
        }

        backgroundScope.launch {
            val result = GameModeController.setActive(this@GameModeTileService, activate)
            toast(
                getString(
                    when {
                        !result.succeeded -> R.string.game_mode_failed_toast
                        activate -> R.string.game_mode_enabled_toast
                        else -> R.string.game_mode_disabled_toast
                    }
                )
            )
            mainHandler.post { refreshTile() }
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val ready = GameModeController.canApply(this)
        val active = GameModeController.isActive(this)

        tile.label = getString(R.string.game_mode_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_videogame_asset)
        tile.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            active -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !ready -> getString(R.string.preset_tile_subtitle_no_permission)
                active -> getString(R.string.game_mode_tile_subtitle_on)
                else -> getString(R.string.game_mode_tile_subtitle_off)
            }
        }
        tile.updateTile()
    }

    private fun toast(message: String) {
        mainHandler.post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
