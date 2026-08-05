package com.arslan.customanimator.notify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class RuleAction(
    @SerializedName("type")
    val type: RuleType,

    @SerializedName("flashPattern")
    val flashPattern: FlashPattern? = null,

    @SerializedName("customPatternId")
    val customPatternId: String? = null,

    @SerializedName("screenDurationSeconds")
    val screenDurationSeconds: Int? = null,

    @SerializedName("pocketModeEnabled")
    val pocketModeEnabled: Boolean? = null,

    @SerializedName("aodDurationSeconds")
    val aodDurationSeconds: Int? = null,

    @SerializedName("screenFlashColor")
    val screenFlashColor: String? = null,

    @SerializedName("screenFlashDurationSeconds")
    val screenFlashDurationSeconds: Int? = null,
) : Parcelable {

    companion object {
        fun flash(
            pattern: FlashPattern = FlashPattern.HEARTBEAT,
            customPatternId: String? = null,
        ) = RuleAction(
            type = RuleType.FLASH,
            flashPattern = pattern,
            customPatternId = customPatternId,
        )

        fun wakeUp(
            screenDurationSeconds: Int = 10,
            pocketModeEnabled: Boolean = true,
        ) = RuleAction(
            type = RuleType.WAKE_UP,
            screenDurationSeconds = screenDurationSeconds,
            pocketModeEnabled = pocketModeEnabled,
        )

        fun aod(durationSeconds: Int = 10) = RuleAction(
            type = RuleType.AOD,
            aodDurationSeconds = durationSeconds,
        )

        fun flashScreen(
            color: ScreenFlashColor = ScreenFlashColor.RED,
            durationSeconds: Int = 5,
        ) = RuleAction(
            type = RuleType.FLASH_SCREEN,
            screenFlashColor = color.name,
            screenFlashDurationSeconds = durationSeconds,
        )
    }
}
