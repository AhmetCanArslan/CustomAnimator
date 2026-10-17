package com.arslan.customanimator.utils

import android.content.Context
import android.util.Log
import org.json.JSONException
import java.io.File
import java.io.IOException

object LinkCleanerRules {

    private const val TAG = "LinkCleanerRules"
    private const val SOURCE_URL = "https://rules2.clearurls.xyz/data.minify.json"
    private const val FILE_NAME = "clearurls_rules.json"
    private const val PREFS_NAME = "link_cleaner_rules"
    private const val KEY_ETAG = "etag"
    private const val MAX_BYTES = 4L * 1024 * 1024

    private class Loaded(val stamp: Long, val providers: List<LinkCleaner.Provider>)

    @Volatile
    private var lastLoaded: Loaded? = null

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAvailable(context: Context): Boolean = file(context).exists()

    fun download(context: Context) {
        val knownEtag = prefs(context).getString(KEY_ETAG, null).takeIf { isAvailable(context) }
        val response = ConditionalDownload.fetch(SOURCE_URL, knownEtag, MAX_BYTES, TAG) ?: return
        val body = response.body ?: return
        val providers = parse(body)
        if (providers.isEmpty()) return
        val target = file(context)
        target.writeText(body)
        prefs(context).edit().putString(KEY_ETAG, response.etag).apply()
        lastLoaded = Loaded(target.lastModified(), providers)
    }

    fun load(context: Context): List<LinkCleaner.Provider> {
        val source = file(context)
        if (!source.exists()) return emptyList()
        val stamp = source.lastModified()
        lastLoaded?.takeIf { it.stamp == stamp }?.let { return it.providers }
        val providers = try {
            parse(source.readText())
        } catch (e: IOException) {
            Log.e(TAG, "Stored rules are unreadable", e)
            emptyList()
        }
        lastLoaded = Loaded(stamp, providers)
        return providers
    }

    private fun parse(json: String): List<LinkCleaner.Provider> = try {
        LinkCleaner.parseRules(json)
    } catch (e: JSONException) {
        Log.e(TAG, "Rules are not valid JSON", e)
        emptyList()
    }
}
