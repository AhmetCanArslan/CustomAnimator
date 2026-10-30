package com.arslan.customanimator.service.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DelayedPackageActions(private val delayMs: Long) {

    private val jobs = mutableMapOf<String, Job>()

    fun schedule(packageName: String, scope: CoroutineScope, action: () -> Unit) {
        synchronized(jobs) {
            if (jobs[packageName]?.isActive == true) return
            jobs[packageName] = scope.launch(Dispatchers.IO) {
                delay(delayMs)
                action()
            }
        }
    }

    fun cancel(packageName: String) {
        synchronized(jobs) { jobs.remove(packageName)?.cancel() }
    }

    fun cancelAll() {
        synchronized(jobs) {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }
    }
}
