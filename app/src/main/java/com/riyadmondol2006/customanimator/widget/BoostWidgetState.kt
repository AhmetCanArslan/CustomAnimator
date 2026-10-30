package com.riyadmondol2006.customanimator.widget

import android.content.Context

object BoostWidgetState {

    private const val PREFS = "boost_widget_state"
    private const val KEY_RUNNING = "running"
    private const val KEY_RESULT = "result"
    private const val KEY_STARTED_AT = "started_at"
    private const val STALE_AFTER_MS = 2 * 60 * 1000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRunning(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_RUNNING, false)) return false
        val age = System.currentTimeMillis() - prefs.getLong(KEY_STARTED_AT, 0L)
        return age in 0..STALE_AFTER_MS
    }

    fun setRunning(context: Context, running: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_RUNNING, running)
            .putLong(KEY_STARTED_AT, if (running) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun result(context: Context): String = prefs(context).getString(KEY_RESULT, "") ?: ""

    fun setResult(context: Context, result: String) {
        prefs(context).edit().putString(KEY_RESULT, result).apply()
    }
}
