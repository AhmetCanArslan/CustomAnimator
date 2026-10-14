package com.arslan.customanimator.screenshot

import android.content.Context

class ScreenshotPrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var notificationDelaySeconds: Int
        get() = sp.getInt(KEY_DELAY, DEFAULT_DELAY)
        set(value) = sp.edit().putInt(KEY_DELAY, value).apply()

    var notificationAutoDismissSeconds: Int
        get() = sp.getInt(KEY_AUTO_DISMISS, DEFAULT_AUTO_DISMISS)
        set(value) = sp.edit().putInt(KEY_AUTO_DISMISS, value).apply()

    var watcherEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    var notificationShowCopy: Boolean
        get() = sp.getBoolean(KEY_NOTIF_COPY, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIF_COPY, value).apply()

    var notificationShowDelete: Boolean
        get() = sp.getBoolean(KEY_NOTIF_DELETE, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIF_DELETE, value).apply()

    var notificationShowPreview: Boolean
        get() = sp.getBoolean(KEY_NOTIF_PREVIEW, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIF_PREVIEW, value).apply()

    var overlayEnabled: Boolean
        get() = sp.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var overlayShowCopy: Boolean
        get() = sp.getBoolean(KEY_OVERLAY_COPY, true)
        set(value) = sp.edit().putBoolean(KEY_OVERLAY_COPY, value).apply()

    var overlayShowDelete: Boolean
        get() = sp.getBoolean(KEY_OVERLAY_DELETE, true)
        set(value) = sp.edit().putBoolean(KEY_OVERLAY_DELETE, value).apply()

    var overlayTimeoutSeconds: Int
        get() = sp.getInt(KEY_OVERLAY_TIMEOUT, DEFAULT_OVERLAY_TIMEOUT)
        set(value) = sp.edit().putInt(KEY_OVERLAY_TIMEOUT, value).apply()

    var showConfirmationToasts: Boolean
        get() = sp.getBoolean(KEY_CONFIRMATION_TOASTS, true)
        set(value) = sp.edit().putBoolean(KEY_CONFIRMATION_TOASTS, value).apply()

    var folderUri: String?
        get() = sp.getString(KEY_FOLDER, null)
        set(value) = sp.edit().putString(KEY_FOLDER, value).apply()

    var overlayX: Int
        get() = sp.getInt(KEY_OVERLAY_X, DEFAULT_OVERLAY_X)
        set(value) = sp.edit().putInt(KEY_OVERLAY_X, value).apply()

    var overlayY: Int
        get() = sp.getInt(KEY_OVERLAY_Y, DEFAULT_OVERLAY_Y)
        set(value) = sp.edit().putInt(KEY_OVERLAY_Y, value).apply()

    companion object {
        private const val NAME = "screenshot_actions_prefs"
        private const val KEY_DELAY = "notification_delay_seconds"
        private const val KEY_AUTO_DISMISS = "notification_auto_dismiss_seconds"
        private const val KEY_ENABLED = "watcher_enabled"
        private const val KEY_NOTIF_COPY = "notification_show_copy"
        private const val KEY_NOTIF_DELETE = "notification_show_delete"
        private const val KEY_NOTIF_PREVIEW = "notification_show_preview"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_OVERLAY_COPY = "overlay_show_copy"
        private const val KEY_OVERLAY_DELETE = "overlay_show_delete"
        private const val KEY_OVERLAY_TIMEOUT = "overlay_timeout_seconds"
        private const val KEY_CONFIRMATION_TOASTS = "show_confirmation_toasts"
        private const val KEY_FOLDER = "screenshot_folder"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"

        const val DEFAULT_DELAY = 2
        const val DELAY_MAX = 10

        const val AUTO_DISMISS_OFF = 0
        const val DEFAULT_AUTO_DISMISS = AUTO_DISMISS_OFF
        const val AUTO_DISMISS_MAX = 60

        const val DEFAULT_OVERLAY_TIMEOUT = 5
        const val OVERLAY_TIMEOUT_MIN = 1
        const val OVERLAY_TIMEOUT_MAX = 30

        const val DEFAULT_OVERLAY_X = 190
        const val DEFAULT_OVERLAY_Y = 54
        const val OVERLAY_POSITION_MAX = 400
    }
}
