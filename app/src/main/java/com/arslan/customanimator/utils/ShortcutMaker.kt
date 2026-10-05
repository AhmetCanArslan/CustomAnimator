package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.arslan.customanimator.R
import com.arslan.customanimator.data.ShortcutActivityInfo
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutFileInfo
import com.arslan.customanimator.data.ShortcutHandlerInfo
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.service.ShortcutLaunchActivity

object ShortcutMaker {
    const val EXTRA_SHORTCUT_ID = "com.arslan.customanimator.extra.SHORTCUT_ID"

    private const val ICON_PX = 324
    private const val LEGACY_INSET = 0.19f
    private const val GLYPH_INSET = 0.34f
    private const val IMAGE_MIME_PREFIX = "image/"
    private const val DEFAULT_LINK_SCHEME = "https://"

    fun listActivities(context: Context, packageName: String): List<ShortcutActivityInfo> {
        val packageManager = context.packageManager
        val activities = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
        } catch (e: Exception) {
            null
        }
        return activities.orEmpty()
            .map { activity ->
                ShortcutActivityInfo(
                    name = activity.name,
                    label = activity.loadLabel(packageManager).toString(),
                    exported = activity.exported,
                    permission = activity.permission
                )
            }
            .sortedWith(compareBy({ it.needsShizuku }, { it.name.substringAfterLast('.').lowercase() }))
    }

    fun describeFile(context: Context, uri: Uri): ShortcutFileInfo {
        val name = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return ShortcutFileInfo(
            name = name ?: uri.lastPathSegment.orEmpty(),
            mimeType = context.contentResolver.getType(uri)
        )
    }

    fun describeFolder(context: Context, treeUri: Uri): ShortcutFileInfo {
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val folder = describeFile(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId))
        return folder.copy(mimeType = DocumentsContract.Document.MIME_TYPE_DIR)
    }

    fun folderUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUri(treeUri.authority, DocumentsContract.getTreeDocumentId(treeUri))

    fun listHandlers(context: Context, entry: ShortcutEntry): List<ShortcutHandlerInfo> {
        val packageManager = context.packageManager
        val intent = viewIntent(entry.copy(packageName = null, activityName = null))
        val resolved = try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (e: Exception) {
            emptyList()
        }
        return resolved
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .map { activity ->
                ShortcutHandlerInfo(
                    packageName = activity.packageName,
                    activityName = activity.name,
                    label = activity.loadLabel(packageManager).toString(),
                    icon = activity.loadIcon(packageManager)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun normalizeLink(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.contains("://")) trimmed else DEFAULT_LINK_SCHEME + trimmed
    }

    fun activityIcon(context: Context, packageName: String, activityName: String): Drawable? {
        return try {
            context.packageManager.getActivityIcon(ComponentName(packageName, activityName))
        } catch (e: Exception) {
            null
        }
    }

    fun drawableIcon(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(0, 0, ICON_PX, ICON_PX)
                layer.draw(canvas)
            }
        } else {
            canvas.drawColor(Color.WHITE)
            drawInset(canvas, drawable, LEGACY_INSET)
        }
        return bitmap
    }

    val glyphColors: List<Int> = listOf(
        0xFF5B5FC7.toInt(),
        0xFF8E4EC6.toInt(),
        0xFF3B6EA8.toInt(),
        0xFF2E7D6B.toInt(),
        0xFF37474F.toInt(),
        0xFFC2410C.toInt(),
        0xFFB3261E.toInt(),
        0xFF1F1F1F.toInt()
    )

    fun glyphIcon(context: Context, type: ShortcutType): Bitmap =
        glyphIcon(context, glyphRes(type), glyphColor(type))

    fun glyphIcon(context: Context, drawableRes: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        ContextCompat.getDrawable(context, drawableRes)?.mutate()?.let { glyph ->
            glyph.setTint(Color.WHITE)
            drawInset(canvas, glyph, GLYPH_INSET)
        }
        return bitmap
    }

    fun fileIcon(context: Context, entry: ShortcutEntry, handlerIcon: Drawable?): Bitmap {
        val uri = entry.uri?.let(Uri::parse)
        if (uri != null && entry.mimeType?.startsWith(IMAGE_MIME_PREFIX) == true) {
            thumbnailIcon(context, uri)?.let { return it }
        }
        return handlerIcon?.let(::drawableIcon) ?: glyphIcon(context, entry.type)
    }

    fun pin(context: Context, entry: ShortcutEntry, icon: Bitmap): Boolean {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo(context, entry, icon), null)
        } catch (e: Exception) {
            false
        }
    }

    fun updatePinned(context: Context, entry: ShortcutEntry, icon: Bitmap) {
        try {
            ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo(context, entry, icon)))
        } catch (e: Exception) {
        }
    }

    fun remove(context: Context, id: String) {
        ShortcutStore.delete(context, id)
        try {
            ShortcutManagerCompat.disableShortcuts(context, listOf(id), context.getString(R.string.shortcut_missing))
        } catch (e: Exception) {
        }
    }

    fun launch(context: Context, entry: ShortcutEntry): Boolean {
        if (entry.type == ShortcutType.COMMAND) return runCommand(entry)
        if (entry.viaShizuku) return startThroughShizuku(entry)
        val intent = targetIntent(context, entry) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun failureMessageRes(entry: ShortcutEntry): Int =
        if (entry.needsShizuku) R.string.action_failed else R.string.shortcut_launch_failed

    private fun shortcutInfo(context: Context, entry: ShortcutEntry, icon: Bitmap): ShortcutInfoCompat {
        val shortcutIcon = if (entry.legacyIcon) IconCompat.createWithBitmap(icon) else IconCompat.createWithAdaptiveBitmap(icon)
        return ShortcutInfoCompat.Builder(context, entry.id)
            .setShortLabel(entry.label)
            .setIcon(shortcutIcon)
            .setIntent(launcherIntent(context, entry.id))
            .build()
    }

    private fun launcherIntent(context: Context, id: String): Intent =
        Intent(context, ShortcutLaunchActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_SHORTCUT_ID, id)

    private fun targetIntent(context: Context, entry: ShortcutEntry): Intent? = when (entry.type) {
        ShortcutType.APP -> entry.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
        ShortcutType.ACTIVITY -> component(entry)?.let { Intent().setComponent(it) }
        ShortcutType.FILE, ShortcutType.FOLDER, ShortcutType.LINK -> viewIntent(entry)
        ShortcutType.COMMAND -> null
    }

    private fun viewIntent(entry: ShortcutEntry): Intent {
        val uri = Uri.parse(entry.uri.orEmpty())
        val intent = Intent(Intent.ACTION_VIEW)
        when (entry.type) {
            ShortcutType.FILE -> intent.setDataAndType(uri, entry.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ShortcutType.FOLDER -> intent.setDataAndType(uri, entry.mimeType)
            else -> intent.setData(uri)
        }
        return intent.setComponent(component(entry))
    }

    private fun component(entry: ShortcutEntry): ComponentName? {
        val packageName = entry.packageName ?: return null
        val activityName = entry.activityName ?: return null
        return ComponentName(packageName, activityName)
    }

    private fun runCommand(entry: ShortcutEntry): Boolean {
        val command = entry.command?.takeIf { it.isNotBlank() } ?: return false
        return ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", command)).isSuccess
    }

    private fun startThroughShizuku(entry: ShortcutEntry): Boolean {
        val target = component(entry) ?: return false
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("am", "start", "-n", target.flattenToString())
        )
        return result.isSuccess && !result.output.contains("Error") && !result.output.contains("Exception")
    }

    private fun glyphRes(type: ShortcutType): Int = when (type) {
        ShortcutType.APP -> R.drawable.ic_tile_apps
        ShortcutType.ACTIVITY -> R.drawable.ic_tile_launch
        ShortcutType.FILE -> R.drawable.ic_tile_folder_open
        ShortcutType.LINK -> R.drawable.ic_tile_public
        ShortcutType.COMMAND -> R.drawable.ic_tile_terminal
        ShortcutType.FOLDER -> R.drawable.ic_tile_folder
    }

    private fun glyphColor(type: ShortcutType): Int = glyphColors[type.ordinal]

    private fun drawInset(canvas: Canvas, drawable: Drawable, fraction: Float) {
        val inset = (ICON_PX * fraction).toInt()
        drawable.setBounds(inset, inset, ICON_PX - inset, ICON_PX - inset)
        drawable.draw(canvas)
    }

    private fun thumbnailIcon(context: Context, uri: Uri): Bitmap? {
        val source = decodeSampled(context, uri) ?: return null
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        canvas.rotate(exifRotation(context, uri), ICON_PX / 2f, ICON_PX / 2f)
        canvas.drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            Rect(0, 0, ICON_PX, ICON_PX),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return bitmap
    }

    private fun decodeSampled(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(minOf(bounds.outWidth, bounds.outHeight))
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            null
        }
    }

    private fun sampleSize(shortestSide: Int): Int {
        var sample = 1
        while (shortestSide / (sample * 2) >= ICON_PX) sample *= 2
        return sample
    }

    private fun exifRotation(context: Context, uri: Uri): Float {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (e: Exception) {
            null
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
}
