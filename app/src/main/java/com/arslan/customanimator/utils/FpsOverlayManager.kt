package com.arslan.customanimator.utils

import android.content.Context
import android.net.Uri
import android.provider.Settings

enum class SystemMeterMetric(val key: String, val defaultEnabled: Boolean) {
    FPS("fps", true),
    CPU_FREQ("cpu_freq", false),
    CPU_TEMP("cpu_temp", false),
    GPU_FREQ("gpu_freq", false),
    RAM("ram", false),
    BATTERY_LEVEL("battery_level", false),
    BATTERY_TEMP("battery_temp", false),
    BATTERY_CURRENT("battery_current", false)
}

object FpsOverlayManager {

    private const val PREFS_NAME = "fps_overlay_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_POSITION_X = "position_x"
    private const val KEY_POSITION_Y = "position_y"
    private const val KEY_METRIC_PREFIX = "metric_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isMetricEnabled(context: Context, metric: SystemMeterMetric): Boolean =
        prefs(context).getBoolean(KEY_METRIC_PREFIX + metric.key, metric.defaultEnabled)

    fun setMetricEnabled(context: Context, metric: SystemMeterMetric, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_METRIC_PREFIX + metric.key, enabled).apply()
    }

    fun enabledMetrics(context: Context): List<SystemMeterMetric> =
        SystemMeterMetric.entries.filter { isMetricEnabled(context, it) }

    fun getPosition(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(KEY_POSITION_X, 0) to p.getInt(KEY_POSITION_Y, 120)
    }

    fun setPosition(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt(KEY_POSITION_X, x).putInt(KEY_POSITION_Y, y).apply()
    }

    fun canDrawOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isActive(context: Context): Boolean = isEnabled(context) && canDrawOverlay(context)

    fun overlayPermissionIntent(context: Context) = android.content.Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
}
