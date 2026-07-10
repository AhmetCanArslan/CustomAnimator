package com.arslan.customanimator.data

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)
