package com.arslan.customanimator.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class SettingsNamespace(val table: String) {
    SYSTEM("system"),
    SECURE("secure"),
    GLOBAL("global")
}

data class SettingEntry(val key: String, val value: String)

data class BootSetting(val namespace: SettingsNamespace, val key: String, val value: String)

object SettingsDatabaseManager {
    private const val TAG = "SettingsDatabase"
    private const val PREFS_NAME = "settings_database"
    private const val KEY_BOOT_SETTINGS = "boot_settings"
    private const val KEY_SHIZUKU_PENDING = "shizuku_pending"
    private const val FIELD_NAMESPACE = "namespace"
    private const val FIELD_KEY = "key"
    private const val FIELD_VALUE = "value"

    fun list(namespace: SettingsNamespace): List<SettingEntry>? {
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("settings", "list", namespace.table),
            maxLines = Int.MAX_VALUE,
            maxChars = Int.MAX_VALUE
        )
        return if (result.isSuccess) parse(result.output) else null
    }

    fun put(namespace: SettingsNamespace, key: String, value: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("settings", "put", namespace.table, key, value))

    fun delete(namespace: SettingsNamespace, key: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", namespace.table, key))

    fun usesShizukuOnBoot(namespace: SettingsNamespace): Boolean = namespace == SettingsNamespace.SYSTEM

    fun putForBoot(context: Context, namespace: SettingsNamespace, key: String, value: String): Boolean =
        if (usesShizukuOnBoot(namespace)) put(namespace, key, value) else putWithSecureSettings(context, namespace, key, value)

    private fun putWithSecureSettings(context: Context, namespace: SettingsNamespace, key: String, value: String): Boolean {
        val resolver = context.contentResolver
        return try {
            when (namespace) {
                SettingsNamespace.SYSTEM -> Settings.System.putString(resolver, key, value)
                SettingsNamespace.SECURE -> Settings.Secure.putString(resolver, key, value)
                SettingsNamespace.GLOBAL -> Settings.Global.putString(resolver, key, value)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot write ${namespace.table} $key with WRITE_SECURE_SETTINGS", e)
            false
        }
    }

    fun bootSettings(context: Context): List<BootSetting> {
        val array = prefs(context).getJsonArray(KEY_BOOT_SETTINGS)
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(::toBootSetting) }
    }

    fun bootKeys(context: Context, namespace: SettingsNamespace): Set<String> =
        bootSettings(context).filter { it.namespace == namespace }.mapTo(HashSet()) { it.key }

    fun setAppliedOnBoot(context: Context, namespace: SettingsNamespace, key: String, value: String?) {
        val others = bootSettings(context).filterNot { it.namespace == namespace && it.key == key }
        val updated = if (value == null) others else others + BootSetting(namespace, key, value)
        val array = JSONArray()
        updated.forEach { array.put(toJson(it)) }
        prefs(context).edit { putString(KEY_BOOT_SETTINGS, array.toString()) }
    }

    fun reapplyBootSettings(context: Context): Int {
        val (viaShizuku, viaSecureSettings) = bootSettings(context).partition { usesShizukuOnBoot(it.namespace) }
        prefs(context).edit { putBoolean(KEY_SHIZUKU_PENDING, viaShizuku.isNotEmpty()) }
        return viaSecureSettings.count { putWithSecureSettings(context, it.namespace, it.key, it.value) }
    }

    @Synchronized
    fun applyPendingShizukuSettings(context: Context): Int {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_SHIZUKU_PENDING, false) || !ShizukuHelper.hasShizukuPermission()) return 0
        val applied = bootSettings(context)
            .filter { usesShizukuOnBoot(it.namespace) }
            .count { put(it.namespace, it.key, it.value) }
        prefs.edit { putBoolean(KEY_SHIZUKU_PENDING, false) }
        return applied
    }

    fun isValidKey(key: String): Boolean =
        key.isNotEmpty() && key.none { it.isWhitespace() || it == '=' }

    fun parse(output: String): List<SettingEntry> {
        val entries = mutableListOf<SettingEntry>()
        output.trimEnd('\n', '\r').lineSequence().forEach { line ->
            val separator = line.indexOf('=')
            val key = if (separator > 0) line.substring(0, separator) else ""
            if (isValidKey(key)) {
                entries += SettingEntry(key, line.substring(separator + 1))
            } else if (entries.isNotEmpty()) {
                val last = entries.removeAt(entries.lastIndex)
                entries += last.copy(value = last.value + "\n" + line)
            }
        }
        return entries.distinctBy { it.key }.sortedBy { it.key.lowercase() }
    }

    fun filter(entries: List<SettingEntry>, query: String): List<SettingEntry> {
        val needle = query.trim()
        if (needle.isEmpty()) return entries
        return entries.filter { it.key.contains(needle, ignoreCase = true) || it.value.contains(needle, ignoreCase = true) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun toBootSetting(json: JSONObject): BootSetting? {
        val namespace = SettingsNamespace.entries.firstOrNull { it.name == json.optString(FIELD_NAMESPACE) } ?: return null
        val key = json.optString(FIELD_KEY)
        if (!isValidKey(key) || !json.has(FIELD_VALUE)) return null
        return BootSetting(namespace, key, json.optString(FIELD_VALUE))
    }

    private fun toJson(setting: BootSetting): JSONObject =
        JSONObject()
            .put(FIELD_NAMESPACE, setting.namespace.name)
            .put(FIELD_KEY, setting.key)
            .put(FIELD_VALUE, setting.value)
}
