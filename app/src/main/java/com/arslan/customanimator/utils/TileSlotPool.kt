package com.arslan.customanimator.utils

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService

abstract class TileSlotPool(private val slotClasses: List<Class<out TileService>>) {

    val slotCount: Int get() = slotClasses.size

    fun componentFor(context: Context, slot: Int): ComponentName =
        ComponentName(context.applicationContext, slotClasses[slot])

    fun sync(context: Context, claimedSlots: Set<Int>) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        for (slot in slotClasses.indices) {
            val component = componentFor(appContext, slot)
            val wanted = if (slot in claimedSlots) {
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
                if (slot in claimedSlots) {
                    TileService.requestListeningState(appContext, component)
                }
            }
        }
    }

    fun canRequestAdd(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAddTile(context: Context, slot: Int, label: String, icon: Icon) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val appContext = context.applicationContext
        runCatching {
            appContext.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
                componentFor(appContext, slot),
                label,
                icon,
                { runnable -> runnable.run() },
                { }
            )
        }
    }
}
