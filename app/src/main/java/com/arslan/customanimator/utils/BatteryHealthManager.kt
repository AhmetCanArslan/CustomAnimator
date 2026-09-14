package com.arslan.customanimator.utils

data class BatteryHealthSnapshot(
    val service: Map<String, String>,
    val supplies: Map<String, Map<String, String>>,
    val healthHal: Map<String, String>,
    val thermalStatus: Int?,
    val temperatures: List<Pair<String, Float>>,
    val doze: Map<String, String>
) {
    val supply: Map<String, String> get() = supplies["battery"].orEmpty()
    val isEmpty: Boolean get() = service.isEmpty() && supplies.isEmpty()
}

object BatteryHealthManager {

    private const val SUPPLY_PREFIX = "POWER_SUPPLY_"
    private const val SUPPLY_MARKER = "##"
    private const val SUPPLIES_SCRIPT =
        "for f in /sys/class/power_supply/*/uevent; do d=\${f%/uevent}; echo \"##\${d##*/}\"; cat \"\$f\" 2>/dev/null; done; true"
    private const val HEALTH_HAL = "android.hardware.health.IHealth/default"
    private const val MISSING_SERVICE = "Can't find service"
    private const val MICRO = 1_000_000.0

    private val TEMPERATURE_REGEX = Regex("""Temperature\{mValue=(-?[\d.]+), mType=-?\d+, mName=([^,]+),""")
    private val THERMAL_STATUS_REGEX = Regex("""Thermal Status: (\d+)""")
    private val DOZE_REGEX = Regex(
        """^\s*(mState|mLightState|mDeepEnabled|mLightEnabled|mForceIdle|mScreenOn|mCharging)=(\S+)""",
        RegexOption.MULTILINE
    )

    fun readSnapshot(): BatteryHealthSnapshot {
        val thermal = runForOutput(arrayOf("dumpsys", "thermalservice"))
        return BatteryHealthSnapshot(
            service = parseColonPairs(runForOutput(arrayOf("dumpsys", "battery"))),
            supplies = parseSupplies(runForOutput(arrayOf("sh", "-c", SUPPLIES_SCRIPT))),
            healthHal = parseColonPairs(runForOutput(arrayOf("dumpsys", HEALTH_HAL))),
            thermalStatus = THERMAL_STATUS_REGEX.find(thermal)?.groupValues?.get(1)?.toIntOrNull(),
            temperatures = parseTemperatures(thermal),
            doze = parseDoze(runForOutput(arrayOf("dumpsys", "deviceidle")))
        )
    }

    internal fun runForOutput(command: Array<String>): String {
        val result = ShizukuHelper.executeShellCommandWithOutput(command)
        if (!result.isSuccess || result.output.startsWith(MISSING_SERVICE)) return ""
        return result.output
    }

    fun parseColonPairs(raw: String): Map<String, String> =
        raw.lineSequence()
            .map { it.trim() }
            .mapNotNull { line -> splitPair(line, ": ") }
            .toMap(LinkedHashMap())

    fun parseSupplies(raw: String): Map<String, Map<String, String>> {
        val result = LinkedHashMap<String, MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        for (line in raw.lineSequence().map { it.trim() }) {
            if (line.startsWith(SUPPLY_MARKER)) {
                current = LinkedHashMap<String, String>().also { result[line.removePrefix(SUPPLY_MARKER)] = it }
                continue
            }
            splitPair(line.removePrefix(SUPPLY_PREFIX), "=")?.let { current?.put(it.first, it.second) }
        }
        return result.filterValues { it.isNotEmpty() }
    }

    fun parseTemperatures(raw: String): List<Pair<String, Float>> =
        TEMPERATURE_REGEX.findAll(raw)
            .mapNotNull { match -> match.groupValues[1].toFloatOrNull()?.let { match.groupValues[2] to it } }
            .distinctBy { it.first }
            .toList()

    fun parseDoze(raw: String): Map<String, String> =
        DOZE_REGEX.findAll(raw)
            .distinctBy { it.groupValues[1] }
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun splitPair(line: String, separator: String): Pair<String, String>? {
        val index = line.indexOf(separator)
        if (index <= 0) return null
        return line.substring(0, index).trim() to line.substring(index + separator.length).trim()
    }

    fun fullCapacityMicroAh(snapshot: BatteryHealthSnapshot): Long? =
        snapshot.supply["CHARGE_FULL"]?.toLongOrNull()?.takeIf { it > 0 }

    fun designCapacityMicroAh(snapshot: BatteryHealthSnapshot): Long? =
        snapshot.supply["CHARGE_FULL_DESIGN"]?.toLongOrNull()?.takeIf { it > 0 }

    fun healthPercent(snapshot: BatteryHealthSnapshot): Int? {
        val full = fullCapacityMicroAh(snapshot) ?: return null
        val design = designCapacityMicroAh(snapshot) ?: return null
        return (full * 100 / design).toInt()
    }

    fun maxChargingWatts(snapshot: BatteryHealthSnapshot): Double? {
        val current = snapshot.service["Max charging current"]?.toLongOrNull() ?: return null
        val voltage = snapshot.service["Max charging voltage"]?.toLongOrNull() ?: return null
        return (current / MICRO) * (voltage / MICRO)
    }
}
