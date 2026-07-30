package com.arslan.customanimator.data

data class TerminalTileConfig(
    val slot: Int,
    val label: String,
    val iconKey: String,
    val showToast: Boolean = true,
    val collapsePanel: Boolean = true
)
