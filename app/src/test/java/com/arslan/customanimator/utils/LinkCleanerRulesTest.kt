package com.arslan.customanimator.utils

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkCleanerRulesTest {

    private val providers = LinkCleanerRules.load(ApplicationProvider.getApplicationContext())

    @Test
    fun bundledRulesAreParsed() {
        assertTrue(providers.size > 100)
    }

    @Test
    fun bundledRulesStripTrackingParameters() {
        val cleaned = LinkCleaner.clean("https://www.example.com/page?utm_source=a&id=7", providers)
        assertEquals("https://www.example.com/page?id=7", cleaned)
    }
}
