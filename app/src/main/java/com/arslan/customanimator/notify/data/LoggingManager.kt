package com.arslan.customanimator.notify.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Keep
class LoggingManager private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val entries: MutableList<LogEntry> by lazy { loadFromPrefs() }

    @Synchronized
    fun logNotification(
        packageName: String,
        appName: String,
        title: String,
        body: String,
        matchedRules: List<MatchedRuleInfo>
    ) {
        val now = System.currentTimeMillis()
        val isDuplicate = entries.any { e ->
            e.packageName == packageName && e.title == title && e.body == body &&
                (now - e.timestamp) < DEDUP_WINDOW_MS
        }
        if (isDuplicate) return

        val entry = LogEntry(
            packageName = packageName,
            appName = appName,
            title = title,
            body = body,
            matchedRules = matchedRules
        )
        entries.add(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
        persist()
    }

    @Synchronized
    fun getLogs(filter: RuleType? = null): List<LogEntry> {
        val source = if (filter == null) {
            entries
        } else {
            entries.filter { entry -> entry.matchedRules.any { it.ruleType == filter } }
        }
        return source.reversed()
    }

    @Synchronized
    fun deleteLog(id: String) {
        entries.removeAll { it.id == id }
        persist()
    }

    @Synchronized
    fun clearLogs() {
        entries.clear()
        persist()
    }

    @Synchronized
    fun purgeOlderThan(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        val before = entries.size
        entries.removeAll { it.timestamp < cutoff }
        if (entries.size != before) persist()
    }

    private fun persist() {
        val json = gson.toJson(entries)
        prefs.edit().putString(LOG_ENTRIES_KEY, json).apply()
    }

    private fun loadFromPrefs(): MutableList<LogEntry> {
        val json = prefs.getString(LOG_ENTRIES_KEY, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<LogEntry>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    companion object {
        private const val PREFS_NAME = "prime_notify_logs"
        private const val LOG_ENTRIES_KEY = "log_entries_list"
        private const val MAX_ENTRIES = 500
        private const val DEDUP_WINDOW_MS = 3_000L

        @Volatile
        private var instance: LoggingManager? = null

        fun getInstance(context: Context): LoggingManager =
            instance ?: synchronized(this) {
                instance ?: LoggingManager(context.applicationContext).also { instance = it }
            }
    }
}
