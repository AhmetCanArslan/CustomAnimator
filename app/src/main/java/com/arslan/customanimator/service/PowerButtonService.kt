package com.arslan.customanimator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonTrigger
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.ShizukuUserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class PowerButtonService : Service() {

    companion object {
        private const val TAG = "PowerButtonService"
        private const val CHANNEL_ID = "power_button_channel"
        private const val NOTIF_ID = 4203
        private const val SERVICE_VERSION = 12
        private const val RECONNECT_BASE_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val PRESS_WAKE_MARGIN_MS = 1000L

        fun sync(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, PowerButtonService::class.java)
            if (isNeeded(appContext)) {
                ContextCompat.startForegroundService(appContext, intent)
            } else {
                appContext.stopService(intent)
            }
        }

        private fun isNeeded(context: Context): Boolean =
            ButtonRemapperManager.activeServiceTriggers(context).isNotEmpty()
    }

    private enum class Status(val textRes: Int) {
        WAITING(R.string.app_watcher_notif_text_waiting),
        ACTIVE(R.string.power_button_notif_active),
        UNSUPPORTED(R.string.power_button_notif_unsupported)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val reconnectBackoff = ReconnectBackoff(RECONNECT_BASE_DELAY_MS, MAX_RECONNECT_ATTEMPTS)
    private var reconnectJob: Job? = null
    private var status = Status.WAITING
    private var bound = false

    @Volatile
    private var userService: IPowerButtonUserService? = null

    @Volatile
    private var pressWakeMs = PRESS_WAKE_MARGIN_MS

    private val pressWakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CustomAnimator:PowerPress")
            .apply { setReferenceCounted(false) }
    }

    private val args by lazy {
        ShizukuUserService.args(this, PowerButtonUserService::class.java, "power_button", daemon = false, revision = SERVICE_VERSION)
    }

    private val listener = object : IPowerButtonListener.Stub() {
        override fun onTrigger(trigger: String?) {
            ButtonTrigger.fromName(trigger)?.let { scope.launch { perform(it) } }
        }

        override fun onButtonKey() {
            pressWakeLock.acquire(pressWakeMs)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = binder?.takeIf { it.pingBinder() }?.let { IPowerButtonUserService.Stub.asInterface(it) }
            userService = service
            scope.launch { startListening(service) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            if (!bound) return
            bound = false
            updateStatus(Status.WAITING)
            scheduleReconnect()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { connect() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        disconnect()
        updateStatus(Status.WAITING)
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ -> connect() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scope.launch { applyGrab() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground()
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isNeeded(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val service = userService
        if (service == null) connect() else scope.launch { startListening(service) }
        return START_STICKY
    }

    private fun connect() {
        if (bound || !ShizukuHelper.hasShizukuPermission()) return
        bound = true
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Throwable) {
            Log.e(TAG, "bindUserService failed", e)
            bound = false
            scheduleReconnect()
        }
    }

    private fun disconnect() {
        userService = null
        if (!bound) return
        bound = false
        runCatching { Shizuku.unbindUserService(args, connection, true) }
    }

    private suspend fun startListening(service: IPowerButtonUserService?) {
        val started = withContext(Dispatchers.IO) {
            val triggers = ButtonRemapperManager.activeServiceTriggers(applicationContext)
            val windowMs = ButtonRemapperManager.getDoublePressWindowMs(applicationContext)
            pressWakeMs = windowMs + PRESS_WAKE_MARGIN_MS
            val triggerNames = triggers.map { it.name }.toTypedArray()
            runCatching { service?.start(listener, windowMs, triggerNames) == true }
                .getOrDefault(false)
        }
        if (!started) {
            disconnect()
            updateStatus(Status.UNSUPPORTED)
            return
        }
        reconnectBackoff.reset()
        reconnectJob?.cancel()
        applyGrab()
    }

    private suspend fun applyGrab() {
        val service = userService ?: return
        val interactive = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        val applied = withContext(Dispatchers.IO) {
            runCatching { service.setGrabbed(interactive) }.getOrDefault(false)
        }
        updateStatus(if (applied) Status.ACTIVE else Status.UNSUPPORTED)
    }

    private suspend fun perform(trigger: ButtonTrigger) {
        val performed = withContext(Dispatchers.IO) { ButtonRemapperManager.perform(applicationContext, trigger) }
        if (!performed) {
            Toast.makeText(this, ShizukuHelper.failureMessage(this), Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val delayMs = reconnectBackoff.nextDelayMs() ?: return
        reconnectJob = scope.launch {
            delay(delayMs)
            connect()
        }
    }

    private fun updateStatus(newStatus: Status) {
        if (status == newStatus) return
        status = newStatus
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification())
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification permission not granted, skipping notification refresh")
        }
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.button_remapper_power_double_press),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setSound(null, null) }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.button_remapper))
            .setContentText(getString(status.textRes))
            .setSmallIcon(R.drawable.ic_tile_power_settings_new)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        disconnect()
        if (pressWakeLock.isHeld) pressWakeLock.release()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Screen receiver already unregistered")
        }
        scope.cancel()
        super.onDestroy()
    }
}
