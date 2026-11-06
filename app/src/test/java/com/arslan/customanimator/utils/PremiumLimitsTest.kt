package com.arslan.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PremiumLimitsTest {

    private lateinit var context: Context
    private val noon = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("premium_limits", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun featureIsAvailableUntilUsed() {
        assertTrue(PremiumLimits.isAvailable(context, PremiumLimits.Limited.BOOSTER, noon))
        PremiumLimits.markUsed(context, PremiumLimits.Limited.BOOSTER, noon)
        assertFalse(PremiumLimits.isAvailable(context, PremiumLimits.Limited.BOOSTER, noon + 1000))
    }

    @Test
    fun featuresAreCountedSeparately() {
        PremiumLimits.markUsed(context, PremiumLimits.Limited.BOOSTER, noon)
        assertTrue(PremiumLimits.isAvailable(context, PremiumLimits.Limited.GAME_MODE, noon))
    }

    @Test
    fun dailyFeatureResetsOnTheNextDay() {
        PremiumLimits.markUsed(context, PremiumLimits.Limited.GAME_MODE, noon)
        assertTrue(PremiumLimits.isAvailable(context, PremiumLimits.Limited.GAME_MODE, noon + dayMs))
    }

    @Test
    fun weeklyFeatureWaitsSevenDays() {
        PremiumLimits.markUsed(context, PremiumLimits.Limited.CLEANER, noon)
        assertFalse(PremiumLimits.isAvailable(context, PremiumLimits.Limited.CLEANER, noon + 6 * dayMs))
        assertTrue(PremiumLimits.isAvailable(context, PremiumLimits.Limited.CLEANER, noon + 7 * dayMs))
    }

    @Test
    fun turningTheClockBackDoesNotRestoreTheUse() {
        PremiumLimits.markUsed(context, PremiumLimits.Limited.BOOSTER, noon)
        assertFalse(PremiumLimits.isAvailable(context, PremiumLimits.Limited.BOOSTER, noon - 3 * dayMs))
    }

    @Test
    fun freeShortcutLimitIsThree() {
        assertEquals(3, PremiumLimits.FREE_SHORTCUTS)
    }

    @Test
    fun iconChangesRunOutAfterTwoAndNeverComeBack() {
        assertEquals(2, PremiumLimits.iconChangesLeft(context))
        PremiumLimits.recordIconChange(context)
        PremiumLimits.recordIconChange(context)
        assertEquals(0, PremiumLimits.iconChangesLeft(context))
        PremiumLimits.recordIconChange(context)
        assertEquals(0, PremiumLimits.iconChangesLeft(context))
    }

    @Test
    fun premiumPromptWaitsADayAfterItWasHandled() {
        assertTrue(SettingsManager.shouldShowPremiumPrompt(context, noon))
        SettingsManager.markPremiumPromptHandled(context, noon)
        assertFalse(SettingsManager.shouldShowPremiumPrompt(context, noon + dayMs - 1))
        assertTrue(SettingsManager.shouldShowPremiumPrompt(context, noon + dayMs))
    }
}
