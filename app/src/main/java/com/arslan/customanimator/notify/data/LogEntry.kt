package com.arslan.customanimator.notify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class RuleType {
    FLASH,
    WAKE_UP,
    AOD,
    FLASH_SCREEN,
    WIDGET
}

@Keep
@Parcelize
data class MatchedRuleInfo(
    @SerializedName("ruleId")
    val ruleId: String,

    @SerializedName("ruleName")
    val ruleName: String,

    @SerializedName("ruleType")
    val ruleType: RuleType,

    @SerializedName("wasExecuted")
    val wasExecuted: Boolean
) : Parcelable

@Keep
@Parcelize
data class LogEntry(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("packageName")
    val packageName: String,

    @SerializedName("appName")
    val appName: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("matchedRules")
    val matchedRules: List<MatchedRuleInfo>
) : Parcelable
