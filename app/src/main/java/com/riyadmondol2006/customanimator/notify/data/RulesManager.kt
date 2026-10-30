package com.riyadmondol2006.customanimator.notify.data

import android.content.Context
import com.riyadmondol2006.customanimator.utils.ParsedPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RulesManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("prime_notify_rules", Context.MODE_PRIVATE)

    private val CUSTOM_PATTERNS_KEY = "custom_patterns_list"
    private val HAS_PROXIMITY_SENSOR_KEY = "has_proximity_sensor"

    fun hasProximitySensor(): Boolean {
        if (!prefs.contains(HAS_PROXIMITY_SENSOR_KEY)) {
            val hasSensor = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_PROXIMITY)
            prefs.edit().putBoolean(HAS_PROXIMITY_SENSOR_KEY, hasSensor).apply()
        }
        return prefs.getBoolean(HAS_PROXIMITY_SENSOR_KEY, true)
    }

    fun getCustomPatterns(): List<CustomPattern> {
        val json = prefs.getString(CUSTOM_PATTERNS_KEY, null) ?: return emptyList()
        return parsedPatterns.get(json) {
            try {
                val listType = object : TypeToken<List<CustomPattern>>() {}.type
                gson.fromJson<List<CustomPattern>>(it, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
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

    fun getRules(): List<NotificationRule> = ruleSet().all

    fun enabledRulesFor(packageName: String): List<NotificationRule> =
        ruleSet().enabledByPackage[packageName].orEmpty()

    private fun ruleSet(): RuleSet {
        val json = prefs.getString(NOTIFICATION_RULES_KEY, null) ?: return RuleSet.EMPTY
        return parsedRules.get(json) {
            val stored = try {
                val type = object : TypeToken<List<NotificationRule>>() {}.type
                gson.fromJson<List<NotificationRule>>(it, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val sanitized = sanitize(stored)
            if (sanitized != stored) {
                prefs.edit().putString(NOTIFICATION_RULES_KEY, gson.toJson(sanitized)).apply()
            }
            RuleSet(sanitized)
        }
    }

    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun sanitize(rules: List<NotificationRule?>?): List<NotificationRule> {
        if (rules == null) return emptyList()
        return rules.filterNotNull()
            .filter { it.id != null && it.packageNames != null }
            .map { rule ->
                val rawActions: List<RuleAction?>? = rule.actions
                val actions = rawActions.orEmpty().filterNotNull().filter { it.type != null }
                val clean = rule.copy(
                    appNames = rule.appNames ?: emptyList(),
                    keywords = rule.keywords ?: emptyList(),
                    titleKeywords = rule.titleKeywords ?: emptyList(),
                    bodyKeywords = rule.bodyKeywords ?: emptyList(),
                    actions = actions,
                )
                if (clean == rule) rule else clean
            }
            .filter { it.actions.isNotEmpty() }
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
        val lastExecution = lastExecutions[ruleId] ?: return false
        return (System.currentTimeMillis() - lastExecution) < THROTTLE_WINDOW_MS
    }

    fun updateRuleExecutionTime(ruleId: String) {
        lastExecutions[ruleId] = System.currentTimeMillis()
    }

    private class RuleSet(val all: List<NotificationRule>) {
        val enabledByPackage: Map<String, List<NotificationRule>> by lazy {
            val index = HashMap<String, MutableList<NotificationRule>>()
            all.forEach { rule ->
                if (rule.isEnabled) {
                    rule.packageNames.distinct().forEach { index.getOrPut(it) { ArrayList() }.add(rule) }
                }
            }
            index
        }

        companion object {
            val EMPTY = RuleSet(emptyList())
        }
    }

    private companion object {
        const val THROTTLE_WINDOW_MS = 10000L
        val gson = Gson()
        val parsedRules = ParsedPref<RuleSet>()
        val parsedPatterns = ParsedPref<List<CustomPattern>>()
        val lastExecutions = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }
}
