package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.data.HotspotSnapshot
import com.arslan.customanimator.utils.HotspotManager
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HotspotTileService : TileService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var snapshot: HotspotSnapshot? = null

    override fun onStartListening() {
        super.onStartListening()
        reload()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        reload()
    }

    override fun onClick() {
        super.onClick()
        if (!ShizukuHelper.hasShizukuPermission()) {
            toast(getString(R.string.preset_tile_needs_permission))
            refreshTile()
            return
        }
        val enable = snapshot?.isEnabled != true
        backgroundScope.launch {
            val outcome = HotspotManager.setEnabled(this@HotspotTileService, enable)
            if (outcome is HotspotManager.Outcome.Failure) {
                toast(getString(R.string.hotspot_action_failed))
            }
            delay(1200)
            snapshot = HotspotManager.readState(this@HotspotTileService)
            mainHandler.post { refreshTile() }
        }
    }

    private fun reload() {
        refreshTile()
        if (!ShizukuHelper.hasShizukuPermission()) return
        backgroundScope.launch {
            snapshot = HotspotManager.readState(this@HotspotTileService)
            mainHandler.post { refreshTile() }
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val ready = ShizukuHelper.hasShizukuPermission()
        val current = snapshot
        val active = ready && current?.isEnabled == true

        tile.label = getString(R.string.hotspot_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_hotspot)
        tile.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            active -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !ready -> getString(R.string.preset_tile_subtitle_no_permission)
                active -> current?.config?.ssid?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.hotspot_state_enabled)
                else -> getString(R.string.hotspot_state_disabled)
            }
        }
        tile.updateTile()
    }

    private fun toast(message: String) {
        mainHandler.post { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
