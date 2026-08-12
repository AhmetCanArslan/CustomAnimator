package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.arslan.customanimator.data.HotspotCapabilities
import com.arslan.customanimator.data.HotspotClient
import com.arslan.customanimator.data.HotspotConfig
import com.arslan.customanimator.data.HotspotSecurity
import com.arslan.customanimator.data.HotspotSnapshot
import com.arslan.customanimator.data.HotspotState
import com.arslan.customanimator.service.HotspotUserService
import com.arslan.customanimator.service.IHotspotUserService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.util.UUID
import kotlin.coroutines.resume

object HotspotManager {

    private const val TAG = "HotspotManager"
    private const val BIND_TIMEOUT_MS = 15_000L
    private const val SERVICE_VERSION = 1

    const val SSID_MIN_LENGTH = 1
    const val SSID_MAX_LENGTH = 32
    const val PSK_MIN_LENGTH = 8
    const val PSK_MAX_LENGTH = 63

    sealed class Outcome {
        object Success : Outcome()
        data class Failure(val message: String?) : Outcome()
    }

    enum class SsidError { TOO_SHORT, TOO_LONG }

    enum class PassphraseError { TOO_SHORT, TOO_LONG }

    private val lock = Mutex()
    private var service: IHotspotUserService? = null
    private var connection: ServiceConnection? = null

    private val serviceArgs: (Context) -> Shizuku.UserServiceArgs = { context ->
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, HotspotUserService::class.java.name)
        )
            .daemon(true)
            .processNameSuffix("hotspot")
            .debuggable(false)
            .version(SERVICE_VERSION)
    }

    fun validateSsid(ssid: String): SsidError? = when {
        ssid.length < SSID_MIN_LENGTH -> SsidError.TOO_SHORT
        ssid.toByteArray(Charsets.UTF_8).size > SSID_MAX_LENGTH -> SsidError.TOO_LONG
        else -> null
    }

    fun validatePassphrase(passphrase: String, security: Int): PassphraseError? {
        if (security == HotspotSecurity.OPEN) return null
        val usesPsk = security == HotspotSecurity.WPA2_PSK ||
            security == HotspotSecurity.WPA3_SAE_TRANSITION
        if (!usesPsk) return null
        return when {
            passphrase.length < PSK_MIN_LENGTH -> PassphraseError.TOO_SHORT
            passphrase.length > PSK_MAX_LENGTH -> PassphraseError.TOO_LONG
            else -> null
        }
    }

    fun generatePassphrase(): String {
        val uuid = UUID.randomUUID().toString()
        return uuid.substring(0, 8) + uuid.substring(9, 13)
    }

    fun systemHotspotSettingsIntent(): Intent =
        Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun readState(context: Context): HotspotSnapshot? {
        val json = withService(context) { it.stateJson } ?: return null
        return parseSnapshot(json)
    }

    suspend fun applyConfig(context: Context, config: HotspotConfig): Outcome {
        val json = withService(context) { it.applyConfig(encodeConfig(config).toString()) }
            ?: return Outcome.Failure(null)
        return parseOutcome(json)
    }

    suspend fun setEnabled(context: Context, enabled: Boolean): Outcome {
        val json = withService(context) { it.setHotspotEnabled(enabled) }
            ?: return Outcome.Failure(null)
        return parseOutcome(json)
    }

    suspend fun blockClient(context: Context, config: HotspotConfig, mac: String): Outcome {
        val normalized = mac.lowercase()
        if (config.blockedDevices.any { it.equals(normalized, ignoreCase = true) }) {
            return Outcome.Success
        }
        return applyConfig(context, config.copy(blockedDevices = config.blockedDevices + normalized))
    }

    suspend fun unblockClient(context: Context, config: HotspotConfig, mac: String): Outcome {
        val remaining = config.blockedDevices.filterNot { it.equals(mac, ignoreCase = true) }
        return applyConfig(context, config.copy(blockedDevices = remaining))
    }

    fun release() {
        service = null
        connection = null
    }

    private suspend fun <T> withService(context: Context, block: (IHotspotUserService) -> T): T? {
        if (!ShizukuHelper.hasShizukuPermission()) return null
        val binder = lock.withLock { obtainService(context) } ?: return null
        return try {
            block(binder)
        } catch (e: Throwable) {
            Log.e(TAG, "Privileged hotspot call failed", e)
            lock.withLock { service = null }
            null
        }
    }

    private suspend fun obtainService(context: Context): IHotspotUserService? {
        service?.let { cached ->
            if (runCatching { cached.asBinder().pingBinder() }.getOrDefault(false)) return cached
            service = null
        }
        val appContext = context.applicationContext
        val args = serviceArgs(appContext)
        return try {
            withTimeout(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine<IHotspotUserService?> { continuation ->
                    val serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            if (!continuation.isActive) return
                            val bound = runCatching {
                                if (binder == null || !binder.pingBinder()) {
                                    null
                                } else {
                                    IHotspotUserService.Stub.asInterface(binder)
                                }
                            }.getOrNull()
                            service = bound
                            continuation.resume(bound)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            service = null
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                    connection = serviceConnection
                    try {
                        Shizuku.bindUserService(args, serviceConnection)
                    } catch (e: Throwable) {
                        Log.e(TAG, "bindUserService failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Hotspot service bind failed: ${e.message}")
            null
        }
    }

    private fun parseOutcome(json: String): Outcome = runCatching {
        val root = JSONObject(json)
        if (root.optBoolean("success", false)) {
            Outcome.Success
        } else {
            Outcome.Failure(root.optString("error").takeIf { it.isNotBlank() })
        }
    }.getOrElse { Outcome.Failure(null) }

    private fun parseSnapshot(json: String): HotspotSnapshot? = runCatching {
        val root = JSONObject(json)
        val configJson = root.optJSONObject("config")
        val capsJson = root.optJSONObject("caps")
        HotspotSnapshot(
            isSupported = root.optBoolean("supported", true),
            state = root.optInt("state", HotspotState.DISABLED),
            config = configJson?.let(::decodeConfig) ?: HotspotConfig(),
            capabilities = capsJson?.let(::decodeCapabilities) ?: HotspotCapabilities(),
            clients = decodeClients(root.optJSONArray("clients")),
            error = root.optString("error").takeIf { it.isNotBlank() }
        )
    }.getOrElse {
        Log.d(TAG, "Malformed hotspot payload", it)
        null
    }

    private fun decodeConfig(json: JSONObject) = HotspotConfig(
        ssid = json.optString("ssid").takeIf { it.isNotBlank() && it != "null" }.orEmpty(),
        passphrase = json.optString("passphrase"),
        security = json.optInt("security", HotspotSecurity.WPA2_PSK),
        band = json.optInt("band", 1),
        isHidden = json.optBoolean("hidden", false),
        macRandomization = json.optInt("macRandomization", 0),
        isAutoShutdownEnabled = json.optBoolean("autoShutdown", true),
        autoShutdownTimeout = json.optLong("shutdownTimeout", -1L),
        maxClients = json.optInt("maxClients", 0),
        blockedDevices = decodeStrings(json.optJSONArray("blocked")),
        allowedDevices = decodeStrings(json.optJSONArray("allowed"))
    )

    private fun decodeCapabilities(json: JSONObject) = HotspotCapabilities(
        supportedBands = decodeInts(json.optJSONArray("bands")).ifEmpty { listOf(1) },
        supportedSecurityTypes = decodeInts(json.optJSONArray("security")).ifEmpty { listOf(0, 1) },
        isMacRandomizationSupported = json.optBoolean("macRandomization", false),
        maxSupportedClients = json.optInt("maxClients", 0)
    )

    private fun decodeClients(array: JSONArray?): List<HotspotClient> {
        array ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val mac = item.optString("mac").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            HotspotClient(
                macAddress = mac,
                ipAddress = item.optString("ip").takeIf { it.isNotBlank() },
                hostname = item.optString("hostname").takeIf { it.isNotBlank() }
            )
        }.distinctBy { it.macAddress.lowercase() }
    }

    private fun decodeStrings(array: JSONArray?): List<String> {
        array ?: return emptyList()
        return (0 until array.length()).mapNotNull {
            array.optString(it).takeIf { value -> value.isNotBlank() }
        }
    }

    private fun decodeInts(array: JSONArray?): List<Int> {
        array ?: return emptyList()
        return (0 until array.length()).map { array.optInt(it) }
    }

    fun encodeConfig(config: HotspotConfig): JSONObject = JSONObject().apply {
        put("ssid", config.ssid)
        put("passphrase", config.passphrase)
        put("security", config.security)
        put("band", config.band)
        put("hidden", config.isHidden)
        put("macRandomization", config.macRandomization)
        put("autoShutdown", config.isAutoShutdownEnabled)
        put("shutdownTimeout", config.autoShutdownTimeout)
        put("maxClients", config.maxClients)
        put("blocked", JSONArray().apply { config.blockedDevices.forEach { put(it) } })
        put("allowed", JSONArray().apply { config.allowedDevices.forEach { put(it) } })
    }

    fun configFromJson(json: JSONObject): HotspotConfig = decodeConfig(json)
}
