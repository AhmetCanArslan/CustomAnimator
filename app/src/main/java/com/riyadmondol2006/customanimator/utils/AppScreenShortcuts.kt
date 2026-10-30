package com.riyadmondol2006.customanimator.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.riyadmondol2006.customanimator.HomeScreen
import com.riyadmondol2006.customanimator.HomeTab
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.SearchTarget
import com.riyadmondol2006.customanimator.data.ShortcutType

object AppScreenShortcuts {

    private class Destination(val target: SearchTarget, val labelRes: Int, val iconRes: Int)

    private val destinations = listOf(
        Destination(SearchTarget.Tab(HomeTab.TOOLS), R.string.tools_title, R.drawable.ic_tile_handyman),
        Destination(SearchTarget.Screen(HomeScreen.SHORTCUTS), R.string.shortcuts, R.drawable.ic_tile_shortcut),
        Destination(SearchTarget.Tab(HomeTab.OPTIMIZER), R.string.optimizer_title, R.drawable.ic_tile_bolt),
        Destination(SearchTarget.Tab(HomeTab.TERMINAL), R.string.nav_terminal, R.drawable.ic_tile_terminal)
    )

    fun publish(context: Context) {
        val color = ShortcutMaker.glyphColors[ShortcutType.APP_SCREEN.ordinal]
        val shortcuts = destinations.map { destination ->
            val label = context.getString(destination.labelRes)
            ShortcutInfoCompat.Builder(context, destination.target.key)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(IconCompat.createWithAdaptiveBitmap(ShortcutMaker.glyphIcon(context, destination.iconRes, color)))
                .setIntent(ShortcutMaker.screenIntent(context, destination.target.key).setAction(Intent.ACTION_VIEW))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
