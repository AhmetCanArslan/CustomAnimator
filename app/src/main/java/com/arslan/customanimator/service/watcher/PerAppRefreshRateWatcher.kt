package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.PerAppRefreshRateManager
import com.arslan.customanimator.utils.RefreshRateManager
import kotlinx.coroutines.CoroutineScope

object PerAppRefreshRateWatcher : AppVisibilityWatcher {

    private const val TAG = "PerAppRefreshRateWatcher"

    @Volatile
    private var overrides: Map<String, Float> = emptyMap()

    @Volatile
    private var appliedPackage: String? = null

    @Volatile
    private var baselineMinRate: Float? = null

    @Volatile
    private var baselinePeakRate: Float? = null

    override val isEnabled: Boolean
        get() = overrides.isNotEmpty()

    override fun refresh(context: Context) {
        overrides = PerAppRefreshRateManager(context).getOverrides()
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        val targetRate = overrides[packageName] ?: return
        if (appliedPackage == packageName) return
        if (appliedPackage == null) {
            baselineMinRate = RefreshRateManager.getMinRate(context.contentResolver)
            baselinePeakRate = RefreshRateManager.getPeakRate(context.contentResolver)
        }
        val success = RefreshRateManager.applyRate(targetRate)
        if (success) {
            appliedPackage = packageName
        }
        Log.d(TAG, "Applied rate=$targetRate for $packageName success=$success")
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName == appliedPackage) restoreBaseline()
    }

    override fun onWatchStopped(context: Context) = restoreBaseline()

    private fun restoreBaseline() {
        if (appliedPackage == null) return
        val success = RefreshRateManager.restoreRates(baselineMinRate, baselinePeakRate)
        Log.d(TAG, "Restored min=$baselineMinRate peak=$baselinePeakRate success=$success")
        appliedPackage = null
        baselineMinRate = null
        baselinePeakRate = null
    }
}
