package com.arslan.customanimator.service.watcher

import android.content.Context
import android.util.Log
import com.arslan.customanimator.utils.DangerousPermissionsHelper
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.PermissionDisablerManager
import com.arslan.customanimator.utils.RevokedPermissionsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PermissionDisablerWatcher : AppVisibilityWatcher {

    private const val TAG = "PermissionDisablerWatcher"
    private const val BACKGROUND_GRACE_MS = 1200L

    @Volatile
    private var selectedPackages: Set<String> = emptySet()

    private val pendingRevokes = mutableMapOf<String, Long>()

    override val isEnabled: Boolean
        get() = selectedPackages.isNotEmpty()

    override fun refresh(context: Context) {
        selectedPackages = PermissionDisablerManager(context).getSelectedPackages()
    }

    override fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope) {
        synchronized(pendingRevokes) { pendingRevokes.remove(packageName) }
        if (packageName !in selectedPackages) return
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) { regrantPermissionsForPackage(appContext, packageName) }
    }

    override fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope) {
        if (packageName !in selectedPackages) return
        synchronized(pendingRevokes) {
            if (packageName !in pendingRevokes) {
                pendingRevokes[packageName] = System.currentTimeMillis()
            }
        }
    }

    override fun onTick(context: Context, scope: CoroutineScope) {
        if (synchronized(pendingRevokes) { pendingRevokes.isEmpty() }) return
        val now = System.currentTimeMillis()
        val due = synchronized(pendingRevokes) {
            val expired = pendingRevokes.filter { now - it.value >= BACKGROUND_GRACE_MS }.keys.toList()
            expired.forEach { pendingRevokes.remove(it) }
            expired
        }
        val appContext = context.applicationContext
        for (packageName in due) {
            if (packageName !in selectedPackages) continue
            scope.launch(Dispatchers.IO) { revokePermissionsForPackage(appContext, packageName) }
        }
    }

    override fun onWatchStopped(context: Context) = clearPending()

    private fun clearPending() {
        synchronized(pendingRevokes) { pendingRevokes.clear() }
    }

    private fun revokePermissionsForPackage(context: Context, packageName: String) {
        val granted = DangerousPermissionsHelper.getGrantedDangerousPermissions(context, packageName)
        if (granted.isEmpty()) return
        val actuallyRevoked = granted.filter { DeveloperOptionsManager.revokePermission(packageName, it) }
        RevokedPermissionsStore(context).recordRevoked(packageName, actuallyRevoked)
        Log.d(TAG, "Revoked ${actuallyRevoked.size}/${granted.size} permissions for $packageName")
    }

    private fun regrantPermissionsForPackage(context: Context, packageName: String) {
        val store = RevokedPermissionsStore(context)
        val toRegrant = store.getRevoked(packageName)
        if (toRegrant.isEmpty()) return
        val allSucceeded = toRegrant.map { DeveloperOptionsManager.grantPermission(packageName, it) }.all { it }
        if (allSucceeded) {
            store.clearRevoked(packageName)
        }
        Log.d(TAG, "Regranted permissions for $packageName allSucceeded=$allSucceeded")
    }
}
