package com.arslan.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoForceStopService : Service() {

    companion object {
        private const val TAG = "AutoForceStopService"
        private const val CHANNEL_ID = "auto_force_stop_channel"
        private const val NOTIF_ID = 4201
        private const val POLL_INTERVAL_MS = 1500L
        private const val RECENTLY_KILLED_TTL_MS = 3000L

        fun start(context: Context) {
            val intent = Intent(context, AutoForceStopService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutoForceStopService::class.java))
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var pollingJob: Job? = null
    private lateinit var manager: AutoForceStopManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        manager = AutoForceStopManager(applicationContext)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prerequisitesMet = ShizukuHelper.hasShizukuPermission() &&
            UsageAccessHelper.hasUsageAccess(applicationContext)

        if (manager.getSelectedPackages().isEmpty() || !prerequisitesMet) {
            Log.d(TAG, "Stopping: selection empty or prerequisites missing")
            stopSelf()
            return START_NOT_STICKY
        }

        if (pollingJob?.isActive != true) {
            pollingJob = scope.launch { pollLoop() }
        }

        return START_NOT_STICKY
    }

    private suspend fun pollLoop() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val recentlyKilled = mutableMapOf<String, Long>()
        var previousForegroundPackage: String? = null
        var lastEventTime = System.currentTimeMillis() - POLL_INTERVAL_MS

        while (true) {
            delay(POLL_INTERVAL_MS)

            val selected = manager.getSelectedPackages()
            if (selected.isEmpty()) {
                Log.d(TAG, "Selection became empty, stopping service")
                stopSelf()
                return
            }

            if (!ShizukuHelper.hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission revoked, stopping service")
                stopSelf()
                return
            }

            val now = System.currentTimeMillis()
            recentlyKilled.entries.removeAll { now - it.value > RECENTLY_KILLED_TTL_MS }

            val currentForegroundPackage = try {
                queryLatestForegroundPackage(usageStatsManager, lastEventTime, now)
            } catch (e: SecurityException) {
                Log.d(TAG, "Usage access revoked, stopping service")
                stopSelf()
                return
            } catch (e: Exception) {
                null
            }
            lastEventTime = now

            if (currentForegroundPackage != null && currentForegroundPackage != previousForegroundPackage) {
                val leftPackage = previousForegroundPackage
                if (leftPackage != null &&
                    leftPackage != applicationContext.packageName &&
                    leftPackage in selected &&
                    !recentlyKilled.containsKey(leftPackage)
                ) {
                    recentlyKilled[leftPackage] = now
                    scope.launch(Dispatchers.IO) {
                        val success = DeveloperOptionsManager.forceStopApp(leftPackage)
                        Log.d(TAG, "Force-stopped $leftPackage success=$success")
                    }
                }
                previousForegroundPackage = currentForegroundPackage
            }
        }
    }

    private fun queryLatestForegroundPackage(
        usageStatsManager: UsageStatsManager,
        beginTime: Long,
        endTime: Long
    ): String? {
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var latestPackage: String? = null
        var latestTimestamp = -1L
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.timeStamp >= latestTimestamp
            ) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.auto_force_stop_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.auto_force_stop_channel_desc)
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.auto_force_stop_notif_title))
            .setContentText(getString(R.string.auto_force_stop_notif_text))
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        job.cancel()
        super.onDestroy()
    }
}
