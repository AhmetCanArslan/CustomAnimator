package com.arslan.customanimator.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VolumeComboDetectorTest {

    private val events = mutableListOf<String>()
    private lateinit var detector: VolumeComboDetector

    @Before
    fun setUp() {
        detector = VolumeComboDetector(
            object : VolumeComboDetector.Output {
                override fun pressKey(code: Int) {
                    events += "down $code"
                }

                override fun releaseKey(code: Int) {
                    events += "up $code"
                }

                override fun combo() {
                    events += "combo"
                }
            }
        )
        detector.enabled = true
    }

    @Test
    fun disabledDetectorForwardsKeysAtOnce() {
        detector.enabled = false
        detector.onDown(VOLUME_UP, 0)
        detector.onUp(VOLUME_UP)

        assertEquals(listOf("down $VOLUME_UP", "up $VOLUME_UP"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun quickTapIsForwardedOnRelease() {
        detector.onDown(VOLUME_DOWN, 0)
        assertEquals(emptyList<String>(), events)

        detector.onUp(VOLUME_DOWN)
        assertEquals(listOf("down $VOLUME_DOWN", "up $VOLUME_DOWN"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun heldKeyIsForwardedOnceTheWindowPasses() {
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(VolumeComboDetector.COMBO_WINDOW_MS - 1)
        assertEquals(emptyList<String>(), events)

        detector.onTimeout(VolumeComboDetector.COMBO_WINDOW_MS)
        detector.onUp(VOLUME_UP)
        assertEquals(listOf("down $VOLUME_UP", "up $VOLUME_UP"), events)
    }

    @Test
    fun bothKeysTogetherRunTheComboAndForwardNothing() {
        detector.onDown(VOLUME_UP, 0)
        detector.onDown(VOLUME_DOWN, 40)
        detector.onUp(VOLUME_DOWN)
        detector.onUp(VOLUME_UP)
        detector.onTimeout(1_000)

        assertEquals(listOf("combo"), events)
    }

    @Test
    fun secondKeyAfterTheWindowIsNotACombo() {
        detector.onDown(VOLUME_UP, 0)
        detector.onTimeout(VolumeComboDetector.COMBO_WINDOW_MS)
        detector.onDown(VOLUME_DOWN, 200)
        detector.onUp(VOLUME_DOWN)
        detector.onUp(VOLUME_UP)

        assertEquals(
            listOf("down $VOLUME_UP", "down $VOLUME_DOWN", "up $VOLUME_DOWN", "up $VOLUME_UP"),
            events
        )
    }

    @Test
    fun resetDropsThePendingKeyAndSwallowedReleases() {
        detector.onDown(VOLUME_UP, 0)
        detector.reset()
        detector.onTimeout(1_000)
        assertEquals(emptyList<String>(), events)

        detector.onDown(VOLUME_UP, 0)
        detector.onDown(VOLUME_DOWN, 10)
        detector.reset()
        detector.onUp(VOLUME_UP)
        assertEquals(listOf("combo", "up $VOLUME_UP"), events)
    }

    private companion object {
        const val VOLUME_DOWN = 114
        const val VOLUME_UP = 115
    }
}
