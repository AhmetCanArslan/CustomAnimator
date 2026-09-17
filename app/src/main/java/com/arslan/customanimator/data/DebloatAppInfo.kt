package com.arslan.customanimator.data

import android.graphics.drawable.Drawable
import com.arslan.customanimator.utils.DebloatCatalog

enum class DebloatState { ACTIVE, DISABLED, REMOVED }

data class DebloatAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val apkPath: String?,
    val isSystemApp: Boolean,
    val state: DebloatState,
    val risk: DebloatCatalog.Risk,
    val group: DebloatCatalog.Group,
    val isProtected: Boolean,
    val isRestorable: Boolean,
    val isChangedByApp: Boolean,
    val description: String?
)
