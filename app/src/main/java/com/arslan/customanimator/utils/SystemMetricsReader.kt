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

    fun cpuFreqMhz(): Int? {
        val policyFreqs = CPU_FREQ_POLICY_DIR.listFiles()
            ?.filter { it.name.startsWith("policy") }
            ?.mapNotNull { readFile(File(it, "scaling_cur_freq").absolutePath)?.toLongOrNull() }
            ?: emptyList()
        val freqs = if (policyFreqs.isNotEmpty()) {
            policyFreqs
        } else {
            (0 until Runtime.getRuntime().availableProcessors()).mapNotNull {
                readFile("/sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq")?.toLongOrNull()
            }
        }
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
        val zones = THERMAL_DIR.listFiles()?.filter { it.name.startsWith("thermal_zone") } ?: return null
        val matching = zones.firstOrNull { zone ->
            val type = readFile(File(zone, "type").absolutePath)?.lowercase() ?: return@firstOrNull false
            CPU_ZONE_KEYWORDS.any { type.contains(it) }
        } ?: return null
        val raw = readFile(File(matching, "temp").absolutePath)?.toLongOrNull() ?: return null
        return if (raw > 1000L) (raw / 1000L).toInt() else raw.toInt()
    }

    private fun batteryIntent(context: Context): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun batteryTempC(context: Context): Int? {
        val tenths = batteryIntent(context)
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE } ?: return null
        return (tenths / 10f).roundToInt()
    }

    fun batteryLevel(context: Context): Int? {
        val intent = batteryIntent(context) ?: return null
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
