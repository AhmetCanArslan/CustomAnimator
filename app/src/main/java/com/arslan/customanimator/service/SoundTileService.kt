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
import com.arslan.customanimator.utils.SoundTileAction
import com.arslan.customanimator.utils.SoundTileActions
import com.arslan.customanimator.utils.SoundTilePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundTileService : TileService() {

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

        val prefs = SoundTilePrefs(this)
        val action = prefs.clickAction
        val collapseDelay = prefs.collapseDelayMs

        if (SoundTileActions.needsShizuku(action) && !ShizukuHelper.hasShizukuPermission()) {
            toast(getString(R.string.sound_tile_needs_shizuku))
            return
        }

        if (collapseDelay >= 0) {
            backgroundScope.launch {
                SoundTileActions.collapseShade()
                mainHandler.postDelayed({ runAction(action) }, collapseDelay.toLong())
            }
        } else {
            runAction(action)
        }

        refreshTile()
    }

    private fun runAction(action: SoundTileAction) {
        when (action) {
            SoundTileAction.VOLUME_PANEL -> SoundTileActions.showVolumePanel(this)
            SoundTileAction.MEDIA_OUTPUT -> backgroundScope.launch {
                if (!SoundTileActions.openMediaOutputDialog()) {
                    toast(getString(R.string.action_failed))
                }
            }
            SoundTileAction.NONE -> Unit
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.sound_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_volume_up)
        tile.state = Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(actionLabel(SoundTilePrefs(this).clickAction))
        }
        tile.updateTile()
    }

    private fun actionLabel(action: SoundTileAction): Int = when (action) {
        SoundTileAction.VOLUME_PANEL -> R.string.sound_tile_action_volume_panel
        SoundTileAction.MEDIA_OUTPUT -> R.string.sound_tile_action_media_output
        SoundTileAction.NONE -> R.string.sound_tile_action_none
    }

    private fun toast(message: String) {
        mainHandler.post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
