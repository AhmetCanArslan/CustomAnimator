package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.arslan.customanimator.data.InstalledAppInfo

object InstalledAppsProvider {

    /**
     * Packages that must never be force-stopped in bulk. Killing the launcher leaves a black
     * screen, killing the active keyboard makes text input impossible, and killing Shizuku
     * removes the very channel this app runs its commands through.
     */
    fun getUnsafeToKillPackages(context: Context): Set<String> {
        val unsafe = mutableSetOf(context.packageName, "android", "com.android.systemui")

        // Launcher
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(home, 0)?.activityInfo?.packageName?.let { unsafe.add(it) }
        } catch (e: Exception) {
            // Resolving the launcher is best-effort; the rest of the list still applies.
        }

        // Active input method (keyboard)
        try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            id?.substringBefore('/')?.takeIf { it.isNotBlank() }?.let { unsafe.add(it) }
        } catch (e: Exception) {
            // Ignored — falls back to the enabled-IME sweep below.
        }
        try {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.enabledInputMethodList?.forEach { unsafe.add(it.packageName) }
        } catch (e: Exception) {
            // Ignored.
        }

        // Shizuku itself, plus its Sui variant.
        unsafe.add("moe.shizuku.privileged.api")
        unsafe.add("rikka.shizuku")

        return unsafe
    }

    fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }

        return resolveInfos
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    label = try {
                        appInfo.loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        appInfo.packageName
                    },
                    icon = try {
                        appInfo.loadIcon(packageManager)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
