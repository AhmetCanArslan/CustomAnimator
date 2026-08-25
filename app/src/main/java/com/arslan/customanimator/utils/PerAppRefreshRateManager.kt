package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONObject

class PerAppRefreshRateManager(context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOverrides(): Map<String, Float> {
        return try {
            val stored = sharedPreferences.getString(KEY_OVERRIDES, "{}") ?: "{}"
            val json = JSONObject(stored)
            val result = mutableMapOf<String, Float>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optDouble(key, 0.0).toFloat()
                if (value in MIN_RATE..MAX_RATE) {
                    result[key] = value
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getRate(packageName: String): Float? = getOverrides()[packageName]

    fun setRate(packageName: String, rate: Float?) {
        val current = getOverrides().toMutableMap()
        if (rate == null || rate !in MIN_RATE..MAX_RATE) {
            current.remove(packageName)
        } else {
            current[packageName] = rate
        }
        persist(current)
    }

    fun clearAll() = persist(emptyMap())

    private fun persist(overrides: Map<String, Float>) {
        val json = JSONObject()
        overrides.forEach { (packageName, rate) -> json.put(packageName, rate.toDouble()) }
        sharedPreferences.edit().putString(KEY_OVERRIDES, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "per_app_refresh_rate"
        private const val KEY_OVERRIDES = "overrides"
        const val MIN_RATE = 24f
        const val MAX_RATE = 240f
    }
}
