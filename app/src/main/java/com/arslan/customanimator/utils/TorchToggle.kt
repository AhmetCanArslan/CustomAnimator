package com.arslan.customanimator.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object TorchToggle {

    private const val STATE_TIMEOUT_MS = 1000L

    fun toggle(context: Context): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            val cameraId = flashCameraId(cameraManager) ?: return false
            cameraManager.setTorchMode(cameraId, !isOn(cameraManager, cameraId))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun flashCameraId(cameraManager: CameraManager): String? =
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

    private fun isOn(cameraManager: CameraManager, cameraId: String): Boolean {
        val state = CompletableFuture<Boolean>()
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) state.complete(enabled)
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) state.complete(false)
            }
        }
        cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        return try {
            state.get(STATE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } finally {
            cameraManager.unregisterTorchCallback(callback)
        }
    }
}
