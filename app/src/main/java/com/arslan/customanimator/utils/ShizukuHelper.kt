package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Method

data class ShellResult(val exitCode: Int, val output: String) {
    val isSuccess: Boolean get() = exitCode == 0
}

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"

    private const val MAX_OUTPUT_LINES = 2000
    private const val MAX_OUTPUT_CHARS = 64_000
    
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

            val granted = executeShellCommand(
                arrayOf("pm", "grant", packageName, "android.permission.WRITE_SECURE_SETTINGS")
            )

            val animationScaleGranted = executeShellCommand(
                arrayOf("pm", "grant", packageName, "android.permission.SET_ANIMATION_SCALE")
            )
            Log.d(TAG, "SET_ANIMATION_SCALE grant result: $animationScaleGranted")

            granted
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
    
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.WRITE_SECURE_SETTINGS"
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun canWriteSystemSettings(context: Context): Boolean {
        return try {
            Settings.System.canWrite(context)
        } catch (e: Exception) {
            false
        }
    }

    fun writeSystemSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun executeShellCommand(command: Array<String>): Boolean {
        return try {
            if (!hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission not granted for command: ${command.joinToString(" ")}")
                return false
            }

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
                command,
                null,
                null
            ) as Any

            val waitForMethod = process.javaClass.getDeclaredMethod("waitFor")
            val result = waitForMethod.invoke(process) as Int
            Log.d(TAG, "Shizuku command result=$result, cmd=${command.joinToString(" ")}")
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed Shizuku shell command: ${command.joinToString(" ")}", e)
            false
        }
    }

    fun executeShellCommandWithOutput(command: Array<String>): ShellResult {
        return try {
            if (!hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission not granted for command: ${command.joinToString(" ")}")
                return ShellResult(-1, "Shizuku permission not granted")
            }

            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            val process = newProcessMethod.invoke(null, command, null, null) as Any

            val stdout = readStreamMethod(process, "getInputStream")
            val stderr = readStreamMethod(process, "getErrorStream")

            val lines = mutableListOf<String>()
            val stdoutThread = drainInto(stdout, lines)
            val stderrThread = drainInto(stderr, lines)
            stdoutThread.join()
            stderrThread.join()

            val waitForMethod = process.javaClass.getDeclaredMethod("waitFor")
            val exitCode = waitForMethod.invoke(process) as Int

            Log.d(TAG, "Shizuku command result=$exitCode, cmd=${command.joinToString(" ")}")
            ShellResult(exitCode, truncate(lines))
        } catch (e: Exception) {
            Log.e(TAG, "Failed Shizuku shell command: ${command.joinToString(" ")}", e)
            ShellResult(-1, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun readStreamMethod(process: Any, name: String): InputStream? {
        return try {
            val method = process.javaClass.getDeclaredMethod(name)
            method.isAccessible = true
            method.invoke(process) as? InputStream
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain $name from Shizuku process", e)
            null
        }
    }

    private fun drainInto(stream: InputStream?, sink: MutableList<String>): Thread {
        val thread = Thread {
            if (stream == null) return@Thread
            try {
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        synchronized(sink) {
                            sink.add(line)
                            if (sink.size > MAX_OUTPUT_LINES) sink.removeAt(0)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading Shizuku process stream", e)
            }
        }
        thread.start()
        return thread
    }

    private fun truncate(lines: List<String>): String {
        val text = synchronized(lines) { lines.joinToString("\n") }
        return if (text.length > MAX_OUTPUT_CHARS) {
            "… output truncated …\n" + text.takeLast(MAX_OUTPUT_CHARS)
        } else {
            text
        }
    }
}
