package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.BuildConfig

data class ChangelogRelease(
    val versionCode: Int,
    val title: String,
    val entries: List<String>
)

object ChangelogManager {

    private const val ASSET_NAME = "changelog.txt"

    fun readAll(context: Context): List<ChangelogRelease> {
        val lines = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readLines() }
        }.getOrElse { return emptyList() }

        val releases = mutableListOf<ChangelogRelease>()
        var versionCode = 0
        var title = ""
        var entries = mutableListOf<String>()

        fun flush() {
            if (versionCode > 0) {
                releases += ChangelogRelease(versionCode, title, entries.toList())
            }
        }

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("##") -> {
                    flush()
                    val header = line.removePrefix("##").trim()
                    versionCode = header.substringBefore('-').trim().toIntOrNull() ?: 0
                    title = header.substringAfter('-', "").trim().ifEmpty { header }
                    entries = mutableListOf()
                }
                line.startsWith("-") -> entries += line.removePrefix("-").trim()
                line.isNotEmpty() -> entries += line
            }
        }
        flush()

        return releases.sortedByDescending { it.versionCode }
    }

    fun unseenReleases(context: Context): List<ChangelogRelease> {
        val lastSeen = SettingsManager.getLastSeenChangelogVersion(context)
        return readAll(context).filter { it.versionCode > lastSeen }
    }

    fun markCurrentSeen(context: Context) {
        SettingsManager.setLastSeenChangelogVersion(context, BuildConfig.VERSION_CODE)
    }
}
