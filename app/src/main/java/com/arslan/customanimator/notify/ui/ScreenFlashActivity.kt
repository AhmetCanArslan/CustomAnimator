package com.arslan.customanimator.notify.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

class ScreenFlashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_COLOR_ARGB = "extra_color_argb"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_OVERLAY_MODE = "extra_overlay_mode"
        const val ACTION_STOP_FLASH = "com.arslan.customanimator.STOP_SCREEN_FLASH"
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val colorArgb = intent.getLongExtra(EXTRA_COLOR_ARGB, 0xFFFF1744)
        val durationSec = intent.getIntExtra(EXTRA_DURATION_SEC, 5)
        val overlayMode = intent.getBooleanExtra(EXTRA_OVERLAY_MODE, false)
        val flashColor = Color(colorArgb)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = overlayMode
            isAppearanceLightNavigationBars = overlayMode
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_FLASH), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_FLASH))
        }

        setContent {
            ScreenFlashContent(
                flashColor = flashColor,
                durationSeconds = durationSec,
                overlayMode = overlayMode,
                onFinish = { finish() }
            )
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) finish()
    }
}

@Composable
private fun ScreenFlashContent(
    flashColor: Color,
    durationSeconds: Int,
    overlayMode: Boolean,
    onFinish: () -> Unit,
) {
    var showColor by remember { mutableStateOf(false) }
    val effectiveDurationSeconds = if (durationSeconds == -1) 5 else durationSeconds
    val untilInteraction = effectiveDurationSeconds == -1
    val initialOverlayDelayMs = if (overlayMode) 0L else 500L
    val visibleFlashColor = flashColor.copy(alpha = if (overlayMode) 0.25f else 0.72f)
    val dimBackground = if (overlayMode) Color.Transparent else Color.Black.copy(alpha = 0.35f)

    LaunchedEffect(Unit) {
        delay(initialOverlayDelayMs)
        val totalMs = if (untilInteraction) Long.MAX_VALUE else effectiveDurationSeconds.coerceAtLeast(1) * 1000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < totalMs) {
            showColor = true
            delay(250)
            showColor = false
            delay(250)
        }
        onFinish()
    }

    val currentColor = if (showColor) visibleFlashColor else dimBackground

    Box(modifier = Modifier.fillMaxSize()) {
        if (!overlayMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .background(currentColor)
        )
    }
}
