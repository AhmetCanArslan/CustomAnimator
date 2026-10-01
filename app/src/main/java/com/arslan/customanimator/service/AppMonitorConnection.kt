package com.arslan.customanimator.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku

class AppMonitorConnection(
    context: Context,
    private val listener: IForegroundAppListener,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onConnectionLost: () -> Unit
) {

    private val args = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, AppMonitorUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("monitor")
        .debuggable(false)
        .version(SERVICE_VERSION)

    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val listening = attachListener(binder)
            if (!listening) disconnect()
            onListeningChanged(listening)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (!bound) return
            bound = false
            onConnectionLost()
        }
    }

    fun connect() {
        if (bound) return
        bound = true
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Throwable) {
            Log.e(TAG, "bindUserService failed", e)
            bound = false
            onConnectionLost()
        }
    }

    fun disconnect() {
        if (!bound) return
        bound = false
        runCatching { Shizuku.unbindUserService(args, connection, true) }
    }

    private fun attachListener(binder: IBinder?): Boolean {
        if (binder == null || !binder.pingBinder()) return false
        return try {
            IAppMonitorUserService.Stub.asInterface(binder).setListener(listener)
        } catch (e: Throwable) {
            Log.e(TAG, "setListener failed", e)
            false
        }
    }

    private companion object {
        const val TAG = "AppMonitorConnection"
        const val SERVICE_VERSION = 1
    }
}
