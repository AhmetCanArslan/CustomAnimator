package com.riyadmondol2006.customanimator.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.data.SharedItem
import com.riyadmondol2006.customanimator.screenshot.ScreenshotActions
import com.riyadmondol2006.customanimator.screenshot.ScreenshotItem
import com.riyadmondol2006.customanimator.ui.theme.CustomAnimatorTheme
import com.riyadmondol2006.customanimator.utils.LinkCleaner
import com.riyadmondol2006.customanimator.utils.LinkCleanerRules
import com.riyadmondol2006.customanimator.utils.SaveToDeviceManager
import com.riyadmondol2006.customanimator.utils.skipTransitions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareActionsActivity : ComponentActivity() {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isSaving = false
    private var sheet by mutableStateOf<ShareSheetState?>(null)
    private val fileRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onPicked(result) { target, items -> SaveToDeviceManager.saveTo(this, target, items.first()) }
    }
    private val folderRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onPicked(result) { target, items -> SaveToDeviceManager.saveInto(this, target, items) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        skipTransitions()
        isSaving = savedInstanceState?.getBoolean(KEY_SAVING) == true
        if (isSaving) return

        val items = SaveToDeviceManager.sharedItems(intent)
        when {
            items.isEmpty() -> finishWith(false)
            items.size > 1 -> saveToDevice(items)
            else -> showSheet(items.first())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SAVING, isSaving)
    }

    override fun finish() {
        super.finish()
        skipTransitions()
    }

    private fun onPicked(result: ActivityResult, saveTo: (Uri, List<SharedItem>) -> Boolean) {
        val target = result.data?.data?.takeIf { result.resultCode == RESULT_OK }
        if (target == null) {
            finish()
            return
        }
        val items = SaveToDeviceManager.sharedItems(intent)
        save { saveTo(target, items) }
    }

    private fun showSheet(item: SharedItem) {
        sheet = ShareSheetState(item = item, hasLink = LinkCleaner.hasLink(item.text.orEmpty()))
        setContent {
            CustomAnimatorTheme {
                sheet?.let { state ->
                    ShareActionsSheet(
                        state = state,
                        onDescribe = { SaveToDeviceManager.describe(this, item) },
                        onCopy = { copyToClipboard(item) },
                        onSave = { saveToDevice(listOf(item)) },
                        onCleanLink = { cleanLink(item.text.orEmpty()) },
                        onCopyCleaned = ::copyCleaned,
                        onShareCleaned = ::shareCleaned,
                        onDismiss = ::finish
                    )
                }
            }
        }
    }

    private fun cleanLink(text: String) {
        sheet = sheet?.copy(isCleaning = true)
        lifecycleScope.launch {
            val providers = withContext(Dispatchers.IO) { loadRules() }
            if (providers.isEmpty()) {
                Toast.makeText(applicationContext, R.string.link_cleaner_rules_failed, Toast.LENGTH_SHORT).show()
                sheet = sheet?.copy(isCleaning = false)
            } else {
                sheet = sheet?.copy(isCleaning = false, cleaned = LinkCleaner.clean(text, providers))
            }
        }
    }

    private fun loadRules(): List<LinkCleaner.Provider> {
        val hadRules = LinkCleanerRules.isAvailable(applicationContext)
        if (!hadRules) LinkCleanerRules.download(applicationContext)
        val providers = LinkCleanerRules.load(applicationContext)
        if (hadRules) backgroundScope.launch { LinkCleanerRules.download(applicationContext) }
        return providers
    }

    private fun copyCleaned(text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, text))
        Toast.makeText(applicationContext, R.string.link_cleaner_copied, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun shareCleaned(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, null)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this, ShareActionsActivity::class.java)))
        startActivity(chooser)
        finish()
    }

    private fun copyToClipboard(item: SharedItem) {
        sheet = null
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
            ScreenshotActions.copyToClipboard(this, ScreenshotItem(stream, name, 0))
        }
    }

    private fun saveToDevice(items: List<SharedItem>) {
        sheet = null
        val folder = SaveToDeviceManager.getDefaultFolder(this)
        when {
            folder != null -> save { SaveToDeviceManager.saveInto(this, folder, items) }
            items.size == 1 -> askForFile(items.first())
            else -> askForFolder()
        }
    }

    private fun askForFile(item: SharedItem) {
        val file = SaveToDeviceManager.describe(this, item)
        isSaving = true
        fileRequest.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(file.mimeType)
                .putExtra(Intent.EXTRA_TITLE, file.name)
        )
    }

    private fun askForFolder() {
        isSaving = true
        folderRequest.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
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
    }
}
