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
        val manager = PerAppRefreshRateManager(context)
        if (appliedPackage == null) {
            baselineMinRate = RefreshRateManager.getMinRate(context.contentResolver)
            baselinePeakRate = RefreshRateManager.getPeakRate(context.contentResolver)
            manager.recordBaseline(baselineMinRate, baselinePeakRate)
        }
        val success = RefreshRateManager.applyRate(targetRate)
        if (success) {
            appliedPackage = packageName
        } else if (appliedPackage == null) {
            manager.clearRecordedBaseline()
        }
        Log.d(TAG, "Applied rate=$targetRate for $packageName success=$success")
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName == appliedPackage) restoreBaseline(context)
    }

    override fun onWatchStopped(context: Context) = restoreBaseline(context)

    override fun recoverStaleState(context: Context) {
        if (appliedPackage != null) return
        val manager = PerAppRefreshRateManager(context)
        if (!manager.hasRecordedBaseline()) return
        val min = manager.recordedBaselineMin()
        val peak = manager.recordedBaselinePeak()
        val success = RefreshRateManager.restoreRates(min, peak)
        if (success) manager.clearRecordedBaseline()
        Log.d(TAG, "Recovered stale baseline min=$min peak=$peak success=$success")
    }

    private fun restoreBaseline(context: Context) {
        if (appliedPackage == null) return
        val success = RefreshRateManager.restoreRates(baselineMinRate, baselinePeakRate)
        if (success) PerAppRefreshRateManager(context).clearRecordedBaseline()
        Log.d(TAG, "Restored min=$baselineMinRate peak=$baselinePeakRate success=$success")
        appliedPackage = null
        baselineMinRate = null
        baselinePeakRate = null
    }
}
