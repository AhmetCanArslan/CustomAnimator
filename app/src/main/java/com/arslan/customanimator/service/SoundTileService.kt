package com.arslan.customanimator.service

import android.service.quicksettings.Tile
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.SoundTileAction
import com.arslan.customanimator.utils.SoundTileActions
import com.arslan.customanimator.utils.SoundTilePrefs
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
                toast(R.string.action_failed)
            }
        }
    }

    private fun actionLabel(action: SoundTileAction): Int = when (action) {
        SoundTileAction.VOLUME_PANEL -> R.string.sound_tile_action_volume_panel
        SoundTileAction.MEDIA_OUTPUT -> R.string.sound_tile_action_media_output
        SoundTileAction.NONE -> R.string.sound_tile_action_none
    }
}
