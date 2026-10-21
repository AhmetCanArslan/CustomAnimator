package com.arslan.customanimator.service

class VolumeComboDetector(private val output: Output) {

    interface Output {
        fun pressKey(code: Int)
        fun releaseKey(code: Int)
        fun combo()
    }

    var enabled = false

    var deadline = PowerPressDetector.NO_DEADLINE
        private set

    private var pending: Int? = null
    private val swallowed = mutableSetOf<Int>()

    fun onDown(code: Int, now: Long) {
        val waiting = pending
        when {
            !enabled -> output.pressKey(code)
            waiting != null && waiting != code -> fireCombo(waiting, code)
            else -> {
                pending = code
                deadline = now + COMBO_WINDOW_MS
            }
        }
    }

    fun onUp(code: Int) {
        when {
            swallowed.remove(code) -> Unit
            pending == code -> {
                clearPending()
                output.pressKey(code)
                output.releaseKey(code)
            }
            else -> output.releaseKey(code)
        }
    }

    fun onTimeout(now: Long) {
        val waiting = pending ?: return
        if (now < deadline) return
        clearPending()
        output.pressKey(waiting)
    }

    fun reset() {
        clearPending()
        swallowed.clear()
    }

    private fun fireCombo(first: Int, second: Int) {
        clearPending()
        swallowed += first
        swallowed += second
        output.combo()
    }

    private fun clearPending() {
        pending = null
        deadline = PowerPressDetector.NO_DEADLINE
    }

    companion object {
        const val COMBO_WINDOW_MS = 100L
    }
}
