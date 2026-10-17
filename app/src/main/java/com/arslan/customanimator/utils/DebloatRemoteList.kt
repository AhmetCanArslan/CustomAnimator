package com.arslan.customanimator.utils

import android.content.Context
import android.util.Log
import com.arslan.customanimator.data.DebloatGroup
import com.arslan.customanimator.data.DebloatRisk
import org.json.JSONObject
import java.io.File

object DebloatRemoteList {

    private const val TAG = "DebloatList"
    private const val SOURCE_URL =
        "https://raw.githubusercontent.com/Universal-Debloater-Alliance/" +
            "universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
    private const val FILE_NAME = "uad_lists.json"
    private const val PREFS_NAME = "debloater_list"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_COUNT = "entry_count"
    private const val KEY_ETAG = "etag"
    private const val MAX_BYTES = 16L * 1024 * 1024

    data class Entry(
        val risk: DebloatRisk,
        val group: DebloatGroup,
        val description: String?
    )

    enum class DownloadResult { UPDATED, UNCHANGED, FAILED }

    private class Loaded(val stamp: Long, val packages: Set<String>, val entries: Map<String, Entry>)

    @Volatile
    private var lastLoaded: Loaded? = null

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAvailable(context: Context): Boolean = file(context).exists()

    fun updatedAtMs(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    fun entryCount(context: Context): Int = prefs(context).getInt(KEY_COUNT, 0)

    fun clear(context: Context) {
        file(context).delete()
        prefs(context).edit().clear().apply()
        lastLoaded = null
    }

    fun download(context: Context): DownloadResult {
        val knownEtag = prefs(context).getString(KEY_ETAG, null).takeIf { file(context).exists() }
        val response = ConditionalDownload.fetch(SOURCE_URL, knownEtag, MAX_BYTES, TAG) ?: return DownloadResult.FAILED
        val body = response.body
        if (body == null) {
            prefs(context).edit().putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
            return DownloadResult.UNCHANGED
        }
        val parsed = try {
            JSONObject(body)
        } catch (e: Exception) {
            Log.e(TAG, "Downloaded list is not valid JSON", e)
            return DownloadResult.FAILED
        }
        if (parsed.length() == 0) return DownloadResult.FAILED

        file(context).writeText(body)
        lastLoaded = null
        prefs(context).edit()
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .putInt(KEY_COUNT, parsed.length())
            .putString(KEY_ETAG, response.etag)
            .apply()
        return DownloadResult.UPDATED
    }

    fun load(context: Context, packages: Set<String>): Map<String, Entry> {
        val source = file(context)
        if (!source.exists() || packages.isEmpty()) return emptyMap()
        val stamp = source.lastModified()
        lastLoaded?.takeIf { it.stamp == stamp && it.packages == packages }?.let { return it.entries }
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
        lastLoaded = Loaded(stamp, packages, entries)
        return entries
    }

    private fun riskOf(removal: String?): DebloatRisk? = when (removal) {
        "Recommended" -> DebloatRisk.RECOMMENDED
        "Advanced" -> DebloatRisk.ADVANCED
        "Expert" -> DebloatRisk.EXPERT
        "Unsafe" -> DebloatRisk.UNSAFE
        else -> null
    }

    private fun groupOf(list: String?): DebloatGroup = when (list) {
        "Google" -> DebloatGroup.GOOGLE
        "Aosp" -> DebloatGroup.SYSTEM
        "Carrier" -> DebloatGroup.CARRIER
        "Oem" -> DebloatGroup.OEM
        else -> DebloatGroup.PARTNER
    }
}
