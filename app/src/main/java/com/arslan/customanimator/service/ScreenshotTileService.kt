package com.arslan.customanimator.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.arslan.customanimator.R
import com.arslan.customanimator.screenshot.ScreenshotActionActivity
import com.arslan.customanimator.screenshot.ScreenshotPermissions

/**
 * Quick Settings tile that copies the newest screenshot to the clipboard,
 * without any overlay or watcher running.
 */
class ScreenshotTileService : TileService() {

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
        val intent = ScreenshotActionActivity.intent(
            this,
            ScreenshotActionActivity.ACTION_COPY,
            -1L,
            -1
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.screenshot_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_screenshot)
        tile.state = if (ScreenshotPermissions.hasImagesPermission(this)) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_UNAVAILABLE
        }
        tile.updateTile()
    }
}
