package com.riyadmondol2006.customanimator.service

import android.service.quicksettings.Tile
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.DeveloperOptionsManager
import com.riyadmondol2006.customanimator.utils.ShizukuHelper

class RestartSystemUiTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec {
        val ready = ShizukuHelper.isShizukuAvailable() && ShizukuHelper.hasShizukuPermission()
        return TileSpec(
            label = getString(R.string.restart_system_ui_tile_label),
            icon = TileIcon.Res(R.drawable.ic_tile_restart_alt),
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE,
            subtitle = if (ready) null else getString(R.string.terminal_tile_subtitle_no_shizuku)
        )
    }

    override fun onClick() {
        super.onClick()
        runUnlockedTileAction {
            if (!ShizukuHelper.hasShizukuPermission()) {
                return@runUnlockedTileAction toast(R.string.terminal_tile_needs_shizuku)
            }
            if (!DeveloperOptionsManager.restartSystemUi()) {
                toastFailure()
            }
        }
    }
}
