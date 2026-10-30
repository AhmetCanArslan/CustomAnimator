package com.arslan.customanimator.utils

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager

data class AlarmSource(
    val packageName: String,
    val label: String,
    val triggerTime: Long
)

object AlarmRevealer {

    private val ALARM_HEADER = Regex("""Alarm\{[^}]*\s(\S+)\}""")

    fun getNextAlarm(context: Context): AlarmSource? {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        val info = manager.nextAlarmClock ?: return null
        val packageName = info.showIntent?.creatorPackage ?: return AlarmSource(
            "",
            context.getString(com.arslan.customanimator.R.string.alarm_owner_unknown),
            info.triggerTime
        )
        return AlarmSource(packageName, labelOf(context, packageName), info.triggerTime)
    }

    fun getScheduledAlarmClocks(context: Context): List<AlarmSource> {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("dumpsys", "alarm"))
        if (!result.isSuccess) return emptyList()

        val packages = mutableSetOf<String>()
        var current: String? = null
        result.output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val header = ALARM_HEADER.find(line)
            when {
                header != null -> current = header.groupValues[1]
                trimmed.isEmpty() -> current = null
                trimmed.startsWith("alarmClock") -> current?.let { packages.add(it) }
            }
        }
        return packages.sorted().map { AlarmSource(it, labelOf(context, it), 0L) }
    }

    private fun labelOf(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
