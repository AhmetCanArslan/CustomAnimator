package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.arslan.customanimator.HomeScreen
import com.arslan.customanimator.HomeTab
import com.arslan.customanimator.R
import com.arslan.customanimator.SearchTarget
import com.arslan.customanimator.data.ShortcutType

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
