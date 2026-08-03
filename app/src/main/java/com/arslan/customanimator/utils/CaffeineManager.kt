package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

/**
 * Keeps the screen awake by parking the display timeout at its maximum, remembering whatever the
 * user had configured so the exact previous value comes back when caffeine is switched off.
 */
object CaffeineManager {

    private const val PREFS_NAME = "caffeine_prefs"
    private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
    private const val NO_SAVED_TIMEOUT = -1

    const val INFINITE_TIMEOUT_MS = Int.MAX_VALUE
    private const val DEFAULT_TIMEOUT_MS = 30_000

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
        getScreenTimeout(contentResolver) == INFINITE_TIMEOUT_MS

    fun canApply(context: Context): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(context)

    /** Turns caffeine on or off, returning false when the write did not go through. */
    fun setActive(context: Context, contentResolver: ContentResolver, active: Boolean): Boolean {
        if (active) {
            val current = getScreenTimeout(contentResolver)
            // Never record the infinite value as the "previous" one: that would strand the user
            // with a screen that no longer sleeps after toggling caffeine off.
            if (current != INFINITE_TIMEOUT_MS) {
                prefs(context).edit().putInt(KEY_PREVIOUS_TIMEOUT, current).apply()
            }
            return writeTimeout(context, contentResolver, INFINITE_TIMEOUT_MS)
        }

        val previous = prefs(context).getInt(KEY_PREVIOUS_TIMEOUT, NO_SAVED_TIMEOUT)
        val restored = if (previous == NO_SAVED_TIMEOUT) DEFAULT_TIMEOUT_MS else previous
        val success = writeTimeout(context, contentResolver, restored)
        if (success) prefs(context).edit().remove(KEY_PREVIOUS_TIMEOUT).apply()
        return success
    }

    private fun writeTimeout(context: Context, contentResolver: ContentResolver, value: Int): Boolean {
        if (ShizukuHelper.hasShizukuPermission()) {
            val applied = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "system", Settings.System.SCREEN_OFF_TIMEOUT, value.toString())
            )
            if (applied) return true
        }
        return try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, value)
        } catch (e: Exception) {
            false
        }
    }
}
