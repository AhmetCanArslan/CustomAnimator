package com.arslan.customanimator.service

class PowerPressDetector(
    var multiPressTimeoutMs: Long,
    var pressCounts: Set<Int>,
    private val output: Output
) {

    interface Output {
        fun replayPresses(count: Int)
        fun holdDown()
        fun holdUp()
        fun multiPress(count: Int)
    }

    var deadline = NO_DEADLINE
        private set

    private var presses = 0
    private var pressing = false
    private var holding = false
    private var otherKeysDown = 0

    fun onPowerDown(now: Long) {
        if (holding) return
        onTimeout(now)
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
        if (presses == pressCounts.maxOrNull()) output.multiPress(presses)
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

    private fun replayPending() {
        when {
            presses == 0 || presses == pressCounts.maxOrNull() -> Unit
            presses in pressCounts -> output.multiPress(presses)
            else -> output.replayPresses(presses)
        }
        presses = 0
        deadline = NO_DEADLINE
    }

    companion object {
        const val NO_DEADLINE = -1L
        const val PASS_THROUGH = 0L
    }
}
