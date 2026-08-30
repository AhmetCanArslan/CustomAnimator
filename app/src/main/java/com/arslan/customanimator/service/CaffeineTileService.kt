package com.arslan.customanimator.service

import com.arslan.customanimator.R
import com.arslan.customanimator.utils.CaffeineManager

class CaffeineTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec {
        val ready = CaffeineManager.canApply(this)
        val active = ready && CaffeineManager.isActive(contentResolver)
        return TileSpec(
            label = getString(R.string.caffeine_tile_label),
            icon = TileIcon.Res(R.drawable.ic_tile_caffeine),
            state = stateOf(ready, active),
            subtitle = getString(subtitleFor(ready, active))
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            if (!CaffeineManager.canApply(this)) {
                return@runTileAction toast(R.string.preset_tile_needs_permission)
            }
            val activate = !CaffeineManager.isActive(contentResolver)
            if (!CaffeineManager.setActive(this, contentResolver, activate)) {
                toast(R.string.action_failed)
            }
        }
    }

    private fun subtitleFor(ready: Boolean, active: Boolean): Int = when {
        !ready -> R.string.preset_tile_subtitle_no_permission
        active -> R.string.caffeine_tile_subtitle_on
        else -> R.string.caffeine_tile_subtitle_off
    }
}
