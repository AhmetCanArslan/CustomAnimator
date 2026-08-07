package com.arslan.customanimator.notify.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.WidgetConfig
import com.arslan.customanimator.notify.data.WidgetConfigStore
import com.arslan.customanimator.notify.data.WidgetNotification
import com.arslan.customanimator.notify.data.WidgetNotificationStore

class NotificationWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return NotificationWidgetFactory(applicationContext, appWidgetId)
    }
}

private class NotificationWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetNotification> = emptyList()
    private var config: WidgetConfig = WidgetConfig()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        config = WidgetConfigStore.load(context, appWidgetId)
        items = WidgetNotificationStore.getAll(context)
            .filter { config.ruleIds.isEmpty() || it.ruleId in config.ruleIds }
            .take(config.maxItems.coerceIn(1, 50))
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount() = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_notification_item)
        val item = items.getOrNull(position) ?: return views

        val textColor = config.textColor.toInt()
        val secondaryColor = (textColor and 0x00FFFFFF) or (0xB0 shl 24)

        views.setTextViewText(R.id.item_title, item.title.ifBlank { item.appName })
        views.setTextColor(R.id.item_title, textColor)
        views.setTextViewTextSize(R.id.item_title, TypedValue.COMPLEX_UNIT_SP, config.textSizeSp.toFloat())

        views.setViewVisibility(
            R.id.item_body,
            if (config.showBody && item.body.isNotBlank()) View.VISIBLE else View.GONE
        )
        views.setTextViewText(R.id.item_body, item.body)
        views.setTextColor(R.id.item_body, secondaryColor)
        views.setTextViewTextSize(R.id.item_body, TypedValue.COMPLEX_UNIT_SP, (config.textSizeSp - 1).toFloat())

        val metaParts = mutableListOf<String>()
        if (config.showAppName) metaParts.add(item.appName)
        if (config.showTime) {
            metaParts.add(
                DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            )
        }
        views.setViewVisibility(R.id.item_meta, if (metaParts.isEmpty()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.item_meta, metaParts.joinToString(" · "))
        views.setTextColor(R.id.item_meta, config.accentColor.toInt())
        views.setTextViewTextSize(R.id.item_meta, TypedValue.COMPLEX_UNIT_SP, (config.textSizeSp - 3).toFloat())

        if (config.showAppIcon) {
            views.setViewVisibility(R.id.item_icon, View.VISIBLE)
            val icon = loadIcon(item.packageName)
            if (icon != null) {
                views.setImageViewBitmap(R.id.item_icon, icon)
            } else {
                views.setImageViewResource(R.id.item_icon, R.drawable.ic_notification_prime)
            }
        } else {
            views.setViewVisibility(R.id.item_icon, View.GONE)
        }

        views.setOnClickFillInIntent(
            R.id.item_root,
            Intent().putExtra(NotificationWidgetProvider.EXTRA_PACKAGE_NAME, item.packageName)
        )

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount() = 1

    override fun getItemId(position: Int) = items.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds() = true

    private fun loadIcon(packageName: String): Bitmap? = try {
        drawableToBitmap(context.packageManager.getApplicationIcon(packageName))
    } catch (_: Exception) {
        null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, ICON_SIZE_PX, ICON_SIZE_PX, true)
        }
        val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val ICON_SIZE_PX = 96
    }
}
