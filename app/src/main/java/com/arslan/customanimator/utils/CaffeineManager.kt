package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

object CaffeineManager {

    private const val PREFS_NAME = "caffeine_prefs"
    private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
    private const val NO_SAVED_TIMEOUT = -1

    const val INFINITE_TIMEOUT_MS = Int.MAX_VALUE
    private const val DEFAULT_TIMEOUT_MS = 30_000
    private const val MAX_REAL_TIMEOUT_MS = 1_800_000

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getScreenTimeout(contentResolver: ContentResolver): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT_MS)
        } catch (e: Exception) {
            DEFAULT_TIMEOUT_MS
        }
    }

    fun isActive(contentResolver: ContentResolver): Boolean =
        getScreenTimeout(contentResolver) > MAX_REAL_TIMEOUT_MS

    fun canApply(context: Context): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(context)

    fun setActive(context: Context, contentResolver: ContentResolver, active: Boolean): Boolean {
        if (active) {
            val current = getScreenTimeout(contentResolver)
            if (current in 1..MAX_REAL_TIMEOUT_MS) {
                prefs(context).edit().putInt(KEY_PREVIOUS_TIMEOUT, current).commit()
            }
            return writeTimeout(context, contentResolver, INFINITE_TIMEOUT_MS)
        }

        val previous = prefs(context).getInt(KEY_PREVIOUS_TIMEOUT, NO_SAVED_TIMEOUT)
        val restored = if (previous in 1..MAX_REAL_TIMEOUT_MS) previous else DEFAULT_TIMEOUT_MS
        val success = writeTimeout(context, contentResolver, restored)
        if (success) prefs(context).edit().remove(KEY_PREVIOUS_TIMEOUT).commit()
        return success
    }

    private fun isApplied(contentResolver: ContentResolver, value: Int): Boolean {
        val current = getScreenTimeout(contentResolver)
        return if (value > MAX_REAL_TIMEOUT_MS) current > MAX_REAL_TIMEOUT_MS else current == value
    }

    private fun writeTimeout(context: Context, contentResolver: ContentResolver, value: Int): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val applied = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "system", Settings.System.SCREEN_OFF_TIMEOUT, value.toString())
            )
            if (applied && isApplied(contentResolver, value)) return true
        }
        return try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, value)
        } catch (e: Exception) {
            false
        }
    }
}
