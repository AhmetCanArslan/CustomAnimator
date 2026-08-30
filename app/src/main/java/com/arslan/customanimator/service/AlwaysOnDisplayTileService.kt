package com.arslan.customanimator.service

import com.arslan.customanimator.R
import com.arslan.customanimator.utils.AlwaysOnDisplayManager

class AlwaysOnDisplayTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec {
        val supported = AlwaysOnDisplayManager.isSupported()
        val ready = AlwaysOnDisplayManager.canApply(this)
        val active = ready && AlwaysOnDisplayManager.isActive(contentResolver)
        return TileSpec(
            label = getString(R.string.aod_tile_label),
            icon = TileIcon.Res(R.drawable.ic_tile_aod),
            state = stateOf(ready, active),
            subtitle = getString(subtitleFor(supported, ready, active))
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            if (!AlwaysOnDisplayManager.isSupported()) {
                return@runTileAction toast(R.string.aod_tile_unsupported)
            }
            if (!AlwaysOnDisplayManager.canApply(this)) {
                return@runTileAction toast(R.string.preset_tile_needs_permission)
            }
            val activate = !AlwaysOnDisplayManager.isActive(contentResolver)
            if (!AlwaysOnDisplayManager.setActive(contentResolver, activate)) {
                toast(R.string.action_failed)
            }
        }
    }

    private fun subtitleFor(supported: Boolean, ready: Boolean, active: Boolean): Int = when {
        !supported -> R.string.aod_tile_unsupported
        !ready -> R.string.preset_tile_subtitle_no_permission
        active -> R.string.aod_tile_subtitle_on
        else -> R.string.aod_tile_subtitle_off
    }
}
