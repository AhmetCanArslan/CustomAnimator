package com.arslan.customanimator.notify.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.WidgetConfigStore
import com.arslan.customanimator.notify.data.WidgetNotificationStore

class NotificationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetConfigStore.delete(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_APP -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (!packageName.isNullOrBlank()) openApp(context, packageName)
            }
            ACTION_CLEAR -> {
                WidgetNotificationStore.clear(context)
                WidgetNotificationStore.notifyWidgets(context)
            }
            ACTION_REFRESH -> WidgetNotificationStore.notifyWidgets(context)
        }
        super.onReceive(context, intent)
    }

    private fun openApp(context: Context, packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        val intent = launch?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ACTION_OPEN_APP = "com.arslan.customanimator.widget.OPEN_APP"
        const val ACTION_CLEAR = "com.arslan.customanimator.widget.CLEAR"
        const val ACTION_REFRESH = "com.arslan.customanimator.widget.REFRESH"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        @Suppress("DEPRECATION")
        fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val config = WidgetConfigStore.load(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_notifications)

            applyBackground(views, config.cornerRadiusDp, config.backgroundColorWithAlpha)

            val headerText = config.headerText.ifBlank { context.getString(R.string.pn_widget_default_header) }
            views.setViewVisibility(R.id.widget_header, if (config.showHeader) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_title, headerText)
            views.setTextColor(R.id.widget_title, config.textColor.toInt())
            views.setTextColor(R.id.widget_empty, config.textColor.toInt())
            views.setInt(R.id.widget_clear, "setColorFilter", config.accentColor.toInt())
            views.setInt(R.id.widget_refresh, "setColorFilter", config.accentColor.toInt())

            val serviceIntent = Intent(context, NotificationWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            views.setPendingIntentTemplate(R.id.widget_list, itemTemplateIntent(context, appWidgetId))
            views.setOnClickPendingIntent(R.id.widget_clear, broadcast(context, appWidgetId, ACTION_CLEAR))
            views.setOnClickPendingIntent(R.id.widget_refresh, broadcast(context, appWidgetId, ACTION_REFRESH))

            return views
        }

        val cornerRadiusOptions = listOf(0, 8, 16, 24, 32)

        private fun backgroundDrawableFor(cornerRadiusDp: Int): Int = when (cornerRadiusDp) {
            0 -> R.drawable.widget_bg_0
            8 -> R.drawable.widget_bg_8
            24 -> R.drawable.widget_bg_24
            32 -> R.drawable.widget_bg_32
            else -> R.drawable.widget_bg_16
        }

        private fun applyBackground(views: RemoteViews, cornerRadiusDp: Int, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setInt(R.id.widget_root, "setBackgroundResource", backgroundDrawableFor(cornerRadiusDp))
                views.setColorStateList(
                    R.id.widget_root,
                    "setBackgroundTintList",
                    android.content.res.ColorStateList.valueOf(color)
                )
            } else {
                views.setInt(R.id.widget_root, "setBackgroundColor", color)
            }
        }

        private fun itemTemplateIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, NotificationWidgetProvider::class.java).apply {
                action = ACTION_OPEN_APP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId * 10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        private fun broadcast(context: Context, appWidgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, NotificationWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + action.hashCode().and(7) + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
