package com.arslan.customanimator.notify.data

import androidx.annotation.Keep

@Keep
enum class FlashPattern(val displayName: String) {
    HEARTBEAT("Heartbeat"),
    PING_PONG("Ping Pong")
}
