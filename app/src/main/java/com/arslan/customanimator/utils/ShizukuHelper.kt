package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class ShellResult(val exitCode: Int, val output: String) {
    val isSuccess: Boolean get() = exitCode == 0
}

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"

    private const val MAX_OUTPUT_LINES = 2000
    private const val MAX_OUTPUT_CHARS = 64_000
    private const val STARTUP_BINDER_WAIT_MS = 3_000L

    private val startupBinder = CountDownLatch(1)

    private val newProcessMethod: Method by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky { startupBinder.countDown() }
        } catch (e: Exception) {
            Log.d(TAG, "Shizuku binder listener unavailable: ${e.message}")
        }
    }
    
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
        awaitStartupBinder()
        return isPermissionGranted()
    }

    fun isRunningAsRoot(): Boolean {
        return try {
            hasShizukuPermission() && Shizuku.getUid() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isPermissionGranted(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun awaitStartupBinder() {
        if (Looper.myLooper() == Looper.getMainLooper() || Shizuku.pingBinder()) return
        val remaining = Process.getStartElapsedRealtime() + STARTUP_BINDER_WAIT_MS -
            SystemClock.elapsedRealtime()
        if (remaining <= 0) return
        try {
            startupBinder.await(remaining, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
    
    fun awaitShizukuPermission(timeoutMs: Long): Boolean {
        if (isPermissionGranted()) return true
        val latch = CountDownLatch(1)
        val listener = Shizuku.OnBinderReceivedListener { latch.countDown() }
        return try {
            Shizuku.addBinderReceivedListenerSticky(listener)
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            isPermissionGranted()
        } catch (e: Exception) {
            Log.d(TAG, "Waiting for Shizuku binder failed: ${e.message}")
            false
        } finally {
            runCatching { Shizuku.removeBinderReceivedListener(listener) }
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

    fun executeShellCommand(command: Array<String>): Boolean =
        executeShellCommandWithOutput(command).isSuccess

    fun executeShellCommandWithOutput(command: Array<String>): ShellResult {
        return try {
            if (!hasShizukuPermission()) {
                Log.d(TAG, "Shizuku permission not granted for command: ${command.joinToString(" ")}")
                return ShellResult(-1, "Shizuku permission not granted")
            }

            val process = newProcess(command)
            try {
                runCatching { process.outputStream.close() }
                val lines = ArrayDeque<String>()
                val stderrThread = Thread { drainInto(process.errorStream, lines) }
                stderrThread.start()
                drainInto(process.inputStream, lines)
                stderrThread.join()
                val exitCode = process.waitFor()
                Log.d(TAG, "Shizuku command result=$exitCode, cmd=${command.joinToString(" ")}")
                ShellResult(exitCode, truncate(lines))
            } finally {
                process.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed Shizuku shell command: ${command.joinToString(" ")}", e)
            ShellResult(-1, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun newProcess(command: Array<String>): java.lang.Process =
        newProcessMethod.invoke(null, command, null, null) as java.lang.Process

    private fun drainInto(stream: InputStream?, sink: ArrayDeque<String>) {
        if (stream == null) return
        try {
            BufferedReader(InputStreamReader(stream)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    synchronized(sink) {
                        sink.addLast(line)
                        if (sink.size > MAX_OUTPUT_LINES) sink.removeFirst()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading Shizuku process stream", e)
        }
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
