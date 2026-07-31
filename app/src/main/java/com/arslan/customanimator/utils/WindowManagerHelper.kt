package com.arslan.customanimator.utils

import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.Display
import org.lsposed.hiddenapibypass.HiddenApiBypass

object WindowManagerHelper {

    private const val TAG = "WindowManagerHelper"

    const val WINDOW_SCALE_INDEX = 0
    const val TRANSITION_SCALE_INDEX = 1
    const val ANIMATOR_SCALE_INDEX = 2

    private var exemptionsRequested = false

    private val currentUserId: Int get() = Process.myUid() / 100000

    private fun ensureHiddenApiAccess() {
        if (exemptionsRequested || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        exemptionsRequested = true
        try {
            HiddenApiBypass.addHiddenApiExemptions("")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to lift hidden API restrictions", e)
        }
    }

    private fun windowManager(): Any? {
        return try {
            ensureHiddenApiAccess()

            val serviceManager = Class.forName("android.os.ServiceManager")
            val binder = serviceManager
                .getMethod("getService", String::class.java)
                .invoke(null, "window") as? IBinder ?: return null

            val stub = Class.forName("android.view.IWindowManager\$Stub")
            stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to obtain IWindowManager", e)
            null
        }
    }

    private fun findMethod(target: Any, name: String, argCount: Int) =
        target.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.size == argCount }

    fun getAnimationScales(): FloatArray? {
        val wm = windowManager() ?: return null
        return try {
            val method = findMethod(wm, "getAnimationScales", 0) ?: return null
            method.invoke(wm) as? FloatArray
        } catch (e: Throwable) {
            Log.e(TAG, "getAnimationScales failed", e)
            null
        }
    }

    fun setAnimationScales(window: Float, transition: Float, animator: Float): Boolean {
        val wm = windowManager() ?: return false
        return try {
            val method = findMethod(wm, "setAnimationScales", 1) ?: return false
            method.invoke(wm, floatArrayOf(window, transition, animator))
            true
        } catch (e: Throwable) {
            Log.e(TAG, "setAnimationScales failed", e)
            false
        }
    }

    fun setForcedDisplayDensity(density: Int): Boolean {
        val wm = windowManager() ?: return false
        return try {
            val forUser = findMethod(wm, "setForcedDisplayDensityForUser", 3)
            if (forUser != null) {
                forUser.invoke(wm, Display.DEFAULT_DISPLAY, density, currentUserId)
            } else {
                val legacy = findMethod(wm, "setForcedDisplayDensity", 2) ?: return false
                legacy.invoke(wm, Display.DEFAULT_DISPLAY, density)
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "setForcedDisplayDensity failed", e)
            false
        }
    }

    fun clearForcedDisplayDensity(): Boolean {
        val wm = windowManager() ?: return false
        return try {
            val forUser = findMethod(wm, "clearForcedDisplayDensityForUser", 2)
            if (forUser != null) {
                forUser.invoke(wm, Display.DEFAULT_DISPLAY, currentUserId)
            } else {
                val legacy = findMethod(wm, "clearForcedDisplayDensity", 1) ?: return false
                legacy.invoke(wm, Display.DEFAULT_DISPLAY)
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "clearForcedDisplayDensity failed", e)
            false
        }
    }

    fun hasSetAnimationScalePermission(context: android.content.Context): Boolean {
        return try {
            context.checkSelfPermission("android.permission.SET_ANIMATION_SCALE") ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }
}
