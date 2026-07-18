package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

    // Keep the log bounded so commands like logcat or dumpsys can't OOM the UI. The screen keeps
    // output in rememberSaveable, so this also has to stay well under the ~1 MB binder limit that
    // savedInstanceState is parceled through on rotation.
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

    /**
     * Runs a command and returns its exit code together with stdout + stderr merged.
     *
     * Blocking — always call from [kotlinx.coroutines.Dispatchers.IO].
     */
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

            // Both pipes must be drained concurrently: draining stdout to EOF while stderr fills
            // its buffer deadlocks the remote process on output-heavy commands like dumpsys.
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
                            // Drop from the top so a runaway command keeps only its tail.
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
