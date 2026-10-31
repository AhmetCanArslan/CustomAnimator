package com.arslan.customanimator.service

import com.arslan.customanimator.utils.ButtonTrigger

class VolumeKeyDetector(private val output: Output) {

    interface Output {
        fun pressKey(code: Int)
        fun releaseKey(code: Int)
        fun repeatKey(code: Int)
        fun trigger(trigger: ButtonTrigger)
    }

    private enum class Phase { DOWN, WAITING_NEXT, FORWARDED, SWALLOWED }

    private class Key(var phase: Phase, var downAt: Long, var deadline: Long, var presses: Int = 1)

    var triggers: Set<ButtonTrigger> = emptySet()
    var doublePressWindowMs = 250L

    private var chordHeld = false
    private val keys = HashMap<Int, Key>()

    val deadline: Long
        get() = keys.values.map { it.deadline }.filter { it != PowerPressDetector.NO_DEADLINE }.minOrNull()
            ?: PowerPressDetector.NO_DEADLINE

    fun onDown(code: Int, now: Long) {
        if (chordHeld || !intercepts(code)) {
            forward(code, now)
            return
        }
        if (fireCombo(code, now)) return
        val key = keys[code]
        if (key?.phase == Phase.WAITING_NEXT) {
            pressAgain(code, key, now)
            return
        }
        keys[code] = Key(Phase.DOWN, now, now + holdDelay(code))
    }

    fun onUp(code: Int, now: Long) {
        val key = keys[code] ?: return
        when (key.phase) {
            Phase.FORWARDED -> {
                keys.remove(code)
                output.releaseKey(code)
            }
            Phase.SWALLOWED -> keys.remove(code)
            Phase.DOWN -> released(code, key, now)
            Phase.WAITING_NEXT -> Unit
        }
    }

    fun onTimeout(now: Long) {
        keys.filterValues { it.deadline != PowerPressDetector.NO_DEADLINE && now >= it.deadline }
            .forEach { (code, key) -> expire(code, key, now) }
    }

    fun onPowerDown(now: Long) {
        chordHeld = true
        keys.entries.toList().forEach { (code, key) ->
            when (key.phase) {
                Phase.DOWN -> {
                    settle(code, key.presses - 1)
                    forward(code, now)
                }
                Phase.WAITING_NEXT -> {
                    keys.remove(code)
                    settle(code, key.presses)
                }
                else -> Unit
            }
        }
    }

    fun onPowerUp() {
        chordHeld = false
    }

    fun reset() {
        keys.clear()
        chordHeld = false
    }

    private fun intercepts(code: Int): Boolean =
        ButtonTrigger.VOLUME_COMBO in triggers || lastPressCount(code) > 0 || longTriggers[code] in triggers

    private fun lastPressCount(code: Int): Int =
        pressTriggers.getValue(code).filter { it in triggers }.maxOfOrNull { it.presses } ?: 0

    private fun pressTrigger(code: Int, presses: Int): ButtonTrigger? =
        pressTriggers.getValue(code).firstOrNull { it.presses == presses && it in triggers }

    private fun pressAgain(code: Int, key: Key, now: Long) {
        key.presses++
        if (key.presses == lastPressCount(code)) {
            swallow(key)
            pressTrigger(code, key.presses)?.let(output::trigger)
            return
        }
        key.phase = Phase.DOWN
        key.downAt = now
        key.deadline = now + holdDelay(code)
    }

    private fun fireCombo(code: Int, now: Long): Boolean {
        if (ButtonTrigger.VOLUME_COMBO !in triggers) return false
        val other = keys[otherKey(code)] ?: return false
        if (other.phase != Phase.DOWN || now - other.downAt > COMBO_WINDOW_MS) return false
        swallow(other)
        keys[code] = Key(Phase.SWALLOWED, now, PowerPressDetector.NO_DEADLINE)
        output.trigger(ButtonTrigger.VOLUME_COMBO)
        return true
    }

    private fun holdDelay(code: Int): Long = when {
        longTriggers[code] in triggers -> LONG_PRESS_MS
        lastPressCount(code) > 0 -> doublePressWindowMs
        else -> COMBO_WINDOW_MS
    }

    private fun released(code: Int, key: Key, now: Long) {
        if (lastPressCount(code) > key.presses) {
            key.phase = Phase.WAITING_NEXT
            key.deadline = now + doublePressWindowMs
        } else {
            tap(code)
        }
    }

    private fun expire(code: Int, key: Key, now: Long) {
        when (key.phase) {
            Phase.WAITING_NEXT -> {
                keys.remove(code)
                settle(code, key.presses)
            }
            Phase.FORWARDED -> {
                key.deadline = now + KEY_REPEAT_DELAY_MS
                output.repeatKey(code)
            }
            Phase.DOWN -> expireHeld(code, key)
            Phase.SWALLOWED -> Unit
        }
    }

    private fun expireHeld(code: Int, key: Key) {
        settle(code, key.presses - 1)
        key.presses = 1
        val longTrigger = longTriggers.getValue(code)
        if (longTrigger in triggers) {
            swallow(key)
            output.trigger(longTrigger)
        } else {
            key.phase = Phase.FORWARDED
            key.deadline = key.downAt + KEY_REPEAT_TIMEOUT_MS
            output.pressKey(code)
        }
    }

    private fun forward(code: Int, now: Long) {
        keys[code] = Key(Phase.FORWARDED, now, now + KEY_REPEAT_TIMEOUT_MS)
        output.pressKey(code)
    }

    private fun tap(code: Int) {
        keys.remove(code)
        settle(code, 1)
    }

    private fun settle(code: Int, presses: Int) {
        val trigger = pressTrigger(code, presses)
        if (trigger != null) {
            output.trigger(trigger)
            return
        }
        repeat(presses) {
            output.pressKey(code)
            output.releaseKey(code)
        }
    }

    private fun swallow(key: Key) {
        key.phase = Phase.SWALLOWED
        key.deadline = PowerPressDetector.NO_DEADLINE
    }

    private fun otherKey(code: Int): Int = if (code == VOLUME_UP) VOLUME_DOWN else VOLUME_UP

    companion object {
        const val VOLUME_DOWN = 114
        const val VOLUME_UP = 115
        const val COMBO_WINDOW_MS = 100L
        const val LONG_PRESS_MS = 500L
        const val KEY_REPEAT_TIMEOUT_MS = 400L
        const val KEY_REPEAT_DELAY_MS = 50L

        private val pressTriggers = mapOf(
            VOLUME_UP to listOf(ButtonTrigger.VOLUME_UP_DOUBLE, ButtonTrigger.VOLUME_UP_TRIPLE),
            VOLUME_DOWN to listOf(ButtonTrigger.VOLUME_DOWN_DOUBLE, ButtonTrigger.VOLUME_DOWN_TRIPLE)
        )
        private val longTriggers = mapOf(
            VOLUME_UP to ButtonTrigger.VOLUME_UP_LONG,
            VOLUME_DOWN to ButtonTrigger.VOLUME_DOWN_LONG
        )
    }
}
