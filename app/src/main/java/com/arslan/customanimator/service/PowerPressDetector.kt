package com.arslan.customanimator.service

class PowerPressDetector(
    var multiPressTimeoutMs: Long,
    private val output: Output
) {

    interface Output {
        fun replayPresses(count: Int)
        fun holdDown()
        fun holdUp()
        fun doublePress()
    }

    var deadline = NO_DEADLINE
        private set

    private var presses = 0
    private var pressing = false
    private var holding = false
    private var otherKeysDown = 0

    fun onPowerDown(now: Long) {
        if (holding) return
        pressing = true
        if (otherKeysDown > 0) {
            startHold()
            return
        }
        deadline = now + multiPressTimeoutMs
    }

    fun onPowerUp(now: Long) {
        if (holding) {
            holding = false
            output.holdUp()
            return
        }
        if (!pressing) return
        pressing = false
        presses++
        if (isDoublePress()) output.doublePress()
        deadline = now + multiPressTimeoutMs
    }

    fun onTimeout(now: Long) {
        if (deadline == NO_DEADLINE || now < deadline) return
        if (pressing) startHold() else replayPending()
    }

    fun onOtherKeyDown() {
        otherKeysDown++
        if (pressing) startHold() else replayPending()
    }

    fun onOtherKeyUp() {
        if (otherKeysDown > 0) otherKeysDown--
    }

    fun reset() {
        if (holding) output.holdUp()
        presses = 0
        pressing = false
        holding = false
        otherKeysDown = 0
        deadline = NO_DEADLINE
    }

    private fun startHold() {
        replayPending()
        pressing = false
        holding = true
        output.holdDown()
    }

    private fun isDoublePress() = presses == DOUBLE_PRESS && multiPressTimeoutMs != PASS_THROUGH

    private fun replayPending() {
        if (presses > 0 && !isDoublePress()) output.replayPresses(presses)
        presses = 0
        deadline = NO_DEADLINE
    }

    companion object {
        const val NO_DEADLINE = -1L
        const val PASS_THROUGH = 0L
        private const val DOUBLE_PRESS = 2
    }
}
