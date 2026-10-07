package com.arslan.customanimator.notify.data

import android.content.Context
import com.arslan.customanimator.utils.ParsedPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IgnoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("prime_notify_ignore", Context.MODE_PRIVATE)
    private val KEY = "ignore_rules"

    fun getRules(): List<IgnoreRule> = ruleSet().all

    fun rulesFor(packageName: String): List<IgnoreRule> = ruleSet().byPackage[packageName].orEmpty()

    private fun ruleSet(): RuleSet {
        val json = prefs.getString(KEY, null) ?: return RuleSet.EMPTY
        return parsedRules.get(json) {
            try {
                val type = object : TypeToken<List<IgnoreRule>>() {}.type
                RuleSet(gson.fromJson<List<IgnoreRule>>(it, type) ?: emptyList())
            } catch (e: Exception) {
                RuleSet.EMPTY
            }
        }
    }

    fun addRule(rule: IgnoreRule) {
        val current = getRules().toMutableList()
        val duplicate = current.any {
            it.type == rule.type &&
                it.packageName == rule.packageName &&
                it.matchValue == rule.matchValue &&
                it.matchValue2 == rule.matchValue2
        }
        if (!duplicate) {
            current.add(rule)
            save(current)
        }
    }

    fun removeRule(id: String) {
        val current = getRules().toMutableList()
        current.removeAll { it.id == id }
        save(current)
    }

    fun updateRule(rule: IgnoreRule) {
        val current = getRules().toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index != -1) {
            current[index] = rule
            save(current)
        }
    }

    private fun save(rules: List<IgnoreRule>) {
        prefs.edit().putString(KEY, gson.toJson(rules)).apply()
    }

    fun isIgnored(packageName: String, title: String, body: String): Boolean =
        IgnoreMatcher.isIgnored(getRules(), packageName, title, body)

    private class RuleSet(val all: List<IgnoreRule>) {
        val byPackage: Map<String, List<IgnoreRule>> by lazy { all.groupBy { it.packageName } }

        companion object {
            val EMPTY = RuleSet(emptyList())
        }
    }

    private companion object {
        val gson = Gson()
        val parsedRules = ParsedPref<RuleSet>()
    }
}
