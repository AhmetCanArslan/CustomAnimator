package com.arslan.customanimator.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports/imports every preference file the app owns as a single JSON document, so a user can
 * recover their presets and app lists after a reinstall or a bad experiment.
 *
 * Only app-local state is covered — system settings (animation scales, density, rotation) are not
 * part of a backup; those are reverted through [SystemResetManager].
 */
object BackupManager {

    private const val FORMAT_VERSION = 1

    // shizuku_prefs is deliberately excluded: it tracks per-install permission state.
    private val PREFS_FILES = listOf(
        "custom_animator_prefs",
        "animator_presets",
        "width_presets",
        "terminal_presets",
        "auto_force_stop",
        "auto_permission_disabler",
        "auto_permission_disabler_state",
        "close_apps_exclusions"
    )

    fun exportToJson(context: Context): String {
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)
        root.put("app_version", BuildConfigVersion.name(context))
        root.put("created_at", System.currentTimeMillis())

        val prefsJson = JSONObject()
        PREFS_FILES.forEach { name ->
            val entries = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                val entry = when (value) {
                    is String -> JSONObject().put("type", "string").put("value", value)
                    is Boolean -> JSONObject().put("type", "boolean").put("value", value)
                    is Int -> JSONObject().put("type", "int").put("value", value)
                    is Long -> JSONObject().put("type", "long").put("value", value)
                    is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
                    is Set<*> -> JSONObject()
                        .put("type", "string_set")
                        .put("value", JSONArray().apply { value.forEach { put(it.toString()) } })
                    else -> null
                }
                if (entry != null) entries.put(key, entry)
            }
            prefsJson.put(name, entries)
        }
        root.put("preferences", prefsJson)
        return root.toString(2)
    }

    fun writeBackup(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(exportToJson(context).toByteArray())
                out.flush()
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restoreBackup(context: Context, uri: Uri): Boolean {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return false
            importFromJson(context, text)
        } catch (e: Exception) {
            false
        }
    }

    fun importFromJson(context: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            if (root.optInt("format", 0) > FORMAT_VERSION) return false
            val prefsJson = root.optJSONObject("preferences") ?: return false

            prefsJson.keys().forEach { prefsName ->
                if (prefsName !in PREFS_FILES) return@forEach
                val entries = prefsJson.optJSONObject(prefsName) ?: return@forEach
                val editor = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                editor.clear()
                entries.keys().forEach { key ->
                    val entry = entries.optJSONObject(key) ?: return@forEach
                    when (entry.optString("type")) {
                        "string" -> editor.putString(key, entry.optString("value"))
                        "boolean" -> editor.putBoolean(key, entry.optBoolean("value"))
                        "int" -> editor.putInt(key, entry.optInt("value"))
                        "long" -> editor.putLong(key, entry.optLong("value"))
                        "float" -> editor.putFloat(key, entry.optDouble("value").toFloat())
                        "string_set" -> {
                            val array = entry.optJSONArray("value") ?: JSONArray()
                            val set = mutableSetOf<String>()
                            for (i in 0 until array.length()) set.add(array.getString(i))
                            editor.putStringSet(key, set)
                        }
                    }
                }
                editor.commit()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun suggestedFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "customanimator-backup-$stamp.json"
    }

    private object BuildConfigVersion {
        fun name(context: Context): String {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
