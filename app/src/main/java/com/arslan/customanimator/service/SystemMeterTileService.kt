package com.arslan.customanimator.service

import com.arslan.customanimator.R
import com.arslan.customanimator.utils.FpsOverlayManager

class SystemMeterTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec {
        val ready = FpsOverlayManager.canDrawOverlay(this)
        val active = FpsOverlayManager.isActive(this)
        return TileSpec(
            label = getString(R.string.system_meter_title),
            icon = TileIcon.Res(R.drawable.ic_tile_speed),
            state = stateOf(ready, active),
            subtitle = getString(subtitleFor(ready, active))
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            if (!FpsOverlayManager.canDrawOverlay(this)) {
                return@runTileAction toast(R.string.fps_overlay_permission_needed)
            }
            val activate = !FpsOverlayManager.isEnabled(this)
            FpsOverlayManager.setEnabled(this, activate)
            if (!activate) return@runTileAction FpsOverlayService.stop(this)
            runCatching { FpsOverlayService.start(this) }.onFailure {
                FpsOverlayManager.setEnabled(this, false)
                toast(R.string.action_failed)
            }
        }
    }

    private fun subtitleFor(ready: Boolean, active: Boolean): Int = when {
        !ready -> R.string.preset_tile_subtitle_no_permission
        active -> R.string.qs_tile_subtitle_on
        else -> R.string.qs_tile_subtitle_off
    }
}
