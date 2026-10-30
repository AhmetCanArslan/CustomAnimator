package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.PackageManager

data class BatteryUsageSnapshot(
    val summary: Map<String, String>,
    val components: List<Pair<String, Double>>,
    val apps: List<Pair<String, Double>>,
    val appWakelocks: List<Pair<String, String>>,
    val kernelWakelocks: List<Pair<String, String>>
)

object BatteryUsageManager {

    const val KEY_COMPUTED_DRAIN = "Computed drain"
    const val KEY_ACTUAL_DRAIN = "actual drain"

    private const val TOP_LIMIT = 20
    private const val APP_UID_BASE = 10000
    private const val PER_USER_RANGE = 100000

    val SUMMARY_KEYS = listOf(
        "Battery time remaining",
        "Charge time remaining",
        "Time on battery",
        "Time on battery screen off",
        "Time on battery screen doze",
        "Total run time",
        "Discharge",
        "Screen on discharge",
        "Screen off discharge",
        "Screen doze discharge",
        "Device light doze discharge",
        "Device deep doze discharge",
        "Estimated battery capacity",
        "Min learned battery capacity",
        "Max learned battery capacity"
    )

    private val CAPACITY_REGEX = Regex("""Capacity: [\d.]+, Computed drain: ([\d.]+), actual drain: ([\d.\-]+)""")
    private val UID_REGEX = Regex("""^\s*(?:UID|Uid) (\S+): ([\d.]+)""")
    private val COMPONENT_REGEX = Regex("""^\s*([A-Za-z][\w ]*?): ([\d.]+)""")
    private val KERNEL_WAKELOCK_REGEX = Regex("""^\s*Kernel Wake lock (.+?): (.+?) realtime""")
    private val APP_WAKELOCK_REGEX = Regex("""^\s*Wake lock (\S+) (.+?): (.+?) realtime""")
    private val MILLIS_REGEX = Regex("""\s\d+ms""")
    private val PACKAGE_UID_REGEX = Regex("""package:(\S+) uid:(\d+)""")
    private val USER_APP_UID_REGEX = Regex("""u\d+a(\d+)""")

    private val SYSTEM_UIDS = mapOf(
        0 to "root",
        1000 to "Android System",
        1001 to "Phone",
        1002 to "Bluetooth",
        1010 to "Wi-Fi",
        1013 to "Media",
        1041 to "Audio",
        1047 to "Camera",
        2000 to "Shell"
    )

    fun readUsage(context: Context): BatteryUsageSnapshot {
        val raw = BatteryHealthManager.runForOutput(arrayOf("dumpsys", "batterystats", "--charged"))
        val packageList = BatteryHealthManager.runForOutput(arrayOf("cmd", "package", "list", "packages", "-U"))
        val names = UidNameResolver(context.packageManager, packageList)
        return BatteryUsageSnapshot(
            summary = parseSummary(raw),
            components = parseComponents(raw),
            apps = parseApps(raw, names::resolve),
            appWakelocks = parseAppWakelocks(raw, names::resolve),
            kernelWakelocks = parseKernelWakelocks(raw)
        )
    }

    fun parseSummary(raw: String): Map<String, String> {
        val lines = raw.lineSequence().map { it.trim() }.toList()
        val values = LinkedHashMap<String, String>()
        SUMMARY_KEYS.forEach { key ->
            lines.firstOrNull { it.startsWith("$key: ") }
                ?.let { values[key] = it.removePrefix("$key: ").replace(MILLIS_REGEX, "") }
        }
        CAPACITY_REGEX.find(raw)?.let { match ->
            values[KEY_COMPUTED_DRAIN] = "${match.groupValues[1]} mAh"
            values[KEY_ACTUAL_DRAIN] = "${match.groupValues[2]} mAh"
        }
        return values
    }

    private fun powerUseBlock(raw: String): List<String> =
        raw.lineSequence()
            .dropWhile { !it.trim().startsWith("Estimated power use") }
            .drop(1)
            .takeWhile { it.isNotBlank() }
            .toList()

    fun parseComponents(raw: String): List<Pair<String, Double>> =
        powerUseBlock(raw)
            .dropWhile { it.trim() != "Global" }
            .drop(1)
            .takeWhile { !UID_REGEX.containsMatchIn(it) }
            .mapNotNull { COMPONENT_REGEX.find(it) }
            .mapNotNull { match -> match.groupValues[2].toDoubleOrNull()?.let { match.groupValues[1] to it } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    fun parseApps(raw: String, resolve: (String) -> String): List<Pair<String, Double>> =
        powerUseBlock(raw)
            .mapNotNull { UID_REGEX.find(it) }
            .mapNotNull { match -> match.groupValues[2].toDoubleOrNull()?.let { resolve(match.groupValues[1]) to it } }
            .groupBy({ it.first }, { it.second })
            .map { (name, values) -> name to values.sum() }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(TOP_LIMIT)

    fun parseAppWakelocks(raw: String, resolve: (String) -> String): List<Pair<String, String>> =
        raw.lineSequence()
            .mapNotNull { APP_WAKELOCK_REGEX.find(it) }
            .map { match -> "${resolve(match.groupValues[1])} · ${match.groupValues[2]}" to cleanDuration(match.groupValues[3]) }
            .take(TOP_LIMIT)
            .toList()

    fun parseKernelWakelocks(raw: String): List<Pair<String, String>> =
        raw.lineSequence()
            .mapNotNull { KERNEL_WAKELOCK_REGEX.find(it) }
            .map { match -> match.groupValues[1] to cleanDuration(match.groupValues[2]) }
            .take(TOP_LIMIT)
            .toList()

    private fun cleanDuration(value: String): String = value.replace(MILLIS_REGEX, "").trim()

    internal fun appIdOf(uid: String): Int? {
        USER_APP_UID_REGEX.matchEntire(uid)?.let { return APP_UID_BASE + it.groupValues[1].toInt() }
        return uid.toIntOrNull()?.rem(PER_USER_RANGE)
    }

    private class UidNameResolver(private val packageManager: PackageManager, packageList: String) {
        private val packagesByAppId: Map<Int, String> = PACKAGE_UID_REGEX.findAll(packageList)
            .associate { it.groupValues[2].toInt() to it.groupValues[1] }
        private val cache = HashMap<String, String>()

        fun resolve(uid: String): String = cache.getOrPut(uid) { lookup(uid) }

        private fun lookup(uid: String): String {
            val appId = appIdOf(uid) ?: return uid
            SYSTEM_UIDS[appId]?.let { return it }
            val packageName = packagesByAppId[appId] ?: return uid
            return runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }
}
