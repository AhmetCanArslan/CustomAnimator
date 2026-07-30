package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.service.AnimationTileService1
import com.arslan.customanimator.service.AnimationTileService2
import com.arslan.customanimator.service.AnimationTileService3
import com.arslan.customanimator.service.AnimationTileService4
import com.arslan.customanimator.service.AnimationTileService5

object AnimationTileSlots : TileSlotPool(
    listOf(
        AnimationTileService1::class.java,
        AnimationTileService2::class.java,
        AnimationTileService3::class.java,
        AnimationTileService4::class.java,
        AnimationTileService5::class.java
    )
) {

    fun sync(context: Context, presetManager: PresetManager) {
        sync(context, presetManager.getAllPresets().mapNotNull { it.tile?.slot }.toSet())
    }
}
