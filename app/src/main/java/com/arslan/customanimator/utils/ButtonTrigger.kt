package com.arslan.customanimator.utils

enum class ButtonTrigger(val prefsKey: String, val presses: Int = 0) {
    POWER_LONG_PRESS("assist"),
    POWER_DOUBLE_PRESS("double_press", 2),
    POWER_TRIPLE_PRESS("triple_press", 3),
    VOLUME_COMBO("volume_combo"),
    VOLUME_UP_DOUBLE("volume_up_double", 2),
    VOLUME_DOWN_DOUBLE("volume_down_double", 2),
    VOLUME_UP_TRIPLE("volume_up_triple", 3),
    VOLUME_DOWN_TRIPLE("volume_down_triple", 3),
    VOLUME_UP_LONG("volume_up_long"),
    VOLUME_DOWN_LONG("volume_down_long");

    companion object {
        val power: List<ButtonTrigger> = listOf(POWER_DOUBLE_PRESS, POWER_TRIPLE_PRESS)

        val volume: List<ButtonTrigger> = listOf(
            VOLUME_COMBO,
            VOLUME_UP_DOUBLE,
            VOLUME_DOWN_DOUBLE,
            VOLUME_UP_TRIPLE,
            VOLUME_DOWN_TRIPLE,
            VOLUME_UP_LONG,
            VOLUME_DOWN_LONG
        )

        val service: List<ButtonTrigger> = power + volume

        fun fromName(name: String?): ButtonTrigger? = entries.firstOrNull { it.name == name }
    }
}
