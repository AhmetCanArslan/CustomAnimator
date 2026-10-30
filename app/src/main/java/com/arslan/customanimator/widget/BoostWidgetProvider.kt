package com.arslan.customanimator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.arslan.customanimator.R
import com.arslan.customanimator.service.BoostService

class BoostWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, appWidgetManager, id))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetManager, appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOST && !BoostWidgetState.isRunning(context)) {
            BoostService.start(context)
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_BOOST = "com.arslan.customanimator.widget.BOOST"

        private const val WIDE_MIN_WIDTH_DP = 140
        private const val FULL_MIN_HEIGHT_DP = 120

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BoostWidgetProvider::class.java))
            for (id in ids) manager.updateAppWidget(id, buildRemoteViews(context, manager, id))
        }

        private fun buildRemoteViews(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return RemoteViews(
                    mapOf(
                        SizeF(60f, 60f) to viewsFor(context, R.layout.widget_boost_tiny, false),
                        SizeF(140f, 60f) to viewsFor(context, R.layout.widget_boost_wide, true),
                        SizeF(140f, 120f) to viewsFor(context, R.layout.widget_boost, true)
                    )
                )
            }

            val options = manager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDE_MIN_WIDTH_DP)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, FULL_MIN_HEIGHT_DP)

            return when {
                width < WIDE_MIN_WIDTH_DP -> viewsFor(context, R.layout.widget_boost_tiny, false)
                height < FULL_MIN_HEIGHT_DP -> viewsFor(context, R.layout.widget_boost_wide, true)
                else -> viewsFor(context, R.layout.widget_boost, true)
            }
        }

        private fun viewsFor(context: Context, layoutId: Int, hasText: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)
            val running = BoostWidgetState.isRunning(context)
            val result = BoostWidgetState.result(context)

            applyBackground(context, views)
            tintButton(context, views, hasText)

            if (hasText) {
                applyColor(context, views, R.id.boost_title, "setTextColor", R.color.widget_dynamic_text)
                applyColor(context, views, R.id.boost_status, "setTextColor", R.color.widget_dynamic_text)
                applyColor(context, views, R.id.boost_icon, "setImageTintList", R.color.widget_dynamic_accent)
                views.setTextViewText(R.id.boost_title, context.getString(R.string.boost_widget_title))
                views.setTextViewText(
                    R.id.boost_status,
                    when {
                        running -> context.getString(R.string.boost_widget_running)
                        result.isNotBlank() -> result
                        else -> context.getString(R.string.boost_widget_idle)
                    }
                )
            }

            views.setViewVisibility(R.id.boost_progress, if (running) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.boost_button, if (running) View.GONE else View.VISIBLE)

            val intent = Intent(context, BoostWidgetProvider::class.java).apply { action = ACTION_BOOST }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.boost_button, pendingIntent)

            return views
        }

        private fun tintButton(context: Context, views: RemoteViews, hasText: Boolean) {
            if (hasText) {
                applyColor(context, views, R.id.boost_button, "setTextColor", R.color.widget_dynamic_background)
            } else {
                applyColor(context, views, R.id.boost_button, "setImageTintList", R.color.widget_dynamic_background)
            }
            applyColor(context, views, R.id.boost_button, "setBackgroundTintList", R.color.widget_dynamic_accent)
        }

        private fun applyBackground(context: Context, views: RemoteViews) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setInt(R.id.boost_root, "setBackgroundResource", R.drawable.widget_bg_24)
                views.setColorStateList(R.id.boost_root, "setBackgroundTintList", R.color.widget_dynamic_background)
            } else {
                views.setInt(
                    R.id.boost_root,
                    "setBackgroundColor",
                    ContextCompat.getColor(context, R.color.widget_dynamic_background)
                )
            }
        }

        private fun applyColor(context: Context, views: RemoteViews, viewId: Int, method: String, colorRes: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(viewId, method, colorRes)
                return
            }
            val color = ContextCompat.getColor(context, colorRes)
            when (method) {
                "setTextColor" -> views.setTextColor(viewId, color)
                "setImageTintList" -> views.setInt(viewId, "setColorFilter", color)
                else -> views.setInt(viewId, "setBackgroundColor", color)
            }
        }
    }
}
