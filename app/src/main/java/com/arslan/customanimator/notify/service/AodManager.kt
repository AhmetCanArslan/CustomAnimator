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
    private var ownsAod: Boolean = false

    fun triggerAod(durationSeconds: Int) {
        aodJob?.cancel()
        unregisterUnlockReceiver()
        currentAodReason = durationSeconds

        aodJob = scope.launch {
            try {
                if (!ownsAod) {
                    val currentState = try {
                        Settings.Secure.getInt(context.contentResolver, "doze_always_on")
                    } catch (e: Settings.SettingNotFoundException) {
                        0
                    }
                    if (currentState != 0) {
                        Log.d("AodManager", "AOD already enabled by user, skipping")
                        currentAodReason = 0
                        return@launch
                    }
                    try {
                        Settings.Secure.putInt(context.contentResolver, "doze_always_on", 1)
                        ownsAod = true
                        Log.d("AodManager", "AOD turned ON")
                    } catch (e: SecurityException) {
                        Log.e("AodManager", "Failed to write secure settings. Need WRITE_SECURE_SETTINGS permission granted via ADB.", e)
                        currentAodReason = 0
                        return@launch
                    }
                } else {
                    Log.d("AodManager", "AOD already on, re-arming for $durationSeconds")
                }

                when {
                    durationSeconds > 0 -> {
                        delay(durationSeconds * 1000L)
                        turnOffAod()
                        currentAodReason = 0
                    }
                    durationSeconds == -2 -> registerUnlockReceiver()
                }
            } catch (e: Exception) {
                Log.e("AodManager", "Error executing AOD rule", e)
            }
        }
    }

    fun stopAodForReason(reason: Int) {
        if (currentAodReason == reason) {
            aodJob?.cancel()
            unregisterUnlockReceiver()
            turnOffAod()
            currentAodReason = 0
        }
    }

    private fun turnOffAod() {
        if (!ownsAod) return
        try {
            Settings.Secure.putInt(context.contentResolver, "doze_always_on", 0)
            ownsAod = false
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
        turnOffAod()
        currentAodReason = 0
    }
}
