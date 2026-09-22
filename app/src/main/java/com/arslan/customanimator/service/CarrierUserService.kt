package com.arslan.customanimator.service

import android.os.IBinder
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.util.Log
import org.json.JSONObject

class CarrierUserService : ICarrierUserService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    override fun setCarrierName(subId: Int, name: String): String = respond {
        val overrides = PersistableBundle()
        overrides.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
        overrides.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, name)
        overrideConfig(subId, overrides)
    }

    override fun resetCarrierName(subId: Int): String = respond {
        overrideConfig(subId, null)
    }

    private fun overrideConfig(subId: Int, overrides: PersistableBundle?) {
        val loader = carrierConfigLoader()
        val method = loader.javaClass.methods.firstOrNull { it.name == "overrideConfig" }
            ?: error("overrideConfig missing")
        val args = method.parameterTypes.map { type ->
            when (type) {
                Int::class.javaPrimitiveType -> subId
                Boolean::class.javaPrimitiveType -> true
                else -> overrides
            }
        }
        method.invoke(loader, *args.toTypedArray())
    }

    private fun carrierConfigLoader(): Any {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, CARRIER_CONFIG_SERVICE) as? IBinder
            ?: error("carrier_config service unavailable")
        val stub = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
        return stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            ?: error("ICarrierConfigLoader unavailable")
    }

    private fun respond(action: () -> Unit): String {
        val response = JSONObject()
        return try {
            action()
            response.put("success", true).toString()
        } catch (e: Throwable) {
            Log.e(TAG, "carrier config override failed", e)
            response.put("success", false).put("error", describe(e)).toString()
        }
    }

    private fun describe(error: Throwable): String {
        val cause = error.cause ?: error
        return cause.javaClass.simpleName + ": " + (cause.message ?: "")
    }

    private companion object {
        const val TAG = "CarrierUserService"
        const val CARRIER_CONFIG_SERVICE = "carrier_config"
    }
}
