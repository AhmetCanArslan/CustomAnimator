package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONObject

class PerAppDpiManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOverrides(): Map<String, Int> {
        return try {
            val stored = sharedPreferences.getString(KEY_OVERRIDES, "{}") ?: "{}"
            val json = JSONObject(stored)
            val result = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optInt(key, 0)
                if (value in MIN_DPI..MAX_DPI) {
                    result[key] = value
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getDpi(packageName: String): Int? = getOverrides()[packageName]

    fun setDpi(packageName: String, dpi: Int?) {
        val current = getOverrides().toMutableMap()
        if (dpi == null || dpi !in MIN_DPI..MAX_DPI) {
            current.remove(packageName)
        } else {
            current[packageName] = dpi
        }
        persist(current)
    }

    fun clearAll() = persist(emptyMap())

    private fun persist(overrides: Map<String, Int>) {
        val json = JSONObject()
        overrides.forEach { (packageName, dpi) -> json.put(packageName, dpi) }
        sharedPreferences.edit().putString(KEY_OVERRIDES, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "per_app_dpi"
        private const val KEY_OVERRIDES = "overrides"
        const val MIN_DPI = 72
        const val MAX_DPI = 1000
    }
}
