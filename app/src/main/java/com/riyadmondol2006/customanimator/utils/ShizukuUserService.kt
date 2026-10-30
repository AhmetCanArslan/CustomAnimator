package com.riyadmondol2006.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import android.util.Log
import com.riyadmondol2006.customanimator.BuildConfig
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

class ShizukuUserService<T : IInterface>(
    private val serviceClass: Class<*>,
    private val processSuffix: String,
    private val daemon: Boolean,
    private val revision: Int,
    private val asInterface: (IBinder) -> T
) {

    private val lock = Mutex()

    @Volatile
    private var service: T? = null

    @Volatile
    private var pending: CancellableContinuation<T?>? = null

    private var binding: Pair<Shizuku.UserServiceArgs, ServiceConnection>? = null

    suspend fun <R> call(context: Context, block: (T) -> R): R? {
        if (!ShizukuHelper.hasShizukuPermission()) return null
        val target = obtain(context) ?: return null
        return try {
            block(target)
        } catch (e: Throwable) {
            Log.e(TAG, "${serviceClass.simpleName} call failed", e)
            service = null
            null
        }
    }

    suspend fun unbind() = lock.withLock {
        binding?.let { (args, connection) -> runCatching { Shizuku.unbindUserService(args, connection, true) } }
        binding = null
        service = null
    }

    private fun alive(): T? =
        service?.takeIf { runCatching { it.asBinder().pingBinder() }.getOrDefault(false) }

    private suspend fun obtain(context: Context): T? {
        alive()?.let { return it }
        return lock.withLock { alive() ?: bind(context.applicationContext) }
    }

    private suspend fun bind(context: Context): T? {
        val args = args(context, serviceClass, processSuffix, daemon, revision)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val connected = binder
                    ?.takeIf { runCatching { it.pingBinder() }.getOrDefault(false) }
                    ?.let(asInterface)
                service = connected
                pending?.takeIf { it.isActive }?.resume(connected)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
        val connected = withTimeoutOrNull(BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                pending = continuation
                try {
                    Shizuku.bindUserService(args, connection)
                } catch (e: Throwable) {
                    Log.e(TAG, "bindUserService failed for ${serviceClass.simpleName}", e)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
        pending = null
        if (connected == null) {
            Log.w(TAG, "${serviceClass.simpleName} did not bind within ${BIND_TIMEOUT_MS}ms")
            runCatching { Shizuku.unbindUserService(args, connection, false) }
            return null
        }
        binding = args to connection
        return connected
    }

    companion object {
        private const val TAG = "ShizukuUserService"
        private const val BIND_TIMEOUT_MS = 15_000L
        private const val REVISIONS_PER_RELEASE = 100

        fun args(
            context: Context,
            serviceClass: Class<*>,
            processSuffix: String,
            daemon: Boolean,
            revision: Int
        ): Shizuku.UserServiceArgs =
            Shizuku.UserServiceArgs(ComponentName(context.packageName, serviceClass.name))
                .daemon(daemon)
                .processNameSuffix(processSuffix)
                .debuggable(false)
                .version(BuildConfig.VERSION_CODE * REVISIONS_PER_RELEASE + revision)
    }
}
