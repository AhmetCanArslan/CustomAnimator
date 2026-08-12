package com.arslan.customanimator.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.screenshot.ScreenshotActionActivity
import com.arslan.customanimator.screenshot.ScreenshotActions
import com.arslan.customanimator.screenshot.ScreenshotNotifier
import com.arslan.customanimator.screenshot.ScreenshotOverlay
import com.arslan.customanimator.screenshot.ScreenshotPermissions
import com.arslan.customanimator.screenshot.ScreenshotPrefs

/**
 * Foreground service that watches MediaStore for newly added screenshots. Each
 * new screenshot can trigger the alert notification, the floating copy/delete
 * buttons, or both, depending on the user's settings.
 */
class ScreenshotWatcherService : Service() {

    private lateinit var prefs: ScreenshotPrefs
    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler

    private val main = Handler(Looper.getMainLooper())
    private var overlay: ScreenshotOverlay? = null
    private val hideOverlay = Runnable { overlay?.hide() }

    private var observer: ContentObserver? = null
    private var lastSeenId = -1L

    override fun onCreate() {
        super.onCreate()
        prefs = ScreenshotPrefs(this)
        ScreenshotNotifier.ensureChannels(this)
        workerThread = HandlerThread("screenshot-observer").apply { start() }
        handler = Handler(workerThread.looper)
        overlay = ScreenshotOverlay(this)
        lastSeenId = ScreenshotActions.newestScreenshot(this)?.id ?: -1L
        registerObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        val notification = ScreenshotNotifier.buildWatcherNotification(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            ScreenshotNotifier.WATCHER_NOTIFICATION_ID,
            notification,
            type
        )
    }

    private fun registerObserver() {
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                onMediaChanged()
            }
        }
        runCatching {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                obs
            )
        }.onSuccess { observer = obs }
    }

    private fun onMediaChanged() {
        val item = ScreenshotActions.latestScreenshot(applicationContext, RECENT_SECONDS) ?: return
        if (item.id == lastSeenId) return
        lastSeenId = item.id

        if (prefs.overlayEnabled && ScreenshotPermissions.hasOverlayPermission(this)) {
            main.post { showOverlay() }
        }

        if (!prefs.watcherEnabled) return
        val delayMs = prefs.notificationDelaySeconds * 1000L
        handler.postDelayed({
            val current = ScreenshotActions.itemFor(applicationContext, item.id) ?: return@postDelayed
            ScreenshotNotifier.notifyScreenshot(applicationContext, current)
        }, delayMs)
    }

    private fun showOverlay() {
        val view = overlay ?: return
        main.removeCallbacks(hideOverlay)
        view.show(
            prefs.overlayX,
            prefs.overlayY,
            prefs.overlayShowCopy,
            prefs.overlayShowDelete,
            { startAction(ScreenshotActionActivity.ACTION_COPY_DELETE) },
            { startAction(ScreenshotActionActivity.ACTION_DELETE) }
        )
        main.postDelayed(hideOverlay, prefs.overlayTimeoutSeconds * 1000L)
    }

    private fun startAction(action: String) {
        main.removeCallbacks(hideOverlay)
        overlay?.hide()
        runCatching {
            startActivity(
                ScreenshotActionActivity.intent(applicationContext, action, -1L, -1)
            )
        }
    }

    override fun onDestroy() {
        observer?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        observer = null
        main.removeCallbacks(hideOverlay)
        overlay?.hide()
        overlay = null
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val RECENT_SECONDS = 20L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenshotWatcherService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenshotWatcherService::class.java))
        }

        /** Runs while either mode is on; stops when both are off. */
        fun sync(context: Context) {
            val prefs = ScreenshotPrefs(context)
            if (prefs.watcherEnabled || prefs.overlayEnabled) start(context) else stop(context)
        }
    }
}
