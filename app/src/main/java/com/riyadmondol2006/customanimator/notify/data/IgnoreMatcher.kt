package com.riyadmondol2006.customanimator.notify.data

object IgnoreMatcher {

    fun matches(
        rule: IgnoreRule,
        packageName: String,
        title: String,
        body: String,
    ): Boolean = when (rule.type) {
        IgnoreType.APP ->
            rule.packageName == packageName
        IgnoreType.TITLE ->
            rule.packageName == packageName &&
                !rule.matchValue.isNullOrBlank() &&
                matchText(title, rule.matchValue, rule.isRegex)
        IgnoreType.BODY ->
            rule.packageName == packageName &&
                !rule.matchValue.isNullOrBlank() &&
                matchText(body, rule.matchValue, rule.isRegex)
        IgnoreType.TITLE_AND_BODY ->
            rule.packageName == packageName &&
                !rule.matchValue.isNullOrBlank() &&
                !rule.matchValue2.isNullOrBlank() &&
                matchText(title, rule.matchValue, rule.isRegex) &&
                matchText(body, rule.matchValue2, rule.isRegex2)
    }

    private fun matchText(text: String, pattern: String, isRegex: Boolean): Boolean {
        return if (isRegex) {
            compiled(pattern)?.containsMatchIn(text) ?: false
        } else {
            text.contains(pattern.trim(), ignoreCase = true)
        }
    }

    private const val MAX_COMPILED_PATTERNS = 64

    private val compiledPatterns = HashMap<String, Regex?>()

    private fun compiled(pattern: String): Regex? = synchronized(compiledPatterns) {
        if (compiledPatterns.containsKey(pattern)) return compiledPatterns[pattern]
        if (compiledPatterns.size >= MAX_COMPILED_PATTERNS) compiledPatterns.clear()
        val regex = try {
            Regex(pattern, RegexOption.IGNORE_CASE)
        } catch (_: Exception) {
            null
        }
        compiledPatterns[pattern] = regex
        regex
    }
}
