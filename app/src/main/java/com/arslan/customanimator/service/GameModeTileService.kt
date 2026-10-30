package com.arslan.customanimator.service

import com.arslan.customanimator.R
import com.arslan.customanimator.utils.GameModeController
import com.arslan.customanimator.utils.GameModeManager

class GameModeTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec {
        val ready = GameModeController.canApply(this)
        val active = GameModeController.isActive(this)
        return TileSpec(
            label = getString(R.string.game_mode_tile_label),
            icon = TileIcon.Res(R.drawable.ic_tile_videogame_asset),
            state = stateOf(ready, active),
            subtitle = getString(subtitleFor(ready, active))
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            if (!GameModeController.canApply(this)) {
                return@runTileAction toast(R.string.preset_tile_needs_permission)
            }
            val activate = !GameModeController.isActive(this)
            if (activate && GameModeManager(this).getSelectedPackages().isEmpty()) {
                return@runTileAction toast(R.string.game_mode_select_games)
            }
            val result = GameModeController.setActive(this, activate)
            toast(toastFor(result.succeeded, activate))
        }
    }

    private fun subtitleFor(ready: Boolean, active: Boolean): Int = when {
        !ready -> R.string.preset_tile_subtitle_no_permission
        active -> R.string.game_mode_tile_subtitle_on
        else -> R.string.game_mode_tile_subtitle_off
    }

    private fun toastFor(succeeded: Boolean, activate: Boolean): Int = when {
        !succeeded -> R.string.game_mode_failed_toast
        activate -> R.string.game_mode_enabled_toast
        else -> R.string.game_mode_disabled_toast
    }
}
