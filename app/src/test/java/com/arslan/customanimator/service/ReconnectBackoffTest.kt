package com.arslan.customanimator.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReconnectBackoffTest {

    @Test
    fun delaysDoubleUntilTheAttemptLimit() {
        val backoff = ReconnectBackoff(baseDelayMs = 2000L, maxAttempts = 5)
        assertEquals(
            listOf(2000L, 4000L, 8000L, 16000L, 32000L),
            List(5) { backoff.nextDelayMs() }
        )
        assertNull(backoff.nextDelayMs())
    }

    @Test
    fun resetStartsOverFromTheBaseDelay() {
        val backoff = ReconnectBackoff(baseDelayMs = 2000L, maxAttempts = 2)
        backoff.nextDelayMs()
        backoff.nextDelayMs()
        assertNull(backoff.nextDelayMs())
        backoff.reset()
        assertEquals(2000L, backoff.nextDelayMs())
    }
}
