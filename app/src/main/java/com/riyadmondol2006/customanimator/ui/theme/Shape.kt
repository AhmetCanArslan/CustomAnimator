package com.riyadmondol2006.customanimator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
)

object AppShapes {
    val card = RoundedCornerShape(30.dp)
    val segmentFirst = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
    val segmentMiddle = RoundedCornerShape(10.dp)
    val segmentLast = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
    val chip = RoundedCornerShape(50)
    val field = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val iconBadge = RoundedCornerShape(16.dp)

    fun segment(index: Int, count: Int): Shape = when {
        count == 1 -> card
        index == 0 -> segmentFirst
        index == count - 1 -> segmentLast
        else -> segmentMiddle
    }
}
