package com.arslan.customanimator.notify.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AodManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var aodJob: Job? = null
    private var unlockReceiver: BroadcastReceiver? = null
    private var currentAodReason: Int = 0

    fun triggerAod(durationSeconds: Int) {
        aodJob?.cancel()
        currentAodReason = durationSeconds

        unregisterUnlockReceiver()

        aodJob = scope.launch {
            try {
                val currentState = try {
                    Settings.Secure.getInt(context.contentResolver, "doze_always_on")
                } catch (e: Settings.SettingNotFoundException) {
                    0
                }

                if (currentState == 0) {
                    try {
                        Settings.Secure.putInt(context.contentResolver, "doze_always_on", 1)
                        Log.d("AodManager", "AOD turned ON")

                        if (durationSeconds > 0) {
                            delay(durationSeconds * 1000L)
                            turnOffAod()
                        } else if (durationSeconds == -2) {
                            registerUnlockReceiver()
                        }
                    } catch (e: SecurityException) {
                        Log.e("AodManager", "Failed to write secure settings. Need WRITE_SECURE_SETTINGS permission granted via ADB.", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AodManager", "Error executing AOD rule", e)
            }
        }
    }

    fun stopAodForReason(reason: Int) {
        if (currentAodReason == reason) {
            turnOffAod()
            currentAodReason = 0
            aodJob?.cancel()
            unregisterUnlockReceiver()
        }
    }

    private fun turnOffAod() {
        try {
            Settings.Secure.putInt(context.contentResolver, "doze_always_on", 0)
            Log.d("AodManager", "AOD turned back OFF")
        } catch (e: SecurityException) {
            Log.e("AodManager", "Failed to turn off AOD", e)
        }
    }

    private fun registerUnlockReceiver() {
        if (unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_USER_PRESENT) {
                        stopAodForReason(-2)
                    }
                }
            }
            context.registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        }
    }

    private fun unregisterUnlockReceiver() {
        unlockReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {}
            unlockReceiver = null
        }
    }

    fun stop() {
        aodJob?.cancel()
        unregisterUnlockReceiver()
    }
}
