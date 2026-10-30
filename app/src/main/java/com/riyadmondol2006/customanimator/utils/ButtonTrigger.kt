package com.riyadmondol2006.customanimator.utils

enum class ButtonTrigger(val prefsKey: String) {
    POWER_LONG_PRESS("assist"),
    POWER_DOUBLE_PRESS("double_press"),
    VOLUME_COMBO("volume_combo"),
    VOLUME_UP_DOUBLE("volume_up_double"),
    VOLUME_DOWN_DOUBLE("volume_down_double"),
    VOLUME_UP_LONG("volume_up_long"),
    VOLUME_DOWN_LONG("volume_down_long");

    companion object {
        val volume: List<ButtonTrigger> =
            listOf(VOLUME_COMBO, VOLUME_UP_DOUBLE, VOLUME_DOWN_DOUBLE, VOLUME_UP_LONG, VOLUME_DOWN_LONG)

        val service: List<ButtonTrigger> = listOf(POWER_DOUBLE_PRESS) + volume

        fun fromName(name: String?): ButtonTrigger? = entries.firstOrNull { it.name == name }
    }
}
