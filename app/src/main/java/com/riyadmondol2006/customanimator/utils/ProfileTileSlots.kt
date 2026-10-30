package com.riyadmondol2006.customanimator.utils

import android.content.Context
import com.riyadmondol2006.customanimator.service.ProfileTileService1
import com.riyadmondol2006.customanimator.service.ProfileTileService2
import com.riyadmondol2006.customanimator.service.ProfileTileService3
import com.riyadmondol2006.customanimator.service.ProfileTileService4
import com.riyadmondol2006.customanimator.service.ProfileTileService5

object ProfileTileSlots : TileSlotPool(
    listOf(
        ProfileTileService1::class.java,
        ProfileTileService2::class.java,
        ProfileTileService3::class.java,
        ProfileTileService4::class.java,
        ProfileTileService5::class.java
    )
) {

    fun sync(context: Context, profileManager: ProfileManager) {
        sync(context, profileManager.getAllProfiles().mapNotNull { it.tile?.slot }.toSet())
    }
}
