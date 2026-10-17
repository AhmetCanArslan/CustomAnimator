package com.arslan.customanimator.widget

import android.content.Context

object ShortcutWidgetState {

    private const val PREFS = "shortcut_widget_state"
    private const val KEY_PREFIX = "widget_"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bind(context: Context, widgetId: Int, shortcutId: String) {
        prefs(context).edit().putString(KEY_PREFIX + widgetId, shortcutId).apply()
    }

    fun shortcutId(context: Context, widgetId: Int): String? = prefs(context).getString(KEY_PREFIX + widgetId, null)

    fun widgetIds(context: Context, shortcutId: String): List<Int> =
        prefs(context).all.mapNotNull { (key, value) ->
            key.removePrefix(KEY_PREFIX).toIntOrNull()?.takeIf { value == shortcutId }
        }

    fun remap(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val shortcutIds = oldWidgetIds.map { shortcutId(context, it) }
        unbind(context, oldWidgetIds)
        newWidgetIds.zip(shortcutIds).forEach { (widgetId, shortcutId) ->
            if (shortcutId != null) bind(context, widgetId, shortcutId)
        }
    }

    fun unbind(context: Context, widgetIds: IntArray) {
        prefs(context).edit().apply { widgetIds.forEach { remove(KEY_PREFIX + it) } }.apply()
    }
}
