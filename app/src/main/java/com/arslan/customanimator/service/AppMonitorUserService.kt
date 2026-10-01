package com.arslan.customanimator.service

import android.app.ActivityManager
import android.app.IProcessObserver
import android.os.Build
import android.os.RemoteException
import android.util.Log

class AppMonitorUserService : IAppMonitorUserService.Stub() {

    @Volatile
    private var listener: IForegroundAppListener? = null

    private var registered = false

    private val observer = object : IProcessObserver.Stub() {
        override fun onForegroundActivitiesChanged(pid: Int, uid: Int, foregroundActivities: Boolean) {
            deliver { it.onForegroundActivitiesChanged(pid, packagesForUid(uid), foregroundActivities) }
        }

        override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) = Unit

        override fun onProcessStateChanged(pid: Int, uid: Int, procState: Int) = Unit

        override fun onProcessStarted(
            pid: Int,
            processUid: Int,
            packageUid: Int,
            packageName: String?,
            processName: String?
        ) = Unit

        override fun onProcessDied(pid: Int, uid: Int) {
            deliver { it.onProcessDied(pid) }
        }
    }

    override fun destroy() {
        unregister()
        System.exit(0)
    }

    @Synchronized
    override fun setListener(listener: IForegroundAppListener?): Boolean {
        this.listener = listener
        if (!registered) {
            try {
                activityManager().javaClass
                    .getMethod("registerProcessObserver", IProcessObserver::class.java)
                    .invoke(activityManager(), observer)
                registered = true
            } catch (e: Throwable) {
                Log.e(TAG, "registerProcessObserver failed", e)
                return false
            }
        }
        sendTopProcessSnapshot()
        return true
    }

    private fun sendTopProcessSnapshot() {
        try {
            val topState = ActivityManager::class.java.getField("PROCESS_STATE_TOP").getInt(null)
            val processStateField = ActivityManager.RunningAppProcessInfo::class.java.getField("processState")
            val processes = activityManager().javaClass
                .getMethod("getRunningAppProcesses")
                .invoke(activityManager()) as? List<*> ?: return
            processes
                .filterIsInstance<ActivityManager.RunningAppProcessInfo>()
                .filter { processStateField.getInt(it) == topState }
                .forEach { info ->
                    deliver { it.onForegroundActivitiesChanged(info.pid, packagesForUid(info.uid), true) }
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Top process snapshot failed", e)
        }
    }

    @Synchronized
    private fun unregister() {
        if (!registered) return
        runCatching {
            activityManager().javaClass
                .getMethod("unregisterProcessObserver", IProcessObserver::class.java)
                .invoke(activityManager(), observer)
        }
        registered = false
    }

    private fun deliver(action: (IForegroundAppListener) -> Unit) {
        val target = listener ?: return
        try {
            action(target)
        } catch (e: RemoteException) {
            Log.d(TAG, "Listener is gone", e)
            listener = null
        }
    }

    private fun packagesForUid(uid: Int): Array<String> {
        return try {
            val packageManager = Class.forName("android.app.ActivityThread")
                .getMethod("getPackageManager")
                .invoke(null)
            @Suppress("UNCHECKED_CAST")
            packageManager.javaClass
                .getMethod("getPackagesForUid", Int::class.javaPrimitiveType)
                .invoke(packageManager, uid) as? Array<String> ?: emptyArray()
        } catch (e: Throwable) {
            Log.e(TAG, "getPackagesForUid failed for uid=$uid", e)
            emptyArray()
        }
    }

    private fun activityManager(): Any {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null)
        } else {
            Class.forName("android.app.ActivityManagerNative").getMethod("getDefault").invoke(null)
        } ?: error("activity manager unavailable")
    }

    private companion object {
        const val TAG = "AppMonitorUserService"
    }
}
