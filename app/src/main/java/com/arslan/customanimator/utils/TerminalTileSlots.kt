package com.arslan.customanimator.utils

import android.content.Context
import android.graphics.drawable.Icon
import com.arslan.customanimator.service.TerminalTileService1
import com.arslan.customanimator.service.TerminalTileService2
import com.arslan.customanimator.service.TerminalTileService3
import com.arslan.customanimator.service.TerminalTileService4
import com.arslan.customanimator.service.TerminalTileService5

object TerminalTileSlots : TileSlotPool(
    listOf(
        TerminalTileService1::class.java,
        TerminalTileService2::class.java,
        TerminalTileService3::class.java,
        TerminalTileService4::class.java,
        TerminalTileService5::class.java
    )
) {

    fun sync(context: Context, presetManager: TerminalPresetManager) {
        sync(context, presetManager.getAllPresets().mapNotNull { it.tile?.slot }.toSet())
    }

    fun requestAddTile(context: Context, slot: Int, label: String, iconKey: String) {
        requestAddTile(
            context,
            slot,
            label,
            Icon.createWithResource(context.applicationContext, TerminalTileIcons.resFor(iconKey))
        )
    }
}
