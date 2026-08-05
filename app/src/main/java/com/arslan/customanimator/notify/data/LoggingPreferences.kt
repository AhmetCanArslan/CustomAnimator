package com.arslan.customanimator.notify.data

import android.content.Context

class LoggingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autoDeleteDays: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, value).apply()

    companion object {
        private const val PREFS_NAME = "prime_notify_log_prefs"
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    }
}
