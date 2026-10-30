package com.riyadmondol2006.customanimator.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.os.Build
import android.util.Log

internal fun Service.startForegroundSafely(start: () -> Unit): Boolean {
    return try {
        start()
        true
    } catch (e: Exception) {
        if (!isStartNotAllowed(e)) throw e
        Log.w(javaClass.simpleName, "Foreground start not allowed, stopping", e)
        stopSelf()
        false
    }
}

private fun isStartNotAllowed(e: Exception): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException
