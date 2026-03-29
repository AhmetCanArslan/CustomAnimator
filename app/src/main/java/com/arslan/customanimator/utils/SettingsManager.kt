package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.DisplayMetrics
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

object SettingsManager {

    data class SmallestWidthResult(
        val success: Boolean,
        val usedWriteSecureFallback: Boolean
    )
    
    private const val PREFS_NAME = "custom_animator_prefs"
    private const val KEY_INPUT_MODE = "input_mode"
    private const val KEY_SKIP_WRITE_SECURE_WIDTH_CONFIRM = "skip_write_secure_width_confirm"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getInputMode(context: Context): String {
        return getPrefs(context).getString(KEY_INPUT_MODE, "slider") ?: "slider"
    }
    
    fun setInputMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_INPUT_MODE, mode).apply()
    }

    fun shouldShowWriteSecureWidthConfirmDialog(context: Context): Boolean {
        return !getPrefs(context).getBoolean(KEY_SKIP_WRITE_SECURE_WIDTH_CONFIRM, false)
    }

    fun setSkipWriteSecureWidthConfirmDialog(context: Context, skip: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SKIP_WRITE_SECURE_WIDTH_CONFIRM, skip).apply()
    }
    
    fun getSimpleMode(context: Context): Boolean {
        // Simple mode is default as requested
        return getPrefs(context).getBoolean("simple_mode", true)
    }
    
    fun setSimpleMode(context: Context, isSimpleMode: Boolean) {
        getPrefs(context).edit().putBoolean("simple_mode", isSimpleMode).apply()
    }
    
    fun getWindowAnimationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    fun getTransitionAnimationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    fun getAnimatorDurationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    private fun setGlobalFloat(
        context: Context,
        contentResolver: ContentResolver,
        key: String,
        value: Float
    ): Boolean {
        val formattedValue = String.format(Locale.US, "%.2f", value)

        // First try Shizuku path.
        if (ShizukuHelper.hasShizukuPermission()) {
            val success = ShizukuHelper.executeShellCommand(
                arrayOf("settings", "put", "global", key, formattedValue)
            )
            if (success) return true
        }

        // Fallback to WRITE_SECURE_SETTINGS path.
        return try {
            Settings.Global.putFloat(contentResolver, key, value)
        } catch (e: Exception) {
            false
        }
    }

    fun setWindowAnimationScale(context: Context, contentResolver: ContentResolver, value: Float): Boolean {
        return setGlobalFloat(context, contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, value)
    }

    fun setTransitionAnimationScale(context: Context, contentResolver: ContentResolver, value: Float): Boolean {
        return setGlobalFloat(context, contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, value)
    }

    fun setAnimatorDurationScale(context: Context, contentResolver: ContentResolver, value: Float): Boolean {
        return setGlobalFloat(context, contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, value)
    }
    
    fun applyAllScales(
        context: Context,
        contentResolver: ContentResolver,
        windowScale: Float,
        transitionScale: Float,
        animatorScale: Float
    ): Boolean {
        return setWindowAnimationScale(context, contentResolver, windowScale) &&
                setTransitionAnimationScale(context, contentResolver, transitionScale) &&
                setAnimatorDurationScale(context, contentResolver, animatorScale)
    }
    
    // Smallest Width methods
    private const val DISPLAY_DENSITY_FORCED = "display_density_forced"

    fun getSmallestWidth(context: Context): Int {
        return context.resources.configuration.smallestScreenWidthDp
    }

    fun setSmallestWidth(contentResolver: ContentResolver, context: Context, width: Int): SmallestWidthResult {
        return try {
            if (width <= 0) {
                // First try Shizuku command path.
                if (ShizukuHelper.hasShizukuPermission()) {
                    val shizukuSuccess = ShizukuHelper.executeShellCommand(
                        arrayOf("wm", "density", "reset")
                    )
                    if (shizukuSuccess) {
                        return SmallestWidthResult(success = true, usedWriteSecureFallback = false)
                    }
                }

                // Fallback to WRITE_SECURE_SETTINGS path.
                val writeSuccess = Settings.Secure.putString(contentResolver, DISPLAY_DENSITY_FORCED, null)
                val verifySuccess = Settings.Secure.getString(contentResolver, DISPLAY_DENSITY_FORCED) == null
                return SmallestWidthResult(
                    success = writeSuccess && verifySuccess,
                    usedWriteSecureFallback = true
                )
            }

            val metrics = context.resources.displayMetrics
            val smallestPx = min(metrics.widthPixels, metrics.heightPixels).toFloat()
            val targetDensity = (smallestPx * DisplayMetrics.DENSITY_DEFAULT / width)
                .roundToInt()
                .coerceIn(72, 1000)

            // First try Shizuku command path.
            if (ShizukuHelper.hasShizukuPermission()) {
                val shizukuSuccess = ShizukuHelper.executeShellCommand(
                    arrayOf("wm", "density", targetDensity.toString())
                )
                if (shizukuSuccess) {
                    return SmallestWidthResult(success = true, usedWriteSecureFallback = false)
                }
            }

            // Fallback to WRITE_SECURE_SETTINGS path.
            val targetDensityString = targetDensity.toString()
            val writeSuccess = Settings.Secure.putString(contentResolver, DISPLAY_DENSITY_FORCED, targetDensityString)
            val currentValue = Settings.Secure.getString(contentResolver, DISPLAY_DENSITY_FORCED)
            SmallestWidthResult(
                success = writeSuccess && currentValue == targetDensityString,
                usedWriteSecureFallback = true
            )
        } catch (e: Exception) {
            SmallestWidthResult(success = false, usedWriteSecureFallback = false)
        }
    }
}
