package com.riyadmondol2006.customanimator.utils

import android.Manifest
import android.content.ContentValues
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.riyadmondol2006.customanimator.data.ApkExtractorApp
import com.riyadmondol2006.customanimator.data.ApkExtractorFormat
import com.riyadmondol2006.customanimator.data.InstalledAppInfo
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

    val DEFAULT_FOLDER: String = Environment.DIRECTORY_DOWNLOADS + File.separator + InstalledAppsProvider.EXPORT_DIR

    private val exportTree = PersistedFolder("apk_extractor_prefs", "export_folder")

    private class Destination(val label: String, val save: (String, String, (OutputStream) -> Unit) -> Unit)

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

    fun exportTree(context: Context): Uri? = exportTree.get(context)

    fun setExportTree(context: Context, folder: Uri?) = exportTree.set(context, folder)

    fun needsStoragePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            exportTree(context) == null &&
            ContextCompat.checkSelfPermission(context, STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED

    fun extract(context: Context, app: ApkExtractorApp, format: ApkExtractorFormat): String? {
        return try {
            val files = InstalledAppsProvider.sourceApks(context, app.packageName)
            val destination = destination(context, app, format)
            if (format == ApkExtractorFormat.SEPARATE) {
                files.forEach { file ->
                    destination.save(file.name, APK_MIME) { writeTo(listOf(file), it, ApkExtractorFormat.BASE) }
                }
            } else {
                val mime = if (format == ApkExtractorFormat.BUNDLE) BUNDLE_MIME else APK_MIME
                destination.save(fileName(app, format), mime) { writeTo(files, it, format) }
            }
            destination.label
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract ${app.packageName}", e)
            null
        }
    }

    private fun destination(context: Context, app: ApkExtractorApp, format: ApkExtractorFormat): Destination {
        val tree = exportTree(context) ?: return exportFolder(app, format).let { folder ->
            Destination(folder) { name, mime, write -> save(context, folder, name, mime, write) }
        }
        val resolver = context.contentResolver
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val label = SaveToDeviceManager.folderLabel(tree)
        if (format != ApkExtractorFormat.SEPARATE) {
            return Destination(label) { name, mime, write -> saveToDocument(resolver, root, name, mime, write) }
        }
        val subfolder = baseName(app)
        val parent = requireNotNull(
            DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, subfolder)
        )
        return Destination(label + File.separator + subfolder) { name, mime, write ->
            saveToDocument(resolver, parent, name, mime, write)
        }
    }

    private fun saveToDocument(
        resolver: ContentResolver,
        parent: Uri,
        name: String,
        mime: String,
        write: (OutputStream) -> Unit
    ) {
        val target = requireNotNull(DocumentsContract.createDocument(resolver, parent, mime, name))
        try {
            requireNotNull(resolver.openOutputStream(target)).use(write)
        } catch (e: Exception) {
            DocumentsContract.deleteDocument(resolver, target)
            throw e
        }
    }

    internal fun exportFolder(app: ApkExtractorApp, format: ApkExtractorFormat): String {
        return if (format == ApkExtractorFormat.SEPARATE) DEFAULT_FOLDER + File.separator + baseName(app) else DEFAULT_FOLDER
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
