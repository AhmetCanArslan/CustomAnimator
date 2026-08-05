package com.arslan.customanimator.notify.data

object IgnoreMatcher {

    fun isIgnored(
        rules: List<IgnoreRule>,
        packageName: String,
        title: String,
        body: String,
    ): Boolean = rules.any { matches(it, packageName, title, body) }

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

    fun matchText(text: String, pattern: String, isRegex: Boolean): Boolean {
        return if (isRegex) {
            try {
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
            } catch (_: Exception) {
                false
            }
        } else {
            text.contains(pattern.trim(), ignoreCase = true)
        }
    }
}
