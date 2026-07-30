package com.arslan.customanimator.utils

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import com.arslan.customanimator.service.TerminalTileService1
import com.arslan.customanimator.service.TerminalTileService2
import com.arslan.customanimator.service.TerminalTileService3
import com.arslan.customanimator.service.TerminalTileService4
import com.arslan.customanimator.service.TerminalTileService5

object TerminalTileSlots {

    private val slotClasses = listOf(
        TerminalTileService1::class.java,
        TerminalTileService2::class.java,
        TerminalTileService3::class.java,
        TerminalTileService4::class.java,
        TerminalTileService5::class.java
    )

    fun componentFor(context: Context, slot: Int): ComponentName =
        ComponentName(context.applicationContext, slotClasses[slot])

    fun sync(context: Context, presetManager: TerminalPresetManager) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val claimed = presetManager.getAllPresets().mapNotNull { it.tile?.slot }.toSet()

        for (slot in slotClasses.indices) {
            val component = componentFor(appContext, slot)
            val wanted = if (slot in claimed) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            runCatching {
                if (packageManager.getComponentEnabledSetting(component) != wanted) {
                    packageManager.setComponentEnabledSetting(
                        component,
                        wanted,
                        PackageManager.DONT_KILL_APP
                    )
                }
                if (slot in claimed) {
                    TileService.requestListeningState(appContext, component)
                }
            }
        }
    }

    fun canRequestAdd(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAddTile(context: Context, slot: Int, label: String, iconKey: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appContext = context.applicationContext
        runCatching {
            appContext.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
                componentFor(appContext, slot),
                label,
                Icon.createWithResource(appContext, TerminalTileIcons.resFor(iconKey)),
                { runnable -> runnable.run() },
                { }
            )
        }
    }
}
