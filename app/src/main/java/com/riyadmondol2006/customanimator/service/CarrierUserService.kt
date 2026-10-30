package com.riyadmondol2006.customanimator.service

import android.annotation.SuppressLint

class CarrierUserService : ICarrierUserService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    @SuppressLint("BlockedPrivateApi")
    override fun overrideConfigTransaction(): Int =
        Class.forName(CARRIER_CONFIG_LOADER_STUB)
            .getDeclaredField(TRANSACTION_OVERRIDE_CONFIG)
            .apply { isAccessible = true }
            .getInt(null)

    private companion object {
        const val CARRIER_CONFIG_LOADER_STUB = "com.android.internal.telephony.ICarrierConfigLoader\$Stub"
        const val TRANSACTION_OVERRIDE_CONFIG = "TRANSACTION_overrideConfig"
    }
}
