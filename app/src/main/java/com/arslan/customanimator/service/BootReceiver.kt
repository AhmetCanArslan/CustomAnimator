package com.arslan.customanimator.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arslan.customanimator.utils.DeveloperOptionsManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Restoring watcher after ${intent.action}")
                try {
                    AutoForceStopService.startIfSelectionExists(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore watcher", e)
                }
                try {
                    PerAppDpiService.startIfOverridesExist(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore per-app dpi watcher", e)
                }
                try {
                    ScreenshotWatcherService.sync(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore screenshot watcher", e)
                }
                try {
                    DeveloperOptionsManager.reapplyHighVolumeWarning(context.applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reapply high volume warning", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
