package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AutoForceStopWatcher : AppVisibilityWatcher {

    private const val TAG = "AutoForceStopWatcher"
    private const val RECENTLY_KILLED_TTL_MS = 3000L
    private const val BACKGROUND_GRACE_MS = 1200L

    @Volatile
    private var selectedPackages: Set<String> = emptySet()

    private val pendingKills = mutableMapOf<String, Long>()
    private val recentlyKilled = mutableMapOf<String, Long>()

    override val isEnabled: Boolean
        get() = selectedPackages.isNotEmpty()

    override fun refresh(context: Context) {
        selectedPackages = AutoForceStopManager(context).getSelectedPackages()
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        synchronized(pendingKills) { pendingKills.remove(packageName) }
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName !in selectedPackages) return
        synchronized(pendingKills) {
            if (packageName !in pendingKills) {
                pendingKills[packageName] = System.currentTimeMillis()
            }
        }
    }

    override fun onTick(context: Context, scope: CoroutineScope) {
        if (synchronized(pendingKills) { pendingKills.isEmpty() } &&
            synchronized(recentlyKilled) { recentlyKilled.isEmpty() }
        ) {
            return
        }
        val now = System.currentTimeMillis()
        synchronized(recentlyKilled) {
            recentlyKilled.entries.removeAll { now - it.value > RECENTLY_KILLED_TTL_MS }
        }
        val due = synchronized(pendingKills) {
            val expired = pendingKills.filter { now - it.value >= BACKGROUND_GRACE_MS }.keys.toList()
            expired.forEach { pendingKills.remove(it) }
            expired
        }
        for (packageName in due) {
            if (packageName !in selectedPackages) continue
            val shouldKill = synchronized(recentlyKilled) {
                if (recentlyKilled.containsKey(packageName)) {
                    false
                } else {
                    recentlyKilled[packageName] = now
                    true
                }
            }
            if (!shouldKill) continue
            scope.launch(Dispatchers.IO) {
                val success = DeveloperOptionsManager.forceStopApp(packageName)
                Log.d(TAG, "Force-stopped $packageName success=$success")
            }
        }
    }

    override fun onWatchStopped(context: Context) = clearPending()

    private fun clearPending() {
        synchronized(pendingKills) { pendingKills.clear() }
        synchronized(recentlyKilled) { recentlyKilled.clear() }
    }
}
