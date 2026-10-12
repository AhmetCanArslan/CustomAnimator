package com.arslan.customanimator.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File
import kotlin.math.roundToInt

object SystemMetricsReader {

    private val CPU_FREQ_POLICY_DIR = File("/sys/devices/system/cpu/cpufreq")

    private val GPU_FREQ_PATHS = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        "/sys/kernel/gpu/gpu_clock",
        "/sys/devices/platform/gpusysfs/gpu_clock"
    )

    private val THERMAL_DIR = File("/sys/class/thermal")

    private val CPU_ZONE_KEYWORDS = listOf("cpu", "soc", "tsens", "ap", "mtktscpu", "big")

    private fun readFile(path: String): String? =
        runCatching { File(path).readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

    private val policyFreqPaths: List<String> by lazy {
        CPU_FREQ_POLICY_DIR.listFiles()
            ?.filter { it.name.startsWith("policy") }
            ?.map { File(it, "scaling_cur_freq").absolutePath }
            ?: emptyList()
    }

    private val coreFreqPaths: List<String> by lazy {
        (0 until Runtime.getRuntime().availableProcessors()).map {
            "/sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq"
        }
    }

    private val cpuTempPath: String? by lazy {
        THERMAL_DIR.listFiles()
            ?.filter { it.name.startsWith("thermal_zone") }
            ?.firstOrNull { zone ->
                val type = readFile(File(zone, "type").absolutePath)?.lowercase() ?: return@firstOrNull false
                CPU_ZONE_KEYWORDS.any { type.contains(it) }
            }
            ?.let { File(it, "temp").absolutePath }
    }

    private fun readFreqs(paths: List<String>): List<Long> = paths.mapNotNull { readFile(it)?.toLongOrNull() }

    fun cpuFreqMhz(): Int? {
        val freqs = readFreqs(policyFreqPaths).ifEmpty { readFreqs(coreFreqPaths) }
        val max = freqs.maxOrNull() ?: return null
        return (max / 1000L).toInt()
    }

    fun gpuFreqMhz(): Int? {
        val raw = GPU_FREQ_PATHS.firstNotNullOfOrNull { readFile(it) }?.toLongOrNull() ?: return null
        return when {
            raw > 10_000_000L -> (raw / 1_000_000L).toInt()
            raw > 10_000L -> (raw / 1000L).toInt()
            else -> raw.toInt()
        }
    }

    fun cpuTempC(): Int? {
        val raw = cpuTempPath?.let(::readFile)?.toLongOrNull() ?: return null
        return if (raw > 1000L) (raw / 1000L).toInt() else raw.toInt()
    }

    fun batteryStatus(context: Context): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun batteryTempC(status: Intent?): Int? {
        val tenths = status
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE } ?: return null
        return (tenths / 10f).roundToInt()
    }

    fun batteryLevel(status: Intent?): Int? {
        val intent = status ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100f / scale).roundToInt()
    }

    fun batteryCurrentMa(context: Context): Int? {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val micro = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (micro == Int.MIN_VALUE || micro == 0) return null
        return micro / 1000
    }

    fun ramUsedMb(context: Context): Pair<Int, Int>? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        if (info.totalMem <= 0L) return null
        val totalMb = (info.totalMem / (1024L * 1024L)).toInt()
        val usedMb = ((info.totalMem - info.availMem) / (1024L * 1024L)).toInt()
        return usedMb to totalMb
    }
}
