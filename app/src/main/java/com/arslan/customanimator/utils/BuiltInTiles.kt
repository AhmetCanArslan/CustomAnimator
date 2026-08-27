package com.arslan.customanimator.utils

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arslan.customanimator.R
import com.arslan.customanimator.service.AlwaysOnDisplayTileService
import com.arslan.customanimator.service.CaffeineTileService
import com.arslan.customanimator.service.GameModeTileService
import com.arslan.customanimator.service.ScreenshotTileService

object BuiltInTiles {

    data class Entry(
        val key: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        @DrawableRes val iconRes: Int,
        val serviceClass: Class<out TileService>
    )

    val all: List<Entry> = listOf(
        Entry(
            "screenshot",
            R.string.screenshot_tile_label,
            R.string.qs_tiles_builtin_screenshot_desc,
            R.drawable.ic_tile_screenshot,
            ScreenshotTileService::class.java
        ),
        Entry(
            "game_mode",
            R.string.game_mode_tile_label,
            R.string.qs_tiles_builtin_game_mode_desc,
            R.drawable.ic_tile_videogame_asset,
            GameModeTileService::class.java
        ),
        Entry(
            "caffeine",
            R.string.caffeine_tile_label,
            R.string.qs_tiles_builtin_caffeine_desc,
            R.drawable.ic_tile_caffeine,
            CaffeineTileService::class.java
        ),
        Entry(
            "aod",
            R.string.aod_tile_label,
            R.string.qs_tiles_builtin_aod_desc,
            R.drawable.ic_tile_aod,
            AlwaysOnDisplayTileService::class.java
        )
    )

    fun canRequestAdd(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAddTile(context: Context, entry: Entry) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appContext = context.applicationContext
        runCatching {
            appContext.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
                ComponentName(appContext, entry.serviceClass),
                appContext.getString(entry.nameRes),
                Icon.createWithResource(appContext, entry.iconRes),
                { runnable -> runnable.run() },
                { }
            )
        }
    }
}
