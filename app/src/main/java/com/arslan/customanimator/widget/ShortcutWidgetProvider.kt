package com.arslan.customanimator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.arslan.customanimator.R
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.service.ShortcutLaunchActivity
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.ShortcutStore

@RequiresApi(Build.VERSION_CODES.O)
class ShortcutWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        ShortcutWidgetState.unbind(context, appWidgetIds)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        ShortcutWidgetState.remap(context, oldWidgetIds, newWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PINNED) bindPinned(context, intent)
        super.onReceive(context, intent)
    }

    private fun bindPinned(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val shortcutId = intent.getStringExtra(ShortcutMaker.EXTRA_SHORTCUT_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || shortcutId == null) return
        ShortcutWidgetState.bind(context, widgetId, shortcutId)
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
    }

    companion object {
        private const val ACTION_PINNED = "com.arslan.customanimator.widget.SHORTCUT_PINNED"
        private const val ICON_PX = 324

        fun requestPin(context: Context, shortcutId: String): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val callback = PendingIntent.getBroadcast(
                context,
                shortcutId.hashCode(),
                Intent(context, ShortcutWidgetProvider::class.java)
                    .setAction(ACTION_PINNED)
                    .putExtra(ShortcutMaker.EXTRA_SHORTCUT_ID, shortcutId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            return manager.requestPinAppWidget(ComponentName(context, ShortcutWidgetProvider::class.java), null, callback)
        }

        fun update(context: Context, shortcutId: String) {
            val manager = AppWidgetManager.getInstance(context)
            for (id in ShortcutWidgetState.widgetIds(context, shortcutId)) {
                manager.updateAppWidget(id, buildRemoteViews(context, id))
            }
        }

        private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_shortcut)
            val shortcutId = ShortcutWidgetState.shortcutId(context, widgetId) ?: return views
            val entry = ShortcutStore.get(context, shortcutId)
            views.setContentDescription(R.id.widget_shortcut_icon, entry?.label ?: context.getString(R.string.shortcut_missing))
            ShortcutStore.loadIcon(context, shortcutId)?.let { icon ->
                views.setImageViewBitmap(R.id.widget_shortcut_icon, shapedIcon(context, icon, entry))
            }
            views.setOnClickPendingIntent(R.id.widget_shortcut_icon, launchIntent(context, widgetId, shortcutId, entry))
            return views
        }

        private fun shapedIcon(context: Context, icon: Bitmap, entry: ShortcutEntry?): Bitmap {
            if (entry?.legacyIcon == true) return icon
            val drawable = AdaptiveIconDrawable(ColorDrawable(Color.TRANSPARENT), BitmapDrawable(context.resources, icon))
            val shaped = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, ICON_PX, ICON_PX)
            drawable.draw(Canvas(shaped))
            return shaped
        }

        private fun launchIntent(context: Context, widgetId: Int, shortcutId: String, entry: ShortcutEntry?): PendingIntent {
            val target = entry?.takeIf { it.opensDirectly }?.let { ShortcutMaker.targetIntent(context, it) }
                ?: Intent(context, ShortcutLaunchActivity::class.java)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(ShortcutMaker.EXTRA_SHORTCUT_ID, shortcutId)
            return PendingIntent.getActivity(
                context,
                widgetId,
                target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
