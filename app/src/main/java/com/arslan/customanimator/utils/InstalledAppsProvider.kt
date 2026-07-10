package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.arslan.customanimator.data.InstalledAppInfo

object InstalledAppsProvider {

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
