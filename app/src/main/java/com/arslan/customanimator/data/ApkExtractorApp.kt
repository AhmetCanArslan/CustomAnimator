package com.arslan.customanimator.data

import android.graphics.drawable.Drawable

data class ApkExtractorApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val versionName: String,
    val sizeBytes: Long,
    val isSplit: Boolean
)

enum class ApkExtractorFormat { BUNDLE, BASE, SEPARATE }
