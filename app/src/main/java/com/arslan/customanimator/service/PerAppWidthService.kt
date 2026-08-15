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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.PerAppWidthManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PerAppWidthService : Service() {

    companion object {
        private const val TAG = "PerAppWidthService"
        private const val CHANNEL_ID = "per_app_width_channel"
        private const val NOTIF_ID = 4203
        private const val POLL_INTERVAL_MS = 1000L
        private const val IDLE_POLL_INTERVAL_MS = 5000L
        private const val ACTIVITY_STOPPED = 23

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PerAppWidthService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PerAppWidthService::class.java))
        }

        fun startIfOverridesExist(context: Context) {
            if (PerAppWidthManager(context).getOverrides().isNotEmpty()) {
                start(context)
            }
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var pollingJob: Job? = null

    @Volatile
    private var isIdle = false

    @Volatile
    private var appliedPackage: String? = null

    @Volatile
    private var baselineDensity: Int? = null

    private lateinit var widthManager: PerAppWidthManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        widthManager = PerAppWidthManager(applicationContext)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (widthManager.getOverrides().isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        refreshNotification()

        if (pollingJob?.isActive != true) {
            pollingJob = scope.launch { pollLoop() }
        }

        return START_STICKY
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
        val visiblePackages = mutableSetOf<String>()
        var lastEventTime = System.currentTimeMillis() - POLL_INTERVAL_MS

        while (true) {
            delay(if (isIdle) IDLE_POLL_INTERVAL_MS else POLL_INTERVAL_MS)

            val overrides = widthManager.getOverrides()
            if (overrides.isEmpty()) {
                Log.d(TAG, "No overrides left, stopping service")
                restoreBaseline()
                stopSelf()
                return
            }

            val prerequisitesMet = ShizukuHelper.hasShizukuPermission() &&
                UsageAccessHelper.hasUsageAccess(applicationContext)
            if (!prerequisitesMet) {
                if (!isIdle) {
                    isIdle = true
                    refreshNotification()
                }
                visiblePackages.clear()
                lastEventTime = System.currentTimeMillis()
                continue
            }

            if (isIdle) {
                isIdle = false
                refreshNotification()
                lastEventTime = System.currentTimeMillis()
                visiblePackages.clear()
            }

            val now = System.currentTimeMillis()
            val transitions = queryVisibilityTransitions(usageStatsManager, lastEventTime, now)
            lastEventTime = now

            for (transition in transitions) {
                val packageName = transition.packageName

                if (transition.visible) {
                    visiblePackages.add(packageName)
                    val targetWidthDp = overrides[packageName] ?: continue
                    if (appliedPackage == packageName) continue
                    if (appliedPackage == null) {
                        baselineDensity = SettingsManager.getForcedDensity(contentResolver)
                    }
                    val targetDensity = SettingsManager.densityForSmallestWidth(this, targetWidthDp)
                    val success = SettingsManager.applyDensity(contentResolver, targetDensity)
                    if (success) {
                        appliedPackage = packageName
                    }
                    Log.d(TAG, "Applied width=${targetWidthDp}dp density=$targetDensity for $packageName success=$success")
                    continue
                }

                visiblePackages.remove(packageName)
                if (packageName == appliedPackage) {
                    restoreBaseline()
                }
            }
        }
    }

    private fun restoreBaseline() {
        if (appliedPackage == null) return
        val success = SettingsManager.applyDensity(contentResolver, baselineDensity)
        Log.d(TAG, "Restored baseline density=$baselineDensity success=$success")
        appliedPackage = null
        baselineDensity = null
    }

    private data class VisibilityTransition(val packageName: String, val visible: Boolean)

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
                    transitions.add(VisibilityTransition(event.packageName, true))

                ACTIVITY_STOPPED ->
                    transitions.add(VisibilityTransition(event.packageName, false))

                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        transitions.add(VisibilityTransition(event.packageName, false))
                    }
            }
        }
        return transitions
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.per_app_width_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.per_app_width_channel_desc)
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
            getString(R.string.per_app_width_notif_text_waiting)
        } else {
            getString(R.string.per_app_width_notif_text)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.per_app_width_notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        restoreBaseline()
        job.cancel()
        super.onDestroy()
    }
}
