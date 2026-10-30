package com.riyadmondol2006.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.IntentCompat
import com.riyadmondol2006.customanimator.data.SharedItem
import com.riyadmondol2006.customanimator.data.ShortcutFileInfo
import java.io.InputStream

object SaveToDeviceManager {
    private const val TEXT_MIME = "text/plain"
    private const val TEXT_FILE_NAME = "Shared text.txt"
    private const val UNKNOWN_MIME = "application/octet-stream"
    private val defaultFolder = PersistedFolder("save_to_device_prefs", "default_folder")

    fun getDefaultFolder(context: Context): Uri? = defaultFolder.get(context)

    fun setDefaultFolder(context: Context, folder: Uri?) = defaultFolder.set(context, folder)

    fun folderLabel(folder: Uri): String {
        val documentId = DocumentsContract.getTreeDocumentId(folder)
        return documentId.substringAfter(':').ifEmpty { documentId }
    }

    fun sharedItems(intent: Intent): List<SharedItem> {
        val streams = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        }
        val items = streams
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            .map { SharedItem(stream = it) }
        if (streams.isNotEmpty()) return items
        return listOfNotNull(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { SharedItem(text = it.toString()) })
    }

    fun describe(context: Context, item: SharedItem): ShortcutFileInfo {
        val stream = item.stream ?: return ShortcutFileInfo(TEXT_FILE_NAME, TEXT_MIME)
        val file = ShortcutMaker.describeFile(context, stream)
        return file.copy(mimeType = file.mimeType ?: UNKNOWN_MIME)
    }

    fun saveTo(context: Context, target: Uri, item: SharedItem): Boolean {
        return try {
            val copied = context.contentResolver.openOutputStream(target)?.use { output ->
                open(context, item)?.use { input -> input.copyTo(output) }
            }
            copied != null
        } catch (e: Exception) {
            false
        }
    }

    fun saveInto(context: Context, folder: Uri, items: List<SharedItem>): Boolean {
        val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
        return items.count { item ->
            val file = describe(context, item)
            val target = try {
                DocumentsContract.createDocument(context.contentResolver, parent, file.mimeType.orEmpty(), file.name)
            } catch (e: Exception) {
                null
            }
            target != null && saveTo(context, target, item)
        } == items.size
    }

    private fun open(context: Context, item: SharedItem): InputStream? {
        val stream = item.stream ?: return item.text?.byteInputStream()
        return context.contentResolver.openInputStream(stream)
    }
}
