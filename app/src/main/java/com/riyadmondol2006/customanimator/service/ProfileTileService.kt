package com.riyadmondol2006.customanimator.service

import android.service.quicksettings.Tile
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.ProfileApplier
import com.riyadmondol2006.customanimator.utils.ProfileManager
import com.riyadmondol2006.customanimator.utils.TerminalTileIcons

abstract class ProfileTileService : BaseTileService() {

    protected abstract val slot: Int

    override suspend fun loadSpec(): TileSpec {
        val profile = ProfileManager(this).getProfileForSlot(slot)
        val config = profile?.tile ?: return unavailableSpec(
            R.string.terminal_tile_unassigned,
            TileIcon.Res(TerminalTileIcons.resFor(ProfileManager.DEFAULT_ICON_KEY))
        )
        val ready = ProfileApplier.canApply(this)
        return TileSpec(
            label = config.label.ifBlank { profile.name },
            icon = TileIcon.Res(TerminalTileIcons.resFor(profile.iconKey)),
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE,
            subtitle = if (ready) {
                resources.getQuantityString(
                    R.plurals.profile_action_count,
                    profile.actionCount,
                    profile.actionCount
                )
            } else {
                getString(R.string.preset_tile_subtitle_no_permission)
            }
        )
    }

    override fun onClick() {
        super.onClick()
        runUnlockedTileAction {
            val profile = ProfileManager(this).getProfileForSlot(slot)
            val config = profile?.tile
                ?: return@runUnlockedTileAction toast(R.string.terminal_tile_unassigned_message)
            if (!ProfileApplier.canApply(this)) {
                return@runUnlockedTileAction toast(R.string.preset_tile_needs_permission)
            }
            if (config.collapsePanel) {
                collapseShade()
            }
            val result = ProfileApplier.apply(this, profile)
            if (!config.showToast) {
                return@runUnlockedTileAction
            }
            val label = config.label.ifBlank { profile.name }
            if (result.failed == 0) {
                toast(R.string.profile_tile_toast_applied, label)
            } else {
                toast(R.string.profile_tile_toast_partial, label, result.applied, result.total)
            }
        }
    }
}

class ProfileTileService1 : ProfileTileService() {
    override val slot = 0
}

class ProfileTileService2 : ProfileTileService() {
    override val slot = 1
}

class ProfileTileService3 : ProfileTileService() {
    override val slot = 2
}

class ProfileTileService4 : ProfileTileService() {
    override val slot = 3
}

class ProfileTileService5 : ProfileTileService() {
    override val slot = 4
}
