package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.IntentCompat
import com.arslan.customanimator.data.SharedItem
import com.arslan.customanimator.data.ShortcutFileInfo
import java.io.InputStream

object SaveToDeviceManager {
    private const val PREFS_NAME = "save_to_device_prefs"
    private const val KEY_FOLDER = "default_folder"
    private const val TEXT_MIME = "text/plain"
    private const val TEXT_FILE_NAME = "Shared text.txt"
    private const val UNKNOWN_MIME = "application/octet-stream"
    private const val FOLDER_PERMISSIONS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultFolder(context: Context): Uri? =
        prefs(context).getString(KEY_FOLDER, null)?.let(Uri::parse)

    fun setDefaultFolder(context: Context, folder: Uri?) {
        val resolver = context.contentResolver
        getDefaultFolder(context)?.let { previous ->
            try {
                resolver.releasePersistableUriPermission(previous, FOLDER_PERMISSIONS)
            } catch (e: Exception) {
            }
        }
        folder?.let { resolver.takePersistableUriPermission(it, FOLDER_PERMISSIONS) }
        prefs(context).edit().apply {
            if (folder == null) remove(KEY_FOLDER) else putString(KEY_FOLDER, folder.toString())
        }.apply()
    }

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

    private fun open(context: Context, item: SharedItem): InputStream? {
        val stream = item.stream ?: return item.text?.byteInputStream()
        return context.contentResolver.openInputStream(stream)
    }
}
