package com.arslan.customanimator.utils

import android.content.Context
import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.lang.reflect.Method

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.checkSelfPermission() >= 0 || 
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
             Shizuku.getVersion() > 0)
        } catch (e: Exception) {
            Log.d(TAG, "Shizuku not available: ${e.message}")
            false
        }
    }
    
    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun requestShizukuPermission(context: Context) {
        try {
            Shizuku.requestPermission(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
        }
    }
    
    fun grantWriteSecureSettingsPermission(context: Context): Boolean {
        return try {
            if (!hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission not granted")
                return false
            }
            
            val packageName = context.packageName
            
            // Use reflection to access the private newProcess method
            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val process = newProcessMethod.invoke(
                null,
                arrayOf("pm", "grant", packageName, "android.permission.WRITE_SECURE_SETTINGS"),
                null,
                null
            ) as Any // ShizukuRemoteProcess
            
            val waitForMethod = process.javaClass.getDeclaredMethod("waitFor")
            val result = waitForMethod.invoke(process) as Int
            
            Log.d(TAG, "Grant permission result: $result")
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to grant permission via Shizuku: ${e.message}", e)
            false
        }
    }
    
    fun markShizukuRequested(context: Context) {
        val prefs = context.getSharedPreferences("shizuku_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("shizuku_request_sent", true).apply()
    }
    
    fun hasShizukuBeenRequested(context: Context): Boolean {
        val prefs = context.getSharedPreferences("shizuku_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("shizuku_request_sent", false)
    }
}
