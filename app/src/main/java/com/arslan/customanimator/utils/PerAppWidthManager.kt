package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONObject

class PerAppWidthManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyDpiOverrides()
    }

    fun getOverrides(): Map<String, Int> {
        return try {
            val stored = sharedPreferences.getString(KEY_OVERRIDES, "{}") ?: "{}"
            val json = JSONObject(stored)
            val result = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optInt(key, 0)
                if (value in MIN_WIDTH..MAX_WIDTH) {
                    result[key] = value
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getWidth(packageName: String): Int? = getOverrides()[packageName]

    fun setWidth(packageName: String, widthDp: Int?) {
        val current = getOverrides().toMutableMap()
        if (widthDp == null || widthDp !in MIN_WIDTH..MAX_WIDTH) {
            current.remove(packageName)
        } else {
            current[packageName] = widthDp
        }
        persist(current)
    }

    fun clearAll() = persist(emptyMap())

    private fun persist(overrides: Map<String, Int>) {
        val json = JSONObject()
        overrides.forEach { (packageName, widthDp) -> json.put(packageName, widthDp) }
        sharedPreferences.edit().putString(KEY_OVERRIDES, json.toString()).apply()
    }

    private fun migrateLegacyDpiOverrides() {
        val legacyPreferences = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val stored = legacyPreferences.getString(KEY_OVERRIDES, null) ?: return
        try {
            val json = JSONObject(stored)
            val migrated = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val density = json.optInt(key, 0)
                if (density < SettingsManager.MIN_DENSITY || density > SettingsManager.MAX_DENSITY) continue
                val widthDp = SettingsManager.smallestWidthForDensity(appContext, density)
                if (widthDp in MIN_WIDTH..MAX_WIDTH) {
                    migrated[key] = widthDp
                }
            }
            if (migrated.isNotEmpty()) {
                val existing = getOverrides().toMutableMap()
                migrated.forEach { (packageName, widthDp) -> existing.putIfAbsent(packageName, widthDp) }
                persist(existing)
            }
        } catch (e: Exception) {
            Unit
        }
        legacyPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "per_app_width"
        private const val LEGACY_PREFS_NAME = "per_app_dpi"
        private const val KEY_OVERRIDES = "overrides"
        const val MIN_WIDTH = SettingsManager.MIN_SMALLEST_WIDTH
        const val MAX_WIDTH = SettingsManager.MAX_SMALLEST_WIDTH
    }
}
