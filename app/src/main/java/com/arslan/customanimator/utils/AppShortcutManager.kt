package com.arslan.customanimator.utils

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.arslan.customanimator.data.AppShortcutInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.data.StaticShortcut
import com.arslan.customanimator.service.IShortcutUserService
import com.arslan.customanimator.service.ShortcutUserService
import org.json.JSONObject

object AppShortcutManager {

    private val service = ShizukuUserService(
        ShortcutUserService::class.java, "shortcut", daemon = true, revision = 4, IShortcutUserService.Stub::asInterface
    )

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    suspend fun listShortcuts(context: Context, packageName: String): List<AppShortcutInfo> {
        val local = StaticShortcuts.find(context, packageName).map { shortcut ->
            AppShortcutInfo(
                id = shortcut.id,
                label = shortcut.label,
                iconResId = shortcut.iconResId,
                hasIconFile = false,
                adaptiveIcon = false,
                needsShizuku = false
            )
        }
        val localIds = local.map { it.id }.toSet()
        val remote = withService(context) { parseShortcuts(it.getShortcutsJson(packageName)) }.orEmpty()
        return local + remote.filterNot { it.id in localIds }
    }

    suspend fun warmUp(context: Context) {
        if (ShortcutStore.getAll(context).none { it.type == ShortcutType.APP_SHORTCUT }) return
        withService(context) { it.asBinder().pingBinder() }
    }

    suspend fun start(context: Context, packageName: String, shortcutId: String): Boolean {
        val local = StaticShortcuts.find(context, packageName).firstOrNull { it.id == shortcutId }
        if (local != null) return startLocal(context, local)
        val shortcutIntent = withService(context) { it.getShortcutIntent(packageName, shortcutId) } ?: return false
        return try {
            shortcutIntent.send(context, 0, null, null, null, null, senderOptions().toBundle())
            true
        } catch (e: PendingIntent.CanceledException) {
            false
        }
    }

    private fun startLocal(context: Context, shortcut: StaticShortcut): Boolean {
        return try {
            context.startActivities(shortcut.intents.toTypedArray())
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun senderOptions(): ActivityOptions {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        return options
    }

    suspend fun loadIcon(context: Context, packageName: String, shortcutId: String): ShortcutIconImage? =
        listShortcuts(context, packageName).firstOrNull { it.id == shortcutId }?.let { loadIcon(context, packageName, it) }

    suspend fun loadIcon(context: Context, packageName: String, shortcut: AppShortcutInfo): ShortcutIconImage? {
        if (shortcut.hasIconFile) return withService(context) { fileIcon(it, packageName, shortcut) }
        if (shortcut.iconResId == 0) return null
        return resourceIcon(context, packageName, shortcut.iconResId)
    }

    private fun fileIcon(
        service: IShortcutUserService,
        packageName: String,
        shortcut: AppShortcutInfo
    ): ShortcutIconImage? {
        val bitmap = service.openShortcutIcon(packageName, shortcut.id)?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)
        }
        return bitmap?.let { ShortcutIconImage(it, legacy = !shortcut.adaptiveIcon) }
    }

    private fun resourceIcon(context: Context, packageName: String, iconResId: Int): ShortcutIconImage? {
        return try {
            val drawable = context.packageManager.getResourcesForApplication(packageName).getDrawable(iconResId, null)
            ShortcutIconImage(ShortcutMaker.drawableIcon(drawable))
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun <T> withService(context: Context, block: (IShortcutUserService) -> T?): T? {
        if (!isSupported()) return null
        return service.call(context, block)
    }

    internal fun parseShortcuts(json: String): List<AppShortcutInfo>? = runCatching {
        val root = JSONObject(json)
        if (!root.optBoolean("success", false)) return null
        val shortcuts = root.getJSONArray("shortcuts")
        (0 until shortcuts.length()).map { index ->
            val shortcut = shortcuts.getJSONObject(index)
            AppShortcutInfo(
                id = shortcut.getString("id"),
                label = shortcut.getString("label"),
                iconResId = shortcut.optInt("iconResId"),
                hasIconFile = shortcut.optBoolean("hasIconFile"),
                adaptiveIcon = shortcut.optBoolean("adaptiveIcon")
            )
        }
    }.getOrNull()
}
