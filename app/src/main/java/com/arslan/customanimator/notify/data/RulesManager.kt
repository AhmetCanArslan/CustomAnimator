package com.arslan.customanimator.notify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RulesManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("prime_notify_rules", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val CUSTOM_PATTERNS_KEY = "custom_patterns_list"
    private val HAS_PROXIMITY_SENSOR_KEY = "has_proximity_sensor"
    private val RULE_THROTTLE_KEY = "rule_throttle_"

    fun hasProximitySensor(): Boolean {
        if (!prefs.contains(HAS_PROXIMITY_SENSOR_KEY)) {
            val hasSensor = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_PROXIMITY)
            prefs.edit().putBoolean(HAS_PROXIMITY_SENSOR_KEY, hasSensor).apply()
        }
        return prefs.getBoolean(HAS_PROXIMITY_SENSOR_KEY, true)
    }

    fun getCustomPatterns(): List<CustomPattern> {
        val json = prefs.getString(CUSTOM_PATTERNS_KEY, null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<CustomPattern>>() {}.type
            gson.fromJson<List<CustomPattern>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomPattern(pattern: CustomPattern) {
        val current = getCustomPatterns().toMutableList()
        val index = current.indexOfFirst { it.id == pattern.id }
        if (index != -1) current[index] = pattern else current.add(pattern)
        prefs.edit().putString(CUSTOM_PATTERNS_KEY, gson.toJson(current)).apply()
    }

    fun removeCustomPattern(id: String) {
        val current = getCustomPatterns().toMutableList()
        current.removeAll { it.id == id }
        prefs.edit().putString(CUSTOM_PATTERNS_KEY, gson.toJson(current)).apply()
    }

    private val NOTIFICATION_RULES_KEY = "notification_rules_list"

    fun getRules(): List<NotificationRule> {
        val json = prefs.getString(NOTIFICATION_RULES_KEY, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<NotificationRule>>() {}.type
            gson.fromJson<List<NotificationRule>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<NotificationRule>) {
        prefs.edit().putString(NOTIFICATION_RULES_KEY, gson.toJson(rules)).apply()
    }

    fun addRule(rule: NotificationRule) {
        val current = getRules().toMutableList()
        current.add(rule)
        saveRules(current)
    }

    fun removeRule(ruleId: String) {
        val current = getRules().toMutableList()
        current.removeAll { it.id == ruleId }
        saveRules(current)
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        val current = getRules().toMutableList()
        val index = current.indexOfFirst { it.id == ruleId }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = isEnabled)
            saveRules(current)
        }
    }

    fun updateRule(updatedRule: NotificationRule) {
        val current = getRules().toMutableList()
        val index = current.indexOfFirst { it.id == updatedRule.id }
        if (index != -1) {
            current[index] = updatedRule
            saveRules(current)
        }
    }

    fun shouldThrottleRule(ruleId: String): Boolean {
        val lastExecution = prefs.getLong("$RULE_THROTTLE_KEY$ruleId", 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastExecution) < 10000L
    }

    fun updateRuleExecutionTime(ruleId: String) {
        prefs.edit().putLong("$RULE_THROTTLE_KEY$ruleId", System.currentTimeMillis()).apply()
    }
}
