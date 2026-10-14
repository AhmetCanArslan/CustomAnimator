package com.arslan.customanimator.screenshot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.R

class ScreenshotActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun handle(intent: Intent) {
        val action = intent.action
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            NotificationManagerCompat.from(this).cancel(notifId)
        }

        val target = intent.data
        val item = when {
            target != null -> ScreenshotActions.itemFor(this, target)
            action == ACTION_COPY -> ScreenshotActions.newestScreenshot(this)
            else -> ScreenshotActions.latestScreenshot(this)
        }
        if (item == null) {
            ScreenshotToasts.show(this, R.string.screenshot_toast_not_found)
            return
        }

        val copied = action != ACTION_DELETE && ScreenshotActions.copyToClipboard(this, item)
        val shouldDelete = action == ACTION_DELETE || (action == ACTION_COPY_DELETE && copied)
        val deleted = shouldDelete && ScreenshotActions.delete(this, item)
        ScreenshotToasts.show(this, messageFor(action, copied, deleted))
    }

    private fun messageFor(action: String?, copied: Boolean, deleted: Boolean): Int = when {
        action != ACTION_DELETE && !copied -> R.string.screenshot_toast_copy_failed
        action == ACTION_COPY -> R.string.screenshot_toast_copied
        !deleted -> R.string.screenshot_toast_delete_failed
        copied -> R.string.screenshot_toast_copied_deleted
        else -> R.string.screenshot_toast_deleted
    }

    companion object {
        const val ACTION_DELETE = "com.arslan.customanimator.action.SCREENSHOT_DELETE"
        const val ACTION_COPY = "com.arslan.customanimator.action.SCREENSHOT_COPY"
        const val ACTION_COPY_DELETE = "com.arslan.customanimator.action.SCREENSHOT_COPY_DELETE"
        const val EXTRA_NOTIF_ID = "extra_notif_id"

        fun intent(
            context: Context,
            action: String,
            screenshot: Uri?,
            notifId: Int
        ): Intent = Intent(context, ScreenshotActionActivity::class.java).apply {
            this.action = action
            data = screenshot
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
    }
}
