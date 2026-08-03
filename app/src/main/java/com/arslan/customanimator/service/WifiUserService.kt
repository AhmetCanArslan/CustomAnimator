package com.arslan.customanimator.service

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.Process
import android.util.Log
import com.arslan.customanimator.data.unquoteWifiValue
import org.json.JSONArray
import org.json.JSONObject

class WifiUserService : IWifiUserService.Stub {

    constructor()

    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context)

    override fun destroy() {
        System.exit(0)
    }

    override fun getSavedNetworksJson(): String {
        val array = JSONArray()
        val error = try {
            readConfigurations().forEach { configuration ->
                val ssid = configuration.SSID?.let(::unquoteWifiValue).orEmpty()
                if (ssid.isBlank()) return@forEach
                val psk = configuration.preSharedKey?.let(::unquoteWifiValue)
                    ?.takeIf { it.isNotBlank() && it != "*" }
                val wep = configuration.wepKeys
                    ?.firstOrNull { !it.isNullOrBlank() && it != "*" }
                    ?.let(::unquoteWifiValue)
                array.put(
                    JSONObject().apply {
                        put("ssid", ssid)
                        put("password", psk ?: wep ?: "")
                        put("security", if (psk != null) "WPA" else if (wep != null) "WEP" else "OPEN")
                        put("hidden", configuration.hiddenSSID)
                    }
                )
            }
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to read privileged networks", e)
            e.javaClass.simpleName + ": " + (e.message ?: "")
        }

        return JSONObject().apply {
            if (error != null) put("error", error)
            put("networks", array)
        }.toString()
    }

    private fun readConfigurations(): List<WifiConfiguration> {
        val binder = getServiceBinder() ?: error("wifi service unavailable")
        val stub = Class.forName("android.net.wifi.IWifiManager\$Stub")
        val wifiManager = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            ?: error("IWifiManager unavailable")

        val method = wifiManager.javaClass.methods
            .firstOrNull { it.name == "getPrivilegedConfiguredNetworks" }
            ?: error("getPrivilegedConfiguredNetworks missing")

        val args = arrayOfNulls<Any>(method.parameterTypes.size)
        var stringIndex = 0
        method.parameterTypes.forEachIndexed { index, type ->
            args[index] = when {
                type == String::class.java -> {
                    val value = if (stringIndex == 0) callerName() else SHELL_PACKAGE
                    stringIndex++
                    value
                }
                type == Bundle::class.java -> attributionBundle()
                else -> null
            }
        }

        val result = method.invoke(wifiManager, *args) ?: error("null result")
        return extractList(result)
    }

    private fun getServiceBinder(): IBinder? {
        val serviceManager = Class.forName("android.os.ServiceManager")
        return serviceManager.getMethod("getService", String::class.java)
            .invoke(null, Context.WIFI_SERVICE) as? IBinder
    }

    private fun callerName(): String = when (Process.myUid()) {
        0 -> "root"
        1000 -> "system"
        else -> "shell"
    }

    private fun attributionBundle(): Bundle {
        val bundle = Bundle()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return bundle
        try {
            val builderClass = Class.forName("android.content.AttributionSource\$Builder")
            val builder = builderClass.getConstructor(Int::class.javaPrimitiveType)
                .newInstance(Process.myUid())
            builderClass.getMethod("setPackageName", String::class.java).invoke(builder, SHELL_PACKAGE)
            val source = builderClass.getMethod("build").invoke(builder) as Parcelable
            bundle.putParcelable(ATTRIBUTION_SOURCE_KEY, source)
        } catch (e: Throwable) {
            Log.d(TAG, "AttributionSource unavailable: ${e.message}")
        }
        return bundle
    }

    private fun extractList(result: Any): List<WifiConfiguration> {
        if (result is List<*>) return result.filterIsInstance<WifiConfiguration>()
        val getList = result.javaClass.methods.firstOrNull { it.name == "getList" }
            ?: error("unexpected result ${result.javaClass.name}")
        val list = getList.invoke(result) as? List<*> ?: error("empty slice")
        return list.filterIsInstance<WifiConfiguration>()
    }


    private companion object {
        const val TAG = "WifiUserService"
        const val SHELL_PACKAGE = "com.android.shell"
        const val ATTRIBUTION_SOURCE_KEY = "EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE"
    }
}
