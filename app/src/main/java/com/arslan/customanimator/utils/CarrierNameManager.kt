package com.arslan.customanimator.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

data class SimSlot(
    val subId: Int,
    val slotIndex: Int,
    val carrierName: String
)

object CarrierNameManager {

    private const val CARRIER_CONFIG_SERVICE = "carrier_config"
    private const val DESCRIPTOR = "com.android.internal.telephony.ICarrierConfigLoader"
    private const val TRANSACTION_OVERRIDE_CONFIG = 3
    private const val TRANSACTION_GET_DEFAULT_PACKAGE = 6

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

    fun setCarrierName(subId: Int, name: String): Boolean {
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
        bundle.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, name)
        return overrideConfig(subId, bundle)
    }

    fun resetCarrierName(subId: Int): Boolean {
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)
        bundle.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, "")
        val cleared = overrideConfig(subId, bundle)
        return overrideConfig(subId, null) || cleared
    }

    private fun carrierConfigBinder(): IBinder? {
        if (!isSupported() || !ShizukuHelper.hasShizukuPermission()) return null
        val service = SystemServiceHelper.getSystemService(CARRIER_CONFIG_SERVICE) ?: return null
        return ShizukuBinderWrapper(service)
    }

    private fun defaultCarrierServicePackage(binder: IBinder): String? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            binder.transact(TRANSACTION_GET_DEFAULT_PACKAGE, data, reply, 0)
            reply.readException()
            reply.readString()
        } catch (e: Exception) {
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun overrideConfig(subId: Int, bundle: PersistableBundle?): Boolean {
        val binder = carrierConfigBinder() ?: return false
        if (defaultCarrierServicePackage(binder).isNullOrEmpty()) return false
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(subId)
            if (bundle != null) {
                data.writeInt(1)
                bundle.writeToParcel(data, 0)
            } else {
                data.writeInt(0)
            }
            data.writeInt(1)
            binder.transact(TRANSACTION_OVERRIDE_CONFIG, data, reply, 0)
            reply.readException()
            true
        } catch (e: Exception) {
            false
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}
