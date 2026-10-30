package com.arslan.customanimator.widget

import android.content.Context

object BoostWidgetState {

    private const val PREFS = "boost_widget_state"
    private const val KEY_RUNNING = "running"
    private const val KEY_RESULT = "result"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun result(context: Context): String = prefs(context).getString(KEY_RESULT, "") ?: ""

    fun setResult(context: Context, result: String) {
        prefs(context).edit().putString(KEY_RESULT, result).apply()
    }
}
