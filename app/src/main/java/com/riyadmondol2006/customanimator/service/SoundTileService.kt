package com.riyadmondol2006.customanimator.service

import android.service.quicksettings.Tile
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.ShizukuHelper
import com.riyadmondol2006.customanimator.utils.SoundTileAction
import com.riyadmondol2006.customanimator.utils.SoundTileActions
import com.riyadmondol2006.customanimator.utils.SoundTilePrefs
import kotlinx.coroutines.delay

class SoundTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec = TileSpec(
        label = getString(R.string.sound_tile_label),
        icon = TileIcon.Res(R.drawable.ic_tile_volume_up),
        state = Tile.STATE_INACTIVE,
        subtitle = getString(actionLabel(SoundTilePrefs(this).clickAction))
    )

    override fun onClick() {
        super.onClick()
        runTileAction {
            val prefs = SoundTilePrefs(this)
            val action = prefs.clickAction
            if (SoundTileActions.needsShizuku(action) && !ShizukuHelper.hasShizukuPermission()) {
                return@runTileAction toast(R.string.sound_tile_needs_shizuku)
            }
            val collapseDelay = prefs.collapseDelayMs
            if (collapseDelay >= 0) {
                SoundTileActions.collapseShade()
                delay(collapseDelay.toLong())
            }
            if (!SoundTileActions.perform(this, action)) {
                toastFailure()
            }
        }
    }

    private fun actionLabel(action: SoundTileAction): Int = when (action) {
        SoundTileAction.VOLUME_PANEL -> R.string.sound_tile_action_volume_panel
        SoundTileAction.MEDIA_OUTPUT -> R.string.sound_tile_action_media_output
        SoundTileAction.NONE -> R.string.sound_tile_action_none
    }
}
