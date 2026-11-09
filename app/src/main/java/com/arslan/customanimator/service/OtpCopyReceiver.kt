package com.arslan.customanimator.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arslan.customanimator.utils.OtpCopierManager

class OtpCopyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        OtpCopierManager.copy(context, code)
    }

    companion object {
        private const val EXTRA_CODE = "code"

        fun pendingIntent(context: Context, code: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, OtpCopyReceiver::class.java).putExtra(EXTRA_CODE, code),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
