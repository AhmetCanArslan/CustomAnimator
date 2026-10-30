package com.arslan.customanimator.utils

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arslan.customanimator.R
import com.arslan.customanimator.service.AlwaysOnDisplayTileService
import com.arslan.customanimator.service.CaffeineTileService
import com.arslan.customanimator.service.GameModeTileService
import com.arslan.customanimator.service.RestartSystemUiTileService
import com.arslan.customanimator.service.ScreenshotTileService
import com.arslan.customanimator.service.SoundTileService

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
            "restart_system_ui",
            R.string.restart_system_ui_tile_label,
            R.string.qs_tiles_builtin_restart_system_ui_desc,
            R.drawable.ic_tile_restart_alt,
            RestartSystemUiTileService::class.java
        ),
        Entry(
            "sound",
            R.string.sound_tile_label,
            R.string.qs_tiles_builtin_sound_desc,
            R.drawable.ic_tile_volume_up,
            SoundTileService::class.java
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

    fun isEnabled(context: Context, entry: Entry): Boolean {
        val appContext = context.applicationContext
        val state = appContext.packageManager.getComponentEnabledSetting(component(appContext, entry))
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setEnabled(context: Context, entry: Entry, enabled: Boolean) {
        val appContext = context.applicationContext
        val wanted = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            appContext.packageManager.setComponentEnabledSetting(
                component(appContext, entry),
                wanted,
                PackageManager.DONT_KILL_APP
            )
            if (enabled) {
                TileService.requestListeningState(appContext, component(appContext, entry))
            }
        }
    }

    fun requestAddTile(context: Context, entry: Entry) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appContext = context.applicationContext
        runCatching {
            appContext.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
                component(appContext, entry),
                appContext.getString(entry.nameRes),
                Icon.createWithResource(appContext, entry.iconRes),
                { runnable -> runnable.run() },
                { }
            )
        }
    }

    private fun component(context: Context, entry: Entry): ComponentName =
        ComponentName(context, entry.serviceClass)
}
