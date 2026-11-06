package com.arslan.customanimator.utils

import android.content.Context
import java.util.TimeZone

object PremiumLimits {
    const val FREE_SHORTCUTS = 3
    const val FREE_APPS = 2
    const val FREE_ICON_CHANGES = 2

    enum class Limited(val key: String, val periodDays: Int) {
        BOOSTER("booster", 7),
        CLEANER("cleaner", 7),
        GAME_MODE("game_mode", 1)
    }

    private const val PREFS_NAME = "premium_limits"
    private const val KEY_LAST_SEEN_MS = "last_seen_ms"
    private const val KEY_ICON_CHANGES = "icon_changes"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun isAvailable(context: Context, feature: Limited, nowMs: Long = System.currentTimeMillis()): Boolean {
        val today = today(context, nowMs)
        val lastUsed = prefs(context).getLong(feature.key, today - feature.periodDays)
        return today - lastUsed >= feature.periodDays
    }

    fun markUsed(context: Context, feature: Limited, nowMs: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(feature.key, today(context, nowMs)).apply()
    }

    fun iconChangesLeft(context: Context): Int =
        (FREE_ICON_CHANGES - prefs(context).getInt(KEY_ICON_CHANGES, 0)).coerceAtLeast(0)

    fun recordIconChange(context: Context) {
        val prefs = prefs(context)
        prefs.edit().putInt(KEY_ICON_CHANGES, prefs.getInt(KEY_ICON_CHANGES, 0) + 1).apply()
    }

    private fun today(context: Context, nowMs: Long): Long {
        val prefs = prefs(context)
        val effectiveMs = maxOf(nowMs, prefs.getLong(KEY_LAST_SEEN_MS, 0L))
        prefs.edit().putLong(KEY_LAST_SEEN_MS, effectiveMs).apply()
        return (effectiveMs + TimeZone.getDefault().getOffset(effectiveMs)) / DAY_MS
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
