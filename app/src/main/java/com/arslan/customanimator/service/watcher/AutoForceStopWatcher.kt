package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import kotlinx.coroutines.CoroutineScope

object AutoForceStopWatcher : AppVisibilityWatcher {

    private const val TAG = "AutoForceStopWatcher"
    private const val BACKGROUND_GRACE_MS = 1200L

    @Volatile
    private var selectedPackages: Set<String> = emptySet()

    private val pendingKills = DelayedPackageActions(BACKGROUND_GRACE_MS)

    override val isEnabled: Boolean
        get() = selectedPackages.isNotEmpty()

    override fun refresh(context: Context) {
        selectedPackages = AutoForceStopManager(context).getSelectedPackages()
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        pendingKills.cancel(packageName)
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName !in selectedPackages) return
        pendingKills.schedule(packageName, scope) {
            if (packageName in selectedPackages) {
                val success = DeveloperOptionsManager.forceStopApp(packageName)
                Log.d(TAG, "Force-stopped $packageName success=$success")
            }
        }
    }

    override fun onWatchStopped(context: Context) = pendingKills.cancelAll()
}
