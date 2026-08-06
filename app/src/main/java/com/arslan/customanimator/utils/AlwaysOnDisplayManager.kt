package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.provider.Settings

object AlwaysOnDisplayManager {

    private const val DOZE_ALWAYS_ON = "doze_always_on"

    fun isSupported(): Boolean {
        return try {
            val resources = Resources.getSystem()
            val id = resources.getIdentifier(
                "config_dozeAlwaysOnDisplayAvailable",
                "bool",
                "android"
            )
            if (id == 0) true else resources.getBoolean(id)
        } catch (e: Exception) {
            true
        }
    }

    fun isActive(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, DOZE_ALWAYS_ON, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun canApply(context: Context): Boolean =
        isSupported() &&
            (ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(context))

    fun setActive(contentResolver: ContentResolver, active: Boolean): Boolean {
        val value = if (active) 1 else 0
        if (ShizukuHelper.hasShizukuPermission()) {
            val applied = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "secure", DOZE_ALWAYS_ON, value.toString())
            )
            if (applied) return true
        }
        return try {
            Settings.Secure.putInt(contentResolver, DOZE_ALWAYS_ON, value)
        } catch (e: Exception) {
            false
        }
    }
}
