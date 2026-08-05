package com.arslan.customanimator.notify.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.arslan.customanimator.notify.data.ScreenFlashColor
import com.arslan.customanimator.notify.ui.ScreenFlashActivity

class ScreenFlashManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var overlayRunnable: Runnable? = null

    fun triggerFlash(color: ScreenFlashColor, durationSeconds: Int) {
        stop()

        val isScreenOff = !powerManager.isInteractive
        val isDeviceLocked = keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        val isScreenInUse = !isScreenOff && !isDeviceLocked

        if (isScreenInUse) {
            if (Settings.canDrawOverlays(context)) showOverlay(color, durationSeconds)
            return
        }

        if (isScreenOff) {
            @Suppress("DEPRECATION")
            val wl = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
                "CustomAnimator:NotifyScreenFlashWake"
            )
            wl.acquire(3_000L)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (wl.isHeld) wl.release()
                } catch (_: Exception) {}
            }, 3_000L)
        }

        val intent = Intent(context, ScreenFlashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ScreenFlashActivity.EXTRA_COLOR_ARGB, color.colorArgb)
            putExtra(ScreenFlashActivity.EXTRA_DURATION_SEC, durationSeconds)
            putExtra(ScreenFlashActivity.EXTRA_OVERLAY_MODE, isScreenInUse)
        }
        context.startActivity(intent)
    }

    private fun showOverlay(color: ScreenFlashColor, durationSeconds: Int) {
        val view = View(context)
        val type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val durationMs = if (durationSeconds > 0) durationSeconds * 1000L else 5_000L
        val colorInt = color.colorArgb.toInt()
        var showColor = true
        val startTime = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                if (overlayView !== view) return
                if (System.currentTimeMillis() - startTime >= durationMs) {
                    stopOverlay()
                    return
                }
                view.setBackgroundColor(
                    if (showColor) Color.argb(
                        64,
                        Color.red(colorInt),
                        Color.green(colorInt),
                        Color.blue(colorInt)
                    )
                    else Color.TRANSPARENT
                )
                showColor = !showColor
                handler.postDelayed(this, 250L)
            }
        }

        runCatching { windowManager.addView(view, params) }
            .onFailure { return }
        overlayView = view
        overlayRunnable = runnable
        handler.post(runnable)
    }

    private fun stopOverlay() {
        overlayRunnable?.let(handler::removeCallbacks)
        overlayRunnable = null
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }

    fun stop() {
        stopOverlay()
        context.sendBroadcast(
            Intent(ScreenFlashActivity.ACTION_STOP_FLASH).setPackage(context.packageName)
        )
    }
}
