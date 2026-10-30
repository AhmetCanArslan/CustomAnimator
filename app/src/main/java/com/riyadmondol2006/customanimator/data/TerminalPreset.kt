package com.riyadmondol2006.customanimator.data

data class TerminalPreset(
    val id: String,
    val name: String,
    val command: String,
    val tile: TerminalTileConfig? = null
)
