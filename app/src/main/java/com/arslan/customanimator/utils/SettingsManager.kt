package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import com.arslan.customanimator.ui.theme.ThemeMode
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
    private const val KEY_REMOVE_ADS_PROMPT_DISMISSED_UNTIL = "remove_ads_prompt_dismissed_until"
    private const val KEY_REMOVE_ADS_SUPPORT_NEXT_SHOW = "remove_ads_support_next_show"
    private const val KEY_LAST_TAB = "last_tab"
    private const val KEY_LAST_SCREEN = "last_screen"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getThemeMode(context: Context): ThemeMode {
        val stored = getPrefs(context).getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
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

    const val CHANGELOG_NEVER_SEEN = -1

    private const val KEY_LAST_CHANGELOG_VERSION = "last_changelog_version"

    fun getLastSeenChangelogVersion(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_CHANGELOG_VERSION, CHANGELOG_NEVER_SEEN)
    }

    fun setLastSeenChangelogVersion(context: Context, versionCode: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_CHANGELOG_VERSION, versionCode).apply()
    }

    fun hasShownAdInfoDialog(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AD_INFO_DIALOG_SHOWN, false)
    }

    fun markAdInfoDialogShown(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_AD_INFO_DIALOG_SHOWN, true).apply()
    }

    fun isRemoveAdsPromptDismissed(context: Context): Boolean {
        return getPrefs(context).getLong(KEY_REMOVE_ADS_PROMPT_DISMISSED_UNTIL, 0L) > System.currentTimeMillis()
    }

    fun getRemoveAdsPromptDismissedUntil(context: Context): Long =
        getPrefs(context).getLong(KEY_REMOVE_ADS_PROMPT_DISMISSED_UNTIL, 0L)

    fun dismissRemoveAdsPrompt(context: Context) {
        getPrefs(context).edit()
            .putLong(
                KEY_REMOVE_ADS_PROMPT_DISMISSED_UNTIL,
                System.currentTimeMillis() + 24L * 60 * 60 * 1000
            )
            .apply()
    }

    fun shouldShowRemoveAdsSupportDialog(context: Context): Boolean {
        val nextShow = getPrefs(context).getLong(KEY_REMOVE_ADS_SUPPORT_NEXT_SHOW, 0L)
        return nextShow == 0L || System.currentTimeMillis() >= nextShow
    }

    fun markRemoveAdsSupportDialogLater(context: Context) {
        getPrefs(context).edit()
            .putLong(
                KEY_REMOVE_ADS_SUPPORT_NEXT_SHOW,
                System.currentTimeMillis() + 24L * 60 * 60 * 1000
            )
            .apply()
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
        val windowSuccess = setWindowAnimationScale(context, contentResolver, windowScale)
        val transitionSuccess = setTransitionAnimationScale(context, contentResolver, transitionScale)
        val animatorSuccess = setAnimatorDurationScale(context, contentResolver, animatorScale)
        return windowSuccess and transitionSuccess and animatorSuccess
    }
    
    private const val DISPLAY_DENSITY_FORCED = "display_density_forced"

    const val MIN_DENSITY = 72
    const val MAX_DENSITY = 1000
    const val MIN_SMALLEST_WIDTH = 320
    const val MAX_SMALLEST_WIDTH = 1024

    fun getSmallestWidthPx(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            min(bounds.width(), bounds.height())
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            min(metrics.widthPixels, metrics.heightPixels)
        }
    }

    fun densityForSmallestWidth(context: Context, widthDp: Int): Int {
        val smallestPx = getSmallestWidthPx(context).toFloat()
        return (smallestPx * DisplayMetrics.DENSITY_DEFAULT / widthDp)
            .roundToInt()
            .coerceIn(MIN_DENSITY, MAX_DENSITY)
    }

    fun smallestWidthForDensity(context: Context, density: Int): Int {
        val smallestPx = getSmallestWidthPx(context).toFloat()
        return (smallestPx * DisplayMetrics.DENSITY_DEFAULT / density).roundToInt()
    }

    fun getSmallestWidth(context: Context): Int {
        return smallestWidthForDensity(context, context.resources.configuration.densityDpi)
    }

    fun setSmallestWidth(contentResolver: ContentResolver, context: Context, width: Int): SmallestWidthResult {
        return try {
            if (width <= 0) {
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

            val targetDensity = densityForSmallestWidth(context, width)

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

    fun getForcedDensity(contentResolver: ContentResolver): Int? {
        return try {
            Settings.Secure.getString(contentResolver, DISPLAY_DENSITY_FORCED)?.trim()?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun applyDensity(contentResolver: ContentResolver, density: Int?): Boolean {
        return try {
            if (density == null) {
                if (ShizukuHelper.hasShizukuPermission() &&
                    ShizukuHelper.executeShellCommand(arrayOf("wm", "density", "reset"))
                ) {
                    return true
                }
                Settings.Secure.putString(contentResolver, DISPLAY_DENSITY_FORCED, null) &&
                    Settings.Secure.getString(contentResolver, DISPLAY_DENSITY_FORCED) == null
            } else {
                val target = density.coerceIn(MIN_DENSITY, MAX_DENSITY)
                if (ShizukuHelper.hasShizukuPermission() &&
                    ShizukuHelper.executeShellCommand(arrayOf("wm", "density", target.toString()))
                ) {
                    return true
                }
                val targetString = target.toString()
                Settings.Secure.putString(contentResolver, DISPLAY_DENSITY_FORCED, targetString) &&
                    Settings.Secure.getString(contentResolver, DISPLAY_DENSITY_FORCED) == targetString
            }
        } catch (e: Exception) {
            false
        }
    }
}
