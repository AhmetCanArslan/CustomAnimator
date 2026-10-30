package com.riyadmondol2006.customanimator.utils

import android.content.Context
import com.riyadmondol2006.customanimator.service.ToggleTileService1
import com.riyadmondol2006.customanimator.service.ToggleTileService10
import com.riyadmondol2006.customanimator.service.ToggleTileService11
import com.riyadmondol2006.customanimator.service.ToggleTileService12
import com.riyadmondol2006.customanimator.service.ToggleTileService2
import com.riyadmondol2006.customanimator.service.ToggleTileService3
import com.riyadmondol2006.customanimator.service.ToggleTileService4
import com.riyadmondol2006.customanimator.service.ToggleTileService5
import com.riyadmondol2006.customanimator.service.ToggleTileService6
import com.riyadmondol2006.customanimator.service.ToggleTileService7
import com.riyadmondol2006.customanimator.service.ToggleTileService8
import com.riyadmondol2006.customanimator.service.ToggleTileService9

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
}
