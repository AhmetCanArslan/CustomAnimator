package com.arslan.customanimator.notify.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson

@Keep
data class WidgetConfig(
    val ruleIds: List<String> = emptyList(),
    val maxItems: Int = 10,
    val showHeader: Boolean = true,
    val headerText: String = "",
    val showAppIcon: Boolean = true,
    val showAppName: Boolean = true,
    val showBody: Boolean = true,
    val showTime: Boolean = true,
    val backgroundColor: Long = 0xFF1C1B1F,
    val backgroundAlphaPercent: Int = 85,
    val textColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFF9C27B0,
    val cornerRadiusDp: Int = 20,
    val textSizeSp: Int = 14,
) {
    val backgroundColorWithAlpha: Int
        get() {
            val alpha = (backgroundAlphaPercent.coerceIn(0, 100) * 255 / 100)
            return (alpha shl 24) or (backgroundColor.toInt() and 0x00FFFFFF)
        }
}

object WidgetConfigStore {

    private const val PREFS_NAME = "notify_widget_configs"
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int) = "config_$appWidgetId"

    fun load(context: Context, appWidgetId: Int): WidgetConfig {
        val json = prefs(context).getString(key(appWidgetId), null) ?: return WidgetConfig()
        return try {
            gson.fromJson(json, WidgetConfig::class.java) ?: WidgetConfig()
        } catch (_: Exception) {
            WidgetConfig()
        }
    }

    fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        prefs(context).edit().putString(key(appWidgetId), gson.toJson(config)).apply()
    }

    fun delete(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(key(appWidgetId)).apply()
    }
}
