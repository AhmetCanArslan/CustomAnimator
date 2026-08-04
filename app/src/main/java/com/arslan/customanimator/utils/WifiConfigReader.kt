package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.annotation.StringRes
import com.arslan.customanimator.R
import com.arslan.customanimator.data.WifiNetwork
import com.arslan.customanimator.data.WifiSecurity
import com.arslan.customanimator.data.unquoteWifiValue
import com.arslan.customanimator.service.IWifiUserService
import com.arslan.customanimator.service.WifiUserService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object WifiConfigReader {

    private const val TAG = "WifiConfigReader"
    private const val BIND_TIMEOUT_MS = 15_000L
    private const val SERVICE_VERSION = 1

    private val CONNECTED_SSID = Regex("""connected to "(.*?)"""")

    sealed class Result {
        data class Success(val networks: List<WifiNetwork>) : Result()
        data class Error(
            @StringRes val messageRes: Int,
            val needsShizukuPermission: Boolean = false
        ) : Result()
    }

    suspend fun readSavedNetworks(context: Context): Result {
        if (!ShizukuHelper.hasShizukuPermission()) {
            return Result.Error(R.string.developer_needs_shizuku, needsShizukuPermission = true)
        }
        val networks = readThroughUserService(context)
        return if (networks != null) Result.Success(networks) else Result.Error(R.string.wifi_unreadable)
    }

    private suspend fun readThroughUserService(context: Context): List<WifiNetwork>? {
        var connection: ServiceConnection? = null
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, WifiUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("wifi")
            .debuggable(false)
            .version(SERVICE_VERSION)

        return try {
            val json = withTimeout(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine<String?> { continuation ->
                    val serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            if (!continuation.isActive) return
                            val result = try {
                                if (binder == null || !binder.pingBinder()) {
                                    null
                                } else {
                                    IWifiUserService.Stub.asInterface(binder).savedNetworksJson
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "User service call failed", e)
                                null
                            }
                            continuation.resume(result)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                    connection = serviceConnection
                    continuation.invokeOnCancellation { unbind(args, serviceConnection) }
                    try {
                        Shizuku.bindUserService(args, serviceConnection)
                    } catch (e: Throwable) {
                        Log.e(TAG, "bindUserService failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
            json?.let(::parseNetworks)
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "Privileged read timed out")
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Privileged read failed", e)
            null
        } finally {
            connection?.let { unbind(args, it) }
        }
    }

    private fun unbind(args: Shizuku.UserServiceArgs, connection: ServiceConnection) {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
    }

    private fun parseNetworks(json: String): List<WifiNetwork>? {
        return try {
            val root = JSONObject(json)
            root.optString("error").takeIf { it.isNotBlank() }?.let { Log.d(TAG, "User service error: $it") }
            val array = root.optJSONArray("networks") ?: return null
            (0 until array.length())
                .mapNotNull { array.optJSONObject(it) }
                .mapNotNull { item ->
                    val ssid = item.optString("ssid").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    WifiNetwork(
                        ssid = ssid,
                        password = item.optString("password"),
                        security = runCatching {
                            WifiSecurity.valueOf(item.optString("security", "WPA"))
                        }.getOrDefault(WifiSecurity.WPA),
                        isHidden = item.optBoolean("hidden", false)
                    )
                }
                .distinctBy { it.ssid }
                .sortedBy { it.ssid.lowercase() }
        } catch (e: Exception) {
            Log.d(TAG, "Malformed user service payload", e)
            null
        }
    }

    fun getConnectedSsid(context: Context): String? {
        return try {
            val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ssid = manager.connectionInfo?.ssid ?: return connectedSsidFromShell()
            val cleaned = unquoteWifiValue(ssid)
            if (cleaned.isBlank() || cleaned == "<unknown ssid>" || cleaned == "0x") {
                connectedSsidFromShell()
            } else {
                cleaned
            }
        } catch (e: Exception) {
            connectedSsidFromShell()
        }
    }

    private fun connectedSsidFromShell(): String? {
        if (!ShizukuHelper.hasShizukuPermission()) return null
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("cmd", "-w", "wifi", "status"))
        if (!result.isSuccess) return null
        val match = CONNECTED_SSID.find(result.output) ?: return null
        return match.groupValues[1].takeIf { it.isNotBlank() }
    }
}
