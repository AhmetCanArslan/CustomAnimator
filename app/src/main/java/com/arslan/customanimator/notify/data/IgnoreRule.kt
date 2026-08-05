package com.arslan.customanimator.notify.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class IgnoreType { APP, TITLE, BODY, TITLE_AND_BODY }

@Keep
@Parcelize
data class IgnoreRule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("type")
    val type: IgnoreType,

    @SerializedName("packageName")
    val packageName: String,

    @SerializedName("appName")
    val appName: String? = null,

    @SerializedName("matchValue")
    val matchValue: String? = null,

    @SerializedName("isRegex")
    val isRegex: Boolean = false,

    @SerializedName("matchValue2")
    val matchValue2: String? = null,

    @SerializedName("isRegex2")
    val isRegex2: Boolean = false
) : Parcelable
