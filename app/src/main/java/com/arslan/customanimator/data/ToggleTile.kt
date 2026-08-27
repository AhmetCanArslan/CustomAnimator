package com.arslan.customanimator.data

data class ToggleTile(
    val id: String,
    val presetKey: String,
    val label: String,
    val onCommand: String,
    val offCommand: String,
    val readCommand: String,
    val onValue: String,
    val iconKey: String,
    val slot: Int,
    val collapsePanel: Boolean,
    val showToast: Boolean
)
