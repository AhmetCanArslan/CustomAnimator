package com.arslan.customanimator.utils

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.arslan.customanimator.data.InstalledAppInfo
import java.io.File

object InstalledAppsProvider {

    const val EXPORT_DIR = "CustomAnimator"

    fun sourceApks(context: Context, packageName: String): List<File> {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        return (listOf(info.sourceDir) + info.splitSourceDirs.orEmpty()).map(::File)
    }

    fun getUnsafeToKillPackages(context: Context): Set<String> {
        val unsafe = mutableSetOf(context.packageName, "android", "com.android.systemui")

        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(home, 0)?.activityInfo?.packageName?.let { unsafe.add(it) }
        } catch (e: Exception) {
        }

        try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            id?.substringBefore('/')?.takeIf { it.isNotBlank() }?.let { unsafe.add(it) }
        } catch (e: Exception) {
        }
        try {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.enabledInputMethodList?.forEach { unsafe.add(it.packageName) }
        } catch (e: Exception) {
        }

        unsafe.add("moe.shizuku.privileged.api")
        unsafe.add("rikka.shizuku")

        return unsafe
    }

    private class CachedApp(val sourceDir: String?, val info: InstalledAppInfo)

    @Volatile
    private var launchableApps: List<InstalledAppInfo>? = null
    private var cachedApps: Map<String, CachedApp> = emptyMap()
    private var cachedLocales: String? = null
    private var trimCallbacksRegistered = false

    fun cachedLaunchableApps(): List<InstalledAppInfo>? = launchableApps

    @Synchronized
    fun clearLaunchableAppsCache() {
        launchableApps = null
        cachedApps = emptyMap()
    }

    @Synchronized
    fun getLaunchableApps(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        registerTrimCallbacks(context)

        val locales = context.resources.configuration.locales.toLanguageTags()
        if (locales != cachedLocales) {
            cachedApps = emptyMap()
            cachedLocales = locales
        }

        val resolveInfos = try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }

        val entries = resolveInfos
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { appInfo ->
                cachedApps[appInfo.packageName]?.takeIf { it.sourceDir == appInfo.sourceDir }
                    ?: CachedApp(appInfo.sourceDir, describe(packageManager, appInfo))
                        .also { AppIconBitmaps.evict(appInfo.packageName) }
            }
        cachedApps = entries.associateBy { it.info.packageName }
        return entries.map { it.info }.sortedBy { it.label.lowercase() }.also { launchableApps = it }
    }

    private fun describe(packageManager: PackageManager, appInfo: ApplicationInfo) = InstalledAppInfo(
        packageName = appInfo.packageName,
        label = try {
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appInfo.packageName
        },
        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    )

    private fun registerTrimCallbacks(context: Context) {
        if (trimCallbacksRegistered) return
        trimCallbacksRegistered = true
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) dropCaches()
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = dropCaches()
        })
    }

    private fun dropCaches() {
        clearLaunchableAppsCache()
        AppIconBitmaps.clear()
    }
}
