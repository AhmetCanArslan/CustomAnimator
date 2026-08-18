package com.arslan.customanimator.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.utils.BatteryAlertNotifier
import com.arslan.customanimator.utils.BatteryAlertPrefs

class BatteryAlertService : Service() {

    private lateinit var prefs: BatteryAlertPrefs
    private var receiver: BroadcastReceiver? = null
    private var lastLevel = -1

    override fun onCreate() {
        super.onCreate()
        prefs = BatteryAlertPrefs(this)
        BatteryAlertNotifier.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(lastLevel.coerceAtLeast(currentLevel()))
        registerReceiver()
        return START_STICKY
    }

    private fun currentLevel(): Int {
        val bm = getSystemService(BatteryManager::class.java) ?: return 0
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun startInForeground(level: Int) {
        val notification = BatteryAlertNotifier.buildWatcherNotification(this, level)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            BatteryAlertNotifier.WATCHER_NOTIFICATION_ID,
            notification,
            type
        )
    }

    private fun registerReceiver() {
        if (receiver != null) return
        val rcv = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onBatteryChanged(intent)
            }
        }
        ContextCompat.registerReceiver(
            this,
            rcv,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiver = rcv
    }

    private fun onBatteryChanged(intent: Intent) {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (raw < 0 || scale <= 0) return
        val level = raw * 100 / scale
        if (level == lastLevel) return
        lastLevel = level

        startInForeground(level)

        if (!prefs.anyEnabled) {
            stopSelf()
            return
        }

        if (prefs.lowEnabled) {
            val threshold = prefs.lowLevel
            if (level <= threshold) {
                if (!prefs.lowNotified || prefs.repeatAlerts) {
                    BatteryAlertNotifier.notifyLow(this, level, threshold)
                    prefs.lowNotified = true
                }
            } else {
                prefs.lowNotified = false
            }
        }

        if (prefs.highEnabled) {
            val threshold = prefs.highLevel
            if (level >= threshold) {
                if (!prefs.highNotified || prefs.repeatAlerts) {
                    BatteryAlertNotifier.notifyHigh(this, level, threshold)
                    prefs.highNotified = true
                }
            } else {
                prefs.highNotified = false
            }
        }
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BatteryAlertService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryAlertService::class.java))
            NotificationManagerCompat.from(context).apply {
                cancel(BatteryAlertNotifier.LOW_NOTIFICATION_ID)
                cancel(BatteryAlertNotifier.HIGH_NOTIFICATION_ID)
            }
        }

        fun sync(context: Context) {
            val prefs = BatteryAlertPrefs(context)
            if (prefs.anyEnabled) start(context) else stop(context)
        }
    }
}
