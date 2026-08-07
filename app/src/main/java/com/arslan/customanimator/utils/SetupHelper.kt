package com.arslan.customanimator.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import rikka.shizuku.Shizuku

enum class SetupStage {
    NOT_INSTALLED,
    NOT_RUNNING,
    NOT_AUTHORIZED,
    AUTHORIZED,
    READY
}

object SetupHelper {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private const val PLAY_URL = "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"
    private const val GITHUB_URL = "https://github.com/RikkaApps/Shizuku/releases/latest"
    private const val GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun stage(context: Context): SetupStage {
        if (ShizukuHelper.hasWriteSecureSettingsPermission(context)) return SetupStage.READY
        if (!isShizukuInstalled(context)) return SetupStage.NOT_INSTALLED
        if (!isShizukuRunning()) return SetupStage.NOT_RUNNING
        if (!ShizukuHelper.hasShizukuPermission()) return SetupStage.NOT_AUTHORIZED
        return SetupStage.AUTHORIZED
    }

    fun supportsWirelessDebugging(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun adbCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    fun openShizukuStore(context: Context) {
        if (!openUrl(context, PLAY_URL)) openUrl(context, GITHUB_URL)
    }

    fun openShizukuGithub(context: Context) = openUrl(context, GITHUB_URL)

    fun openShizukuGuide(context: Context) = openUrl(context, GUIDE_URL)

    fun openShizukuApp(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openDeveloperOptions(context: Context): Boolean =
        startSettings(context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

    fun openWirelessDebugging(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            startSettings(context, "android.settings.WIRELESS_DEBUGGING_SETTINGS")
        ) {
            return true
        }
        return openDeveloperOptions(context)
    }

    fun openAboutPhone(context: Context): Boolean =
        startSettings(context, Settings.ACTION_DEVICE_INFO_SETTINGS)

    private fun startSettings(context: Context, action: String): Boolean {
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun openUrl(context: Context, url: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
