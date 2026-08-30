package com.arslan.customanimator.service

import android.service.quicksettings.Tile
import com.arslan.customanimator.R
import com.arslan.customanimator.data.AnimatorPreset
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TileNumberIcon

abstract class AnimationTileService : BaseTileService() {

    protected abstract val slot: Int

    override suspend fun loadSpec(): TileSpec {
        val preset = PresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile ?: return unavailableSpec(
            R.string.terminal_tile_unassigned,
            TileIcon.Number(PLACEHOLDER_TEXT)
        )
        val ready = canApply()
        return TileSpec(
            label = config.label.ifBlank { preset.name },
            icon = TileIcon.Number(animationText(preset)),
            state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE,
            subtitle = if (ready) {
                animationSubtitle(preset)
            } else {
                getString(R.string.preset_tile_subtitle_no_permission)
            }
        )
    }

    override fun onClick() {
        super.onClick()
        runTileAction {
            val preset = PresetManager(this).getPresetForSlot(slot)
            val config = preset?.tile
                ?: return@runTileAction toast(R.string.terminal_tile_unassigned_message)
            if (!canApply()) {
                return@runTileAction toast(R.string.preset_tile_needs_permission)
            }
            if (config.collapsePanel) {
                collapseShade()
            }
            val success = SettingsManager.applyAllScales(
                this,
                contentResolver,
                preset.windowAnimationScale,
                preset.transitionAnimationScale,
                preset.animatorDurationScale
            )
            if (config.showToast) {
                val message = if (success) {
                    R.string.preset_tile_toast_applied
                } else {
                    R.string.preset_tile_toast_failed
                }
                toast(message, config.label.ifBlank { preset.name })
            }
        }
    }

    private fun canApply(): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(this)

    private fun animationText(preset: AnimatorPreset): String = TileNumberIcon.animationText(
        preset.windowAnimationScale,
        preset.transitionAnimationScale,
        preset.animatorDurationScale
    )

    private fun animationSubtitle(preset: AnimatorPreset): String? =
        TileNumberIcon.animationSubtitle(
            preset.windowAnimationScale,
            preset.transitionAnimationScale,
            preset.animatorDurationScale
        )

    private companion object {
        const val PLACEHOLDER_TEXT = "--"
    }
}

class AnimationTileService1 : AnimationTileService() {
    override val slot = 0
}

class AnimationTileService2 : AnimationTileService() {
    override val slot = 1
}

class AnimationTileService3 : AnimationTileService() {
    override val slot = 2
}

class AnimationTileService4 : AnimationTileService() {
    override val slot = 3
}

class AnimationTileService5 : AnimationTileService() {
    override val slot = 4
}
