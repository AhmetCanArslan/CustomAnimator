package com.arslan.customanimator.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DebloatRemoteList {

    private const val TAG = "DebloatList"
    private const val SOURCE_URL =
        "https://raw.githubusercontent.com/Universal-Debloater-Alliance/" +
            "universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
    private const val FILE_NAME = "uad_lists.json"
    private const val PREFS_NAME = "debloater_list"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_COUNT = "entry_count"
    private const val TIMEOUT_MS = 30_000
    private const val MAX_BYTES = 16L * 1024 * 1024

    data class Entry(
        val risk: DebloatCatalog.Risk,
        val group: DebloatCatalog.Group,
        val description: String?
    )

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAvailable(context: Context): Boolean = file(context).exists()

    fun updatedAtMs(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    fun entryCount(context: Context): Int = prefs(context).getInt(KEY_COUNT, 0)

    fun clear(context: Context) {
        file(context).delete()
        prefs(context).edit().clear().apply()
    }

    fun download(context: Context): Int {
        val body = fetch() ?: return 0
        val parsed = try {
            JSONObject(body)
        } catch (e: Exception) {
            Log.e(TAG, "Downloaded list is not valid JSON", e)
            return 0
        }
        if (parsed.length() == 0) return 0

        file(context).writeText(body)
        prefs(context).edit()
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .putInt(KEY_COUNT, parsed.length())
            .apply()
        return parsed.length()
    }

    fun load(context: Context, packages: Set<String>): Map<String, Entry> {
        val source = file(context)
        if (!source.exists() || packages.isEmpty()) return emptyMap()
        val root = try {
            JSONObject(source.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Stored list is unreadable", e)
            return emptyMap()
        }

        val entries = HashMap<String, Entry>()
        packages.forEach { packageName ->
            val item = root.optJSONObject(packageName) ?: return@forEach
            val risk = riskOf(item.optString("removal")) ?: return@forEach
            entries[packageName] = Entry(
                risk = risk,
                group = groupOf(item.optString("list")),
                description = item.optString("description").takeIf { it.isNotBlank() }
            )
        }
        return entries
    }

    private fun fetch(): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(SOURCE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Unexpected response: ${connection.responseCode}")
                return null
            }
            connection.inputStream.bufferedReader().use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    builder.append(buffer, 0, read)
                    if (builder.length > MAX_BYTES) {
                        Log.w(TAG, "List exceeds the size limit")
                        return null
                    }
                }
                builder.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download the package list", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun riskOf(removal: String?): DebloatCatalog.Risk? = when (removal) {
        "Recommended" -> DebloatCatalog.Risk.RECOMMENDED
        "Advanced" -> DebloatCatalog.Risk.ADVANCED
        "Expert" -> DebloatCatalog.Risk.EXPERT
        "Unsafe" -> DebloatCatalog.Risk.UNSAFE
        else -> null
    }

    private fun groupOf(list: String?): DebloatCatalog.Group = when (list) {
        "Google" -> DebloatCatalog.Group.GOOGLE
        "Aosp" -> DebloatCatalog.Group.SYSTEM
        "Carrier" -> DebloatCatalog.Group.CARRIER
        "Oem" -> DebloatCatalog.Group.OEM
        else -> DebloatCatalog.Group.PARTNER
    }
}
