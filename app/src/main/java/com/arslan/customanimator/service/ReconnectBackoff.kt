package com.arslan.customanimator.service

class ReconnectBackoff(
    private val baseDelayMs: Long,
    private val maxAttempts: Int
) {

    private var attempts = 0

    fun nextDelayMs(): Long? {
        if (attempts >= maxAttempts) return null
        val delayMs = baseDelayMs shl attempts
        attempts++
        return delayMs
    }

    fun reset() {
        attempts = 0
    }
}
