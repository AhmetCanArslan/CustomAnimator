package com.arslan.customanimator.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.FpsOverlayManager
import kotlin.math.abs
import kotlin.math.roundToInt

class FpsOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "fps_overlay_channel"
        private const val NOTIF_ID = 4202
        private const val UPDATE_INTERVAL_NS = 500_000_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, FpsOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FpsOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var frameCount = 0
    private var windowStartNs = 0L
    private var frameCallback: Choreographer.FrameCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FpsOverlayManager.canDrawOverlay(this)) {
            FpsOverlayManager.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlayView == null) {
            addOverlay()
            startMeasuring()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlay() {
        val density = resources.displayMetrics.density
        val view = TextView(this).apply {
            text = getString(R.string.fps_overlay_value, 0)
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(
                (8 * density).roundToInt(),
                (4 * density).roundToInt(),
                (8 * density).roundToInt(),
                (4 * density).roundToInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(160, 0, 0, 0))
                cornerRadius = 8 * density
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (savedX, savedY) = FpsOverlayManager.getPosition(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) FpsOverlayManager.setPosition(this, params.x, params.y)
                    true
                }
                else -> false
            }
        }

        runCatching { windowManager.addView(view, params) }
            .onFailure {
                FpsOverlayManager.setEnabled(this, false)
                stopSelf()
                return
            }

        overlayView = view
        layoutParams = params
    }

    private fun startMeasuring() {
        val choreographer = Choreographer.getInstance()
        windowStartNs = 0L
        frameCount = 0
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (windowStartNs == 0L) {
                    windowStartNs = frameTimeNanos
                } else {
                    frameCount++
                    val elapsed = frameTimeNanos - windowStartNs
                    if (elapsed >= UPDATE_INTERVAL_NS) {
                        val fps = (frameCount * 1_000_000_000.0 / elapsed).roundToInt()
                        overlayView?.text = getString(R.string.fps_overlay_value, fps)
                        windowStartNs = frameTimeNanos
                        frameCount = 0
                    }
                }
                choreographer.postFrameCallback(this)
            }
        }
        frameCallback = callback
        choreographer.postFrameCallback(callback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.fps_overlay_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.fps_overlay_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.fps_overlay_notif_title))
            .setContentText(getString(R.string.fps_overlay_notif_text))
            .setSmallIcon(R.drawable.ic_notification_fps)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        frameCallback = null
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
        layoutParams = null
        runCatching { NotificationManagerCompat.from(this).cancel(NOTIF_ID) }
        super.onDestroy()
    }
}
