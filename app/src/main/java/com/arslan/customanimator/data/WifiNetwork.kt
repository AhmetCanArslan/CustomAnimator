package com.arslan.customanimator.data

enum class WifiSecurity { WPA, WEP, OPEN }

data class WifiNetwork(
    val ssid: String,
    val password: String,
    val security: WifiSecurity,
    val isHidden: Boolean = false
)

internal fun unquoteWifiValue(value: String): String {
    return if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
        value.substring(1, value.length - 1)
    } else {
        value
    }
}
