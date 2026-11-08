package com.arslan.customanimator.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.SettingsDatabaseManager
import kotlin.concurrent.thread

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Restoring watcher after ${intent.action}")
                try {
                    ForegroundAppWatcherService.sync(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore foreground app watcher", e)
                }
                try {
                    ScreenshotWatcherService.sync(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore screenshot watcher", e)
                }
                try {
                    BatteryAlertService.sync(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore battery alert service", e)
                }
                try {
                    PowerButtonService.sync(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore power button service", e)
                }
                try {
                    DeveloperOptionsManager.reapplyHighVolumeWarning(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reapply high volume warning", e)
                }
                try {
                    SettingsDatabaseManager.reapplyBootSettings(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reapply settings database values", e)
                }
                applyPendingShizukuSettings(context.applicationContext)
            }
        }
    }

    private fun applyPendingShizukuSettings(context: Context) {
        val pending = goAsync()
        thread {
            try {
                SettingsDatabaseManager.applyPendingShizukuSettings(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reapply settings database values through Shizuku", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
