package com.arslan.customanimator.notify.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.annotation.Keep
import com.arslan.customanimator.notify.widget.NotificationWidgetProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

@Keep
data class WidgetNotification(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val ruleId: String,
)

object WidgetNotificationStore {

    private const val PREFS_NAME = "notify_widget_items"
    private const val ITEMS_KEY = "items"
    private const val MAX_ITEMS = 100
    private const val DEDUP_WINDOW_MS = 3000L

    private val gson = Gson()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<WidgetNotification> {
        val json = prefs(context).getString(ITEMS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WidgetNotification>>() {}.type
            gson.fromJson<List<WidgetNotification>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(context: Context, item: WidgetNotification) {
        val current = getAll(context).toMutableList()
        val isDuplicate = current.any {
            it.packageName == item.packageName && it.title == item.title &&
                it.body == item.body && (item.timestamp - it.timestamp) < DEDUP_WINDOW_MS
        }
        if (isDuplicate) return

        current.add(0, item)
        while (current.size > MAX_ITEMS) current.removeAt(current.size - 1)
        persist(context, current)
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        persist(context, getAll(context).filterNot { it.id == id })
    }

    @Synchronized
    fun clear(context: Context) {
        persist(context, emptyList())
    }

    private fun persist(context: Context, items: List<WidgetNotification>) {
        prefs(context).edit().putString(ITEMS_KEY, gson.toJson(items)).apply()
    }

    fun notifyWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, NotificationWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        manager.notifyAppWidgetViewDataChanged(ids, com.arslan.customanimator.R.id.widget_list)
    }
}
