package com.riyadmondol2006.customanimator.data

data class AnimatorPreset(
    val id: String,
    val name: String,
    val windowAnimationScale: Float,
    val transitionAnimationScale: Float,
    val animatorDurationScale: Float,
    val tile: PresetTileConfig? = null
)
