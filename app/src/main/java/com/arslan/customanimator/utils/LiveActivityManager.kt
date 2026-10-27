package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object LiveActivityManager {

    private const val PREFS_NAME = "live_activity_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val RESUMED_ACTIVITY_SCRIPT =
        "dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity'"
    private const val TOP_RESUMED_KEY = "topResumedActivity"

    private val COMPONENT_PATTERN = Regex("""\s([A-Za-z0-9_.]+)/([A-Za-z0-9_.$]+)""")

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun canRun(context: Context): Boolean =
        ShizukuHelper.hasShizukuPermission() && Settings.canDrawOverlays(context)

    fun resumedActivity(): ComponentName? {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", RESUMED_ACTIVITY_SCRIPT))
        return if (result.isSuccess) parseResumedActivity(result.output) else null
    }

    fun parseResumedActivity(output: String): ComponentName? {
        val lines = output.lines()
        val line = lines.firstOrNull { it.contains(TOP_RESUMED_KEY) } ?: lines.firstOrNull() ?: return null
        val match = COMPONENT_PATTERN.find(line) ?: return null
        val (packageName, className) = match.destructured
        val fullClassName = if (className.startsWith(".")) packageName + className else className
        return ComponentName(packageName, fullClassName)
    }
}
