package com.arslan.customanimator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.widget.WidgetMaterialColors
import com.arslan.customanimator.service.GameModeToggleService
import com.arslan.customanimator.utils.GameModeController

class GameModeWidgetProvider : AppWidgetProvider() {

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
        if (intent.action == ACTION_TOGGLE && !GameModeWidgetState.isRunning(context)) {
            GameModeToggleService.start(context)
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.arslan.customanimator.widget.GAME_MODE_TOGGLE"

        private const val WIDE_MIN_WIDTH_DP = 140
        private const val FULL_MIN_HEIGHT_DP = 120

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GameModeWidgetProvider::class.java))
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
                        SizeF(60f, 60f) to viewsFor(context, R.layout.widget_game_mode_tiny, false),
                        SizeF(140f, 60f) to viewsFor(context, R.layout.widget_game_mode_wide, true),
                        SizeF(140f, 120f) to viewsFor(context, R.layout.widget_game_mode, true)
                    )
                )
            }

            val options = manager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDE_MIN_WIDTH_DP)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, FULL_MIN_HEIGHT_DP)

            return when {
                width < WIDE_MIN_WIDTH_DP -> viewsFor(context, R.layout.widget_game_mode_tiny, false)
                height < FULL_MIN_HEIGHT_DP -> viewsFor(context, R.layout.widget_game_mode_wide, true)
                else -> viewsFor(context, R.layout.widget_game_mode, true)
            }
        }

        private fun viewsFor(context: Context, layoutId: Int, hasText: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)
            val colors = WidgetMaterialColors.resolve(context)
            val running = GameModeWidgetState.isRunning(context)
            val active = GameModeController.isActive(context)
            val accent = if (active) colors.accent.toInt() else colors.text.toInt()

            applyBackground(views, WidgetMaterialColors.withAlpha(colors.background, 100))
            tintButton(views, accent, hasText, colors.background.toInt())

            if (hasText) {
                views.setTextColor(R.id.game_mode_title, colors.text.toInt())
                views.setTextColor(R.id.game_mode_status, colors.text.toInt())
                views.setInt(R.id.game_mode_icon, "setColorFilter", accent)
                views.setTextViewText(R.id.game_mode_title, context.getString(R.string.game_mode))
                views.setTextViewText(
                    R.id.game_mode_status,
                    context.getString(
                        if (active) R.string.game_mode_status_active else R.string.game_mode_status_inactive
                    )
                )
                views.setTextViewText(
                    R.id.game_mode_button,
                    context.getString(
                        if (active) R.string.game_mode_turn_off else R.string.game_mode_turn_on
                    )
                )
            }

            views.setViewVisibility(R.id.game_mode_progress, if (running) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.game_mode_button, if (running) View.GONE else View.VISIBLE)

            val intent = Intent(context, GameModeWidgetProvider::class.java).apply { action = ACTION_TOGGLE }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.game_mode_button, pendingIntent)

            return views
        }

        private fun tintButton(views: RemoteViews, color: Int, hasText: Boolean, contentColor: Int) {
            if (hasText) {
                views.setTextColor(R.id.game_mode_button, contentColor)
            } else {
                views.setInt(R.id.game_mode_button, "setColorFilter", contentColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.game_mode_button,
                    "setBackgroundTintList",
                    ColorStateList.valueOf(color)
                )
            } else {
                views.setInt(R.id.game_mode_button, "setBackgroundColor", color)
            }
        }

        private fun applyBackground(views: RemoteViews, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setInt(R.id.game_mode_root, "setBackgroundResource", R.drawable.widget_bg_24)
                views.setColorStateList(
                    R.id.game_mode_root,
                    "setBackgroundTintList",
                    ColorStateList.valueOf(color)
                )
            } else {
                views.setInt(R.id.game_mode_root, "setBackgroundColor", color)
            }
        }
    }
}
