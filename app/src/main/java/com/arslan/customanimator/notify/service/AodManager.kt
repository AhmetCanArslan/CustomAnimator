package com.arslan.customanimator.notify.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AodManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var aodJob: Job? = null
    private var unlockReceiver: BroadcastReceiver? = null
    private var currentAodReason: Int = 0

    private var ownsAod: Boolean
        get() = prefs.getBoolean(KEY_OWNS_AOD, false)
        set(value) {
            prefs.edit().putBoolean(KEY_OWNS_AOD, value).commit()
        }

    fun recoverStaleState() {
        scope.launch {
            if (currentAodReason == 0) turnOffAod()
        }
    }

    fun triggerAod(durationSeconds: Int) {
        aodJob?.cancel()
        aodJob = scope.launch {
            unregisterUnlockReceiver()
            try {
                if (!acquireAod(durationSeconds)) {
                    currentAodReason = 0
                    return@launch
                }
                currentAodReason = durationSeconds
                when {
                    durationSeconds > 0 -> holdAodFor(durationSeconds)
                    durationSeconds == -2 -> registerUnlockReceiver()
                }
            } catch (e: Exception) {
                Log.e("AodManager", "Error executing AOD rule", e)
            }
        }
    }

    private fun acquireAod(durationSeconds: Int): Boolean {
        if (ownsAod) {
            Log.d("AodManager", "AOD already on, re-arming for $durationSeconds")
            return true
        }
        val currentState = try {
            Settings.Secure.getInt(context.contentResolver, "doze_always_on")
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        if (currentState != 0) {
            Log.d("AodManager", "AOD already enabled by user, skipping")
            return false
        }
        ownsAod = true
        return try {
            Settings.Secure.putInt(context.contentResolver, "doze_always_on", 1)
            Log.d("AodManager", "AOD turned ON")
            true
        } catch (e: SecurityException) {
            ownsAod = false
            Log.e("AodManager", "Failed to write secure settings. Need WRITE_SECURE_SETTINGS permission granted via ADB.", e)
            false
        }
    }

    private suspend fun holdAodFor(durationSeconds: Int) {
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CustomAnimator:NotifyAod")
        wakeLock.acquire(durationSeconds * 1000L + WAKE_LOCK_MARGIN_MS)
        try {
            delay(durationSeconds * 1000L)
            turnOffAod()
            currentAodReason = 0
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    fun stopAodForReason(reason: Int) {
        scope.launch {
            if (currentAodReason == reason) {
                aodJob?.cancel()
                unregisterUnlockReceiver()
                turnOffAod()
                currentAodReason = 0
            }
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

    private companion object {
        const val PREFS_NAME = "notify_aod_state"
        const val KEY_OWNS_AOD = "owns_aod"
        const val WAKE_LOCK_MARGIN_MS = 5_000L
    }
}
