package com.arslan.customanimator.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.arslan.customanimator.R
import java.util.concurrent.ConcurrentHashMap

object LiveUpdatesManager {

    private const val PREFS_NAME = "live_updates_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_NAVIGATION = "navigation"
    private const val KEY_HIDE_ORIGINAL = "hide_original"
    private const val KEY_CODES = "codes"
    private const val KEY_NAVIGATION_APPS = "navigation_apps"
    private const val CHANNEL_ID = "live_updates"
    private const val MIRROR_ID = 4601
    private const val CODE_ID = 4602
    private const val CODE_TIMEOUT_MS = 60_000L
    private const val MAX_ACTIONS = 3
    private const val FULL_PERCENT = 100
    private const val HIDDEN_SOURCE_TIMEOUT_MS = 60_000L
    private const val EXCLUDED_FLAGS = Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_PROMOTED_ONGOING

    enum class Kind { PROGRESS, NAVIGATION }

    data class Source(
        val category: String?,
        val flags: Int,
        val progress: Int,
        val progressMax: Int,
        val indeterminate: Boolean,
        val inNavigationApps: Boolean
    ) {
        val hasProgress: Boolean get() = indeterminate || progressMax > 0
        val isNavigation: Boolean
            get() = flags and Notification.FLAG_ONGOING_EVENT != 0 &&
                (category == Notification.CATEGORY_NAVIGATION || inNavigationApps)
    }

    data class Options(val progress: Boolean, val navigation: Boolean)

    private val mirroredKeys = ConcurrentHashMap.newKeySet<String>()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) = update(context) { putBoolean(KEY_ENABLED, enabled) }

    fun isProgressEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_PROGRESS, true)

    fun setProgressEnabled(context: Context, enabled: Boolean) = update(context) { putBoolean(KEY_PROGRESS, enabled) }

    fun isNavigationEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_NAVIGATION, true)

    fun setNavigationEnabled(context: Context, enabled: Boolean) =
        update(context) { putBoolean(KEY_NAVIGATION, enabled) }

    fun isHideOriginalEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_HIDE_ORIGINAL, false)

    fun setHideOriginalEnabled(context: Context, enabled: Boolean) =
        update(context) { putBoolean(KEY_HIDE_ORIGINAL, enabled) }

    fun isCodesEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_CODES, true)

    fun setCodesEnabled(context: Context, enabled: Boolean) = update(context) { putBoolean(KEY_CODES, enabled) }

    fun getNavigationApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_NAVIGATION_APPS, null)?.toSet() ?: emptySet()

    fun toggleNavigationApp(context: Context, packageName: String): Set<String> {
        val current = getNavigationApps(context)
        val updated = if (packageName in current) current - packageName else current + packageName
        update(context) { putStringSet(KEY_NAVIGATION_APPS, updated) }
        return updated
    }

    fun kindOf(source: Source, options: Options): Kind? = when {
        source.flags and EXCLUDED_FLAGS != 0 -> null
        options.progress && source.hasProgress -> Kind.PROGRESS
        options.navigation && source.isNavigation -> Kind.NAVIGATION
        else -> null
    }

    fun percentOf(source: Source): Int =
        if (source.progressMax <= 0) 0 else (source.progress.toLong() * FULL_PERCENT / source.progressMax).toInt()
            .coerceIn(0, FULL_PERCENT)

    fun onPosted(listener: NotificationListenerService, sbn: StatusBarNotification) {
        if (!isSupported()) return
        val source = sourceOf(listener, sbn)
        val kind = if (isEnabled(listener)) kindOf(source, optionsOf(listener)) else null
        if (kind == null) {
            if (sbn.key in mirroredKeys) cancelMirror(listener, sbn.key)
            return
        }
        val hideOriginal = isHideOriginalEnabled(listener) && sbn.isClearable
        if (!postMirror(listener, sbn, source, kind, hideOriginal)) return
        if (hideOriginal) listener.cancelNotification(sbn.key)
    }

    fun onRemoved(context: Context, sbn: StatusBarNotification, reason: Int) {
        if (sbn.key !in mirroredKeys) return
        if (reason == NotificationListenerService.REASON_LISTENER_CANCEL && isHideOriginalEnabled(context)) return
        cancelMirror(context, sbn.key)
    }

    fun onCodeCopied(context: Context, code: String) {
        if (!isSupported() || !isEnabled(context) || !isCodesEnabled(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_pin)
            .setContentTitle(code)
            .setContentText(context.getString(R.string.otp_copier_copied, code))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShortCriticalText(code)
            .setTimeoutAfter(CODE_TIMEOUT_MS)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setRequestPromotedOngoing(true)
            .build()
        notificationManager(context).notify(CODE_ID, notification)
    }

    fun reconcile(listener: NotificationListenerService, active: Array<out StatusBarNotification>) {
        if (!isSupported()) return
        val sources = active.filter { it.packageName != listener.packageName }
        val activeKeys = sources.mapTo(HashSet()) { it.key }
        mirrorTags(listener).filter { it !in activeKeys }.forEach { cancelMirror(listener, it) }
        sources.forEach { onPosted(listener, it) }
    }

    private fun clear(context: Context) {
        mirrorTags(context).forEach { cancelMirror(context, it) }
        notificationManager(context).cancel(CODE_ID)
    }

    private fun update(context: Context, edit: SharedPreferences.Editor.() -> Unit) {
        prefs(context).edit().apply(edit).apply()
        clear(context)
    }

    private fun optionsOf(context: Context) =
        Options(progress = isProgressEnabled(context), navigation = isNavigationEnabled(context))

    private fun sourceOf(context: Context, sbn: StatusBarNotification): Source {
        val notification = sbn.notification
        val extras = notification.extras
        return Source(
            category = notification.category,
            flags = notification.flags,
            progress = extras.getInt(Notification.EXTRA_PROGRESS, 0),
            progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
            inNavigationApps = sbn.packageName in getNavigationApps(context)
        )
    }

    private fun notificationManager(context: Context) =
        context.getSystemService(NotificationManager::class.java)

    private fun mirrorTags(context: Context): List<String> =
        notificationManager(context).activeNotifications
            .filter { it.id == MIRROR_ID && it.tag != null }
            .map { it.tag }

    private fun cancelMirror(context: Context, key: String) {
        mirroredKeys.remove(key)
        notificationManager(context).cancel(key, MIRROR_ID)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun postMirror(
        context: Context,
        sbn: StatusBarNotification,
        source: Source,
        kind: Kind,
        hideOriginal: Boolean
    ): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().ifBlank { text }
        if (title.isBlank()) return false
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text.takeIf { it != title })
            .setSubText(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            .setContentIntent(notification.contentIntent)
            .setColor(notification.color)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setRequestPromotedOngoing(true)
        notification.getLargeIcon()?.let(builder::setLargeIcon)
        if (hideOriginal) builder.setTimeoutAfter(HIDDEN_SOURCE_TIMEOUT_MS)
        notification.actions.orEmpty().filter { it.remoteInputs.isNullOrEmpty() }.take(MAX_ACTIONS).forEach { action ->
            val icon = action.getIcon()?.let { IconCompat.createFromIcon(context, it) }
            builder.addAction(NotificationCompat.Action.Builder(icon, action.title, action.actionIntent).build())
        }
        if (kind == Kind.PROGRESS) applyProgress(builder, source) else applyNavigation(builder, title)

        notificationManager(context).notify(sbn.key, MIRROR_ID, builder.build())
        mirroredKeys.add(sbn.key)
        return true
    }

    private fun applyProgress(builder: NotificationCompat.Builder, source: Source) {
        builder.setSmallIcon(R.drawable.ic_tile_downloading)
        builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (source.indeterminate || source.progressMax <= 0) {
            builder.setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true))
            return
        }
        val percent = percentOf(source)
        builder.setStyle(NotificationCompat.ProgressStyle().setProgress(percent))
        builder.setShortCriticalText("$percent%")
    }

    private fun applyNavigation(builder: NotificationCompat.Builder, title: String) {
        builder.setSmallIcon(R.drawable.ic_tile_navigation)
        builder.setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        builder.setShortCriticalText(title)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun ensureChannel(context: Context) {
        val manager = notificationManager(context)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.live_updates),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}
