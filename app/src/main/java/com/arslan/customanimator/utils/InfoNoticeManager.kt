package com.arslan.customanimator.utils

import android.content.Context

object InfoNoticeManager {

    private const val PREFS_NAME = "info_notices"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDismissed(context: Context, key: String): Boolean =
        prefs(context).getBoolean(key, false)

    fun dismiss(context: Context, key: String) {
        prefs(context).edit().putBoolean(key, true).apply()
    }
}
