package com.arslan.customanimator.data

import android.graphics.drawable.Drawable
enum class DebloatState { ACTIVE, DISABLED, REMOVED }

enum class DebloatRisk { RECOMMENDED, ADVANCED, EXPERT, UNSAFE }

enum class DebloatGroup { GOOGLE, OEM, CARRIER, PARTNER, SYSTEM }

data class DebloatAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val apkPath: String?,
    val isSystemApp: Boolean,
    val state: DebloatState,
    val risk: DebloatRisk?,
    val group: DebloatGroup?,
    val isProtected: Boolean,
    val isRestorable: Boolean,
    val isChangedByApp: Boolean,
    val description: String?
)
