package com.arslan.customanimator.screenshot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object ScreenshotFolder {

    private const val ACCESS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val DEFAULT_DOCUMENT_ID = "primary:Pictures/Screenshots"

    val initialUri: Uri =
        DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, DEFAULT_DOCUMENT_ID)

    fun tree(context: Context): Uri? {
        val folder = ScreenshotPrefs(context).folderUri?.let(Uri::parse) ?: return null
        val granted = context.contentResolver.persistedUriPermissions.any {
            it.uri == folder && it.isReadPermission && it.isWritePermission
        }
        return folder.takeIf { granted }
    }

    fun hasAccess(context: Context): Boolean = tree(context) != null

    fun choose(context: Context, folder: Uri) {
        val resolver = context.contentResolver
        val prefs = ScreenshotPrefs(context)
        prefs.folderUri?.let { previous ->
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(previous), ACCESS) }
        }
        resolver.takePersistableUriPermission(folder, ACCESS)
        prefs.folderUri = folder.toString()
    }

    fun children(tree: Uri): Uri =
        DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
}
