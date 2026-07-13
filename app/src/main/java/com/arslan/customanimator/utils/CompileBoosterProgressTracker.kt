package com.arslan.customanimator.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory, process-wide progress state for the Compile Booster foreground service.
 * Lets the Developer screen show live percent/label while visible, independent of the
 * service's own notification (which is what keeps progress visible while navigating away).
 */
object CompileBoosterProgressTracker {

    data class Progress(
        val isRunning: Boolean = false,
        val current: Int = 0,
        val total: Int = 0,
        val currentLabel: String = ""
    ) {
        val percent: Int
            get() = if (total <= 0) 0 else (current * 100 / total).coerceIn(0, 100)
    }

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    fun update(isRunning: Boolean, current: Int, total: Int, currentLabel: String) {
        _progress.value = Progress(isRunning, current, total, currentLabel)
    }

    fun reset() {
        _progress.value = Progress()
    }
}
