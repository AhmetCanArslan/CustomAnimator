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
import com.arslan.customanimator.utils.GameModeController
import com.arslan.customanimator.utils.GameModeManager
import com.arslan.customanimator.widget.GameModeWidgetProvider
import com.arslan.customanimator.widget.GameModeWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GameModeToggleService : Service() {

    companion object {
        private const val CHANNEL_ID = "game_mode_widget_channel"
        private const val NOTIF_ID = 4302

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, GameModeToggleService::class.java))
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

        GameModeWidgetState.setRunning(applicationContext, true)
        GameModeWidgetProvider.updateAll(applicationContext)

        scope.launch { toggle() }
        return START_NOT_STICKY
    }

    private fun toggle() {
        if (!GameModeController.canApply(applicationContext)) {
            finish(getString(R.string.game_mode_needs_shizuku))
            return
        }

        val enable = !GameModeController.isActive(applicationContext)
        if (enable && GameModeManager(applicationContext).getSelectedPackages().isEmpty()) {
            finish(getString(R.string.game_mode_select_games))
            return
        }

        val result = GameModeController.setActive(applicationContext, enable)
        finish(
            getString(
                when {
                    !result.succeeded -> R.string.game_mode_failed_toast
                    enable -> R.string.game_mode_enabled_toast
                    else -> R.string.game_mode_disabled_toast
                }
            )
        )
    }

    private fun finish(message: String) {
        GameModeWidgetState.setRunning(applicationContext, false)
        GameModeWidgetProvider.updateAll(applicationContext)
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        working = false
        stopSelf()
    }

    override fun onDestroy() {
        GameModeWidgetState.setRunning(applicationContext, false)
        GameModeWidgetProvider.updateAll(applicationContext)
        job.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.game_mode),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.game_mode))
            .setContentText(getString(R.string.working))
            .setSmallIcon(R.drawable.ic_notification_auto_force_stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
