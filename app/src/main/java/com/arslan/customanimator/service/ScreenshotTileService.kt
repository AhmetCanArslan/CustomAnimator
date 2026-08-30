package com.arslan.customanimator.service

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import com.arslan.customanimator.R
import com.arslan.customanimator.screenshot.ScreenshotActionActivity
import com.arslan.customanimator.screenshot.ScreenshotPermissions

class ScreenshotTileService : BaseTileService() {

    override suspend fun loadSpec(): TileSpec = TileSpec(
        label = getString(R.string.screenshot_tile_label),
        icon = TileIcon.Res(R.drawable.ic_tile_screenshot),
        state = if (ScreenshotPermissions.hasImagesPermission(this)) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_UNAVAILABLE
        }
    )

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
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
