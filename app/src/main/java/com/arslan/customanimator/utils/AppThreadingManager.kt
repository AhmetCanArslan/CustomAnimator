package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONObject

enum class ThreadAffinityMode(val value: String) {
    ALL("all"),
    BIG("big"),
    LITTLE("little");

    companion object {
        fun fromValue(value: String?): ThreadAffinityMode =
            entries.firstOrNull { it.value == value } ?: ALL
    }
}

enum class ThreadPriority(val value: String, val nice: Int) {
    HIGH("high", -10),
    NORMAL("normal", 0),
    LOW("low", 10);

    companion object {
        fun fromValue(value: String?): ThreadPriority =
            entries.firstOrNull { it.value == value } ?: NORMAL
    }
}

data class AppThreadingConfig(
    val affinity: ThreadAffinityMode = ThreadAffinityMode.ALL,
    val priority: ThreadPriority = ThreadPriority.NORMAL
) {
    val isDefault: Boolean
        get() = affinity == ThreadAffinityMode.ALL && priority == ThreadPriority.NORMAL
}

data class ThreadingApplyResult(
    val affinityApplied: Boolean = false,
    val priorityApplied: Boolean = false,
    val priorityDenied: Boolean = false
) {
    val isSuccess: Boolean
        get() = affinityApplied || priorityApplied
}

class AppThreadingManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfigs(): Map<String, AppThreadingConfig> {
        return try {
            val stored = sharedPreferences.getString(KEY_CONFIGS, "{}") ?: "{}"
            val json = JSONObject(stored)
            val result = mutableMapOf<String, AppThreadingConfig>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = json.optJSONObject(key) ?: continue
                val config = AppThreadingConfig(
                    affinity = ThreadAffinityMode.fromValue(entry.optString(FIELD_AFFINITY)),
                    priority = ThreadPriority.fromValue(entry.optString(FIELD_PRIORITY))
                )
                if (!config.isDefault) result[key] = config
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getConfig(packageName: String): AppThreadingConfig =
        getConfigs()[packageName] ?: AppThreadingConfig()

    fun setConfig(packageName: String, config: AppThreadingConfig) {
        val current = getConfigs().toMutableMap()
        if (config.isDefault) current.remove(packageName) else current[packageName] = config
        persist(current)
    }

    fun clearAll() = persist(emptyMap())

    private fun persist(configs: Map<String, AppThreadingConfig>) {
        val json = JSONObject()
        configs.forEach { (packageName, config) ->
            json.put(
                packageName,
                JSONObject()
                    .put(FIELD_AFFINITY, config.affinity.value)
                    .put(FIELD_PRIORITY, config.priority.value)
            )
        }
        sharedPreferences.edit().putString(KEY_CONFIGS, json.toString()).apply()
    }

    fun apply(packageName: String, config: AppThreadingConfig): ThreadingApplyResult =
        apply(config, runningPids(packageName))

    fun apply(config: AppThreadingConfig, pids: List<String>): ThreadingApplyResult {
        if (pids.isEmpty()) return ThreadingApplyResult()
        val mask = CpuTopology.affinityMask(config.affinity)
        var affinityApplied = false
        var priorityApplied = false
        var priorityDenied = false
        pids.forEach { pid ->
            if (ShizukuHelper.executeShellCommand(arrayOf("taskset", "-ap", mask, pid))) {
                affinityApplied = true
            }
            val priority = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("renice", "-n", config.priority.nice.toString(), "-p", pid)
            )
            if (priority.isSuccess) {
                priorityApplied = true
            } else if (config.priority.nice < 0) {
                priorityDenied = true
            }
        }
        return ThreadingApplyResult(affinityApplied, priorityApplied, priorityDenied)
    }

    fun runningPids(packageName: String): List<String> {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", "ps -Ao PID,ARGS"))
        if (!result.isSuccess) return emptyList()
        return result.output.lineSequence()
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                val process = parts[1]
                val matches = process == packageName ||
                    process.startsWith("$packageName:") ||
                    process.startsWith("$packageName/")
                if (matches) pid.toString() else null
            }
            .toList()
    }

    private companion object {
        const val PREFS_NAME = "app_threading"
        const val KEY_CONFIGS = "configs"
        const val FIELD_AFFINITY = "affinity"
        const val FIELD_PRIORITY = "priority"
    }
}

object CpuTopology {

    @Volatile
    private var cachedFrequencies: List<Long>? = null

    private val maxFrequencies: List<Long>
        get() {
            cachedFrequencies?.let { return it }
            val read = readMaxFrequencies()
            if (read.isNotEmpty()) cachedFrequencies = read
            return read
        }

    val coreCount: Int get() = maxFrequencies.size

    fun bigCoreCount(): Int {
        val frequencies = maxFrequencies
        if (frequencies.isEmpty()) return 0
        val top = frequencies.max()
        return frequencies.count { it == top }
    }

    fun littleCoreCount(): Int {
        val frequencies = maxFrequencies
        if (frequencies.isEmpty()) return 0
        val bottom = frequencies.min()
        return frequencies.count { it == bottom }
    }

    fun affinityMask(mode: ThreadAffinityMode): String {
        val frequencies = maxFrequencies
        if (frequencies.isEmpty()) {
            val count = Runtime.getRuntime().availableProcessors()
            return java.lang.Long.toHexString((1L shl count) - 1)
        }
        val top = frequencies.max()
        val bottom = frequencies.min()
        var mask = 0L
        frequencies.forEachIndexed { index, frequency ->
            val selected = when (mode) {
                ThreadAffinityMode.ALL -> true
                ThreadAffinityMode.BIG -> frequency == top
                ThreadAffinityMode.LITTLE -> frequency == bottom
            }
            if (selected) mask = mask or (1L shl index)
        }
        if (mask == 0L) mask = (1L shl frequencies.size) - 1
        return java.lang.Long.toHexString(mask)
    }

    private fun readMaxFrequencies(): List<Long> {
        val count = Runtime.getRuntime().availableProcessors()
        val frequencies = mutableListOf<Long>()
        for (index in 0 until count) {
            val result = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("cat", "/sys/devices/system/cpu/cpu$index/cpufreq/cpuinfo_max_freq")
            )
            val frequency = result.output.trim().toLongOrNull()
            if (!result.isSuccess || frequency == null || frequency <= 0L) return emptyList()
            frequencies.add(frequency)
        }
        return frequencies
    }
}
