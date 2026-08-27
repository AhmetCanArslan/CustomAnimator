package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.PerAppWidthManager
import com.arslan.customanimator.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope

object PerAppWidthWatcher : AppVisibilityWatcher {

    private const val TAG = "PerAppWidthWatcher"

    @Volatile
    private var overrides: Map<String, Int> = emptyMap()

    @Volatile
    private var appliedPackage: String? = null

    @Volatile
    private var baselineDensity: Int? = null

    override val isEnabled: Boolean
        get() = overrides.isNotEmpty()

    override fun refresh(context: Context) {
        overrides = PerAppWidthManager(context).getOverrides()
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        val targetWidthDp = overrides[packageName] ?: return
        if (appliedPackage == packageName) return
        if (appliedPackage == null) {
            baselineDensity = SettingsManager.getForcedDensity(context.contentResolver)
        }
        val targetDensity = SettingsManager.densityForSmallestWidth(context, targetWidthDp)
        val success = SettingsManager.applyDensity(context.contentResolver, targetDensity)
        if (success) {
            appliedPackage = packageName
        }
        Log.d(TAG, "Applied width=${targetWidthDp}dp density=$targetDensity for $packageName success=$success")
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName == appliedPackage) restoreBaseline(context)
    }

    override fun onWatchStopped(context: Context) = restoreBaseline(context)

    private fun restoreBaseline(context: Context) {
        if (appliedPackage == null) return
        val success = SettingsManager.applyDensity(context.contentResolver, baselineDensity)
        Log.d(TAG, "Restored baseline density=$baselineDensity success=$success")
        appliedPackage = null
        baselineDensity = null
    }
}
