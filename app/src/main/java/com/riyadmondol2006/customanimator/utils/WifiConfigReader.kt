package com.riyadmondol2006.customanimator.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.StringRes
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.data.WifiNetwork
import com.riyadmondol2006.customanimator.data.WifiSecurity
import com.riyadmondol2006.customanimator.data.unquoteWifiValue
import com.riyadmondol2006.customanimator.service.IWifiUserService
import com.riyadmondol2006.customanimator.service.WifiUserService
import org.json.JSONObject

object WifiConfigReader {

    private const val TAG = "WifiConfigReader"

    private val service = ShizukuUserService(
        WifiUserService::class.java, "wifi", daemon = false, revision = 1, IWifiUserService.Stub::asInterface
    )

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

    private suspend fun readThroughUserService(context: Context): List<WifiNetwork>? =
        try {
            service.call(context) { it.savedNetworksJson }?.let(::parseNetworks)
        } finally {
            service.unbind()
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
