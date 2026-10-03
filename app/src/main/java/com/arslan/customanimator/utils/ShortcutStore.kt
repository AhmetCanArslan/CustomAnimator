package com.arslan.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.arslan.customanimator.data.ShortcutEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ShortcutStore {
    private const val PREFS_NAME = "shortcut_maker_prefs"
    private const val KEY_SHORTCUTS = "shortcuts"
    private const val KEY_HANDLER_PREFIX = "default_handler_"
    private const val ICON_DIR = "shortcut_icons"
    private const val UNKNOWN_MIME = "*/*"

    private val gson = Gson()
    private val listType = object : TypeToken<List<ShortcutEntry>>() {}.type

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<ShortcutEntry> {
        val json = prefs(context).getString(KEY_SHORTCUTS, null) ?: return emptyList()
        return try {
            gson.fromJson<List<ShortcutEntry>>(json, listType).orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun get(context: Context, id: String): ShortcutEntry? = getAll(context).firstOrNull { it.id == id }

    @Synchronized
    fun save(context: Context, entry: ShortcutEntry, icon: Bitmap) {
        writeIcon(context, entry.id, icon)
        update(context, entry)
    }

    @Synchronized
    fun update(context: Context, entry: ShortcutEntry) {
        val current = getAll(context)
        val updated = if (current.any { it.id == entry.id }) {
            current.map { if (it.id == entry.id) entry else it }
        } else {
            current + entry
        }
        persist(context, updated)
    }

    @Synchronized
    fun delete(context: Context, id: String) {
        persist(context, getAll(context).filterNot { it.id == id })
        iconFile(context, id).delete()
    }

    fun loadIcon(context: Context, id: String): Bitmap? {
        val file = iconFile(context, id)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.path)
        } catch (e: Exception) {
            null
        }
    }

    fun getDefaultHandler(context: Context, mimeType: String?): ComponentName? {
        val flattened = prefs(context).getString(handlerKey(mimeType), null) ?: return null
        return ComponentName.unflattenFromString(flattened)
    }

    fun setDefaultHandler(context: Context, mimeType: String?, handler: ComponentName?) {
        prefs(context).edit().apply {
            if (handler == null) remove(handlerKey(mimeType)) else putString(handlerKey(mimeType), handler.flattenToString())
        }.apply()
    }

    private fun handlerKey(mimeType: String?): String = KEY_HANDLER_PREFIX + (mimeType ?: UNKNOWN_MIME)

    private fun persist(context: Context, entries: List<ShortcutEntry>) {
        prefs(context).edit().putString(KEY_SHORTCUTS, gson.toJson(entries)).apply()
    }

    private fun iconFile(context: Context, id: String): File =
        File(File(context.filesDir, ICON_DIR).apply { mkdirs() }, "$id.png")

    private fun writeIcon(context: Context, id: String, icon: Bitmap) {
        iconFile(context, id).outputStream().use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
