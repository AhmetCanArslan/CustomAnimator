package com.riyadmondol2006.customanimator.utils

import android.content.Context
import com.riyadmondol2006.customanimator.service.TerminalTileService1
import com.riyadmondol2006.customanimator.service.TerminalTileService2
import com.riyadmondol2006.customanimator.service.TerminalTileService3
import com.riyadmondol2006.customanimator.service.TerminalTileService4
import com.riyadmondol2006.customanimator.service.TerminalTileService5

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
}
