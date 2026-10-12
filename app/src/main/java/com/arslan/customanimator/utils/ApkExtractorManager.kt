package com.arslan.customanimator.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.arslan.customanimator.data.ApkExtractorApp
import com.arslan.customanimator.data.ApkExtractorFormat
import com.arslan.customanimator.data.InstalledAppInfo
import java.io.File
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ApkExtractorManager {
    private const val TAG = "ApkExtractorManager"
    private const val APK_EXTENSION = ".apk"
    private const val BUNDLE_EXTENSION = ".apks"
    private const val BUFFER_SIZE = 64 * 1024
    private val UNSAFE_FILE_CHARACTERS = Regex("[^A-Za-z0-9._-]")

    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val BUNDLE_MIME = "application/octet-stream"

    const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

    private class Described(val source: InstalledAppInfo, val details: ApkExtractorApp)

    private val described = ConcurrentHashMap<String, Described>()

    fun cachedDetails(app: InstalledAppInfo): ApkExtractorApp? =
        described[app.packageName]?.takeIf { it.source === app }?.details

    fun formats(app: ApkExtractorApp): List<ApkExtractorFormat> =
        if (app.isSplit) ApkExtractorFormat.entries else listOf(ApkExtractorFormat.BASE)

    fun baseName(app: ApkExtractorApp): String {
        val version = app.versionName.replace(UNSAFE_FILE_CHARACTERS, "_")
        val suffix = if (version.isEmpty()) "" else "_$version"
        return app.packageName + suffix
    }

    fun fileName(app: ApkExtractorApp, format: ApkExtractorFormat): String =
        baseName(app) + if (format == ApkExtractorFormat.BUNDLE) BUNDLE_EXTENSION else APK_EXTENSION

    fun needsStoragePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED

    fun extract(context: Context, app: ApkExtractorApp, format: ApkExtractorFormat): String? {
        return try {
            val files = InstalledAppsProvider.sourceApks(context, app.packageName)
            val folder = exportFolder(app, format)
            if (format == ApkExtractorFormat.SEPARATE) {
                files.forEach { file ->
                    save(context, folder, file.name, APK_MIME) { writeTo(listOf(file), it, ApkExtractorFormat.BASE) }
                }
            } else {
                val mime = if (format == ApkExtractorFormat.BUNDLE) BUNDLE_MIME else APK_MIME
                save(context, folder, fileName(app, format), mime) { writeTo(files, it, format) }
            }
            folder
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract ${app.packageName}", e)
            null
        }
    }

    internal fun exportFolder(app: ApkExtractorApp, format: ApkExtractorFormat): String {
        val root = Environment.DIRECTORY_DOWNLOADS + File.separator + InstalledAppsProvider.EXPORT_DIR
        return if (format == ApkExtractorFormat.SEPARATE) root + File.separator + baseName(app) else root
    }

    internal fun writeTo(files: List<File>, output: OutputStream, format: ApkExtractorFormat) {
        if (format != ApkExtractorFormat.BUNDLE) {
            files.first().inputStream().use { it.copyTo(output, BUFFER_SIZE) }
            return
        }
        ZipOutputStream(output).use { zip ->
            zip.setLevel(0)
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
                zip.closeEntry()
            }
        }
    }

    private fun save(context: Context, folder: String, name: String, mime: String, write: (OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(context, folder, name, mime, write)
        } else {
            val directory = File(Environment.getExternalStorageDirectory(), folder)
            directory.mkdirs()
            File(directory, name).outputStream().use(write)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(
        context: Context,
        folder: String,
        name: String,
        mime: String,
        write: (OutputStream) -> Unit
    ) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, folder)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val target = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(target)).use(write)
            resolver.update(target, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            throw e
        }
    }

    fun describe(context: Context, app: InstalledAppInfo): ApkExtractorApp? {
        cachedDetails(app)?.let { return it }
        return try {
            val files = InstalledAppsProvider.sourceApks(context, app.packageName)
            ApkExtractorApp(
                packageName = app.packageName,
                label = app.label,
                versionName = context.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty(),
                sizeBytes = files.sumOf { it.length() },
                isSplit = files.size > 1
            ).also { described[app.packageName] = Described(app, it) }
        } catch (e: Exception) {
            null
        }
    }
}
