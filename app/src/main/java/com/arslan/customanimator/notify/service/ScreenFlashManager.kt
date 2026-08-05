package com.arslan.customanimator.notify.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.arslan.customanimator.notify.data.ScreenFlashColor
import com.arslan.customanimator.notify.ui.ScreenFlashActivity

class ScreenFlashManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    fun triggerFlash(color: ScreenFlashColor, durationSeconds: Int) {
        stop()

        val isScreenOff = !powerManager.isInteractive
        val isDeviceLocked = keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        val isScreenInUse = !isScreenOff && !isDeviceLocked

        if (isScreenOff) {
            @Suppress("DEPRECATION")
            val wl = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
                "CustomAnimator:NotifyScreenFlashWake"
            )
            wl.acquire(3_000L)
        }

        val intent = Intent(context, ScreenFlashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenFlashActivity.EXTRA_COLOR_ARGB, color.colorArgb)
            putExtra(ScreenFlashActivity.EXTRA_DURATION_SEC, durationSeconds)
            putExtra(ScreenFlashActivity.EXTRA_OVERLAY_MODE, isScreenInUse)
        }
        context.startActivity(intent)
    }

    fun stop() {
        context.sendBroadcast(
            Intent(ScreenFlashActivity.ACTION_STOP_FLASH).setPackage(context.packageName)
        )
    }
}
