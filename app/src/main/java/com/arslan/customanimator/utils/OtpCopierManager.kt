package com.arslan.customanimator.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.arslan.customanimator.R

object OtpCopierManager {

    private const val PREFS_NAME = "otp_copier_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_KEYWORDS = "keywords"
    private const val KEY_PATTERNS = "patterns"
    private const val ENTRY_SEPARATOR = "\n"

    val DEFAULT_KEYWORDS = listOf(
        "code", "otp", "pin", "passcode", "password", "verification",
        "kod", "şifre", "parola", "doğrulama", "onay"
    )

    private const val CODE = "(?<!\\d)(\\d{4,8})(?!\\d)"
    private const val SENSITIVE_OP = "RECEIVE_SENSITIVE_NOTIFICATIONS"
    private const val SENSITIVE_OP_ALLOWED = "allow"
    private const val CLIP_LABEL = "OTP"

    @Volatile
    private var lastCopied: String? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getKeywords(context: Context): List<String> =
        readEntries(context, KEY_KEYWORDS) ?: DEFAULT_KEYWORDS

    fun addKeyword(context: Context, keyword: String): Boolean {
        val word = keyword.trim()
        val current = getKeywords(context)
        if (word.isEmpty() || current.any { it.equals(word, ignoreCase = true) }) return false
        writeEntries(context, KEY_KEYWORDS, current + word)
        return true
    }

    fun removeKeyword(context: Context, keyword: String) {
        writeEntries(context, KEY_KEYWORDS, getKeywords(context) - keyword)
    }

    fun resetKeywords(context: Context) {
        prefs(context).edit().remove(KEY_KEYWORDS).apply()
    }

    fun getPatterns(context: Context): List<String> =
        readEntries(context, KEY_PATTERNS) ?: emptyList()

    fun addPattern(context: Context, pattern: String): Boolean {
        val trimmed = pattern.trim()
        val current = getPatterns(context)
        if (!isValidPattern(trimmed) || trimmed in current) return false
        writeEntries(context, KEY_PATTERNS, current + trimmed)
        return true
    }

    fun removePattern(context: Context, pattern: String) {
        writeEntries(context, KEY_PATTERNS, getPatterns(context) - pattern)
    }

    fun isValidPattern(pattern: String): Boolean = pattern.isNotBlank() && compile(pattern) != null

    fun extractCode(keywords: List<String>, patterns: List<String>, text: String): String? =
        patterns.firstNotNullOfOrNull { matchPattern(it, text) } ?: matchKeywords(keywords, text)

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun needsSensitiveAccess(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    fun hasSensitiveAccess(context: Context): Boolean {
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("appops", "get", context.packageName, SENSITIVE_OP)
        )
        return result.isSuccess && result.output.contains(SENSITIVE_OP_ALLOWED)
    }

    fun grantSensitiveAccess(context: Context): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("appops", "set", context.packageName, SENSITIVE_OP, SENSITIVE_OP_ALLOWED)
        )

    fun onNotification(context: Context, key: String, text: String) {
        if (!isEnabled(context)) return
        val code = extractCode(getKeywords(context), getPatterns(context), text) ?: return
        val signature = "$key:$code"
        if (signature == lastCopied) return
        lastCopied = signature
        copy(context, code)
    }

    private fun readEntries(context: Context, key: String): List<String>? =
        prefs(context).getString(key, null)?.split(ENTRY_SEPARATOR)?.filter { it.isNotEmpty() }

    private fun writeEntries(context: Context, key: String, entries: List<String>) {
        prefs(context).edit().putString(key, entries.joinToString(ENTRY_SEPARATOR)).apply()
    }

    private fun compile(pattern: String): Regex? = runCatching { Regex(pattern) }.getOrNull()

    private fun firstGroupOrMatch(match: MatchResult): String =
        match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: match.value

    private fun matchPattern(pattern: String, text: String): String? =
        compile(pattern)?.find(text)?.let(::firstGroupOrMatch)

    private fun foldTurkishI(text: String): String = text.replace('İ', 'I').replace('ı', 'i')

    private fun matchKeywords(keywords: List<String>, text: String): String? {
        if (keywords.isEmpty()) return null
        val words = keywords.joinToString("|") { Regex.escape(foldTurkishI(it)) }
        val keyword = "(?<!\\p{L})(?:$words)"
        val regex = Regex("(?iu)$keyword\\p{L}*\\D{0,40}?$CODE|$CODE\\D{0,40}?$keyword")
        return regex.find(foldTurkishI(text))?.let(::firstGroupOrMatch)
    }

    private fun copy(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))
        Toast.makeText(context, context.getString(R.string.otp_copier_copied, code), Toast.LENGTH_SHORT).show()
    }
}
