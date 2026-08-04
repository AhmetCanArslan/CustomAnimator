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
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.DangerousPermissionsHelper
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.PermissionDisablerManager
import com.arslan.customanimator.utils.RevokedPermissionsStore
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
        private const val IDLE_POLL_INTERVAL_MS = 5000L
        private const val RECENTLY_KILLED_TTL_MS = 3000L

        fun start(context: Context) {
            val intent = Intent(context, AutoForceStopService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutoForceStopService::class.java))
        }

        fun startIfSelectionExists(context: Context) {
            val hasSelection = AutoForceStopManager(context).getSelectedPackages().isNotEmpty() ||
                PermissionDisablerManager(context).getSelectedPackages().isNotEmpty()
            if (hasSelection) {
                start(context)
            }
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var pollingJob: Job? = null

    @Volatile
    private var isIdle = false

    private lateinit var forceStopManager: AutoForceStopManager
    private lateinit var permissionDisablerManager: PermissionDisablerManager
    private lateinit var revokedStore: RevokedPermissionsStore

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        forceStopManager = AutoForceStopManager(applicationContext)
        permissionDisablerManager = PermissionDisablerManager(applicationContext)
        revokedStore = RevokedPermissionsStore(applicationContext)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val anySelection = forceStopManager.getSelectedPackages().isNotEmpty() ||
            permissionDisablerManager.getSelectedPackages().isNotEmpty()

        if (!anySelection) {
            Log.d(TAG, "Stopping: no apps selected for either tool")
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
        val recentlyKilled = mutableMapOf<String, Long>()
        var previousForegroundPackage: String? = null
        var lastEventTime = System.currentTimeMillis() - POLL_INTERVAL_MS

        while (true) {
            delay(if (isIdle) IDLE_POLL_INTERVAL_MS else POLL_INTERVAL_MS)

            val forceStopSelected = forceStopManager.getSelectedPackages()
            val permissionSelected = permissionDisablerManager.getSelectedPackages()
            if (forceStopSelected.isEmpty() && permissionSelected.isEmpty()) {
                Log.d(TAG, "Selection became empty, stopping service")
                stopSelf()
                return
            }

            val prerequisitesMet = ShizukuHelper.hasShizukuPermission() &&
                UsageAccessHelper.hasUsageAccess(applicationContext)
            if (!prerequisitesMet) {
                if (!isIdle) {
                    Log.d(TAG, "Prerequisites missing, idling until they come back")
                    isIdle = true
                    refreshNotification()
                }
                previousForegroundPackage = null
                lastEventTime = System.currentTimeMillis()
                continue
            }
            if (isIdle) {
                Log.d(TAG, "Prerequisites restored, resuming watch")
                isIdle = false
                lastEventTime = System.currentTimeMillis()
                refreshNotification()
            }

            val now = System.currentTimeMillis()
            recentlyKilled.entries.removeAll { now - it.value > RECENTLY_KILLED_TTL_MS }

            val currentForegroundPackage = try {
                queryLatestForegroundPackage(usageStatsManager, lastEventTime, now)
            } catch (e: SecurityException) {
                Log.d(TAG, "Usage access revoked, idling until it is granted again")
                isIdle = true
                refreshNotification()
                previousForegroundPackage = null
                lastEventTime = System.currentTimeMillis()
                continue
            } catch (e: Exception) {
                null
            }
            lastEventTime = now

            if (currentForegroundPackage != null && currentForegroundPackage != previousForegroundPackage) {
                val leftPackage = previousForegroundPackage

                if (leftPackage != null && leftPackage != applicationContext.packageName) {
                    if (leftPackage in forceStopSelected && !recentlyKilled.containsKey(leftPackage)) {
                        recentlyKilled[leftPackage] = now
                        scope.launch(Dispatchers.IO) {
                            val success = DeveloperOptionsManager.forceStopApp(leftPackage)
                            Log.d(TAG, "Force-stopped $leftPackage success=$success")
                        }
                    }

                    if (leftPackage in permissionSelected) {
                        scope.launch(Dispatchers.IO) {
                            revokePermissionsForPackage(leftPackage)
                        }
                    }
                }

                if (currentForegroundPackage != applicationContext.packageName &&
                    currentForegroundPackage in permissionSelected
                ) {
                    scope.launch(Dispatchers.IO) {
                        regrantPermissionsForPackage(currentForegroundPackage)
                    }
                }

                previousForegroundPackage = currentForegroundPackage
            }
        }
    }

    private fun revokePermissionsForPackage(packageName: String) {
        val granted = DangerousPermissionsHelper.getGrantedDangerousPermissions(applicationContext, packageName)
        if (granted.isEmpty()) return
        val actuallyRevoked = granted.filter { DeveloperOptionsManager.revokePermission(packageName, it) }
        revokedStore.recordRevoked(packageName, actuallyRevoked)
        Log.d(TAG, "Revoked ${actuallyRevoked.size}/${granted.size} permissions for $packageName")
    }

    private fun regrantPermissionsForPackage(packageName: String) {
        val toRegrant = revokedStore.getRevoked(packageName)
        if (toRegrant.isEmpty()) return
        val allSucceeded = toRegrant.map { DeveloperOptionsManager.grantPermission(packageName, it) }.all { it }
        if (allSucceeded) {
            revokedStore.clearRevoked(packageName)
        }
        Log.d(TAG, "Regranted permissions for $packageName allSucceeded=$allSucceeded")
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
        val hasPermissionSelection = permissionDisablerManager.getSelectedPackages().isNotEmpty()
        val text = when {
            isIdle -> getString(R.string.auto_force_stop_notif_text_waiting)
            hasPermissionSelection -> getString(R.string.auto_force_stop_notif_text_combined)
            else -> getString(R.string.auto_force_stop_notif_text)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.auto_force_stop_notif_title))
            .setContentText(text)
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
