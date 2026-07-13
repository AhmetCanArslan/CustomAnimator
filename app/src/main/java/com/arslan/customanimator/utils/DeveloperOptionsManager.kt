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

    // USB debugging
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

    // Wireless (WiFi) debugging - Android 11+
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

    // "Don't keep activities"
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

    // Limit background processes (max cached processes kept alive by the system)
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

    // Quick actions
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

    // Compile Booster
    fun compileApp(packageName: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "package", "compile", "-m", "speed", "-f", packageName))
    }

    fun compileAllApps(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "package", "compile", "-m", "speed", "-a"))
    }

    // Graphics API Override (per-app ANGLE driver selection)
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
