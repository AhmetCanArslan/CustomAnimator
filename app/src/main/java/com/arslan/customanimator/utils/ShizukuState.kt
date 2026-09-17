package com.arslan.customanimator.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

val LocalShizukuAvailable = compositionLocalOf { false }
val LocalShizukuPermission = compositionLocalOf { false }
val LocalWriteSecureSettings = compositionLocalOf { false }

object ShizukuState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    var isAvailable by mutableStateOf(false)
        private set
    var hasPermission by mutableStateOf(false)
        private set
    var hasWriteSecureSettings by mutableStateOf(false)
        private set

    private var appContext: Context? = null
    private var listenersRegistered = false
    private var grantInFlight = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { onBinderAlive() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    fun start(context: Context) {
        appContext = context.applicationContext
        if (!listenersRegistered) {
            listenersRegistered = true
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        refresh()
    }

    fun refresh() {
        val context = appContext ?: return
        val available = ShizukuHelper.isShizukuAvailable()
        val permission = ShizukuHelper.hasShizukuPermission()
        val secureSettings = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        onMain {
            isAvailable = available
            hasPermission = permission
            hasWriteSecureSettings = secureSettings
        }
        if (permission && !secureSettings) grantSecureSettings(context)
    }

    fun requestPermission(context: Context) {
        ShizukuHelper.requestShizukuPermission(context)
        ShizukuHelper.markShizukuRequested(context)
    }

    private fun onBinderAlive() {
        val context = appContext ?: return
        refresh()
        if (!ShizukuHelper.hasShizukuPermission() && !ShizukuHelper.hasShizukuBeenRequested(context)) {
            requestPermission(context)
        }
    }

    private fun grantSecureSettings(context: Context) {
        if (grantInFlight) return
        grantInFlight = true
        scope.launch {
            ShizukuHelper.grantWriteSecureSettingsPermission(context)
            val granted = ShizukuHelper.hasWriteSecureSettingsPermission(context)
            onMain { hasWriteSecureSettings = granted }
            grantInFlight = false
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}

@Composable
fun ProvideShizukuState(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        ShizukuState.start(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ShizukuState.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    CompositionLocalProvider(
        LocalShizukuAvailable provides ShizukuState.isAvailable,
        LocalShizukuPermission provides ShizukuState.hasPermission,
        LocalWriteSecureSettings provides ShizukuState.hasWriteSecureSettings,
        content = content
    )
}
