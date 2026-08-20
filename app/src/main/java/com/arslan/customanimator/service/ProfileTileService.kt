package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.ProfileApplier
import com.arslan.customanimator.utils.ProfileManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.TerminalTileIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class ProfileTileService : TileService() {

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

        val profile = ProfileManager(this).getProfileForSlot(slot)
        val config = profile?.tile
        if (profile == null || config == null) {
            toast(getString(R.string.terminal_tile_unassigned_message))
            refreshTile()
            return
        }

        if (!ProfileApplier.canApply(this)) {
            toast(getString(R.string.preset_tile_needs_permission))
            refreshTile()
            return
        }

        val label = config.label.ifBlank { profile.name }
        backgroundScope.launch {
            if (config.collapsePanel) {
                collapseQuickSettings()
            }

            val result = ProfileApplier.apply(this@ProfileTileService, profile)

            if (config.showToast) {
                val message = if (result.failed == 0) {
                    getString(R.string.profile_tile_toast_applied, label)
                } else {
                    getString(R.string.profile_tile_toast_partial, label, result.applied, result.total)
                }
                toast(message)
            }
            setTileState(Tile.STATE_INACTIVE)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val profile = ProfileManager(this).getProfileForSlot(slot)
        val config = profile?.tile

        if (profile == null || config == null) {
            tile.label = getString(R.string.terminal_tile_unassigned)
            tile.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(ProfileManager.DEFAULT_ICON_KEY))
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = null
            }
        } else {
            tile.label = config.label.ifBlank { profile.name }
            tile.icon = Icon.createWithResource(this, TerminalTileIcons.resFor(profile.iconKey))
            val ready = ProfileApplier.canApply(this)
            tile.state = if (ready) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (ready) {
                    resources.getQuantityString(
                        R.plurals.profile_action_count,
                        profile.actionCount,
                        profile.actionCount
                    )
                } else {
                    getString(R.string.preset_tile_subtitle_no_permission)
                }
            }
        }
        tile.updateTile()
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
