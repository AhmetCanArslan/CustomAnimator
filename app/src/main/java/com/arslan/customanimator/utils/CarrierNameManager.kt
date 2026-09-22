package com.arslan.customanimator.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.arslan.customanimator.service.CarrierUserService
import com.arslan.customanimator.service.ICarrierUserService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

data class SimSlot(
    val subId: Int,
    val slotIndex: Int,
    val carrierName: String
)

object CarrierNameManager {

    private const val TAG = "CarrierNameManager"
    private const val BIND_TIMEOUT_MS = 15_000L
    private const val SERVICE_VERSION = 1

    sealed class Outcome {
        object Success : Outcome()
        data class Failure(val message: String?) : Outcome()
    }

    private val lock = Mutex()
    private var service: ICarrierUserService? = null

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun hasPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getSimSlots(context: Context): List<SimSlot> {
        if (!hasPhonePermission(context)) return emptyList()
        val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as? SubscriptionManager ?: return emptyList()
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return try {
            manager.activeSubscriptionInfoList.orEmpty().map { info ->
                val name = info.carrierName?.toString().takeUnless { it.isNullOrBlank() }
                    ?: telephony?.networkOperatorName.orEmpty()
                SimSlot(info.subscriptionId, info.simSlotIndex, name)
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    suspend fun setCarrierName(context: Context, subId: Int, name: String): Outcome =
        withService(context) { it.setCarrierName(subId, name) }

    suspend fun resetCarrierName(context: Context, subId: Int): Outcome =
        withService(context) { it.resetCarrierName(subId) }

    private suspend fun withService(
        context: Context,
        block: (ICarrierUserService) -> String
    ): Outcome {
        if (!isSupported() || !ShizukuHelper.hasShizukuPermission()) return Outcome.Failure(null)
        val binder = lock.withLock { obtainService(context) } ?: return Outcome.Failure(null)
        return try {
            parseOutcome(block(binder))
        } catch (e: Throwable) {
            Log.e(TAG, "Privileged carrier call failed", e)
            lock.withLock { service = null }
            Outcome.Failure(null)
        }
    }

    private suspend fun obtainService(context: Context): ICarrierUserService? {
        service?.let { cached ->
            if (runCatching { cached.asBinder().pingBinder() }.getOrDefault(false)) return cached
            service = null
        }
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, CarrierUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("carrier")
            .debuggable(false)
            .version(SERVICE_VERSION)
        return try {
            withTimeout(BIND_TIMEOUT_MS) { bind(args) }
        } catch (e: Throwable) {
            Log.d(TAG, "Carrier service bind failed: ${e.message}")
            null
        }
    }

    private suspend fun bind(args: Shizuku.UserServiceArgs): ICarrierUserService? =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (!continuation.isActive) return
                    val bound = binder?.takeIf { it.pingBinder() }
                        ?.let { ICarrierUserService.Stub.asInterface(it) }
                    service = bound
                    continuation.resume(bound)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                    if (continuation.isActive) continuation.resume(null)
                }
            }
            try {
                Shizuku.bindUserService(args, connection)
            } catch (e: Throwable) {
                Log.e(TAG, "bindUserService failed", e)
                if (continuation.isActive) continuation.resume(null)
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
}
