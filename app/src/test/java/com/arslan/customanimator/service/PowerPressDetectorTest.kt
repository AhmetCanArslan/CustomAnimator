package com.arslan.customanimator.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PowerPressDetectorTest {

    private val events = mutableListOf<String>()
    private lateinit var detector: PowerPressDetector

    @Before
    fun setUp() {
        detector = PowerPressDetector(
            TIMEOUT,
            object : PowerPressDetector.Output {
                override fun replayPresses(count: Int) {
                    events += "replay $count"
                }

                override fun holdDown() {
                    events += "hold down"
                }

                override fun holdUp() {
                    events += "hold up"
                }

                override fun doublePress() {
                    events += "double"
                }
            }
        )
    }

    @Test
    fun singlePressIsReplayedOnceTheTimeoutPasses() {
        press(at = 0)
        detector.onTimeout(10 + TIMEOUT - 1)
        assertEquals(emptyList<String>(), events)

        detector.onTimeout(10 + TIMEOUT)
        assertEquals(listOf("replay 1"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    @Test
    fun aShorterWindowReplaysTheSinglePressSooner() {
        detector.multiPressTimeoutMs = 150
        press(at = 0)
        detector.onTimeout(10 + 150)

        assertEquals(listOf("replay 1"), events)
    }

    @Test
    fun doublePressRunsTheActionAndNothingIsReplayed() {
        press(at = 0)
        press(at = 100)
        detector.onTimeout(110 + TIMEOUT)

        assertEquals(listOf("double"), events)
    }

    @Test
    fun threeOrMorePressesAreReplayedSoEmergencyGesturesKeepWorking() {
        (0 until 5).forEach { press(at = it * 100L) }
        detector.onTimeout(410 + TIMEOUT)

        assertEquals(listOf("replay 5"), events)
    }

    @Test
    fun holdingThePowerButtonBecomesARealLongPress() {
        detector.onPowerDown(0)
        detector.onTimeout(TIMEOUT)
        assertEquals(listOf("hold down"), events)

        detector.onPowerUp(1000)
        assertEquals(listOf("hold down", "hold up"), events)
    }

    @Test
    fun pressThenHoldReplaysTheFirstPressBeforeTheHold() {
        press(at = 0)
        detector.onPowerDown(100)
        detector.onTimeout(100 + TIMEOUT)

        assertEquals(listOf("replay 1", "hold down"), events)
    }

    @Test
    fun otherKeyDuringPowerDownPassesPowerThroughForChords() {
        detector.onPowerDown(0)
        detector.onOtherKeyDown()
        detector.onOtherKeyUp()
        detector.onPowerUp(50)

        assertEquals(listOf("hold down", "hold up"), events)
    }

    @Test
    fun powerWhileAnotherKeyIsHeldIsPassedThroughImmediately() {
        detector.onOtherKeyDown()
        detector.onPowerDown(0)
        detector.onPowerUp(50)
        detector.onOtherKeyUp()

        assertEquals(listOf("hold down", "hold up"), events)
    }

    @Test
    fun resetReleasesAHeldPowerKey() {
        detector.onPowerDown(0)
        detector.onTimeout(TIMEOUT)
        detector.reset()
        detector.onPowerUp(1000)

        assertEquals(listOf("hold down", "hold up"), events)
        assertEquals(PowerPressDetector.NO_DEADLINE, detector.deadline)
    }

    private fun press(at: Long) {
        detector.onPowerDown(at)
        detector.onPowerUp(at + 10)
    }

    private companion object {
        const val TIMEOUT = 300L
    }
}
