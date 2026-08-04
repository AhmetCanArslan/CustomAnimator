package com.arslan.customanimator.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
