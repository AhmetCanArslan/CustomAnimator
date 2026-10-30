package com.arslan.customanimator.utils

import android.content.Context

class BatteryAlertPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("battery_alerts", Context.MODE_PRIVATE)

    var lowEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOW_ENABLED, value).apply()

    var lowLevel: Int
        get() = prefs.getInt(KEY_LOW_LEVEL, 20)
        set(value) = prefs.edit().putInt(KEY_LOW_LEVEL, value.coerceIn(5, 50)).apply()

    var highEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_ENABLED, value).apply()

    var highLevel: Int
        get() = prefs.getInt(KEY_HIGH_LEVEL, 80)
        set(value) = prefs.edit().putInt(KEY_HIGH_LEVEL, value.coerceIn(50, 100)).apply()

    var repeatAlerts: Boolean
        get() = prefs.getBoolean(KEY_REPEAT, false)
        set(value) = prefs.edit().putBoolean(KEY_REPEAT, value).apply()

    var lowNotified: Boolean
        get() = prefs.getBoolean(KEY_LOW_NOTIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOW_NOTIFIED, value).apply()

    var highNotified: Boolean
        get() = prefs.getBoolean(KEY_HIGH_NOTIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_NOTIFIED, value).apply()

    val anyEnabled: Boolean
        get() = lowEnabled || highEnabled

    companion object {
        private const val KEY_LOW_ENABLED = "low_enabled"
        private const val KEY_LOW_LEVEL = "low_level"
        private const val KEY_HIGH_ENABLED = "high_enabled"
        private const val KEY_HIGH_LEVEL = "high_level"
        private const val KEY_REPEAT = "repeat_alerts"
        private const val KEY_LOW_NOTIFIED = "low_notified"
        private const val KEY_HIGH_NOTIFIED = "high_notified"
    }
}
