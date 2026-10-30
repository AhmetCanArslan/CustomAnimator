package com.arslan.customanimator.notify.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreenWakeManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var wakeJob: Job? = null
    private var proximityListener: SensorEventListener? = null

    fun wakeScreen(durationSeconds: Int, pocketModeEnabled: Boolean) {
        wakeJob?.cancel()
        unregisterProximityListener()

        if (pocketModeEnabled) {
            checkProximityAndWake(durationSeconds)
        } else {
            performWake(durationSeconds)
        }
    }

    private fun checkProximityAndWake(durationSeconds: Int) {
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            performWake(durationSeconds)
            return
        }

        var handled = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || handled) return
                handled = true
                unregisterProximityListener()

                val distance = event.values[0]
                val isInPocket = distance < proximitySensor.maximumRange

                if (!isInPocket) {
                    performWake(durationSeconds)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        proximityListener = listener
        sensorManager.registerListener(
            listener,
            proximitySensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        scope.launch {
            delay(500)
            if (!handled) {
                handled = true
                unregisterProximityListener()
                performWake(durationSeconds)
            }
        }
    }

    private fun unregisterProximityListener() {
        proximityListener?.let {
            try {
                sensorManager.unregisterListener(it)
            } catch (_: Exception) {}
            proximityListener = null
        }
    }

    private fun performWake(durationSeconds: Int) {
        wakeJob = scope.launch {
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                        or PowerManager.ACQUIRE_CAUSES_WAKEUP
                        or PowerManager.ON_AFTER_RELEASE,
                "CustomAnimator:NotifyScreenWake"
            )
            try {
                if (durationSeconds > 0) {
                    wakeLock.acquire(durationSeconds * 1000L)
                    delay(durationSeconds * 1000L)
                } else {
                    wakeLock.acquire(100L)
                    delay(100L)
                }
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    fun stop() {
        wakeJob?.cancel()
        unregisterProximityListener()
    }
}
