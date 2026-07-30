package com.arslan.customanimator.service

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TileNumberIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class AnimationTileService : TileService() {

    protected abstract val slot: Int

    private val mainHandler = Handler(Looper.getMainLooper())

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

        val preset = PresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile
        if (preset == null || config == null) {
            toast(getString(R.string.terminal_tile_unassigned_message))
            refreshTile()
            return
        }

        if (!canApply()) {
            toast(getString(R.string.preset_tile_needs_permission))
            refreshTile()
            return
        }

        val label = config.label.ifBlank { preset.name }
        setTileState(Tile.STATE_ACTIVE)

        backgroundScope.launch {
            if (config.collapsePanel) {
                collapseQuickSettings()
            }

            val success = SettingsManager.applyAllScales(
                this@AnimationTileService,
                contentResolver,
                preset.windowAnimationScale,
                preset.transitionAnimationScale,
                preset.animatorDurationScale
            )

            if (config.showToast) {
                val message = if (success) {
                    getString(R.string.preset_tile_toast_applied, label)
                } else {
                    getString(R.string.preset_tile_toast_failed, label)
                }
                toast(message)
            }
            refreshTileAsync()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val preset = PresetManager(this).getPresetForSlot(slot)
        val config = preset?.tile

        if (preset == null || config == null) {
            tile.label = getString(R.string.terminal_tile_unassigned)
            tile.icon = TileNumberIcon.create("--")
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = null
            }
        } else {
            tile.label = config.label.ifBlank { preset.name }
            tile.icon = TileNumberIcon.create(
                TileNumberIcon.animationText(
                    preset.windowAnimationScale,
                    preset.transitionAnimationScale,
                    preset.animatorDurationScale
                )
            )
            val ready = canApply()
            tile.state = when {
                !ready -> Tile.STATE_UNAVAILABLE
                isCurrentlyApplied(preset.windowAnimationScale, preset.transitionAnimationScale, preset.animatorDurationScale) ->
                    Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (ready) {
                    TileNumberIcon.animationSubtitle(
                        preset.windowAnimationScale,
                        preset.transitionAnimationScale,
                        preset.animatorDurationScale
                    )
                } else {
                    getString(R.string.preset_tile_subtitle_no_permission)
                }
            }
        }
        tile.updateTile()
    }

    private fun isCurrentlyApplied(window: Float, transition: Float, animator: Float): Boolean =
        TileNumberIcon.nearlyEqual(SettingsManager.getWindowAnimationScale(contentResolver), window) &&
            TileNumberIcon.nearlyEqual(SettingsManager.getTransitionAnimationScale(contentResolver), transition) &&
            TileNumberIcon.nearlyEqual(SettingsManager.getAnimatorDurationScale(contentResolver), animator)

    private fun canApply(): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(this)

    private fun refreshTileAsync() {
        mainHandler.post { refreshTile() }
    }

    private fun setTileState(state: Int) {
        mainHandler.post {
            qsTile?.let {
                it.state = state
                it.updateTile()
            }
        }
    }

    private fun collapseQuickSettings() {
        runCatching {
            ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "collapse"))
        }
    }

    private fun toast(message: String) {
        val appContext = applicationContext
        mainHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
