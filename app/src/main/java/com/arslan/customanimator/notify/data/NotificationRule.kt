package com.arslan.customanimator.notify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class NotificationRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("packageNames")
    val packageNames: List<String>,

    @SerializedName("appNames")
    val appNames: List<String>,

    @SerializedName("keywords")
    val keywords: List<String> = emptyList(),

    @SerializedName("titleKeywords")
    val titleKeywords: List<String> = emptyList(),

    @SerializedName("bodyKeywords")
    val bodyKeywords: List<String> = emptyList(),

    @SerializedName("actions")
    val actions: List<RuleAction>,

    @SerializedName("applyOnVibration")
    val applyOnVibration: Boolean = true,

    @SerializedName("applyOnSilent")
    val applyOnSilent: Boolean = true,

    @SerializedName("applyOnDND")
    val applyOnDND: Boolean = true,

    @SerializedName("preventMultipleNotifications")
    val preventMultipleNotifications: Boolean = false,

    @SerializedName("isEnabled")
    val isEnabled: Boolean = true,
) : Parcelable
