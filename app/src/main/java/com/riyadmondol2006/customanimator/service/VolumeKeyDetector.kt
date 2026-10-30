package com.riyadmondol2006.customanimator.service

import com.riyadmondol2006.customanimator.utils.ButtonTrigger

class VolumeKeyDetector(private val output: Output) {

    interface Output {
        fun pressKey(code: Int)
        fun releaseKey(code: Int)
        fun trigger(trigger: ButtonTrigger)
    }

    private enum class Phase { DOWN, WAITING_SECOND, FORWARDED, SWALLOWED }

    private class Key(var phase: Phase, val downAt: Long, var deadline: Long)

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
        if (key?.phase == Phase.WAITING_SECOND) {
            swallow(key)
            output.trigger(doubleTriggers.getValue(code))
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
            Phase.WAITING_SECOND -> Unit
        }
    }

    fun onTimeout(now: Long) {
        keys.filterValues { it.deadline != PowerPressDetector.NO_DEADLINE && now >= it.deadline }
            .forEach { (code, key) -> expire(code, key) }
    }

    fun onPowerDown(now: Long) {
        chordHeld = true
        keys.entries.toList().forEach { (code, key) ->
            when (key.phase) {
                Phase.DOWN -> forward(code, now)
                Phase.WAITING_SECOND -> tap(code)
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
        ButtonTrigger.VOLUME_COMBO in triggers || doubleTriggers[code] in triggers || longTriggers[code] in triggers

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
        doubleTriggers[code] in triggers -> doublePressWindowMs
        else -> COMBO_WINDOW_MS
    }

    private fun released(code: Int, key: Key, now: Long) {
        if (doubleTriggers[code] in triggers) {
            key.phase = Phase.WAITING_SECOND
            key.deadline = now + doublePressWindowMs
        } else {
            tap(code)
        }
    }

    private fun expire(code: Int, key: Key) {
        val longTrigger = longTriggers.getValue(code)
        when {
            key.phase == Phase.WAITING_SECOND -> tap(code)
            key.phase != Phase.DOWN -> Unit
            longTrigger in triggers -> {
                swallow(key)
                output.trigger(longTrigger)
            }
            else -> {
                key.phase = Phase.FORWARDED
                key.deadline = PowerPressDetector.NO_DEADLINE
                output.pressKey(code)
            }
        }
    }

    private fun forward(code: Int, now: Long) {
        keys[code] = Key(Phase.FORWARDED, now, PowerPressDetector.NO_DEADLINE)
        output.pressKey(code)
    }

    private fun tap(code: Int) {
        keys.remove(code)
        output.pressKey(code)
        output.releaseKey(code)
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

        private val doubleTriggers = mapOf(
            VOLUME_UP to ButtonTrigger.VOLUME_UP_DOUBLE,
            VOLUME_DOWN to ButtonTrigger.VOLUME_DOWN_DOUBLE
        )
        private val longTriggers = mapOf(
            VOLUME_UP to ButtonTrigger.VOLUME_UP_LONG,
            VOLUME_DOWN to ButtonTrigger.VOLUME_DOWN_LONG
        )
    }
}
