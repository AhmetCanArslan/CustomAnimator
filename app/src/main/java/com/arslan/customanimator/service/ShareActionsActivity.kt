package com.arslan.customanimator.service

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.data.SharedItem
import com.arslan.customanimator.screenshot.ScreenshotActions
import com.arslan.customanimator.screenshot.ScreenshotItem
import com.arslan.customanimator.utils.SaveToDeviceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareActionsActivity : Activity() {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isSaving = false
    private var actionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSaving = savedInstanceState?.getBoolean(KEY_SAVING) == true
        if (isSaving) return

        val items = SaveToDeviceManager.sharedItems(intent)
        when {
            items.isEmpty() -> finishWith(false)
            items.size > 1 -> saveToDevice(items)
            else -> askForAction(items)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SAVING, isSaving)
    }

    override fun onDestroy() {
        actionDialog?.dismiss()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val target = data?.data?.takeIf { resultCode == RESULT_OK }
        if (target == null) {
            finish()
            return
        }
        val items = SaveToDeviceManager.sharedItems(intent)
        save { saveAfterAsking(requestCode, target, items) }
    }

    private fun askForAction(items: List<SharedItem>) {
        val actions = arrayOf(getString(R.string.copy_to_clipboard), getString(R.string.save_to_device))
        actionDialog = AlertDialog.Builder(this, dialogTheme())
            .setTitle(R.string.share_actions)
            .setItems(actions) { _, index ->
                if (index == 0) copyToClipboard(items.first()) else saveToDevice(items)
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun dialogTheme(): Int {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            android.R.style.Theme_DeviceDefault_Dialog_Alert
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
        }
    }

    private fun copyToClipboard(item: SharedItem) {
        backgroundScope.launch {
            copy(item)
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun copy(item: SharedItem) {
        val stream = item.stream
        if (stream == null) {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, item.text))
        } else {
            val name = File(SaveToDeviceManager.describe(this, item).name).name
            ScreenshotActions.copyToClipboard(this, ScreenshotItem(0, stream, name, 0))
        }
    }

    private fun saveToDevice(items: List<SharedItem>) {
        val folder = SaveToDeviceManager.getDefaultFolder(this)
        when {
            folder != null -> save { SaveToDeviceManager.saveInto(this, folder, items) }
            items.size == 1 -> askForFile(items.first())
            else -> askForFolder()
        }
    }

    private fun saveAfterAsking(requestCode: Int, target: Uri, items: List<SharedItem>): Boolean =
        if (requestCode == REQUEST_FILE) {
            SaveToDeviceManager.saveTo(this, target, items.first())
        } else {
            SaveToDeviceManager.saveInto(this, target, items)
        }

    private fun askForFile(item: SharedItem) {
        val file = SaveToDeviceManager.describe(this, item)
        isSaving = true
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(file.mimeType)
                .putExtra(Intent.EXTRA_TITLE, file.name),
            REQUEST_FILE
        )
    }

    private fun askForFolder() {
        isSaving = true
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_FOLDER)
    }

    private fun save(action: () -> Boolean) {
        backgroundScope.launch {
            val saved = action()
            withContext(Dispatchers.Main) { finishWith(saved) }
        }
    }

    private fun finishWith(saved: Boolean) {
        Toast.makeText(
            applicationContext,
            getString(if (saved) R.string.save_to_device_done else R.string.save_to_device_failed),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private companion object {
        const val KEY_SAVING = "saving"
        const val REQUEST_FILE = 1
        const val REQUEST_FOLDER = 2
    }
}
