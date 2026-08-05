package com.arslan.customanimator.notify.data

import android.content.Context

class LoggingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autoDeleteDays: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, value).apply()

    var onlyRuleMatched: Boolean
        get() = prefs.getBoolean(KEY_ONLY_RULE_MATCHED, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_RULE_MATCHED, value).apply()

    var showSystemApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, value).apply()

    companion object {
        private const val PREFS_NAME = "prime_notify_log_prefs"
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
        private const val KEY_ONLY_RULE_MATCHED = "only_rule_matched"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    }
}
