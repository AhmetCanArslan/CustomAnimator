package com.arslan.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.CompileFilterManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class CompileBoosterService : Service() {

    companion object {
        private const val TAG = "CompileBoosterService"
        private const val CHANNEL_ID = "compile_booster_channel"
        private const val NOTIF_ID = 4401

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, CompileBoosterService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CompileBoosterService::class.java))
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var compileJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification(0, 1, ""))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ShizukuHelper.hasShizukuPermission()) {
            Log.d(TAG, "Stopping: Shizuku permission not granted")
            finish()
            return START_NOT_STICKY
        }

        if (compileJob?.isActive != true) {
            compileJob = scope.launch { runCompileAll() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runCompileAll() {
        val apps = InstalledAppsProvider.getLaunchableApps(applicationContext)
        val total = apps.size
        if (total == 0) {
            finish()
            return
        }

        val filter = CompileFilterManager.getFilter(applicationContext)
        CompileBoosterProgressTracker.update(isRunning = true, current = 0, total = total, currentLabel = "")
        var successCount = 0

        for ((index, app) in apps.withIndex()) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()

            if (!ShizukuHelper.hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission revoked mid-run, stopping")
                finish()
                return
            }

            CompileBoosterProgressTracker.update(isRunning = true, current = index, total = total, currentLabel = app.label)
            updateNotification(index, total, app.label)

            val success = DeveloperOptionsManager.compileApp(app.packageName, filter)
            if (success) successCount++
        }

        Log.d(TAG, "Compiled $successCount/$total apps with ${filter.value}")
        finish()
    }

    private fun finish() {
        CompileBoosterProgressTracker.reset()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.compile_booster_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    description = getString(R.string.compile_booster_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun updateNotification(current: Int, total: Int, currentLabel: String) {
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(current, total, currentLabel))
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification permission not granted, skipping notification refresh")
        }
    }

    private fun buildNotification(current: Int, total: Int, currentLabel: String): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val percent = if (total <= 0) 0 else (current * 100 / total).coerceIn(0, 100)
        val text = if (currentLabel.isEmpty()) {
            getString(R.string.compile_booster_notif_text)
        } else {
            getString(R.string.compile_booster_notif_text_app, currentLabel, percent)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.compile_booster_notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setProgress(100, percent, false)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        compileJob?.cancel()
        job.cancel()
        CompileBoosterProgressTracker.reset()
        super.onDestroy()
    }
}
