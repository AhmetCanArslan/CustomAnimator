package com.arslan.customanimator.data

import android.graphics.drawable.Drawable

enum class ShortcutType {
    APP, ACTIVITY, FILE, LINK, COMMAND
}

data class ShortcutEntry(
    val id: String,
    val type: ShortcutType,
    val label: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val uri: String? = null,
    val mimeType: String? = null,
    val command: String? = null,
    val viaShizuku: Boolean = false
) {
    val needsShizuku: Boolean get() = viaShizuku || type == ShortcutType.COMMAND
}

data class ShortcutActivityInfo(
    val name: String,
    val label: String,
    val exported: Boolean,
    val permission: String?
) {
    val needsRoot: Boolean get() = !exported
    val needsShizuku: Boolean get() = !exported || permission != null
}

data class ShortcutHandlerInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?
)

data class ShortcutFileInfo(
    val name: String,
    val mimeType: String?
)
