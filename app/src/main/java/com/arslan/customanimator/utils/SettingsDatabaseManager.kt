package com.arslan.customanimator.utils

enum class SettingsNamespace(val table: String) {
    SYSTEM("system"),
    SECURE("secure"),
    GLOBAL("global")
}

data class SettingEntry(val key: String, val value: String)

object SettingsDatabaseManager {

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
}
