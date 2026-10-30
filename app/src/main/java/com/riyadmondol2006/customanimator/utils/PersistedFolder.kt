package com.riyadmondol2006.customanimator.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class PersistedFolder(private val prefsName: String, private val key: String) {

    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun get(context: Context): Uri? =
        prefs(context).getString(key, null)?.let(Uri::parse)

    fun set(context: Context, folder: Uri?) {
        val resolver = context.contentResolver
        get(context)?.let { previous ->
            try {
                resolver.releasePersistableUriPermission(previous, ACCESS)
            } catch (e: Exception) {
            }
        }
        folder?.let { resolver.takePersistableUriPermission(it, ACCESS) }
        prefs(context).edit().apply {
            if (folder == null) remove(key) else putString(key, folder.toString())
        }.apply()
    }

    private companion object {
        const val ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
