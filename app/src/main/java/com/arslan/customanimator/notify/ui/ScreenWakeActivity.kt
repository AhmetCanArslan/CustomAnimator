package com.arslan.customanimator.notify.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity

class ScreenWakeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DURATION_SEC = "extra_wake_duration_sec"
        const val ACTION_STOP_WAKE = "com.arslan.customanimator.STOP_SCREEN_WAKE"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable { finish() }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_WAKE), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_WAKE))
        }

        scheduleFinish(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        scheduleFinish(intent)
    }

    private fun scheduleFinish(intent: Intent?) {
        val durationSec = intent?.getIntExtra(EXTRA_DURATION_SEC, 10) ?: 10
        handler.removeCallbacks(finishRunnable)
        handler.postDelayed(finishRunnable, (if (durationSec > 0) durationSec else 10) * 1000L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(finishRunnable)
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
