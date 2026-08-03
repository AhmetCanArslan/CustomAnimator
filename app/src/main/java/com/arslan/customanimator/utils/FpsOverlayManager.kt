package com.arslan.customanimator.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings

object FpsOverlayManager {

    private const val PREFS_NAME = "fps_overlay_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_POSITION_X = "position_x"
    private const val KEY_POSITION_Y = "position_y"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getPosition(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(KEY_POSITION_X, 0) to p.getInt(KEY_POSITION_Y, 120)
    }

    fun setPosition(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt(KEY_POSITION_X, x).putInt(KEY_POSITION_Y, y).apply()
    }

    fun canDrawOverlay(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun overlayPermissionIntent(context: Context) = android.content.Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
}
