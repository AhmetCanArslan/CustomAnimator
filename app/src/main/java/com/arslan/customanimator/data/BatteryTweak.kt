package com.arslan.customanimator.data

import androidx.annotation.StringRes

sealed class BatteryTweak {

    abstract val key: String
    @get:StringRes abstract val titleRes: Int
    @get:StringRes abstract val descriptionRes: Int

    data class Toggle(
        override val key: String,
        @StringRes override val titleRes: Int,
        @StringRes override val descriptionRes: Int,
        val default: Boolean
    ) : BatteryTweak()

    data class IntRange(
        override val key: String,
        @StringRes override val titleRes: Int,
        @StringRes override val descriptionRes: Int,
        val default: Int,
        val min: Int,
        val max: Int,
        val step: Int = 1
    ) : BatteryTweak()

    data class FloatRange(
        override val key: String,
        @StringRes override val titleRes: Int,
        @StringRes override val descriptionRes: Int,
        val default: Float,
        val min: Float,
        val max: Float
    ) : BatteryTweak()

    data class Choice(
        override val key: String,
        @StringRes override val titleRes: Int,
        @StringRes override val descriptionRes: Int,
        val default: Int,
        val optionLabels: List<Int>
    ) : BatteryTweak()
}

data class BatterySaverPreset(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val constants: Map<String, String>
)

data class DozePreset(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val constants: Map<String, String>
)
