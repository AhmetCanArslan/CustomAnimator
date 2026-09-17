package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONObject

class DebloatStore(context: Context) {

    data class Record(val packageName: String, val label: String, val removed: Boolean, val atMs: Long)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readMap(): JSONObject {
        return try {
            JSONObject(prefs.getString(KEY_ENTRIES, "{}") ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeMap(map: JSONObject) {
        prefs.edit().putString(KEY_ENTRIES, map.toString()).apply()
    }

    @Synchronized
    fun record(packageName: String, label: String, removed: Boolean) {
        val map = readMap()
        val entry = JSONObject()
            .put(FIELD_LABEL, label)
            .put(FIELD_REMOVED, removed)
            .put(FIELD_AT, System.currentTimeMillis())
        map.put(packageName, entry)
        writeMap(map)
    }

    @Synchronized
    fun forget(packageName: String) {
        val map = readMap()
        map.remove(packageName)
        writeMap(map)
    }

    @Synchronized
    fun labelFor(packageName: String): String? {
        return readMap().optJSONObject(packageName)?.optString(FIELD_LABEL)?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    fun all(): List<Record> {
        val map = readMap()
        val records = mutableListOf<Record>()
        map.keys().forEach { pkg ->
            val entry = map.optJSONObject(pkg) ?: return@forEach
            records.add(
                Record(
                    packageName = pkg,
                    label = entry.optString(FIELD_LABEL).takeIf { it.isNotBlank() } ?: pkg,
                    removed = entry.optBoolean(FIELD_REMOVED, true),
                    atMs = entry.optLong(FIELD_AT, 0L)
                )
            )
        }
        return records
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private companion object {
        const val PREFS_NAME = "debloater_state"
        const val KEY_ENTRIES = "entries"
        const val FIELD_LABEL = "label"
        const val FIELD_REMOVED = "removed"
        const val FIELD_AT = "at"
    }
}
