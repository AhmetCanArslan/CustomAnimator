package com.arslan.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
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
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForegroundAppWatcherService : Service() {

    companion object {
        private const val TAG = "ForegroundAppWatcher"
        private const val CHANNEL_ID = "foreground_app_watcher_channel"
        private const val NOTIF_ID = 4201
        private const val POLL_INTERVAL_MS = 1000L
        private const val IDLE_POLL_INTERVAL_MS = 5000L
        private const val ACTIVITY_STOPPED = 23

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
    private var pollingJob: Job? = null

    @Volatile
    private var isIdle = false

    @Volatile
    private var screenOn = true

    @Volatile
    private var selfGrantAttempted = false

    @Volatile
    private var activeWatchers: List<AppVisibilityWatcher> = emptyList()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> resumePolling()
                Intent.ACTION_SCREEN_OFF -> suspendPolling()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        deleteLegacyChannels()
        createNotificationChannel()
        refreshWatchers()
        screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshWatchers()
        if (activeWatchers.isEmpty()) {
            Log.d(TAG, "No watcher enabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        refreshNotification()

        if (screenOn) resumePolling()

        return START_STICKY
    }

    private fun resumePolling() {
        screenOn = true
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch { pollLoop() }
    }

    private fun suspendPolling() {
        screenOn = false
        pollingJob?.cancel()
        pollingJob = null
        watchers.forEach { it.onWatchStopped(applicationContext) }
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

    private suspend fun pollLoop() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val visibleActivities = mutableMapOf<String, MutableSet<String>>()
        var lastEventTime = System.currentTimeMillis() - POLL_INTERVAL_MS

        while (true) {
            delay(if (isIdle) IDLE_POLL_INTERVAL_MS else POLL_INTERVAL_MS)

            val watchersNow = activeWatchers
            if (watchersNow.isEmpty()) {
                Log.d(TAG, "All watchers disabled, stopping service")
                stopSelf()
                return
            }

            val hasShizuku = ShizukuHelper.hasShizukuPermission()
            var hasUsageAccess = UsageAccessHelper.hasUsageAccess(applicationContext)
            if (hasShizuku && !selfGrantAttempted && !hasUsageAccess) {
                selfGrantAttempted = true
                val granted = UsageAccessHelper.grantUsageAccess(applicationContext)
                hasUsageAccess = UsageAccessHelper.hasUsageAccess(applicationContext)
                Log.d(TAG, "Self-granted usage access success=$granted")
            }

            if (!hasShizuku || !hasUsageAccess) {
                if (!isIdle) {
                    Log.d(TAG, "Prerequisites missing, idling until they come back")
                    isIdle = true
                    watchersNow.forEach { it.onWatchStopped(applicationContext) }
                    refreshNotification()
                }
                visibleActivities.clear()
                lastEventTime = System.currentTimeMillis()
                continue
            }

            if (isIdle) {
                Log.d(TAG, "Prerequisites restored, resuming watch")
                isIdle = false
                visibleActivities.clear()
                lastEventTime = System.currentTimeMillis()
                refreshNotification()
            }

            val now = System.currentTimeMillis()
            val transitions = try {
                queryVisibilityTransitions(usageStatsManager, lastEventTime, now)
            } catch (e: SecurityException) {
                Log.d(TAG, "Usage access revoked, idling until it is granted again")
                isIdle = true
                watchersNow.forEach { it.onWatchStopped(applicationContext) }
                refreshNotification()
                visibleActivities.clear()
                lastEventTime = System.currentTimeMillis()
                continue
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read usage events", e)
                lastEventTime = now
                continue
            }
            lastEventTime = now

            for (transition in transitions) {
                val packageName = transition.packageName
                if (packageName == applicationContext.packageName) continue

                if (transition.visible) {
                    val wasVisible = visibleActivities[packageName]?.isNotEmpty() == true
                    visibleActivities.getOrPut(packageName) { mutableSetOf() }.add(transition.className)
                    if (!wasVisible) {
                        watchersNow.forEach {
                            it.onAppForegrounded(applicationContext, packageName, scope)
                        }
                    }
                    continue
                }

                val activities = visibleActivities[packageName] ?: continue
                activities.remove(transition.className)
                if (activities.isEmpty()) {
                    visibleActivities.remove(packageName)
                    watchersNow.forEach {
                        it.onAppBackgrounded(applicationContext, packageName, scope)
                    }
                }
            }

            watchersNow.forEach { it.onTick(applicationContext, scope) }
        }
    }

    private data class VisibilityTransition(
        val packageName: String,
        val className: String,
        val visible: Boolean
    )

    private fun queryVisibilityTransitions(
        usageStatsManager: UsageStatsManager,
        beginTime: Long,
        endTime: Long
    ): List<VisibilityTransition> {
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        val transitions = mutableListOf<VisibilityTransition>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    transitions.add(VisibilityTransition(event.packageName, activityKey(event), true))

                ACTIVITY_STOPPED ->
                    transitions.add(VisibilityTransition(event.packageName, activityKey(event), false))

                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        transitions.add(VisibilityTransition(event.packageName, activityKey(event), false))
                    }
            }
        }
        return transitions
    }

    private fun activityKey(event: UsageEvents.Event): String =
        event.className ?: event.packageName

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
        val text = if (isIdle) {
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
        pollingJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Screen receiver already unregistered")
        }
        watchers.forEach { it.onWatchStopped(applicationContext) }
        job.cancel()
        super.onDestroy()
    }
}
