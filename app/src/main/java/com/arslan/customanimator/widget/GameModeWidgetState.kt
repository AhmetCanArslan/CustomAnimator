package com.arslan.customanimator.widget

import android.content.Context

object GameModeWidgetState {

    private const val PREFS = "game_mode_widget_state"
    private const val KEY_RUNNING = "running"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_RUNNING, running).apply()
    }
}
