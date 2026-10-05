package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object SaveToDeviceManager {
    private const val PREFS_NAME = "save_to_device_prefs"
    private const val KEY_FOLDER = "default_folder"
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
}
