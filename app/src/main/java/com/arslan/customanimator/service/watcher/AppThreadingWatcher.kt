package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.AppThreadingConfig
import com.arslan.customanimator.utils.AppThreadingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppThreadingWatcher : AppVisibilityWatcher {

    private const val TAG = "AppThreadingWatcher"
    private const val REAPPLY_DELAY_MS = 2000L

    @Volatile
    private var configs: Map<String, AppThreadingConfig> = emptyMap()

    @Volatile
    private var manager: AppThreadingManager? = null

    private val appliedPids = mutableMapOf<String, List<String>>()
    private val pendingJobs = mutableMapOf<String, Job>()

    override val isEnabled: Boolean
        get() = configs.isNotEmpty()

    override fun refresh(context: Context) {
        val threadingManager = AppThreadingManager(context)
        manager = threadingManager
        configs = threadingManager.getConfigs()
        synchronized(appliedPids) { appliedPids.keys.retainAll(configs.keys) }
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        val config = configs[packageName] ?: return
        val threadingManager = manager ?: return
        synchronized(pendingJobs) {
            pendingJobs[packageName]?.cancel()
            pendingJobs[packageName] = scope.launch(Dispatchers.IO) {
                val pids = threadingManager.runningPids(packageName)
                if (pids.isEmpty() || isAlreadyApplied(packageName, pids)) return@launch
                apply(threadingManager, packageName, config, pids)

                delay(REAPPLY_DELAY_MS)
                val laterPids = threadingManager.runningPids(packageName)
                if (laterPids.isEmpty()) return@launch
                apply(threadingManager, packageName, config, laterPids)
            }
        }
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        synchronized(pendingJobs) { pendingJobs.remove(packageName)?.cancel() }
    }

    override fun onWatchStopped(context: Context) {
        synchronized(pendingJobs) {
            pendingJobs.values.forEach { it.cancel() }
            pendingJobs.clear()
        }
        synchronized(appliedPids) { appliedPids.clear() }
    }

    private fun isAlreadyApplied(packageName: String, pids: List<String>): Boolean =
        synchronized(appliedPids) { appliedPids[packageName] == pids }

    private fun apply(
        threadingManager: AppThreadingManager,
        packageName: String,
        config: AppThreadingConfig,
        pids: List<String>
    ) {
        val result = threadingManager.apply(config, pids)
        Log.d(TAG, "Applied threading for $packageName pids=${pids.size} result=$result")
        if (result.isSuccess) {
            synchronized(appliedPids) { appliedPids[packageName] = pids }
        }
    }
}
