package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.service.WidthTileService1
import com.arslan.customanimator.service.WidthTileService2
import com.arslan.customanimator.service.WidthTileService3
import com.arslan.customanimator.service.WidthTileService4
import com.arslan.customanimator.service.WidthTileService5

object WidthTileSlots : TileSlotPool(
    listOf(
        WidthTileService1::class.java,
        WidthTileService2::class.java,
        WidthTileService3::class.java,
        WidthTileService4::class.java,
        WidthTileService5::class.java
    )
) {

    fun sync(context: Context, presetManager: WidthPresetManager) {
        sync(context, presetManager.getAllPresets().mapNotNull { it.tile?.slot }.toSet())
    }
}
