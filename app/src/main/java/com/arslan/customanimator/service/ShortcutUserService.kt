package com.arslan.customanimator.service

import android.app.PendingIntent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.S)
class ShortcutUserService : IShortcutUserService.Stub() {

    private val launcherApps: Any by lazy {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, LAUNCHER_APPS_SERVICE) as? IBinder
            ?: error("launcherapps service unavailable")
        Class.forName("android.content.pm.ILauncherApps\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("ILauncherApps unavailable")
    }

    private val userId: Int by lazy {
        val user = Process.myUserHandle()
        user.javaClass.getMethod("getIdentifier").invoke(user) as Int
    }

    override fun destroy() {
        System.exit(0)
    }

    override fun getShortcutsJson(packageName: String): String = respond { response ->
        val shortcuts = JSONArray()
        queryShortcuts(packageName).forEach { shortcuts.put(describe(it)) }
        response.put("shortcuts", shortcuts)
    }

    override fun openShortcutIcon(packageName: String, shortcutId: String): ParcelFileDescriptor? {
        return try {
            call("getShortcutIconFd", SHELL_PACKAGE, packageName, shortcutId, userId) as? ParcelFileDescriptor
        } catch (e: Throwable) {
            Log.e(TAG, "getShortcutIconFd failed", e)
            null
        }
    }

    override fun getShortcutIntent(packageName: String, shortcutId: String): PendingIntent? {
        return try {
            call("getShortcutIntent", SHELL_PACKAGE, packageName, shortcutId, null, Process.myUserHandle())
                as? PendingIntent
        } catch (e: Throwable) {
            Log.e(TAG, "getShortcutIntent failed", e)
            null
        }
    }

    private fun queryShortcuts(packageName: String): List<ShortcutInfo> {
        val query = LauncherApps.ShortcutQuery().setPackage(packageName).setQueryFlags(QUERY_FLAGS)
        val wrapper = Class.forName("android.content.pm.ShortcutQueryWrapper")
            .getConstructor(LauncherApps.ShortcutQuery::class.java)
            .newInstance(query)
        val slice = call("getShortcuts", SHELL_PACKAGE, wrapper, Process.myUserHandle())
            ?: error("getShortcuts returned nothing")
        val shortcuts = slice.javaClass.getMethod("getList").invoke(slice) as? List<*>
        return shortcuts.orEmpty()
            .filterIsInstance<ShortcutInfo>()
            .filter { it.isEnabled }
            .distinctBy { it.id }
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
    }

    private fun describe(shortcut: ShortcutInfo): JSONObject =
        JSONObject()
            .put("id", shortcut.id)
            .put("label", (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString())
            .put("iconResId", hidden(shortcut, "getIconResourceId") as Int)
            .put("hasIconFile", hidden(shortcut, "hasIconFile") as Boolean)
            .put("adaptiveIcon", hidden(shortcut, "hasAdaptiveBitmap") as Boolean)

    private fun hidden(shortcut: ShortcutInfo, name: String): Any? =
        ShortcutInfo::class.java.getMethod(name).invoke(shortcut)

    private fun call(name: String, vararg args: Any?): Any? {
        val method = launcherApps.javaClass.methods.firstOrNull { it.name == name } ?: error("$name missing")
        return method.invoke(launcherApps, *args)
    }

    private fun respond(action: (JSONObject) -> Unit): String {
        val response = JSONObject()
        return try {
            action(response)
            response.put("success", true).toString()
        } catch (e: Throwable) {
            Log.e(TAG, "launcher apps call failed", e)
            JSONObject().put("success", false).put("error", describe(e)).toString()
        }
    }

    private fun describe(error: Throwable): String {
        val cause = error.cause ?: error
        return cause.javaClass.simpleName + ": " + (cause.message ?: "")
    }

    private companion object {
        const val TAG = "ShortcutUserService"
        const val LAUNCHER_APPS_SERVICE = "launcherapps"
        const val SHELL_PACKAGE = "com.android.shell"
        const val QUERY_FLAGS = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
    }
}
