package com.arslan.customanimator.utils

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.arslan.customanimator.data.AppShortcutInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.data.StaticShortcut
import com.arslan.customanimator.service.IShortcutUserService
import com.arslan.customanimator.service.ShortcutUserService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object AppShortcutManager {

    private const val TAG = "AppShortcutManager"
    private const val BIND_TIMEOUT_MS = 15_000L
    private const val SERVICE_VERSION = 4

    private val lock = Mutex()
    private var service: IShortcutUserService? = null

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
        if (!isSupported() || !ShizukuHelper.hasShizukuPermission()) return null
        val binder = lock.withLock { obtainService(context) } ?: return null
        return try {
            block(binder)
        } catch (e: Throwable) {
            Log.e(TAG, "Privileged shortcut call failed", e)
            lock.withLock { service = null }
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun obtainService(context: Context): IShortcutUserService? {
        service?.let { cached ->
            if (runCatching { cached.asBinder().pingBinder() }.getOrDefault(false)) return cached
            service = null
        }
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShortcutUserService::class.java.name)
        )
            .daemon(true)
            .processNameSuffix("shortcut")
            .debuggable(false)
            .version(SERVICE_VERSION)
        return try {
            withTimeout(BIND_TIMEOUT_MS) { bind(args) }
        } catch (e: Throwable) {
            Log.d(TAG, "Shortcut service bind failed: ${e.message}")
            null
        }
    }

    private suspend fun bind(args: Shizuku.UserServiceArgs): IShortcutUserService? =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (!continuation.isActive) return
                    val bound = binder?.takeIf { it.pingBinder() }
                        ?.let { IShortcutUserService.Stub.asInterface(it) }
                    service = bound
                    continuation.resume(bound)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                    if (continuation.isActive) continuation.resume(null)
                }
            }
            try {
                Shizuku.bindUserService(args, connection)
            } catch (e: Throwable) {
                Log.e(TAG, "bindUserService failed", e)
                if (continuation.isActive) continuation.resume(null)
            }
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
