package com.arslan.customanimator.service

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.InetAddress
import java.util.concurrent.Executor

class HotspotUserService : IHotspotUserService.Stub {

    private var context: Context? = null
    private val liveClients = mutableMapOf<String, ClientRecord>()
    private var callbackRegistered = false

    constructor()

    constructor(context: Context) {
        this.context = context.applicationContext ?: context
        registerTetheringCallback()
    }

    override fun destroy() {
        System.exit(0)
    }

    override fun getStateJson(): String {
        val root = JSONObject()
        val errors = mutableListOf<String>()

        val wifi = wifiManager()
        if (wifi == null) {
            root.put("supported", false)
            root.put("error", "wifi service unavailable")
            return root.toString()
        }

        root.put("supported", true)
        root.put("state", runCatching { readApState(wifi) }.getOrElse { errors += describe(it); STATE_DISABLED })

        runCatching { root.put("config", readConfig(wifi)) }
            .onFailure { errors += describe(it) }

        runCatching { root.put("caps", readCapabilities(wifi)) }
            .onFailure { errors += describe(it) }

        root.put("clients", readClients())

        if (errors.isNotEmpty()) root.put("error", errors.joinToString("; "))
        return root.toString()
    }

    override fun applyConfig(configJson: String): String {
        val response = JSONObject()
        val wifi = wifiManager() ?: return response.put("success", false)
            .put("error", "wifi service unavailable").toString()
        return try {
            val desired = JSONObject(configJson)
            val built = buildConfiguration(desired)
            val method = wifi.javaClass.methods.firstOrNull {
                it.name == "setSoftApConfiguration" && it.parameterTypes.size == 1
            } ?: error("setSoftApConfiguration missing")
            val ok = method.invoke(wifi, built) as? Boolean ?: true
            response.put("success", ok)
            if (!ok) response.put("error", "rejected by framework")
            response.toString()
        } catch (e: Throwable) {
            Log.e(TAG, "applyConfig failed", e)
            response.put("success", false).put("error", describe(e)).toString()
        }
    }

    override fun setHotspotEnabled(enabled: Boolean): String {
        val response = JSONObject()
        return try {
            val started = if (enabled) startTethering() else stopTethering()
            response.put("success", started)
            if (!started) response.put("error", "tethering request refused")
            response.toString()
        } catch (e: Throwable) {
            Log.e(TAG, "setHotspotEnabled failed", e)
            response.put("success", false).put("error", describe(e)).toString()
        }
    }

    private fun wifiManager(): Any? = runCatching {
        context?.getSystemService(Context.WIFI_SERVICE)
    }.getOrNull()

    private fun tetheringManager(): Any? = runCatching {
        context?.getSystemService(TETHERING_SERVICE)
    }.getOrNull()

    private fun readApState(wifi: Any): Int {
        val method = wifi.javaClass.methods.firstOrNull {
            it.name == "getWifiApState" && it.parameterTypes.isEmpty()
        } ?: return STATE_DISABLED
        return method.invoke(wifi) as? Int ?: STATE_DISABLED
    }

    private fun readConfig(wifi: Any): JSONObject {
        val method = wifi.javaClass.methods.firstOrNull {
            it.name == "getSoftApConfiguration" && it.parameterTypes.isEmpty()
        } ?: error("getSoftApConfiguration missing")
        val config = method.invoke(wifi) ?: error("null soft ap configuration")
        return JSONObject().apply {
            put("ssid", readSsid(config))
            put("passphrase", call<String>(config, "getPassphrase").orEmpty())
            put("security", call<Int>(config, "getSecurityType") ?: SECURITY_OPEN)
            put("hidden", call<Boolean>(config, "isHiddenSsid") ?: false)
            put("band", readBand(config))
            put(
                "macRandomization",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    call<Int>(config, "getMacRandomizationSetting") ?: RANDOMIZATION_NONE
                } else {
                    RANDOMIZATION_NONE
                }
            )
            put("autoShutdown", call<Boolean>(config, "isAutoShutdownEnabled") ?: false)
            put("shutdownTimeout", call<Long>(config, "getShutdownTimeoutMillis") ?: DEFAULT_TIMEOUT)
            put("maxClients", call<Int>(config, "getMaxNumberOfClients") ?: 0)
            put("blocked", macList(config, "getBlockedClientList"))
            put("allowed", macList(config, "getAllowedClientList"))
        }
    }

    private fun readSsid(config: Any): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val wifiSsid = call<Any>(config, "getWifiSsid")
            val bytes = wifiSsid?.let { call<ByteArray>(it, "getBytes") }
            if (bytes != null) return String(bytes, Charsets.UTF_8)
        }
        return call<String>(config, "getSsid")
    }

    private fun readBand(config: Any): Int {
        val bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call<IntArray>(config, "getBands")?.maxOrNull()
        } else {
            null
        } ?: call<Int>(config, "getBand") ?: BAND_2GHZ
        return when {
            bands and BAND_6GHZ == BAND_6GHZ -> BAND_6GHZ
            bands and BAND_5GHZ == BAND_5GHZ -> BAND_5GHZ
            bands and BAND_2GHZ == BAND_2GHZ -> BAND_2GHZ
            else -> BAND_2GHZ
        }
    }

    private fun macList(config: Any, methodName: String): JSONArray {
        val list = call<List<*>>(config, methodName).orEmpty()
        return JSONArray().apply {
            list.filterNotNull().forEach { put(it.toString()) }
        }
    }

    private fun readCapabilities(wifi: Any): JSONObject {
        val bands = JSONArray().put(BAND_2GHZ)
        if (call<Boolean>(wifi, "is5GHzBandSupported") == true) bands.put(BAND_5GHZ)
        if (call<Boolean>(wifi, "is6GHzBandSupported") == true) bands.put(BAND_6GHZ)

        val security = JSONArray().put(SECURITY_OPEN).put(SECURITY_WPA2_PSK)
        if (call<Boolean>(wifi, "isWpa3SaeSupported") == true) {
            security.put(SECURITY_WPA3_SAE_TRANSITION).put(SECURITY_WPA3_SAE)
        }

        return JSONObject().apply {
            put("bands", bands)
            put("security", security)
            put(
                "macRandomization",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    call<Boolean>(wifi, "isApMacRandomizationSupported") != false
            )
            put("maxClients", MAX_CLIENT_CEILING)
        }
    }

    private fun buildConfiguration(desired: JSONObject): Any {
        val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
        val builder = builderClass.getConstructor().newInstance()

        val ssid = desired.optString("ssid").takeIf { it.isNotBlank() }
        val security = desired.optInt("security", SECURITY_OPEN)
        val passphrase = desired.optString("passphrase")
            .takeIf { it.isNotBlank() && security != SECURITY_OPEN }

        var ssidApplied = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ssid != null) {
            ssidApplied = runCatching {
                val wifiSsidClass = Class.forName("android.net.wifi.WifiSsid")
                val fromBytes = wifiSsidClass.getMethod("fromBytes", ByteArray::class.java)
                val wifiSsid = fromBytes.invoke(null, ssid.toByteArray(Charsets.UTF_8))
                builderClass.getMethod("setWifiSsid", wifiSsidClass).invoke(builder, wifiSsid)
            }.isSuccess
        }
        if (!ssidApplied) {
            invoke(builder, "setSsid", arrayOf(String::class.java), arrayOf(ssid))
        }

        invoke(
            builder,
            "setPassphrase",
            arrayOf(String::class.java, Int::class.javaPrimitiveType!!),
            arrayOf(passphrase, security)
        )
        invoke(
            builder,
            "setHiddenSsid",
            arrayOf(Boolean::class.javaPrimitiveType!!),
            arrayOf(desired.optBoolean("hidden", false))
        )

        applyBand(builder, builderClass, desired.optInt("band", BAND_2GHZ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                invoke(
                    builder,
                    "setMacRandomizationSetting",
                    arrayOf(Int::class.javaPrimitiveType!!),
                    arrayOf(desired.optInt("macRandomization", RANDOMIZATION_NONE))
                )
            }
        }

        val autoShutdown = desired.optBoolean("autoShutdown", true)
        invoke(
            builder,
            "setAutoShutdownEnabled",
            arrayOf(Boolean::class.javaPrimitiveType!!),
            arrayOf(autoShutdown)
        )
        desired.optLong("shutdownTimeout", DEFAULT_TIMEOUT).takeIf { it > 0 }?.let { timeout ->
            runCatching {
                invoke(
                    builder,
                    "setShutdownTimeoutMillis",
                    arrayOf(Long::class.javaPrimitiveType!!),
                    arrayOf(timeout)
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                invoke(
                    builder,
                    "setBridgedModeOpportunisticShutdownEnabled",
                    arrayOf(Boolean::class.javaPrimitiveType!!),
                    arrayOf(autoShutdown)
                )
            }
        }

        invoke(
            builder,
            "setMaxNumberOfClients",
            arrayOf(Int::class.javaPrimitiveType!!),
            arrayOf(desired.optInt("maxClients", 0))
        )
        invoke(
            builder,
            "setClientControlByUserEnabled",
            arrayOf(Boolean::class.javaPrimitiveType!!),
            arrayOf(false)
        )
        runCatching {
            invoke(
                builder,
                "setBlockedClientList",
                arrayOf(List::class.java),
                arrayOf(toMacAddresses(desired.optJSONArray("blocked")))
            )
            invoke(
                builder,
                "setAllowedClientList",
                arrayOf(List::class.java),
                arrayOf(toMacAddresses(desired.optJSONArray("allowed")))
            )
        }

        return builderClass.getMethod("build").invoke(builder)!!
    }

    private fun applyBand(builder: Any, builderClass: Class<*>, band: Int) {
        val combined = when (band) {
            BAND_6GHZ -> BAND_2GHZ or BAND_5GHZ or BAND_6GHZ
            BAND_5GHZ -> BAND_2GHZ or BAND_5GHZ
            else -> BAND_2GHZ
        }
        val applied = runCatching {
            builderClass.getMethod("setBand", Int::class.javaPrimitiveType)
                .invoke(builder, combined)
        }.isSuccess
        if (!applied) {
            runCatching {
                builderClass.getMethod("setBand", Int::class.javaPrimitiveType)
                    .invoke(builder, BAND_2GHZ)
            }
        }
    }

    private fun toMacAddresses(array: JSONArray?): List<Any> {
        array ?: return emptyList()
        val macClass = Class.forName("android.net.MacAddress")
        val fromString = macClass.getMethod("fromString", String::class.java)
        return (0 until array.length()).mapNotNull { index ->
            val raw = array.optString(index).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            runCatching { fromString.invoke(null, raw) }.getOrNull()
        }
    }

    private fun startTethering(): Boolean {
        val tethering = tetheringManager()
        if (tethering != null) {
            val started = runCatching {
                val requestClass =
                    Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
                val requestBuilder = requestClass
                    .getConstructor(Int::class.javaPrimitiveType)
                    .newInstance(TETHERING_WIFI)
                val request = requestClass.getMethod("build").invoke(requestBuilder)
                val callbackClass =
                    Class.forName("android.net.TetheringManager\$StartTetheringCallback")
                val callback = Proxy.newProxyInstance(
                    callbackClass.classLoader,
                    arrayOf(callbackClass)
                ) { _, method, _ -> defaultReturn(method) }
                val start = tethering.javaClass.methods.firstOrNull {
                    it.name == "startTethering" && it.parameterTypes.size == 3
                } ?: error("startTethering missing")
                start.invoke(tethering, request, directExecutor(), callback)
                true
            }.getOrElse {
                Log.d(TAG, "TetheringManager start failed: ${describe(it)}")
                false
            }
            if (started) return true
        }
        return legacyTethering(enable = true)
    }

    private fun stopTethering(): Boolean {
        val tethering = tetheringManager()
        if (tethering != null) {
            val stopped = runCatching {
                val stop = tethering.javaClass.methods.firstOrNull {
                    it.name == "stopTethering" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType
                } ?: error("stopTethering missing")
                stop.invoke(tethering, TETHERING_WIFI)
                true
            }.getOrElse {
                Log.d(TAG, "TetheringManager stop failed: ${describe(it)}")
                false
            }
            if (stopped) return true
        }
        return legacyTethering(enable = false)
    }

    private fun legacyTethering(enable: Boolean): Boolean = runCatching {
        val connectivity = context?.getSystemService(Context.CONNECTIVITY_SERVICE)
            ?: error("connectivity service unavailable")
        if (!enable) {
            val stop = connectivity.javaClass.methods.firstOrNull {
                it.name == "stopTethering" && it.parameterTypes.size == 1
            } ?: error("stopTethering missing")
            stop.invoke(connectivity, TETHERING_WIFI)
            return@runCatching true
        }
        val callbackClass =
            Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, _ -> defaultReturn(method) }
        val start = connectivity.javaClass.methods.firstOrNull {
            it.name == "startTethering" && it.parameterTypes.size == 3
        } ?: error("startTethering missing")
        start.invoke(connectivity, TETHERING_WIFI, false, callback)
        true
    }.getOrElse {
        Log.e(TAG, "legacy tethering failed", it)
        false
    }

    private fun registerTetheringCallback() {
        if (callbackRegistered) return
        runCatching {
            val tethering = tetheringManager() ?: error("tethering service unavailable")
            val callbackClass =
                Class.forName("android.net.TetheringManager\$TetheringEventCallback")
            val handler = InvocationHandler { _, method, args ->
                if (method.name == "onClientsChanged") {
                    runCatching { cacheClients(args?.getOrNull(0) as? List<*>) }
                }
                defaultReturn(method)
            }
            val callback = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
                handler
            )
            val register = tethering.javaClass.methods.firstOrNull {
                it.name == "registerTetheringEventCallback" && it.parameterTypes.size == 2
            } ?: error("registerTetheringEventCallback missing")
            register.invoke(tethering, directExecutor(), callback)
            callbackRegistered = true
        }.onFailure { Log.d(TAG, "tethering callback unavailable: ${describe(it)}") }
    }

    private fun cacheClients(clients: List<*>?) {
        val parsed = clients.orEmpty().filterNotNull().mapNotNull { client ->
            val type = call<Int>(client, "getTetheringType")
            if (type != null && type != TETHERING_WIFI) return@mapNotNull null
            val mac = call<Any>(client, "getMacAddress")?.toString()?.lowercase()
                ?: return@mapNotNull null
            val addresses = call<List<*>>(client, "getAddresses").orEmpty().filterNotNull()
            val hostname = addresses.firstNotNullOfOrNull { call<String>(it, "getHostname") }
            val ip = addresses.firstNotNullOfOrNull { info ->
                call<Any>(info, "getAddress")?.let { link ->
                    (call<InetAddress>(link, "getAddress"))?.hostAddress
                }
            }
            ClientRecord(mac = mac, ip = ip, hostname = hostname)
        }
        synchronized(liveClients) {
            liveClients.clear()
            parsed.forEach { liveClients[it.mac] = it }
        }
    }

    private fun readClients(): JSONArray {
        registerTetheringCallback()
        val merged = linkedMapOf<String, ClientRecord>()
        synchronized(liveClients) { merged.putAll(liveClients) }
        neighbourRecords().forEach { record ->
            val existing = merged[record.mac]
            merged[record.mac] = ClientRecord(
                mac = record.mac,
                ip = existing?.ip ?: record.ip,
                hostname = existing?.hostname ?: record.hostname
            )
        }
        val leases = dhcpLeases()
        return JSONArray().apply {
            merged.values.forEach { client ->
                put(
                    JSONObject().apply {
                        put("mac", client.mac)
                        put("ip", client.ip ?: leases[client.mac]?.first ?: "")
                        put("hostname", client.hostname ?: leases[client.mac]?.second ?: "")
                    }
                )
            }
        }
    }

    private fun neighbourRecords(): List<ClientRecord> = runCatching {
        File("/proc/net/arp").takeIf { it.canRead() }?.readLines().orEmpty()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 6) return@mapNotNull null
                val ip = parts[0]
                val flags = parts[2]
                val mac = parts[3].lowercase()
                val device = parts[5]
                if (flags == "0x0" || mac == "00:00:00:00:00:00") return@mapNotNull null
                if (!isTetherInterface(device)) return@mapNotNull null
                ClientRecord(mac = mac, ip = ip, hostname = null)
            }
    }.getOrElse {
        Log.d(TAG, "arp scan failed: ${describe(it)}")
        emptyList()
    }

    private fun isTetherInterface(device: String) =
        TETHER_INTERFACE_PREFIXES.any { device.startsWith(it) }

    private fun dhcpLeases(): Map<String, Pair<String, String?>> = runCatching {
        val map = mutableMapOf<String, Pair<String, String?>>()
        LEASE_FILES.map(::File).firstOrNull { it.canRead() }?.readLines()?.forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 4) return@forEach
            val mac = parts[1].lowercase()
            val ip = parts[2]
            val hostname = parts.getOrNull(3)?.takeIf { it != "*" }
            map[mac] = ip to hostname
        }
        map
    }.getOrElse { emptyMap() }

    private fun directExecutor() = Executor { it.run() }

    private fun defaultReturn(method: Method): Any? = when (method.returnType) {
        Boolean::class.javaPrimitiveType -> false
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Void.TYPE -> null
        else -> null
    }

    private fun invoke(
        target: Any,
        name: String,
        types: Array<Class<*>>,
        args: Array<Any?>
    ): Any? = target.javaClass.getMethod(name, *types).invoke(target, *args)

    @Suppress("UNCHECKED_CAST")
    private fun <T> call(target: Any, name: String): T? = runCatching {
        target.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.isEmpty()
        }?.invoke(target) as? T
    }.getOrNull()

    private fun describe(error: Throwable): String {
        val cause = error.cause ?: error
        return cause.javaClass.simpleName + ": " + (cause.message ?: "")
    }

    private data class ClientRecord(val mac: String, val ip: String?, val hostname: String?)

    private companion object {
        const val TAG = "HotspotUserService"
        const val TETHERING_SERVICE = "tethering"
        const val TETHERING_WIFI = 0
        const val STATE_DISABLED = 11
        const val SECURITY_OPEN = 0
        const val SECURITY_WPA2_PSK = 1
        const val SECURITY_WPA3_SAE_TRANSITION = 2
        const val SECURITY_WPA3_SAE = 3
        const val BAND_2GHZ = 1
        const val BAND_5GHZ = 2
        const val BAND_6GHZ = 4
        const val RANDOMIZATION_NONE = 0
        const val DEFAULT_TIMEOUT = -1L
        const val MAX_CLIENT_CEILING = 16

        val TETHER_INTERFACE_PREFIXES = listOf("wlan", "ap", "softap", "swlan", "rndis", "bridge")
        val LEASE_FILES = listOf(
            "/data/misc/dhcp/dnsmasq.leases",
            "/data/misc/dhcp/dnsmasq.leases.softap",
            "/data/misc/connectivityblobdb/dnsmasq.leases"
        )
    }
}
