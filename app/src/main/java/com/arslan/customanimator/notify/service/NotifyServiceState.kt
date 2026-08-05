package com.arslan.customanimator.notify.service

import android.content.Context

private const val PREFS_NAME = "prime_notify_prefs"
private const val KEY_SERVICE_ENABLED = "prime_notify_service_enabled"
private const val KEY_PERSISTENT_NOTIFICATION_HIDDEN = "prime_notify_persistent_hidden"

fun isPrimeNotifyServiceEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_SERVICE_ENABLED, false)
}

fun setPrimeNotifyServiceEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
}

fun isPersistentNotificationHidden(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_PERSISTENT_NOTIFICATION_HIDDEN, false)
}

fun setPersistentNotificationHidden(context: Context, hidden: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION_HIDDEN, hidden).apply()
}
