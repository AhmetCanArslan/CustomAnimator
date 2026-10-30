package com.arslan.customanimator.data

object HotspotSecurity {
    const val OPEN = 0
    const val WPA2_PSK = 1
    const val WPA3_SAE_TRANSITION = 2
    const val WPA3_SAE = 3
}

object HotspotBand {
    const val BAND_2GHZ = 1
    const val BAND_5GHZ = 2
    const val BAND_6GHZ = 4
}

object HotspotRandomization {
    const val NONE = 0
    const val PERSISTENT = 1
    const val NON_PERSISTENT = 2

    val ALL = listOf(NONE, PERSISTENT, NON_PERSISTENT)
}

object HotspotState {
    const val DISABLING = 10
    const val DISABLED = 11
    const val ENABLING = 12
    const val ENABLED = 13
    const val FAILED = 14
}

object HotspotTimeout {
    const val DEFAULT = -1L
    const val FIVE_MINUTES = 5 * 60 * 1000L
    const val TEN_MINUTES = 10 * 60 * 1000L
    const val TWENTY_MINUTES = 20 * 60 * 1000L
    const val THIRTY_MINUTES = 30 * 60 * 1000L
    const val ONE_HOUR = 60 * 60 * 1000L

    val ALL = listOf(DEFAULT, FIVE_MINUTES, TEN_MINUTES, TWENTY_MINUTES, THIRTY_MINUTES, ONE_HOUR)
}

data class HotspotConfig(
    val ssid: String = "",
    val passphrase: String = "",
    val security: Int = HotspotSecurity.WPA2_PSK,
    val band: Int = HotspotBand.BAND_2GHZ,
    val isHidden: Boolean = false,
    val macRandomization: Int = HotspotRandomization.NONE,
    val isAutoShutdownEnabled: Boolean = true,
    val autoShutdownTimeout: Long = HotspotTimeout.DEFAULT,
    val maxClients: Int = 0,
    val blockedDevices: List<String> = emptyList(),
    val allowedDevices: List<String> = emptyList()
)

data class HotspotCapabilities(
    val supportedBands: List<Int> = listOf(HotspotBand.BAND_2GHZ),
    val supportedSecurityTypes: List<Int> = listOf(HotspotSecurity.OPEN, HotspotSecurity.WPA2_PSK),
    val isMacRandomizationSupported: Boolean = false,
    val maxSupportedClients: Int = 0
)

data class HotspotClient(
    val macAddress: String,
    val ipAddress: String? = null,
    val hostname: String? = null
)

data class HotspotSnapshot(
    val isSupported: Boolean = true,
    val state: Int = HotspotState.DISABLED,
    val config: HotspotConfig = HotspotConfig(),
    val capabilities: HotspotCapabilities = HotspotCapabilities(),
    val clients: List<HotspotClient> = emptyList(),
    val error: String? = null
) {
    val isEnabled: Boolean get() = state == HotspotState.ENABLED
    val isBusy: Boolean get() = state == HotspotState.ENABLING || state == HotspotState.DISABLING
}

