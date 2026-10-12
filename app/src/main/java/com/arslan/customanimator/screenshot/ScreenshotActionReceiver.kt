package com.arslan.customanimator.screenshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val appContext = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                val message = remove(appContext, intent)
                withContext(Dispatchers.Main) {
                    ScreenshotToasts.show(appContext, message)
                }
            } finally {
                pending.finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun remove(context: Context, intent: Intent): Int {
        val notifId = intent.getIntExtra(ScreenshotActionActivity.EXTRA_NOTIF_ID, -1)
        if (notifId != -1) NotificationManagerCompat.from(context).cancel(notifId)

        val mediaId = intent.getLongExtra(ScreenshotActionActivity.EXTRA_MEDIA_ID, -1L)
        val item = ScreenshotActions.targetOf(context, mediaId) ?: return R.string.screenshot_toast_not_found
        val copied = intent.action == ScreenshotActionActivity.ACTION_COPY_DELETE &&
            ScreenshotActions.copyToClipboard(context, item)

        return when (ScreenshotActions.moveToTrash(context, item)) {
            ScreenshotTrashResult.TRASHED ->
                if (copied) R.string.screenshot_toast_copied_trashed else R.string.screenshot_toast_trashed
            ScreenshotTrashResult.NO_SHIZUKU -> R.string.screenshot_toast_trash_needs_shizuku
            ScreenshotTrashResult.FAILED -> R.string.screenshot_toast_trash_failed
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun handles(action: String?): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                (action == ScreenshotActionActivity.ACTION_DELETE ||
                    action == ScreenshotActionActivity.ACTION_COPY_DELETE)

        fun intent(context: Context, action: String, mediaId: Long, notifId: Int): Intent =
            Intent(context, ScreenshotActionReceiver::class.java).apply {
                this.action = action
                putExtra(ScreenshotActionActivity.EXTRA_MEDIA_ID, mediaId)
                putExtra(ScreenshotActionActivity.EXTRA_NOTIF_ID, notifId)
            }
    }
}
