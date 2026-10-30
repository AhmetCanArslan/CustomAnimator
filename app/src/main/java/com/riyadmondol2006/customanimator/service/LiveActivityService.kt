package com.riyadmondol2006.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.riyadmondol2006.customanimator.MainActivity
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.LiveActivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class LiveActivityService : Service() {

    companion object {
        private const val CHANNEL_ID = "live_activity_channel"
        private const val NOTIF_ID = 4204
        private const val POLL_INTERVAL_MS = 1000L
        private const val MAX_WIDTH_FRACTION = 0.6f

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LiveActivityService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveActivityService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var inForeground = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> startPolling()
                Intent.ACTION_SCREEN_OFF -> stopPolling()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        inForeground = startForegroundSafely { startForeground(NOTIF_ID, buildNotification()) }
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!inForeground) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) {
            shutDown()
            return START_NOT_STICKY
        }
        if (overlayView == null && !addOverlay()) {
            shutDown()
            return START_NOT_STICKY
        }
        startPolling()
        return START_STICKY
    }

    private fun shutDown() {
        LiveActivityManager.setEnabled(this, false)
        stopSelf()
    }

    private fun addOverlay(): Boolean {
        val density = resources.displayMetrics.density
        val margin = (8 * density).roundToInt()
        val view = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            maxWidth = (resources.displayMetrics.widthPixels * MAX_WIDTH_FRACTION).roundToInt()
            setPadding(margin, (4 * density).roundToInt(), margin, (4 * density).roundToInt())
            background = GradientDrawable().apply {
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

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = margin
            y = margin
        }

        return runCatching { windowManager.addView(view, params) }
            .onSuccess { overlayView = view }
            .isSuccess
    }

    private fun startPolling() {
        stopPolling()
        if (overlayView == null) return
        if (!(getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) return
        pollJob = scope.launch {
            while (true) {
                if (!Settings.canDrawOverlays(this@LiveActivityService)) {
                    shutDown()
                    return@launch
                }
                withContext(Dispatchers.IO) { LiveActivityManager.resumedActivity() }?.let(::render)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun render(activity: ComponentName) {
        overlayView?.text = getString(R.string.live_activity_value, activity.packageName, activity.shortClassName)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.activity_manager),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.live_activity_channel_desc)
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
            .setContentTitle(getString(R.string.live_activity_notif_title))
            .setContentText(getString(R.string.fps_overlay_notif_text))
            .setSmallIcon(R.drawable.ic_notification_fps)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopPolling()
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
        runCatching { NotificationManagerCompat.from(this).cancel(NOTIF_ID) }
        super.onDestroy()
    }
}
