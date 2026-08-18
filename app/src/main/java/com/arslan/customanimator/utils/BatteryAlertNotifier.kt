package com.arslan.customanimator.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R

object BatteryAlertNotifier {

    const val CHANNEL_WATCHER = "battery_alert_watcher"
    const val CHANNEL_ALERT = "battery_alert"

    const val WATCHER_NOTIFICATION_ID = 5120
    const val LOW_NOTIFICATION_ID = 5121
    const val HIGH_NOTIFICATION_ID = 5122

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        val watcher = NotificationChannel(
            CHANNEL_WATCHER,
            context.getString(R.string.bt_alert_channel_watcher_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply { description = context.getString(R.string.bt_alert_channel_watcher_desc) }

        val alert = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.bt_alert_channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.bt_alert_channel_alert_desc) }

        nm.createNotificationChannel(watcher)
        nm.createNotificationChannel(alert)
    }

    fun buildWatcherNotification(context: Context, level: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHER)
            .setSmallIcon(R.drawable.ic_notification_battery)
            .setContentTitle(context.getString(R.string.bt_alert_watcher_title))
            .setContentText(context.getString(R.string.bt_alert_watcher_text, level))
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(appIntent(context))
            .build()

    fun notifyLow(context: Context, level: Int, threshold: Int) {
        notify(
            context,
            LOW_NOTIFICATION_ID,
            context.getString(R.string.bt_alert_low_title, threshold),
            context.getString(R.string.bt_alert_low_text, level)
        )
    }

    fun notifyHigh(context: Context, level: Int, threshold: Int) {
        notify(
            context,
            HIGH_NOTIFICATION_ID,
            context.getString(R.string.bt_alert_high_title, threshold),
            context.getString(R.string.bt_alert_high_text, level)
        )
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification_battery)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(appIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun appIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
