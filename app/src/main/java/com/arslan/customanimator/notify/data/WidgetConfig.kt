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
    val backgroundAlphaPercent: Int = 85,
    val cornerRadiusDp: Int = 16,
    val textSizeSp: Int = 14,
)

object WidgetConfigStore {

    private const val PREFS_NAME = "notify_widget_configs"
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int) = "config_$appWidgetId"

    fun load(context: Context, appWidgetId: Int): WidgetConfig {
        val json = prefs(context).getString(key(appWidgetId), null) ?: return WidgetConfig()
        return try {
            gson.fromJson(json, WidgetConfig::class.java)?.sanitized() ?: WidgetConfig()
        } catch (_: Exception) {
            WidgetConfig()
        }
    }

    @Suppress("USELESS_ELVIS")
    private fun WidgetConfig.sanitized() = copy(
        ruleIds = ruleIds ?: emptyList(),
        headerText = headerText ?: "",
        maxItems = maxItems.coerceIn(1, 50),
        backgroundAlphaPercent = backgroundAlphaPercent.coerceIn(0, 100),
        textSizeSp = textSizeSp.coerceIn(10, 22),
    )

    fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        prefs(context).edit().putString(key(appWidgetId), gson.toJson(config)).apply()
    }

    fun delete(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(key(appWidgetId)).apply()
    }
}
