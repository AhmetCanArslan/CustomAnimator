package com.arslan.customanimator.notify.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IgnoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("prime_notify_ignore", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY = "ignore_rules"

    fun getRules(): List<IgnoreRule> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<IgnoreRule>>() {}.type
            gson.fromJson<List<IgnoreRule>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
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
}
