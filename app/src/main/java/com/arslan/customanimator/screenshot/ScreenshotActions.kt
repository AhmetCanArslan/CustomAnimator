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
import android.os.Process
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.arslan.customanimator.utils.ShizukuHelper
import java.io.File

data class ScreenshotItem(val id: Long, val uri: Uri, val name: String, val dateAdded: Long)

enum class ScreenshotTrashResult { TRASHED, NO_SHIZUKU, FAILED }

object ScreenshotActions {

    private const val PREVIEW_MAX_EDGE = 1024
    private const val PER_USER_RANGE = 100000

    fun copyToClipboard(context: Context, item: ScreenshotItem): Boolean {
        return try {
            val dir = File(context.cacheDir, "clipboard").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
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

    fun targetOf(context: Context, mediaId: Long): ScreenshotItem? =
        if (mediaId >= 0) itemFor(context, mediaId) else latestScreenshot(context)

    fun itemFor(context: Context, id: Long): ScreenshotItem? {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )
        val (selection, args) = screenshotSelection()
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                ),
                selection, args, null
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
        val (selection, args) = screenshotSelection()
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

    private fun screenshotSelection(): Pair<String, Array<String>> {
        val names = arrayOf("%Screenshots%", "%creenshot%")
        val byName = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return "($byName) AND ${MediaStore.MediaColumns.IS_TRASHED} = 0" to names
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return byName to names
        }
        @Suppress("DEPRECATION")
        return "${MediaStore.Images.Media.DATA} LIKE ?" to arrayOf("%Screenshot%")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun moveToTrash(context: Context, item: ScreenshotItem): ScreenshotTrashResult {
        if (!ShizukuHelper.hasShizukuPermission()) return ScreenshotTrashResult.NO_SHIZUKU
        val target = itemFor(context, item.id) ?: return ScreenshotTrashResult.FAILED
        ShizukuHelper.executeShellCommand(trashCommand(target.id))
        return if (itemFor(context, target.id) == null) {
            ScreenshotTrashResult.TRASHED
        } else {
            ScreenshotTrashResult.FAILED
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    internal fun trashCommand(id: Long): Array<String> {
        require(id > 0)
        return arrayOf(
            "content", "update",
            "--user", (Process.myUid() / PER_USER_RANGE).toString(),
            "--uri", ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString(),
            "--bind", "${MediaStore.MediaColumns.IS_TRASHED}:i:1"
        )
    }

    fun deleteOrRequest(context: Context, item: ScreenshotItem): IntentSender? {
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
