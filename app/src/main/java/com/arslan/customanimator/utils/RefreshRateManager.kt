package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import kotlin.math.abs
import kotlin.math.roundToInt

object RefreshRateManager {

    const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
    const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"

    private const val NO_RATE = -1f
    private const val RATE_TOLERANCE = 0.5f

    fun getSupportedRates(context: Context): List<Float> {
        val display = defaultDisplay(context) ?: return emptyList()
        return try {
            display.supportedModes
                .map { it.refreshRate }
                .filter { it > 0f }
                .distinctBy { (it * 10).roundToInt() }
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getActiveRate(context: Context): Float? {
        val display = defaultDisplay(context) ?: return null
        return try {
            display.refreshRate.takeIf { it > 0f }
        } catch (e: Exception) {
            null
        }
    }

    fun getMinRate(resolver: ContentResolver): Float? = readRate(resolver, KEY_MIN_REFRESH_RATE)

    fun getPeakRate(resolver: ContentResolver): Float? = readRate(resolver, KEY_PEAK_REFRESH_RATE)

    fun setMinRate(rate: Float?): Boolean = writeRate(KEY_MIN_REFRESH_RATE, rate)

    fun setPeakRate(rate: Float?): Boolean = writeRate(KEY_PEAK_REFRESH_RATE, rate)

    fun applyRate(rate: Float): Boolean = setMinRate(rate) && setPeakRate(rate)

    fun restoreRates(min: Float?, peak: Float?): Boolean {
        val minRestored = setMinRate(min)
        val peakRestored = setPeakRate(peak)
        return minRestored && peakRestored
    }

    fun isSupportedRate(context: Context, rate: Float): Boolean =
        getSupportedRates(context).any { abs(it - rate) <= RATE_TOLERANCE }

    fun formatRate(rate: Float): String =
        if (abs(rate - rate.roundToInt()) < 0.05f) rate.roundToInt().toString() else String.format("%.1f", rate)

    private fun readRate(resolver: ContentResolver, key: String): Float? = try {
        Settings.System.getFloat(resolver, key, NO_RATE).takeIf { it > 0f }
    } catch (e: Exception) {
        null
    }

    private fun writeRate(key: String, rate: Float?): Boolean {
        return if (rate == null || rate <= 0f) {
            ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "system", key))
        } else {
            ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "system", key, rate.toString()))
        }
    }

    private fun defaultDisplay(context: Context): Display? = try {
        context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
    } catch (e: Exception) {
        null
    }
}
