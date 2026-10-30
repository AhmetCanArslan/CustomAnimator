package com.riyadmondol2006.customanimator.service

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.annotation.RequiresApi
import rikka.shizuku.SystemServiceHelper

class CarrierConfigInstrumentation : Instrumentation() {

    private var arguments = Bundle.EMPTY

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments ?: Bundle.EMPTY
        start()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStart() {
        val result = Bundle()
        try {
            uiAutomation.adoptShellPermissionIdentity(Manifest.permission.MODIFY_PHONE_STATE)
            overrideConfig()
            result.putBoolean(RESULT_SUCCESS, true)
        } catch (e: Throwable) {
            Log.e(TAG, "carrier config override failed", e)
            result.putString(RESULT_ERROR, e.javaClass.simpleName + ": " + (e.message ?: ""))
        } finally {
            runCatching { uiAutomation.dropShellPermissionIdentity() }
        }
        finish(Activity.RESULT_OK, result)
    }

    private fun overrideConfig() {
        val binder: IBinder = SystemServiceHelper.getSystemService(CARRIER_CONFIG_SERVICE)
            ?: error("carrier_config service unavailable")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(binder.interfaceDescriptor ?: error("carrier_config descriptor missing"))
            data.writeInt(arguments.getString(ARG_SUB_ID)?.toInt() ?: error("subscription missing"))
            writeOverrides(data, overrides())
            data.writeInt(OVERRIDE_TRANSIENT)
            val transaction = arguments.getString(ARG_TRANSACTION)?.toInt() ?: error("transaction missing")
            binder.transact(transaction, data, reply, 0)
            reply.readException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun overrides(): PersistableBundle? {
        val name = arguments.getString(ARG_NAME) ?: return null
        return PersistableBundle().apply {
            putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
            putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, name)
        }
    }

    private fun writeOverrides(data: Parcel, overrides: PersistableBundle?) {
        if (overrides == null) {
            data.writeInt(0)
            return
        }
        data.writeInt(1)
        overrides.writeToParcel(data, 0)
    }

    companion object {
        const val ARG_SUB_ID = "sub_id"
        const val ARG_TRANSACTION = "transaction"
        const val ARG_NAME = "name"
        const val RESULT_SUCCESS = "success"
        const val RESULT_ERROR = "error"
        private const val TAG = "CarrierConfigInstrumentation"
        private const val CARRIER_CONFIG_SERVICE = "carrier_config"
        private const val OVERRIDE_TRANSIENT = 0
    }
}
