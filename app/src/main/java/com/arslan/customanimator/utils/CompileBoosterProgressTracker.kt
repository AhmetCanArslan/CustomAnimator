package com.arslan.customanimator.utils

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CompileBoosterProgressTracker {

    data class CompileResult(val packageName: String, val label: String, val success: Boolean)

    data class Progress(
        val isRunning: Boolean = false,
        val total: Int = 0,
        val currentPackage: String = "",
        val currentLabel: String = "",
        val results: List<CompileResult> = emptyList(),
        val startedAtMillis: Long = 0L,
        val finishedAtMillis: Long = 0L
    ) {
        val current: Int
            get() = results.size

        val percent: Int
            get() = if (total <= 0) 0 else (current * 100 / total).coerceIn(0, 100)

        val failedCount: Int
            get() = results.count { !it.success }

        val isFinished: Boolean
            get() = !isRunning && results.isNotEmpty()

        val isStopped: Boolean
            get() = isFinished && current < total

        val elapsedMillis: Long
            get() = finishedAtMillis - startedAtMillis

        val remainingMillis: Long?
            get() = if (current < MIN_RESULTS_FOR_ESTIMATE) {
                null
            } else {
                (SystemClock.elapsedRealtime() - startedAtMillis) / current * (total - current)
            }
    }

    private const val MIN_RESULTS_FOR_ESTIMATE = 3

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    fun prepare() {
        _progress.value = Progress(isRunning = true)
    }

    fun start(total: Int) {
        _progress.value = Progress(isRunning = true, total = total, startedAtMillis = SystemClock.elapsedRealtime())
    }

    fun compiling(packageName: String, label: String) {
        _progress.update { it.copy(currentPackage = packageName, currentLabel = label) }
    }

    fun record(result: CompileResult) {
        _progress.update { it.copy(results = it.results + result) }
    }

    fun finish() {
        _progress.update {
            if (it.isRunning) {
                it.copy(
                    isRunning = false,
                    currentPackage = "",
                    currentLabel = "",
                    finishedAtMillis = SystemClock.elapsedRealtime()
                )
            } else {
                it
            }
        }
    }

    fun reset() {
        _progress.value = Progress()
    }
}
