package com.arslan.customanimator.screenshot

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

data class ScreenshotItem(val id: Long, val uri: Uri, val name: String, val dateAdded: Long)

object ScreenshotActions {

    private const val PREVIEW_MAX_EDGE = 1024

    fun copyToClipboard(context: Context, item: ScreenshotItem): Boolean {
        return try {
            val dir = File(context.cacheDir, "clipboard").apply { mkdirs() }
            val copy = File(dir, item.name.ifEmpty { "screenshot_${item.id}.png" })
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
        val newest = query(context, limitToNewest = true) ?: return null
        val nowSec = System.currentTimeMillis() / 1000
        return if (nowSec - newest.dateAdded <= withinSeconds) newest else null
    }

    fun newestScreenshot(context: Context): ScreenshotItem? = query(context, limitToNewest = true)

    fun itemFor(context: Context, id: Long): ScreenshotItem? {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                ),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    ScreenshotItem(
                        id = c.getLong(0),
                        uri = uri,
                        name = c.getString(1) ?: "",
                        dateAdded = c.getLong(2)
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun query(context: Context, limitToNewest: Boolean): ScreenshotItem? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR " +
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            args = arrayOf("%Screenshots%", "%creenshot%")
        } else {
            @Suppress("DEPRECATION")
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            args = arrayOf("%Screenshot%")
        }
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args, sort
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                val id = c.getLong(0)
                ScreenshotItem(
                    id = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ),
                    name = c.getString(1) ?: "",
                    dateAdded = c.getLong(2)
                )
            }
        }.getOrNull().takeIf { limitToNewest || it != null }
    }

    /**
     * Deletes without any user prompt when the app owns the entry or is running
     * on a pre-scoped-storage system. Returns the [IntentSender] the caller must
     * launch when the system requires the user to confirm.
     */
    fun deleteOrRequest(context: Context, item: ScreenshotItem): IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(item.uri)
            ).intentSender
        }
        return try {
            context.contentResolver.delete(item.uri, null, null)
            null
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                e is RecoverableSecurityException
            ) {
                e.userAction.actionIntent.intentSender
            } else {
                null
            }
        }
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

    fun isDeleteConfirmed(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK
}
