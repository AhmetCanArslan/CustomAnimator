package com.arslan.customanimator.utils

import android.content.Context
import android.graphics.drawable.Icon
import com.arslan.customanimator.service.ToggleTileService1
import com.arslan.customanimator.service.ToggleTileService10
import com.arslan.customanimator.service.ToggleTileService11
import com.arslan.customanimator.service.ToggleTileService12
import com.arslan.customanimator.service.ToggleTileService2
import com.arslan.customanimator.service.ToggleTileService3
import com.arslan.customanimator.service.ToggleTileService4
import com.arslan.customanimator.service.ToggleTileService5
import com.arslan.customanimator.service.ToggleTileService6
import com.arslan.customanimator.service.ToggleTileService7
import com.arslan.customanimator.service.ToggleTileService8
import com.arslan.customanimator.service.ToggleTileService9

object ToggleTileSlots : TileSlotPool(
    listOf(
        ToggleTileService1::class.java,
        ToggleTileService2::class.java,
        ToggleTileService3::class.java,
        ToggleTileService4::class.java,
        ToggleTileService5::class.java,
        ToggleTileService6::class.java,
        ToggleTileService7::class.java,
        ToggleTileService8::class.java,
        ToggleTileService9::class.java,
        ToggleTileService10::class.java,
        ToggleTileService11::class.java,
        ToggleTileService12::class.java
    )
) {

    fun sync(context: Context, manager: ToggleTileManager) {
        sync(context, manager.getTiles().map { it.slot }.toSet())
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
