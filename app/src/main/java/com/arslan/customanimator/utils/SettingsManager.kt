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
    private const val KEY_AD_INFO_DIALOG_SHOWN = "ad_info_dialog_shown"
    private const val KEY_RATE_DIALOG_NEXT_SHOW = "rate_dialog_next_show"
    private const val KEY_REMOVE_ADS_PROMPT_DISMISSED = "remove_ads_prompt_dismissed"
    private const val KEY_LAST_TAB = "last_tab"
    private const val KEY_LAST_SCREEN = "last_screen"
    
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
    
    fun getLastTab(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_TAB, null)
    }

    fun setLastTab(context: Context, tab: String) {
        getPrefs(context).edit().putString(KEY_LAST_TAB, tab).apply()
    }

    fun getLastScreen(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_SCREEN, null)
    }

    fun setLastScreen(context: Context, screen: String) {
        getPrefs(context).edit().putString(KEY_LAST_SCREEN, screen).apply()
    }

    fun getSimpleMode(context: Context): Boolean {
        // Simple mode is default as requested
        return getPrefs(context).getBoolean("simple_mode", true)
    }
    
    fun setSimpleMode(context: Context, isSimpleMode: Boolean) {
        getPrefs(context).edit().putBoolean("simple_mode", isSimpleMode).apply()
    }

    private const val KEY_TERMINAL_RISK_ACCEPTED = "terminal_risk_accepted"

    fun hasAcceptedTerminalRisk(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TERMINAL_RISK_ACCEPTED, false)
    }

    fun markTerminalRiskAccepted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_TERMINAL_RISK_ACCEPTED, true).apply()
    }

    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun hasCompletedOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun markOnboardingCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    fun hasShownAdInfoDialog(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AD_INFO_DIALOG_SHOWN, false)
    }

    fun markAdInfoDialogShown(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_AD_INFO_DIALOG_SHOWN, true).apply()
    }

    fun isRemoveAdsPromptDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMOVE_ADS_PROMPT_DISMISSED, false)
    }

    fun dismissRemoveAdsPrompt(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_REMOVE_ADS_PROMPT_DISMISSED, true).apply()
    }

    fun shouldShowRateDialog(context: Context): Boolean {
        val nextShow = getPrefs(context).getLong(KEY_RATE_DIALOG_NEXT_SHOW, 0L)
        return nextShow == 0L || System.currentTimeMillis() >= nextShow
    }

    fun markRateDialogRated(context: Context) {
        val next = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        getPrefs(context).edit().putLong(KEY_RATE_DIALOG_NEXT_SHOW, next).apply()
    }

    fun markRateDialogLater(context: Context) {
        val next = System.currentTimeMillis() + 24L * 60 * 60 * 1000
        getPrefs(context).edit().putLong(KEY_RATE_DIALOG_NEXT_SHOW, next).apply()
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

        return try {
            Settings.Global.putFloat(contentResolver, key, value) &&
                    scalesMatch(Settings.Global.getFloat(contentResolver, key, Float.NaN), value)
        } catch (e: Exception) {
            false
        }
    }

    private fun scalesMatch(actual: Float, expected: Float): Boolean {
        return !actual.isNaN() && kotlin.math.abs(actual - expected) < 0.001f
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
