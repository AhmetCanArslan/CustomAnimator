package com.riyadmondol2006.customanimator.screenshot

import android.content.Context
import android.widget.Toast
import com.riyadmondol2006.customanimator.R

internal object ScreenshotToasts {

    private val CONFIRMATIONS = setOf(
        R.string.screenshot_toast_copied,
        R.string.screenshot_toast_deleted,
        R.string.screenshot_toast_copied_deleted
    )

    fun show(context: Context, message: Int) {
        if (message in CONFIRMATIONS && !ScreenshotPrefs(context).showConfirmationToasts) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
