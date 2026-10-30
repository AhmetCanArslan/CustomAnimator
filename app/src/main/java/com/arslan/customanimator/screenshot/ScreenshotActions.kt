package com.arslan.customanimator.screenshot

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File

data class ScreenshotItem(val uri: Uri, val name: String, val modifiedMillis: Long)

object ScreenshotActions {

    private const val PREVIEW_MAX_EDGE = 1024
    private const val IMAGE_MIME_PREFIX = "image/"
    private val PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    fun copyToClipboard(context: Context, item: ScreenshotItem): Boolean {
        return try {
            val dir = File(context.cacheDir, "clipboard").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val copy = File(dir, item.name.ifEmpty { "screenshot_${item.modifiedMillis}.png" })
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                copy.outputStream().use { output -> input.copyTo(output) }
            } ?: return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                copy
            )
            val clip = ClipData.newUri(context.contentResolver, "Screenshot", uri).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(clip)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun latestScreenshot(context: Context, withinSeconds: Long = 30): ScreenshotItem? {
        val newest = newestScreenshot(context) ?: return null
        val ageMillis = System.currentTimeMillis() - newest.modifiedMillis
        return newest.takeIf { ageMillis <= withinSeconds * 1000 }
    }

    fun newestScreenshot(context: Context): ScreenshotItem? {
        val tree = ScreenshotFolder.tree(context) ?: return null
        return runCatching {
            context.contentResolver.query(ScreenshotFolder.children(tree), PROJECTION, null, null, null)
                ?.use { cursor -> newestIn(cursor, tree) }
        }.getOrNull()
    }

    fun itemFor(context: Context, uri: Uri): ScreenshotItem? = runCatching {
        context.contentResolver.query(uri, PROJECTION, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) itemAt(cursor, uri) else null
        }
    }.getOrNull()

    fun delete(context: Context, item: ScreenshotItem): Boolean = runCatching {
        DocumentsContract.deleteDocument(context.contentResolver, item.uri)
    }.getOrDefault(false)

    private fun newestIn(cursor: Cursor, tree: Uri): ScreenshotItem? {
        var newest: ScreenshotItem? = null
        while (cursor.moveToNext()) {
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
            val item = itemAt(cursor, uri) ?: continue
            if (newest == null || item.modifiedMillis > newest.modifiedMillis) newest = item
        }
        return newest
    }

    private fun itemAt(cursor: Cursor, uri: Uri): ScreenshotItem? {
        val name = cursor.getString(1).orEmpty()
        val isImage = cursor.getString(3)?.startsWith(IMAGE_MIME_PREFIX) == true
        if (!isImage || name.startsWith(".")) return null
        return ScreenshotItem(uri = uri, name = name, modifiedMillis = cursor.getLong(2))
    }

    fun decodePreview(context: Context, item: ScreenshotItem): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (sample > 0 && longest / sample > PREVIEW_MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
