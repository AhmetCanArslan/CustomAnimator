package com.arslan.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.service.watcher.AppThreadingWatcher
import com.arslan.customanimator.service.watcher.AppVisibilityWatcher
import com.arslan.customanimator.service.watcher.AutoForceStopWatcher
import com.arslan.customanimator.service.watcher.PerAppRefreshRateWatcher
import com.arslan.customanimator.service.watcher.PerAppWidthWatcher
import com.arslan.customanimator.service.watcher.PermissionDisablerWatcher
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rikka.shizuku.Shizuku

class ForegroundAppWatcherService : Service() {

    companion object {
        private const val TAG = "ForegroundAppWatcher"
        private const val CHANNEL_ID = "foreground_app_watcher_channel"
        private const val NOTIF_ID = 4201
        private const val RECONNECT_BASE_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        private val LEGACY_CHANNEL_IDS = listOf(
            "auto_force_stop_channel",
            "per_app_width_channel",
            "per_app_refresh_rate_channel",
            "app_threading_channel"
        )

        val watchers: List<AppVisibilityWatcher> = listOf(
            AutoForceStopWatcher,
            PermissionDisablerWatcher,
            PerAppWidthWatcher,
            PerAppRefreshRateWatcher,
            AppThreadingWatcher
        )

        fun sync(context: Context) {
            val appContext = context.applicationContext
            watchers.forEach { it.refresh(appContext) }
            if (watchers.any { it.isEnabled }) {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, ForegroundAppWatcherService::class.java)
                )
            } else {
                appContext.stopService(Intent(appContext, ForegroundAppWatcherService::class.java))
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            appContext.stopService(Intent(appContext, ForegroundAppWatcherService::class.java))
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private val eventDispatcher = Dispatchers.Default.limitedParallelism(1)

    @Volatile
    private var listening = false

    @Volatile
    private var screenOn = true

    @Volatile
    private var activeWatchers: List<AppVisibilityWatcher> = emptyList()

    private lateinit var tracker: AppVisibilityTracker
    private lateinit var monitor: AppMonitorConnection

    private val reconnectBackoff = ReconnectBackoff(RECONNECT_BASE_DELAY_MS, MAX_RECONNECT_ATTEMPTS)
    private var reconnectJob: Job? = null

    private val foregroundListener = object : IForegroundAppListener.Stub() {
        override fun onForegroundActivitiesChanged(pid: Int, packages: Array<String>?, foreground: Boolean) {
            handleEvent { tracker.onForegroundActivitiesChanged(pid, packages.orEmpty().toSet(), foreground) }
        }

        override fun onProcessDied(pid: Int) {
            handleEvent { tracker.onProcessDied(pid) }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { startMonitoring() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        monitor.disconnect()
        onListeningChanged(false)
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> startMonitoring() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> onScreenOn()
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        deleteLegacyChannels()
        createNotificationChannel()
        refreshWatchers()
        tracker = AppVisibilityTracker(packageName)
        monitor = AppMonitorConnection(this, foregroundListener, ::onListeningChanged, ::onConnectionLost)
        screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        startForeground(NOTIF_ID, buildNotification())
        scope.launch(eventDispatcher) {
            watchers.forEach { it.recoverStaleState(applicationContext) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshWatchers()
        if (activeWatchers.isEmpty()) {
            Log.d(TAG, "No watcher enabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        refreshNotification()
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        if (activeWatchers.isEmpty()) return
        if (!ShizukuHelper.hasShizukuPermission()) {
            onListeningChanged(false)
            return
        }
        monitor.connect()
    }

    private fun onConnectionLost() {
        onListeningChanged(false)
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val delayMs = reconnectBackoff.nextDelayMs()
        if (delayMs == null) {
            Log.d(TAG, "Foreground monitor lost, giving up until the next trigger")
            return
        }
        Log.d(TAG, "Foreground monitor lost, reconnecting in ${delayMs}ms")
        reconnectJob = scope.launch(Dispatchers.Main) {
            delay(delayMs)
            startMonitoring()
        }
    }

    private fun onListeningChanged(isListening: Boolean) {
        val changed = listening != isListening
        listening = isListening
        if (isListening) {
            reconnectBackoff.reset()
            reconnectJob?.cancel()
        }
        if (!isListening) {
            scope.launch(eventDispatcher) {
                tracker.reset()
                stopWatchers()
            }
        }
        if (changed) {
            Log.d(TAG, "Foreground monitor listening=$isListening")
            refreshNotification()
        }
    }

    private fun onScreenOn() {
        screenOn = true
        scope.launch(eventDispatcher) { reassertVisible(activeWatchers, emptySet()) }
        startMonitoring()
    }

    private fun onScreenOff() {
        screenOn = false
        scope.launch(eventDispatcher) { stopWatchers() }
    }

    private fun stopWatchers() {
        watchers.forEach { it.onWatchStopped(applicationContext) }
    }

    private fun handleEvent(update: () -> List<VisibilityChange>) {
        scope.launch(eventDispatcher) {
            val changes = update()
            if (!screenOn) return@launch
            val watchersNow = activeWatchers
            changes.forEach { dispatch(watchersNow, it) }
            if (changes.any { !it.foreground }) {
                val justForegrounded = changes.filter { it.foreground }.map { it.packageName }.toSet()
                reassertVisible(watchersNow, justForegrounded)
            }
        }
    }

    private fun reassertVisible(watchersNow: List<AppVisibilityWatcher>, skip: Set<String>) {
        (tracker.visiblePackages() - skip).forEach {
            dispatch(watchersNow, VisibilityChange(it, foreground = true))
        }
    }

    private fun dispatch(watchersNow: List<AppVisibilityWatcher>, change: VisibilityChange) {
        watchersNow.forEach {
            if (change.foreground) {
                it.onAppForegrounded(applicationContext, change.packageName, scope)
            } else {
                it.onAppBackgrounded(applicationContext, change.packageName, scope)
            }
        }
    }

    private fun refreshWatchers() {
        watchers.forEach { it.refresh(applicationContext) }
        activeWatchers = watchers.filter { it.isEnabled }
    }

    private fun refreshNotification() {
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification())
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification permission not granted, skipping notification refresh")
        }
    }

    private fun deleteLegacyChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            LEGACY_CHANNEL_IDS.forEach { manager.deleteNotificationChannel(it) }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_watcher_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.app_watcher_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (!listening) {
            getString(R.string.app_watcher_notif_text_waiting)
        } else {
            getString(R.string.app_watcher_notif_text, activeWatchers.size)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_watcher_notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        monitor.disconnect()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Screen receiver already unregistered")
        }
        runBlocking(eventDispatcher) { stopWatchers() }
        job.cancel()
        super.onDestroy()
    }
}
