package com.arslan.customanimator.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.screenshot.ScreenshotActionActivity
import com.arslan.customanimator.screenshot.ScreenshotActions
import com.arslan.customanimator.screenshot.ScreenshotFolder
import com.arslan.customanimator.screenshot.ScreenshotItem
import com.arslan.customanimator.screenshot.ScreenshotNotifier
import com.arslan.customanimator.screenshot.ScreenshotOverlay
import com.arslan.customanimator.screenshot.ScreenshotPrefs

class ScreenshotWatcherService : Service() {

    private lateinit var prefs: ScreenshotPrefs
    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler

    private val main = Handler(Looper.getMainLooper())
    private var overlay: ScreenshotOverlay? = null
    private val hideOverlay = Runnable { overlay?.hide() }

    private lateinit var observer: ContentObserver
    private var watchedFolder: Cursor? = null
    private var lastSeenMillis = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = ScreenshotPrefs(this)
        ScreenshotNotifier.ensureChannels(this)
        workerThread = HandlerThread("screenshot-observer").apply { start() }
        handler = Handler(workerThread.looper)
        overlay = ScreenshotOverlay(this)
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onFolderChanged()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        handler.post { watchFolder() }
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

    private fun watchFolder() {
        closeFolder()
        val tree = ScreenshotFolder.tree(this) ?: return
        lastSeenMillis = ScreenshotActions.newestScreenshot(this)?.modifiedMillis ?: 0L
        watchedFolder = runCatching {
            contentResolver.query(ScreenshotFolder.children(tree), FOLDER_PROJECTION, null, null, null)
        }.getOrNull()
        watchedFolder?.registerContentObserver(observer)
    }

    private fun closeFolder() {
        watchedFolder?.let {
            it.unregisterContentObserver(observer)
            it.close()
        }
        watchedFolder = null
    }

    private fun onFolderChanged() {
        val item = ScreenshotActions.latestScreenshot(applicationContext, RECENT_SECONDS) ?: return
        if (item.modifiedMillis <= lastSeenMillis) return
        lastSeenMillis = item.modifiedMillis

        if (prefs.overlayEnabled && Settings.canDrawOverlays(this)) {
            main.post { showOverlay(item) }
        }

        if (!prefs.watcherEnabled) return
        val delayMs = prefs.notificationDelaySeconds * 1000L
        handler.postDelayed({
            val current = ScreenshotActions.itemFor(applicationContext, item.uri) ?: return@postDelayed
            ScreenshotNotifier.notifyScreenshot(applicationContext, current)
            scheduleAutoDismiss(ScreenshotNotifier.notificationIdFor(current))
        }, delayMs)
    }

    private fun scheduleAutoDismiss(notifId: Int) {
        val seconds = prefs.notificationAutoDismissSeconds
        if (seconds <= ScreenshotPrefs.AUTO_DISMISS_OFF) return
        handler.postDelayed(
            { ScreenshotNotifier.cancel(applicationContext, notifId) },
            seconds * 1000L
        )
    }

    private fun showOverlay(item: ScreenshotItem) {
        val view = overlay ?: return
        main.removeCallbacks(hideOverlay)
        view.show(
            prefs.overlayX,
            prefs.overlayY,
            prefs.overlayShowCopy,
            prefs.overlayShowDelete,
            { startAction(ScreenshotActionActivity.ACTION_COPY_DELETE, item) },
            { startAction(ScreenshotActionActivity.ACTION_DELETE, item) }
        )
        main.postDelayed(hideOverlay, prefs.overlayTimeoutSeconds * 1000L)
    }

    private fun startAction(action: String, item: ScreenshotItem) {
        main.removeCallbacks(hideOverlay)
        overlay?.hide()
        runCatching {
            startActivity(
                ScreenshotActionActivity.intent(applicationContext, action, item.uri, -1)
            )
        }
    }

    override fun onDestroy() {
        handler.post { closeFolder() }
        main.removeCallbacks(hideOverlay)
        overlay?.hide()
        overlay = null
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val RECENT_SECONDS = 20L
        private val FOLDER_PROJECTION = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenshotWatcherService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenshotWatcherService::class.java))
        }

        fun sync(context: Context) {
            val prefs = ScreenshotPrefs(context)
            if (prefs.watcherEnabled || prefs.overlayEnabled) start(context) else stop(context)
        }
    }
}
