package com.arslan.customanimator.utils

import com.arslan.customanimator.R

enum class WhatsAppMediaPart(val titleRes: Int, val subfolder: String?) {
    RECEIVED(R.string.whatsapp_cleaner_part_received, null),
    SENT(R.string.whatsapp_cleaner_part_sent, "Sent"),
    PRIVATE(R.string.whatsapp_cleaner_part_private, "Private")
}

enum class WhatsAppMediaKind(val titleRes: Int, val patterns: List<String>) {
    PHOTOS(R.string.whatsapp_cleaner_images, listOf("*.jpg", "*.jpeg", "*.png", "*.webp")),
    VIDEOS(R.string.whatsapp_cleaner_videos, listOf("*.mp4", "*.3gp", "*.mkv", "*.webm")),
    PDF(R.string.whatsapp_cleaner_kind_pdf, listOf("*.pdf")),
    APK(R.string.whatsapp_cleaner_kind_apk, listOf("*.apk", "*.apks", "*.xapk")),
    ARCHIVES(R.string.whatsapp_cleaner_kind_archives, listOf("*.zip", "*.rar", "*.7z", "*.tar", "*.gz")),
    OTHER(R.string.whatsapp_cleaner_kind_other, emptyList())
}

enum class WhatsAppMediaCategory(
    val titleRes: Int,
    val folder: String,
    val isSplit: Boolean = false,
    val kinds: List<WhatsAppMediaKind> = emptyList(),
    val namePattern: String? = null,
    val isMedia: Boolean = true
) {
    IMAGES(R.string.whatsapp_cleaner_images, "Media/WhatsApp Images", isSplit = true),
    VIDEOS(R.string.whatsapp_cleaner_videos, "Media/WhatsApp Video", isSplit = true),
    DOCUMENTS(
        R.string.whatsapp_cleaner_documents,
        "Media/WhatsApp Documents",
        isSplit = true,
        kinds = listOf(
            WhatsAppMediaKind.PDF,
            WhatsAppMediaKind.APK,
            WhatsAppMediaKind.ARCHIVES,
            WhatsAppMediaKind.OTHER
        )
    ),
    AUDIO(R.string.whatsapp_cleaner_audio, "Media/WhatsApp Audio", isSplit = true),
    GIFS(R.string.whatsapp_cleaner_gifs, "Media/WhatsApp Animated Gifs", isSplit = true),
    VOICE_NOTES(R.string.whatsapp_cleaner_voice_notes, "Media/WhatsApp Voice Notes"),
    VIDEO_NOTES(R.string.whatsapp_cleaner_video_notes, "Media/WhatsApp Video Notes"),
    STATUSES(
        R.string.whatsapp_cleaner_statuses,
        "Media/.Statuses",
        kinds = listOf(WhatsAppMediaKind.PHOTOS, WhatsAppMediaKind.VIDEOS, WhatsAppMediaKind.OTHER)
    ),
    STICKERS(R.string.whatsapp_cleaner_stickers, "Media/WhatsApp Stickers"),
    WALLPAPERS(R.string.whatsapp_cleaner_wallpapers, "Media/WallPaper"),
    PROFILE_PHOTOS(R.string.whatsapp_cleaner_profile_photos, "Media/WhatsApp Profile Photos"),
    LINK_PREVIEWS(R.string.whatsapp_cleaner_link_previews, "Media/.Links", isMedia = false),
    SHARED(R.string.whatsapp_cleaner_shared, ".Shared", isMedia = false),
    TRASH(R.string.whatsapp_cleaner_trash, ".trash", isMedia = false),
    THUMBNAILS(R.string.whatsapp_cleaner_thumbnails, ".Thumbs", isMedia = false),
    OLD_BACKUPS(
        R.string.whatsapp_cleaner_old_backups,
        "Databases",
        namePattern = "msgstore-*",
        isMedia = false
    );

    val parts: List<WhatsAppMediaPart>
        get() = if (isSplit) WhatsAppMediaPart.entries else listOf(WhatsAppMediaPart.RECEIVED)

    val hasBreakdown: Boolean
        get() = isSplit || kinds.isNotEmpty()

    val targets: List<WhatsAppMediaTarget>
        get() = parts.flatMap { part ->
            if (kinds.isEmpty()) listOf(WhatsAppMediaTarget(this, part))
            else kinds.map { kind -> WhatsAppMediaTarget(this, part, kind) }
        }
}

data class WhatsAppMediaTarget(
    val category: WhatsAppMediaCategory,
    val part: WhatsAppMediaPart,
    val kind: WhatsAppMediaKind? = null
)

data class WhatsAppMediaUsage(val fileCount: Int, val bytes: Long) {
    operator fun plus(other: WhatsAppMediaUsage) =
        WhatsAppMediaUsage(fileCount + other.fileCount, bytes + other.bytes)

    companion object {
        val NONE = WhatsAppMediaUsage(0, 0L)
    }
}

data class WhatsAppScan(
    val isMediaFolderFound: Boolean,
    val usage: Map<WhatsAppMediaTarget, WhatsAppMediaUsage>
) {
    val total: WhatsAppMediaUsage
        get() = usageOf(usage.keys)

    fun targetsWithFiles(): List<WhatsAppMediaTarget> =
        WhatsAppCleanerManager.allTargets.filter { (usage[it]?.fileCount ?: 0) > 0 }

    fun usageOf(targets: Collection<WhatsAppMediaTarget>): WhatsAppMediaUsage =
        targets.fold(WhatsAppMediaUsage.NONE) { total, target -> total + (usage[target] ?: WhatsAppMediaUsage.NONE) }
}

object WhatsAppCleanerManager {
    val AGE_OPTIONS_DAYS = listOf(0, 30, 180, 365)

    private val APP_ROOTS = listOf(
        "/sdcard/Android/media/com.whatsapp/WhatsApp",
        "/sdcard/WhatsApp"
    )

    private const val MEDIA_FOLDER = "Media"

    private const val ROOT_MARKER = "ROOT"
    private const val KEY_SEPARATOR = "/"
    private const val SECONDS_PER_DAY = 86400
    private const val FIELDS_PER_AGE = 3
    private const val BYTES_PER_KILOBYTE = 1000L
    private const val FILE_FILTER = "-type f ! -name .nomedia"
    private const val DIRECT_FILES = "-maxdepth 1"
    private const val NESTED_FILES = "-mindepth 1"

    val allTargets: List<WhatsAppMediaTarget> = WhatsAppMediaCategory.entries.flatMap { it.targets }

    fun scan(): Map<Int, WhatsAppScan> =
        parseScan(ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", scanScript())).output)

    fun delete(targets: Collection<WhatsAppMediaTarget>, minAgeDays: Int): Boolean =
        targets.isNotEmpty() &&
            ShizukuHelper.executeShellCommand(arrayOf("sh", "-c", deleteScript(targets, minAgeDays)))

    internal fun scanScript(): String {
        val reset = AGE_OPTIONS_DAYS.joinToString(" ") { "n$it=0; t$it=0; r$it=0;" }
        val tally = AGE_OPTIONS_DAYS.joinToString(" ") { days ->
            val add = "n$days=\$((n$days+1)); t$days=\$((t$days+10#\$h)); r$days=\$((r$days+10#\$l));"
            if (days > 0) "if [ \$a -gt $days ]; then $add fi;" else add
        }
        val report = AGE_OPTIONS_DAYS.joinToString(" ") { "\$n$it \$t$it \$r$it" }
        val split = "p=000\$s; h=\${p%???}; l=\${p#\"\$h\"};"
        val counter = "count() { $reset while read s m; do $split a=\$(( (now-m)/$SECONDS_PER_DAY )); $tally done; " +
            "echo \"\$1 $report\"; }"
        val rootCheck = APP_ROOTS.joinToString(" || ") { "[ -d ${quote("$it/$MEDIA_FOLDER")} ]" } +
            " && echo $ROOT_MARKER"
        val scans = allTargets.map { target ->
            "find ${directoriesOf(target).joinToString(" ") { quote(it) }} ${fileFilterOf(target)} " +
                "-exec stat -c '%s %Y' {} + 2>/dev/null | count ${keyOf(target)}"
        }
        return (listOf("now=\$(date +%s)", counter, rootCheck) + scans).joinToString("\n")
    }

    internal fun deleteScript(targets: Collection<WhatsAppMediaTarget>, minAgeDays: Int): String =
        targets.joinToString("\n") { target ->
            "for d in ${directoriesOf(target).joinToString(" ") { quote(it) }}; do if [ -d \"\$d\" ]; then " +
                "find \"\$d\" ${fileFilterOf(target)}${ageFilter(minAgeDays)} -exec rm -f {} + || exit 1; fi; done"
        }

    internal fun parseScan(output: String): Map<Int, WhatsAppScan> {
        val lines = output.lines().map { it.trim() }
        val rows = lines.mapNotNull { parseUsages(it) }
        return AGE_OPTIONS_DAYS.withIndex().associate { (index, days) ->
            days to WhatsAppScan(
                isMediaFolderFound = ROOT_MARKER in lines,
                usage = rows.associate { (target, usages) -> target to usages[index] }
            )
        }
    }

    private fun parseUsages(line: String): Pair<WhatsAppMediaTarget, List<WhatsAppMediaUsage>>? {
        val fields = line.split(" ")
        if (fields.size != 1 + FIELDS_PER_AGE * AGE_OPTIONS_DAYS.size) return null
        val target = allTargets.firstOrNull { keyOf(it) == fields[0] } ?: return null
        val numbers = fields.drop(1).map { it.toLongOrNull() ?: return null }
        return target to numbers.chunked(FIELDS_PER_AGE) { (fileCount, kilobytes, bytes) ->
            WhatsAppMediaUsage(fileCount.toInt(), kilobytes * BYTES_PER_KILOBYTE + bytes)
        }
    }

    private fun keyOf(target: WhatsAppMediaTarget) =
        listOfNotNull(target.category.name, target.part.name, target.kind?.name).joinToString(KEY_SEPARATOR)

    private fun fileFilterOf(target: WhatsAppMediaTarget) = "${depthOf(target)} $FILE_FILTER${nameFilterOf(target)}"

    private fun depthOf(target: WhatsAppMediaTarget) =
        if (target.category.isSplit && target.part == WhatsAppMediaPart.RECEIVED) DIRECT_FILES else NESTED_FILES

    private fun nameFilterOf(target: WhatsAppMediaTarget): String = when (target.kind) {
        null -> target.category.namePattern?.let { " ${anyNameOf(listOf(it))}" }.orEmpty()
        WhatsAppMediaKind.OTHER -> " ! ${anyNameOf(target.category.kinds.flatMap { it.patterns })}"
        else -> " ${anyNameOf(target.kind.patterns)}"
    }

    private fun anyNameOf(patterns: List<String>) =
        patterns.joinToString(" -o ", "\\( ", " \\)") { "-iname ${quote(it)}" }

    private fun directoriesOf(target: WhatsAppMediaTarget): List<String> = APP_ROOTS.map { root ->
        listOfNotNull(root, target.category.folder, target.part.subfolder).joinToString("/")
    }

    private fun ageFilter(minAgeDays: Int) = if (minAgeDays > 0) " -mtime +$minAgeDays" else ""

    private fun quote(value: String) = "'$value'"
}
