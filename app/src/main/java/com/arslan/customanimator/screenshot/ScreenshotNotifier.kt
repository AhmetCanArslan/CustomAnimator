package com.arslan.customanimator.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.R

object ScreenshotNotifier {

    const val CHANNEL_WATCHER = "screenshot_actions_watcher"
    const val CHANNEL_ALERT = "screenshot_actions_alert"

    const val WATCHER_NOTIFICATION_ID = 4711

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        val watcher = NotificationChannel(
            CHANNEL_WATCHER,
            context.getString(R.string.screenshot_channel_watcher_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = context.getString(R.string.screenshot_channel_watcher_desc) }

        val alert = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.screenshot_channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.screenshot_channel_alert_desc) }

        nm.createNotificationChannel(watcher)
        nm.createNotificationChannel(alert)
    }

    fun buildWatcherNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_WATCHER)
            .setSmallIcon(R.drawable.ic_notification_screenshot)
            .setContentTitle(context.getString(R.string.screenshot_watcher_title))
            .setContentText(context.getString(R.string.screenshot_watcher_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun notifyScreenshot(context: Context, item: ScreenshotItem) {
        val prefs = ScreenshotPrefs(context)
        val notifId = notificationIdFor(item)
        val preview = if (prefs.notificationShowPreview) {
            ScreenshotActions.decodePreview(context, item)
        } else {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification_screenshot)
            .setContentTitle(context.getString(R.string.screenshot_alert_title))
            .setContentText(item.name)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (prefs.notificationShowDelete) {
            builder.addAction(
                0,
                context.getString(R.string.screenshot_action_delete),
                actionPendingIntent(
                    context,
                    ScreenshotActionActivity.ACTION_DELETE,
                    item,
                    notifId
                )
            )
        }
        if (prefs.notificationShowCopy) {
            builder.addAction(
                0,
                context.getString(R.string.screenshot_action_copy_delete),
                actionPendingIntent(
                    context,
                    ScreenshotActionActivity.ACTION_COPY_DELETE,
                    item,
                    notifId
                )
            )
        }

        if (preview != null) {
            builder.setLargeIcon(preview)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(preview))
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    fun notificationIdFor(item: ScreenshotItem): Int =
        ((item.id.hashCode()) and 0x7FFFFFFF).coerceAtLeast(WATCHER_NOTIFICATION_ID + 1)

    private fun actionPendingIntent(
        context: Context,
        action: String,
        item: ScreenshotItem,
        notifId: Int
    ): PendingIntent {
        val intent = ScreenshotActionActivity.intent(context, action, item.id, notifId).apply {
            data = Uri.fromParts("screenshot", item.id.toString(), action)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, action.hashCode(), intent, flags)
    }
}
