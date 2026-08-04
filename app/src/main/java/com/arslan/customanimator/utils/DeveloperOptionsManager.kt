package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

object DeveloperOptionsManager {

    private fun putGlobalInt(context: Context, contentResolver: ContentResolver, key: String, value: Int): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "global", key, value.toString()))
            if (success) return true
        }
        return try {
            Settings.Global.putInt(contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    private fun putGlobalString(context: Context, contentResolver: ContentResolver, key: String, value: String?): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = if (value == null) {
                ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "global", key))
            } else {
                ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "global", key, value))
            }
            if (success) return true
        }
        return try {
            Settings.Global.putString(contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    private fun putSecureInt(context: Context, contentResolver: ContentResolver, key: String, value: Int): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "secure", key, value.toString()))
            if (success) return true
        }
        return try {
            Settings.Secure.putInt(contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    private fun putSystemInt(context: Context, contentResolver: ContentResolver, key: String, value: Int): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "system", key, value.toString()))
            if (success) return true
        }
        return try {
            Settings.System.putInt(contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    fun isAdbEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setAdbEnabled(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putGlobalInt(context, contentResolver, Settings.Global.ADB_ENABLED, if (enabled) 1 else 0)
    }

    fun isAdbWifiEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setAdbWifiEnabled(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putGlobalInt(context, contentResolver, "adb_wifi_enabled", if (enabled) 1 else 0)
    }

    fun isAlwaysFinishActivitiesEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setAlwaysFinishActivities(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putGlobalInt(context, contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, if (enabled) 1 else 0)
    }

    private const val AM_CONSTANTS_KEY = "activity_manager_constants"
    private const val MAX_CACHED_PROCESSES_LIMITED = 1

    fun isBackgroundProcessLimitEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            val value = Settings.Global.getString(contentResolver, AM_CONSTANTS_KEY) ?: return false
            value.contains("max_cached_processes=$MAX_CACHED_PROCESSES_LIMITED")
        } catch (e: Exception) {
            false
        }
    }

    fun setBackgroundProcessLimit(context: Context, contentResolver: ContentResolver, limited: Boolean): Boolean {
        val value = if (limited) "max_cached_processes=$MAX_CACHED_PROCESSES_LIMITED" else null
        return putGlobalString(context, contentResolver, AM_CONSTANTS_KEY, value)
    }

    private const val SHOW_TOUCHES_KEY = "show_touches"
    private const val POINTER_LOCATION_KEY = "pointer_location"
    private const val FORCE_RTL_KEY = "debug_force_rtl"
    private const val LAYOUT_BOUNDS_PROP = "debug.layout"
    private const val GPU_PROFILE_PROP = "debug.hwui.profile"
    private const val GPU_PROFILE_ON = "visual_bars"

    private fun getProp(name: String): String {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("getprop", name))
        return if (result.isSuccess) result.output.trim() else ""
    }

    private fun setProp(name: String, value: String): Boolean {
        if (!ShizukuHelper.executeShellCommand(arrayOf("setprop", name, value))) return false
        ShizukuHelper.executeShellCommand(arrayOf("service", "call", "activity", "1599295570"))
        return true
    }

    fun isShowTouchesEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.System.getInt(contentResolver, SHOW_TOUCHES_KEY, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setShowTouches(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putSystemInt(context, contentResolver, SHOW_TOUCHES_KEY, if (enabled) 1 else 0)
    }

    fun isPointerLocationEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.System.getInt(contentResolver, POINTER_LOCATION_KEY, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setPointerLocation(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putSystemInt(context, contentResolver, POINTER_LOCATION_KEY, if (enabled) 1 else 0)
    }

    fun isForceRtlEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, FORCE_RTL_KEY, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setForceRtl(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        return putGlobalInt(context, contentResolver, FORCE_RTL_KEY, if (enabled) 1 else 0)
    }

    fun isLayoutBoundsEnabled(): Boolean = getProp(LAYOUT_BOUNDS_PROP) == "true"

    fun setLayoutBounds(enabled: Boolean): Boolean {
        return setProp(LAYOUT_BOUNDS_PROP, if (enabled) "true" else "false")
    }

    fun isGpuProfilingEnabled(): Boolean = getProp(GPU_PROFILE_PROP) == GPU_PROFILE_ON

    fun setGpuProfiling(enabled: Boolean): Boolean {
        return setProp(GPU_PROFILE_PROP, if (enabled) GPU_PROFILE_ON else "false")
    }

    private const val TXN_GET_SENSOR_PRIVACY = "6"
    private const val TXN_SET_SENSOR_PRIVACY = "9"

    private fun serviceCall(vararg args: String): Int? {
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("service", "call", "sensor_privacy", *args)
        )
        if (!result.isSuccess) return null
        val words = Regex("[0-9a-f]{8}").findAll(result.output.substringAfter("Parcel(")).toList()
        return if (words.size == 2) words.last().value.toLongOrNull(16)?.toInt() else null
    }

    fun isSensorsOffEnabled(): Boolean = serviceCall(TXN_GET_SENSOR_PRIVACY) == 1

    fun setSensorsOff(enabled: Boolean): Boolean {
        serviceCall(TXN_SET_SENSOR_PRIVACY, "i32", if (enabled) "1" else "0")
        return isSensorsOffEnabled() == enabled
    }

    fun clearAllAppCaches(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("pm", "trim-caches", "999000000000"))
    }

    fun forceStopApp(packageName: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("am", "force-stop", packageName))
    }

    fun revokePermission(packageName: String, permission: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("pm", "revoke", packageName, permission))
    }

    fun grantPermission(packageName: String, permission: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("pm", "grant", packageName, permission))
    }

    fun compileApp(packageName: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "package", "compile", "-m", "speed", "-f", packageName))
    }

    fun compileAllApps(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "package", "compile", "-m", "speed", "-a"))
    }

    private const val FANCY_IME_ANIMATIONS_KEY = "fancy_ime_animations"
    private const val CLOCK_SECONDS_KEY = "clock_seconds"
    private const val ACCELEROMETER_ROTATION_KEY = "accelerometer_rotation"
    private const val USER_ROTATION_KEY = "user_rotation"

    fun isFancyImeAnimationsDisabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, FANCY_IME_ANIMATIONS_KEY, 1) == 0
        } catch (e: Exception) {
            false
        }
    }

    fun setFancyImeAnimations(context: Context, contentResolver: ContentResolver, disabled: Boolean): Boolean {
        return putSecureInt(context, contentResolver, FANCY_IME_ANIMATIONS_KEY, if (disabled) 0 else 1)
    }

    private const val ONEUI_CLOCK_SECONDS_KEY = "clockshow_second"

    fun isOneUi(): Boolean = android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    fun isClockSecondsEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            if (isOneUi() && Settings.System.getInt(contentResolver, ONEUI_CLOCK_SECONDS_KEY, -1) != -1) {
                Settings.System.getInt(contentResolver, ONEUI_CLOCK_SECONDS_KEY, 0) == 1
            } else {
                Settings.Secure.getInt(contentResolver, CLOCK_SECONDS_KEY, 0) == 1
            }
        } catch (e: Exception) {
            false
        }
    }

    fun setClockSeconds(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        val value = if (enabled) 1 else 0
        val secure = putSecureInt(context, contentResolver, CLOCK_SECONDS_KEY, value)
        if (!isOneUi()) return secure
        val system = putSystemInt(context, contentResolver, ONEUI_CLOCK_SECONDS_KEY, value)
        return secure || system
    }

    fun restartSystemUi(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("killall", "com.android.systemui")) ||
            ShizukuHelper.executeShellCommand(arrayOf("am", "crash", "com.android.systemui"))
    }

    fun isAutoRotationEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.System.getInt(contentResolver, ACCELEROMETER_ROTATION_KEY, 1) == 1
        } catch (e: Exception) {
            true
        }
    }

    fun setAutoRotation(context: Context, contentResolver: ContentResolver, enabled: Boolean): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.executeShellCommand(
                arrayOf("wm", "set-fix-to-user-rotation", if (enabled) "disabled" else "enabled")
            )
        }
        return putSystemInt(context, contentResolver, ACCELEROMETER_ROTATION_KEY, if (enabled) 1 else 0)
    }

    fun resetRotation(context: Context, contentResolver: ContentResolver): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.executeShellCommand(arrayOf("wm", "set-fix-to-user-rotation", "disabled"))
            ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "system", "user_rotation"))
        }
        val rotation = putSystemInt(context, contentResolver, USER_ROTATION_KEY, 0)
        val auto = putSystemInt(context, contentResolver, ACCELEROMETER_ROTATION_KEY, 1)
        return rotation && auto
    }

    fun getUserRotation(contentResolver: ContentResolver): Int {
        return try {
            Settings.System.getInt(contentResolver, USER_ROTATION_KEY, 0)
        } catch (e: Exception) {
            0
        }
    }

    fun setUserRotation(context: Context, contentResolver: ContentResolver, rotation: Int): Boolean {
        return putSystemInt(context, contentResolver, USER_ROTATION_KEY, rotation.coerceIn(0, 3))
    }

    private const val ANGLE_PKGS_KEY = "angle_gl_driver_selection_pkgs"
    private const val ANGLE_VALUES_KEY = "angle_gl_driver_selection_values"

    fun getAngleDriverSelections(contentResolver: ContentResolver): Map<String, String> {
        return try {
            val pkgsCsv = Settings.Global.getString(contentResolver, ANGLE_PKGS_KEY)
            val valuesCsv = Settings.Global.getString(contentResolver, ANGLE_VALUES_KEY)
            if (pkgsCsv.isNullOrBlank() || valuesCsv.isNullOrBlank()) return emptyMap()
            val pkgs = pkgsCsv.split(",")
            val values = valuesCsv.split(",")
            if (pkgs.size != values.size) return emptyMap()
            pkgs.zip(values).toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun clearAngleDriverSelections(context: Context, contentResolver: ContentResolver): Boolean {
        val pkgs = putGlobalString(context, contentResolver, ANGLE_PKGS_KEY, null)
        val values = putGlobalString(context, contentResolver, ANGLE_VALUES_KEY, null)
        return pkgs && values
    }

    fun setAngleDriverSelection(
        context: Context,
        contentResolver: ContentResolver,
        packageName: String,
        driver: String?
    ): Boolean {
        val current = getAngleDriverSelections(contentResolver).toMutableMap()
        if (driver == null) {
            current.remove(packageName)
        } else {
            current[packageName] = driver
        }

        val pkgsCsv = current.keys.joinToString(",")
        val valuesCsv = current.values.joinToString(",")

        val pkgsResult = putGlobalString(context, contentResolver, ANGLE_PKGS_KEY, pkgsCsv.ifEmpty { null })
        val valuesResult = putGlobalString(context, contentResolver, ANGLE_VALUES_KEY, valuesCsv.ifEmpty { null })
        return pkgsResult && valuesResult
    }
}
