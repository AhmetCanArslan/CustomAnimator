package com.arslan.customanimator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.BoostStats
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.MemoryBooster
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.widget.BoostWidgetProvider
import com.arslan.customanimator.widget.BoostWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BoostService : Service() {

    companion object {
        private const val CHANNEL_ID = "boost_widget_channel"
        private const val NOTIF_ID = 4301
        private const val SETTLE_DELAY_MS = 2000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, BoostService::class.java))
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var working = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (working) return START_NOT_STICKY
        working = true

        BoostWidgetState.setRunning(applicationContext, true)
        BoostWidgetProvider.updateAll(applicationContext)

        scope.launch { runBoost() }
        return START_NOT_STICKY
    }

    private suspend fun runBoost() {
        if (!ShizukuHelper.hasShizukuPermission()) {
            finish(getString(R.string.boost_widget_needs_shizuku))
            return
        }

        val before = BoostStats.snapshot(applicationContext)

        DeveloperOptionsManager.clearAllAppCaches()

        MemoryBooster.boost()

        delay(SETTLE_DELAY_MS)

        val after = BoostStats.snapshot(applicationContext)
        val storageFreed = (after.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L)
        val ramFreed = (after.availableRamBytes - before.availableRamBytes).coerceAtLeast(0L)

        val summary = getString(
            R.string.boost_widget_result,
            BoostStats.formatSize(applicationContext, storageFreed),
            BoostStats.formatSize(applicationContext, ramFreed)
        )
        BoostWidgetState.setResult(applicationContext, summary)

        finish(getString(R.string.boost_widget_toast, summary))
    }

    private fun finish(message: String) {
        BoostWidgetState.setRunning(applicationContext, false)
        BoostWidgetProvider.updateAll(applicationContext)
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        working = false
        stopSelf()
    }

    override fun onDestroy() {
        BoostWidgetState.setRunning(applicationContext, false)
        BoostWidgetProvider.updateAll(applicationContext)
        job.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.boost_widget_title),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.boost_widget_title))
            .setContentText(getString(R.string.boost_widget_running))
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
