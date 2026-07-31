package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.arslan.customanimator.R
import com.arslan.customanimator.data.BatterySaverPreset
import com.arslan.customanimator.data.BatteryTweak
import com.arslan.customanimator.data.DozePreset

/**
 * Reads and writes the battery / battery-saver knobs that Android exposes through
 * Settings.Global. Every key here was verified against a real device: writing it changes what
 * `dumpsys power` or `dumpsys deviceidle` reports.
 */
object BatteryTweaksManager {

    const val KEY_LOW_POWER = "low_power"
    const val KEY_LOW_POWER_TRIGGER = "low_power_trigger_level"
    const val KEY_LOW_POWER_STICKY = "low_power_sticky"
    const val KEY_STICKY_AUTO_DISABLE_ENABLED = "low_power_sticky_auto_disable_enabled"
    const val KEY_STICKY_AUTO_DISABLE_LEVEL = "low_power_sticky_auto_disable_level"
    const val KEY_AUTOMATIC_POWER_SAVE_MODE = "automatic_power_save_mode"
    const val KEY_DYNAMIC_POWER_SAVINGS_ENABLED = "dynamic_power_savings_enabled"
    const val KEY_DYNAMIC_POWER_SAVINGS_THRESHOLD = "dynamic_power_savings_disable_threshold"

    const val KEY_BATTERY_SAVER_CONSTANTS = "battery_saver_constants"
    const val KEY_DEVICE_IDLE_CONSTANTS = "device_idle_constants"

    const val KEY_APP_STANDBY = "app_standby_enabled"
    const val KEY_ADAPTIVE_BATTERY = "adaptive_battery_management_enabled"
    const val KEY_APP_AUTO_RESTRICTION = "app_auto_restriction_enabled"
    const val KEY_CACHED_APPS_FREEZER = "cached_apps_freezer"
    const val KEY_BLE_SCAN_ALWAYS = "ble_scan_always_enabled"

    const val KEY_ADAPTIVE_CHARGING = "adaptive_charging_enabled"
    const val KEY_CHARGING_SOUNDS = "charging_sounds_enabled"
    const val KEY_CHARGING_VIBRATION = "charging_vibration_enabled"

    /**
     * Keys inside [KEY_BATTERY_SAVER_CONSTANTS]. These names changed in Android 10 — the widely
     * copied older names (vibration_disabled, gps_mode, soundtrigger_disabled, aod_disabled) are
     * silently ignored by the current parser, so only the names below have any effect.
     */
    val policyTweaks: List<BatteryTweak> = listOf(
        BatteryTweak.Toggle(
            "disable_vibration", R.string.bt_disable_vibration, R.string.bt_disable_vibration_desc, false
        ),
        BatteryTweak.Toggle(
            "disable_animation", R.string.bt_disable_animation, R.string.bt_disable_animation_desc, false
        ),
        BatteryTweak.Toggle(
            "enable_firewall", R.string.bt_enable_firewall, R.string.bt_enable_firewall_desc, true
        ),
        BatteryTweak.Toggle(
            "enable_datasaver", R.string.bt_enable_datasaver, R.string.bt_enable_datasaver_desc, false
        ),
        BatteryTweak.Toggle(
            "force_all_apps_standby", R.string.bt_force_all_apps_standby,
            R.string.bt_force_all_apps_standby_desc, true
        ),
        BatteryTweak.Toggle(
            "force_background_check", R.string.bt_force_background_check,
            R.string.bt_force_background_check_desc, true
        ),
        BatteryTweak.Toggle(
            "enable_quick_doze", R.string.bt_quick_doze, R.string.bt_quick_doze_desc, true
        ),
        BatteryTweak.Toggle(
            "disable_optional_sensors", R.string.bt_optional_sensors,
            R.string.bt_optional_sensors_desc, true
        ),
        BatteryTweak.Toggle(
            "disable_aod", R.string.bt_disable_aod, R.string.bt_disable_aod_desc, true
        ),
        BatteryTweak.Toggle(
            "enable_night_mode", R.string.bt_night_mode, R.string.bt_night_mode_desc, true
        ),
        BatteryTweak.Toggle(
            "disable_launch_boost", R.string.bt_launch_boost, R.string.bt_launch_boost_desc, true
        ),
        BatteryTweak.Toggle(
            "defer_full_backup", R.string.bt_defer_full_backup, R.string.bt_defer_full_backup_desc, true
        ),
        BatteryTweak.Toggle(
            "defer_keyvalue_backup", R.string.bt_defer_kv_backup, R.string.bt_defer_kv_backup_desc, true
        ),
        BatteryTweak.Toggle(
            "advertise_is_enabled", R.string.bt_advertise, R.string.bt_advertise_desc, true
        ),
        BatteryTweak.Toggle(
            "enable_brightness_adjustment", R.string.bt_brightness_adjust,
            R.string.bt_brightness_adjust_desc, false
        ),
        BatteryTweak.FloatRange(
            "adjust_brightness_factor", R.string.bt_brightness_factor,
            R.string.bt_brightness_factor_desc, 0.5f, 0.1f, 1.0f
        ),
        BatteryTweak.Choice(
            "location_mode", R.string.bt_location_mode, R.string.bt_location_mode_desc, 3,
            listOf(
                R.string.bt_location_no_change,
                R.string.bt_location_gps_off_screen_off,
                R.string.bt_location_all_off_screen_off,
                R.string.bt_location_foreground_only,
                R.string.bt_location_throttle
            )
        ),
        BatteryTweak.Choice(
            "soundtrigger_mode", R.string.bt_soundtrigger, R.string.bt_soundtrigger_desc, 1,
            listOf(
                R.string.bt_soundtrigger_all,
                R.string.bt_soundtrigger_critical,
                R.string.bt_soundtrigger_none
            )
        )
    )

    val saverPresets: List<BatterySaverPreset> = listOf(
        BatterySaverPreset(
            "default", R.string.bt_preset_default, R.string.bt_preset_default_desc, emptyMap()
        ),
        BatterySaverPreset(
            "light", R.string.bt_preset_light, R.string.bt_preset_light_desc, mapOf(
                "disable_vibration" to "false",
                "disable_animation" to "false",
                "enable_firewall" to "false",
                "enable_datasaver" to "false",
                "location_mode" to "0",
                "force_all_apps_standby" to "false",
                "force_background_check" to "false",
                "disable_optional_sensors" to "false",
                "disable_aod" to "false",
                "enable_quick_doze" to "false",
                "enable_night_mode" to "false",
                "enable_brightness_adjustment" to "false"
            )
        ),
        BatterySaverPreset(
            "balanced", R.string.bt_preset_balanced, R.string.bt_preset_balanced_desc, mapOf(
                "disable_vibration" to "false",
                "disable_animation" to "true",
                "enable_firewall" to "true",
                "enable_datasaver" to "false",
                "location_mode" to "3",
                "force_all_apps_standby" to "true",
                "force_background_check" to "true",
                "disable_optional_sensors" to "false",
                "disable_aod" to "true",
                "enable_quick_doze" to "true",
                "enable_night_mode" to "true",
                "enable_brightness_adjustment" to "true",
                "adjust_brightness_factor" to "0.7"
            )
        ),
        BatterySaverPreset(
            "aggressive", R.string.bt_preset_aggressive, R.string.bt_preset_aggressive_desc, mapOf(
                "disable_vibration" to "true",
                "disable_animation" to "true",
                "enable_firewall" to "true",
                "enable_datasaver" to "true",
                "location_mode" to "4",
                "force_all_apps_standby" to "true",
                "force_background_check" to "true",
                "disable_optional_sensors" to "true",
                "disable_aod" to "true",
                "enable_quick_doze" to "true",
                "enable_night_mode" to "true",
                "disable_launch_boost" to "true",
                "defer_full_backup" to "true",
                "defer_keyvalue_backup" to "true",
                "soundtrigger_mode" to "1",
                "enable_brightness_adjustment" to "true",
                "adjust_brightness_factor" to "0.5"
            )
        ),
        BatterySaverPreset(
            "extreme", R.string.bt_preset_extreme, R.string.bt_preset_extreme_desc, mapOf(
                "disable_vibration" to "true",
                "disable_animation" to "true",
                "enable_firewall" to "true",
                "enable_datasaver" to "true",
                "location_mode" to "2",
                "force_all_apps_standby" to "true",
                "force_background_check" to "true",
                "disable_optional_sensors" to "true",
                "disable_aod" to "true",
                "enable_night_mode" to "true",
                "enable_quick_doze" to "true",
                "disable_launch_boost" to "true",
                "defer_full_backup" to "true",
                "defer_keyvalue_backup" to "true",
                "soundtrigger_mode" to "2",
                "enable_brightness_adjustment" to "true",
                "adjust_brightness_factor" to "0.3"
            )
        )
    )

    val dozePresets: List<DozePreset> = listOf(
        DozePreset("default", R.string.bt_doze_default, R.string.bt_doze_default_desc, emptyMap()),
        DozePreset(
            "aggressive", R.string.bt_doze_aggressive, R.string.bt_doze_aggressive_desc, mapOf(
                "inactive_to" to "10000",
                "light_after_inactive_to" to "60000",
                "light_idle_to" to "120000",
                "light_max_idle_to" to "900000",
                "idle_after_inactive_to" to "10000",
                "idle_pending_to" to "60000",
                "max_idle_pending_to" to "120000",
                "idle_to" to "1800000",
                "max_idle_to" to "10800000",
                "quick_doze_delay_to" to "30000",
                "motion_inactive_to" to "15000"
            )
        ),
        DozePreset(
            "extreme", R.string.bt_doze_extreme, R.string.bt_doze_extreme_desc, mapOf(
                "inactive_to" to "5000",
                "light_after_inactive_to" to "20000",
                "light_idle_to" to "60000",
                "light_max_idle_to" to "600000",
                "idle_after_inactive_to" to "5000",
                "idle_pending_to" to "30000",
                "max_idle_pending_to" to "60000",
                "idle_to" to "900000",
                "max_idle_to" to "21600000",
                "quick_doze_delay_to" to "10000",
                "motion_inactive_to" to "10000",
                "min_time_to_alarm" to "600000"
            )
        )
    )

    fun getGlobalInt(resolver: ContentResolver, key: String, default: Int): Int =
        try {
            Settings.Global.getInt(resolver, key, default)
        } catch (e: Exception) {
            default
        }

    /**
     * Since Android 12 a Settings.Global key is only readable by a normal app when the platform
     * marks it @Readable; device_idle_constants is not, so the direct read throws and we fall back
     * to the shell. When neither works the caller has to rely on [getAppliedPreset].
     */
    fun getGlobalString(resolver: ContentResolver, key: String): String {
        val direct = try {
            Settings.Global.getString(resolver, key) ?: ""
        } catch (e: Exception) {
            ""
        }
        if (direct.isNotBlank()) return direct

        if (ShizukuHelper.hasShizukuPermission()) {
            val result = ShizukuHelper.executeShellCommandWithOutput(
                arrayOf("settings", "get", "global", key)
            )
            val out = result.output.trim()
            if (result.exitCode == 0 && out.isNotBlank() && out != "null") return out
        }
        return ""
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("battery_tweaks", Context.MODE_PRIVATE)

    /** Remembers which preset the user picked, for keys the app is not allowed to read back. */
    fun setAppliedPreset(context: Context, group: String, id: String) {
        prefs(context).edit().putString(group, id).apply()
    }

    fun getAppliedPreset(context: Context, group: String): String? =
        prefs(context).getString(group, null)

    const val GROUP_SAVER = "saver_preset"
    const val GROUP_DOZE = "doze_preset"

    fun getSecureInt(resolver: ContentResolver, key: String, default: Int): Int =
        try {
            Settings.Secure.getInt(resolver, key, default)
        } catch (e: Exception) {
            default
        }

    fun putGlobal(context: Context, resolver: ContentResolver, key: String, value: String): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val ok = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "global", key, value)
            )
            if (ok) return true
        }
        return try {
            Settings.Global.putString(resolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    fun putSecure(context: Context, resolver: ContentResolver, key: String, value: String): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val ok = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "secure", key, value)
            )
            if (ok) return true
        }
        return try {
            Settings.Secure.putString(resolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    /** Clearing a constants string restores the platform defaults for that policy. */
    fun clearGlobal(context: Context, resolver: ContentResolver, key: String): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val ok = ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "global", key))
            if (ok) return true
        }
        return try {
            Settings.Global.putString(resolver, key, null)
        } catch (e: Exception) {
            false
        }
    }

    fun parseConstants(raw: String): Map<String, String> =
        raw.split(',')
            .mapNotNull { entry ->
                val i = entry.indexOf('=')
                if (i <= 0) null else entry.substring(0, i).trim() to entry.substring(i + 1).trim()
            }
            .toMap()

    fun serialiseConstants(values: Map<String, String>): String =
        values.entries.joinToString(",") { "${it.key}=${it.value}" }

    /** Only Shizuku can drive this one; it has no Settings.Global equivalent. */
    fun setAdaptivePowerSaver(enabled: Boolean): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "power", "set-adaptive-power-saver-enabled", enabled.toString())
        )

    fun setFixedPerformanceMode(enabled: Boolean): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "power", "set-fixed-performance-mode-enabled", enabled.toString())
        )

    fun setDozeEnabled(enabled: Boolean): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "deviceidle", if (enabled) "enable" else "disable", "all")
        )
}
