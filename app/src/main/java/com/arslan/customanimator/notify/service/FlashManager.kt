package com.arslan.customanimator.notify.service

import android.content.Context
import android.hardware.camera2.CameraManager
import com.arslan.customanimator.notify.data.FlashPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FlashManager(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }

    private var flashJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun executePattern(pattern: FlashPattern) {
        if (cameraId == null) return

        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                when (pattern) {
                    FlashPattern.HEARTBEAT -> performHeartbeat()
                    FlashPattern.PING_PONG -> performPingPong()
                }
            } catch (e: Exception) {
            } finally {
                turnOffFlash()
            }
        }
    }

    fun executeCustomPattern(intervals: List<Long>) {
        if (cameraId == null || intervals.isEmpty()) return

        flashJob?.cancel()
        flashJob = scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                var targetElapsed = 0L

                for (i in intervals.indices) {
                    if (i % 2 == 0) {
                        turnOffFlash()
                    } else {
                        turnOnFlash()
                    }
                    targetElapsed += intervals[i]
                    val remaining = (startTime + targetElapsed) - System.currentTimeMillis()
                    if (remaining > 0) {
                        delay(remaining)
                    }
                }
            } catch (e: Exception) {
            } finally {
                turnOffFlash()
            }
        }
    }

    private suspend fun performHeartbeat() {
        repeat(3) {
            turnOnFlash()
            delay(100)
            turnOffFlash()
            delay(150)
            turnOnFlash()
            delay(100)
            turnOffFlash()

            delay(700)
        }
    }

    private suspend fun performPingPong() {
        repeat(4) {
            turnOnFlash()
            delay(200)
            turnOffFlash()
            delay(100)

            turnOnFlash()
            delay(50)
            turnOffFlash()
            delay(50)

            turnOnFlash()
            delay(50)
            turnOffFlash()
            delay(400)
        }
    }

    fun turnOnFlash() {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, true)
            } catch (e: Exception) {  }
        }
    }

    fun turnOffFlash() {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, false)
            } catch (e: Exception) {  }
        }
    }

    fun stop() {
        flashJob?.cancel()
        turnOffFlash()
    }
}
