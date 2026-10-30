package com.riyadmondol2006.customanimator.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.riyadmondol2006.customanimator.service.CarrierConfigInstrumentation
import com.riyadmondol2006.customanimator.service.CarrierUserService
import com.riyadmondol2006.customanimator.service.ICarrierUserService

data class SimSlot(
    val subId: Int,
    val slotIndex: Int,
    val carrierName: String
)

object CarrierNameManager {

    private val RESULT_PREFIX = Regex("^INSTRUMENTATION_(RESULT|STATUS): ")
    private const val AM_ERROR = "Error"

    sealed class Outcome {
        object Success : Outcome()
        data class Failure(val message: String?) : Outcome()
    }

    private val service = ShizukuUserService(
        CarrierUserService::class.java, "carrier", daemon = false, revision = 2, ICarrierUserService.Stub::asInterface
    )

    @Volatile
    private var transaction: Int? = null

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
        overrideConfig(context, subId, arrayOf("-e", CarrierConfigInstrumentation.ARG_NAME, name))

    suspend fun resetCarrierName(context: Context, subId: Int): Outcome =
        overrideConfig(context, subId, emptyArray())

    private suspend fun overrideConfig(context: Context, subId: Int, nameArgs: Array<String>): Outcome {
        if (!isSupported() || !ShizukuHelper.hasShizukuPermission()) return Outcome.Failure(null)
        val transaction = overrideTransaction(context) ?: return Outcome.Failure(null)
        val command = arrayOf(
            "am", "instrument", "-w", "-r", "--no-restart",
            "-e", CarrierConfigInstrumentation.ARG_SUB_ID, subId.toString(),
            "-e", CarrierConfigInstrumentation.ARG_TRANSACTION, transaction.toString()
        ) + nameArgs + ComponentName(context, CarrierConfigInstrumentation::class.java).flattenToShortString()
        return parseOutcome(ShizukuHelper.executeShellCommandWithOutput(command))
    }

    private suspend fun overrideTransaction(context: Context): Int? {
        transaction?.let { return it }
        return service.call(context) { it.overrideConfigTransaction() }?.also { transaction = it }
    }

    private fun parseOutcome(result: ShellResult): Outcome {
        val values = result.output.lineSequence()
            .map { it.replace(RESULT_PREFIX, "") }
            .filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        return when {
            values[CarrierConfigInstrumentation.RESULT_SUCCESS] == "true" -> Outcome.Success
            else -> Outcome.Failure(
                values[CarrierConfigInstrumentation.RESULT_ERROR]
                    ?: values[AM_ERROR]
                    ?: result.output.lineSequence().lastOrNull { it.isNotBlank() }
            )
        }
    }
}
