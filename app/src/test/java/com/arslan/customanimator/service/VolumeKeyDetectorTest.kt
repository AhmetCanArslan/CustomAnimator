package com.arslan.customanimator.service

import com.arslan.customanimator.service.VolumeKeyDetector.Companion.COMBO_WINDOW_MS
import com.arslan.customanimator.service.VolumeKeyDetector.Companion.LONG_PRESS_MS
import com.arslan.customanimator.service.VolumeKeyDetector.Companion.VOLUME_DOWN
import com.arslan.customanimator.service.VolumeKeyDetector.Companion.VOLUME_UP
import com.arslan.customanimator.utils.ButtonTrigger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VolumeKeyDetectorTest {

    private val events = mutableListOf<String>()
    private lateinit var detector: VolumeKeyDetector

    @Before
    fun setUp() {
        detector = VolumeKeyDetector(
            object : VolumeKeyDetector.Output {
                override fun pressKey(code: Int) {
                    events += "down $code"
                }

                override fun releaseKey(code: Int) {
                    events += "up $code"
                }

                override fun trigger(trigger: ButtonTrigger) {
                    events += trigger.name
                }
            }
        )
        detector.doublePressWindowMs = WINDOW
    }

    @Test
    fun keysWithoutTriggersAreForwardedAtOnce() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_LONG)
        detector.onDown(VOLUME_DOWN, 0)
        detector.onUp(VOLUME_DOWN, 50)

        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun quickTapIsForwardedOnReleaseWhenOnlyTheComboIsSet() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_COMBO)
        detector.onDown(VOLUME_DOWN, 0)
        assertEquals(emptyList<String>(), events)

        detector.onUp(VOLUME_DOWN, 40)
        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun heldKeyBecomesARealHoldOnceTheComboWindowPasses() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_COMBO)
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(COMBO_WINDOW_MS - 1)
        assertEquals(emptyList<String>(), events)

        detector.onTimeout(COMBO_WINDOW_MS)
        detector.onUp(VOLUME_UP, 600)
        assertEquals(listOf("down $VOLUME_UP", "up $VOLUME_UP"), events)
    }

    @Test
    fun bothKeysTogetherRunTheComboAndForwardNothing() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_COMBO)
        detector.onDown(VOLUME_UP, 0)
        detector.onDown(VOLUME_DOWN, 40)
        detector.onUp(VOLUME_DOWN, 100)
        detector.onUp(VOLUME_UP, 110)
        detector.onTimeout(1_000)

        assertEquals(listOf("VOLUME_COMBO"), events)
    }

    @Test
    fun secondKeyAfterTheComboWindowIsNotACombo() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_COMBO)
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(COMBO_WINDOW_MS)
        detector.onDown(VOLUME_DOWN, 200)
        detector.onTimeout(200 + COMBO_WINDOW_MS)
        detector.onUp(VOLUME_DOWN, 400)
        detector.onUp(VOLUME_UP, 410)

        assertEquals(
            listOf("down $VOLUME_UP", "down $VOLUME_DOWN", "up $VOLUME_DOWN", "up $VOLUME_UP"),
            events
        )
    }

    @Test
    fun doublePressRunsTheTriggerAndSwallowsBothPresses() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_DOUBLE)
        detector.onDown(VOLUME_UP, 0)
        detector.onUp(VOLUME_UP, 60)
        detector.onDown(VOLUME_UP, 150)
        detector.onUp(VOLUME_UP, 200)
        detector.onTimeout(2_000)

        assertEquals(listOf("VOLUME_UP_DOUBLE"), events)
    }

    @Test
    fun singlePressIsTappedOnceTheDoublePressWindowPasses() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_DOWN_DOUBLE)
        detector.onDown(VOLUME_DOWN, 0)
        detector.onUp(VOLUME_DOWN, 60)
        detector.onTimeout(60 + WINDOW - 1)
        assertEquals(emptyList<String>(), events)

        detector.onTimeout(60 + WINDOW)
        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun doublePressOnOneKeyLeavesTheOtherKeyAlone() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_DOUBLE)
        detector.onDown(VOLUME_DOWN, 0)
        detector.onUp(VOLUME_DOWN, 30)
        detector.onDown(VOLUME_DOWN, 60)
        detector.onUp(VOLUME_DOWN, 90)

        assertEquals(
            listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN", "down $VOLUME_DOWN", "up $VOLUME_DOWN"),
            events
        )
    }

    @Test
    fun longPressRunsTheTriggerAndTheReleaseIsSwallowed() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_DOWN_LONG)
        detector.onDown(VOLUME_DOWN, 0)
        detector.onTimeout(LONG_PRESS_MS - 1)
        assertEquals(emptyList<String>(), events)

        detector.onTimeout(LONG_PRESS_MS)
        detector.onUp(VOLUME_DOWN, 900)
        assertEquals(listOf("VOLUME_DOWN_LONG"), events)
    }

    @Test
    fun shortPressWithLongPressSetIsATap() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_LONG)
        detector.onDown(VOLUME_UP, 0)
        detector.onUp(VOLUME_UP, 120)

        assertEquals(listOf("down $VOLUME_UP", "up $VOLUME_UP"), events)
    }

    @Test
    fun holdWithOnlyDoublePressSetBecomesARealHold() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_DOUBLE)
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(WINDOW)
        detector.onUp(VOLUME_UP, 800)

        assertEquals(listOf("down $VOLUME_UP", "up $VOLUME_UP"), events)
    }

    @Test
    fun doubleAndLongPressCanShareAKey() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_DOUBLE, ButtonTrigger.VOLUME_UP_LONG)
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(LONG_PRESS_MS)
        detector.onUp(VOLUME_UP, 700)
        detector.onDown(VOLUME_UP, 1_000)
        detector.onUp(VOLUME_UP, 1_050)
        detector.onDown(VOLUME_UP, 1_100)
        detector.onUp(VOLUME_UP, 1_150)

        assertEquals(listOf("VOLUME_UP_LONG", "VOLUME_UP_DOUBLE"), events)
    }

    @Test
    fun powerKeyFlushesPendingVolumeKeysSoTheScreenshotChordWorks() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_DOWN_LONG)
        detector.onDown(VOLUME_DOWN, 0)
        detector.onPowerDown(20)
        assertEquals(listOf("down $VOLUME_DOWN"), events)

        detector.onTimeout(LONG_PRESS_MS)
        detector.onUp(VOLUME_DOWN, 600)
        detector.onPowerUp()
        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
    }

    @Test
    fun volumeKeyPressedWhilePowerIsHeldIsForwardedAtOnce() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_DOWN_LONG)
        detector.onPowerDown(0)
        detector.onDown(VOLUME_DOWN, 10)
        detector.onUp(VOLUME_DOWN, 20)

        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
    }

    @Test
    fun resetDropsPendingKeys() {
        detector.triggers = setOf(ButtonTrigger.VOLUME_UP_LONG)
        detector.onDown(VOLUME_UP, 0)
        detector.reset()
        detector.onTimeout(1_000)
        detector.onUp(VOLUME_UP, 1_100)

        assertEquals(emptyList<String>(), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    private companion object {
        const val WINDOW = 250L
    }
}
