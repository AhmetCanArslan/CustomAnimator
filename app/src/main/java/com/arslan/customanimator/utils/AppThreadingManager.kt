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

    fun applyAll(): Int = getConfigs().count { (packageName, config) -> apply(packageName, config) }

    fun apply(packageName: String, config: AppThreadingConfig): Boolean {
        val pids = runningPids(packageName)
        if (pids.isEmpty()) return false
        val mask = CpuTopology.affinityMask(config.affinity)
        var applied = false
        pids.forEach { pid ->
            val affinity = ShizukuHelper.executeShellCommand(
                arrayOf("taskset", "-ap", mask, pid)
            )
            val priority = ShizukuHelper.executeShellCommand(
                arrayOf("renice", "-n", config.priority.nice.toString(), "-p", pid)
            )
            if (affinity || priority) applied = true
        }
        return applied
    }

    fun isRunning(packageName: String): Boolean = runningPids(packageName).isNotEmpty()

    private fun runningPids(packageName: String): List<String> {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("pidof", packageName))
        if (!result.isSuccess) return emptyList()
        return result.output.trim().split(Regex("\\s+")).filter { it.toIntOrNull() != null }
    }

    private companion object {
        const val PREFS_NAME = "app_threading"
        const val KEY_CONFIGS = "configs"
        const val FIELD_AFFINITY = "affinity"
        const val FIELD_PRIORITY = "priority"
    }
}

object CpuTopology {

    private val maxFrequencies: List<Long> by lazy { readMaxFrequencies() }

    val coreCount: Int get() = maxFrequencies.size

    fun bigCoreCount(): Int {
        if (maxFrequencies.isEmpty()) return 0
        val top = maxFrequencies.max()
        return maxFrequencies.count { it == top }
    }

    fun littleCoreCount(): Int {
        if (maxFrequencies.isEmpty()) return 0
        val bottom = maxFrequencies.min()
        return maxFrequencies.count { it == bottom }
    }

    fun affinityMask(mode: ThreadAffinityMode): String {
        val frequencies = maxFrequencies
        if (frequencies.isEmpty()) return "f"
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

    fun hasClusters(): Boolean = maxFrequencies.distinct().size > 1

    private fun readMaxFrequencies(): List<Long> {
        val count = Runtime.getRuntime().availableProcessors()
        val frequencies = mutableListOf<Long>()
        for (index in 0 until count) {
            val result = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("cat", "/sys/devices/system/cpu/cpu$index/cpufreq/cpuinfo_max_freq")
            )
            val frequency = result.output.trim().toLongOrNull()
            if (result.isSuccess && frequency != null) frequencies.add(frequency) else frequencies.add(0L)
        }
        return frequencies
    }
}
