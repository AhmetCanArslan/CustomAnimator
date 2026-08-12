package com.arslan.customanimator.screenshot

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.R

/**
 * Invisible activity that performs a screenshot action. It exists because the
 * scoped-storage delete flow needs an Activity to launch the system's delete
 * confirmation.
 */
class ScreenshotActionActivity : ComponentActivity() {

    private var copied = false
    private var handled = false

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        finishWithToast(deleted = ScreenshotActions.isDeleteConfirmed(result.resultCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            handled = savedInstanceState.getBoolean(STATE_HANDLED)
            copied = savedInstanceState.getBoolean(STATE_COPIED)
        }
        if (handled) return
        handled = true
        handle(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HANDLED, handled)
        outState.putBoolean(STATE_COPIED, copied)
    }

    private fun handle(intent: Intent) {
        val action = intent.action
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            NotificationManagerCompat.from(this).cancel(notifId)
        }

        val id = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
        val item = if (id >= 0) {
            ScreenshotActions.itemFor(this, id)
        } else if (action == ACTION_COPY) {
            ScreenshotActions.newestScreenshot(this)
        } else {
            ScreenshotActions.latestScreenshot(this)
        }
        if (item == null) {
            Toast.makeText(this, R.string.screenshot_toast_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (action == ACTION_COPY_DELETE || action == ACTION_COPY) {
            copied = ScreenshotActions.copyToClipboard(this, item)
        }

        if (action == ACTION_COPY) {
            finishWithToast(deleted = false)
            return
        }

        val sender: IntentSender? = ScreenshotActions.deleteOrRequest(this, item)
        if (sender == null) {
            finishWithToast(deleted = true)
        } else {
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    private fun finishWithToast(deleted: Boolean) {
        val message = when {
            copied && !deleted && intent.action == ACTION_COPY -> R.string.screenshot_toast_copied
            !copied && intent.action == ACTION_COPY -> R.string.screenshot_toast_copy_failed
            copied && deleted -> R.string.screenshot_toast_copied_deleted
            copied -> R.string.screenshot_toast_copied
            deleted -> R.string.screenshot_toast_deleted
            else -> R.string.screenshot_toast_cancelled
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ACTION_DELETE = "com.arslan.customanimator.action.SCREENSHOT_DELETE"
        const val ACTION_COPY = "com.arslan.customanimator.action.SCREENSHOT_COPY"
        const val ACTION_COPY_DELETE = "com.arslan.customanimator.action.SCREENSHOT_COPY_DELETE"
        const val EXTRA_MEDIA_ID = "extra_media_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"

        private const val STATE_HANDLED = "handled"
        private const val STATE_COPIED = "copied"

        fun intent(
            context: android.content.Context,
            action: String,
            mediaId: Long,
            notifId: Int
        ): Intent = Intent(context, ScreenshotActionActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
    }
}
