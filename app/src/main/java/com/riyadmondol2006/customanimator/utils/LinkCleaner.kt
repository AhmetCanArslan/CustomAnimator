package com.riyadmondol2006.customanimator.utils

import org.json.JSONObject
import java.net.URLDecoder
import java.util.regex.PatternSyntaxException

object LinkCleaner {

    private val URL_PATTERN = Regex("""https?://[^\s<>"]+""")
    private const val TRAILING_PUNCTUATION = ".,;:!?'"
    private const val MAX_REDIRECTS = 5

    class Provider(
        private val urlPattern: Regex,
        private val exceptions: List<Regex>,
        val redirections: List<Regex>,
        val rawRules: List<Regex>,
        val params: List<Regex>
    ) {
        fun matches(url: String): Boolean =
            urlPattern.containsMatchIn(url) && exceptions.none { it.containsMatchIn(url) }
    }

    fun parseRules(json: String): List<Provider> {
        val providers = JSONObject(json).getJSONObject("providers")
        return providers.keys().asSequence().mapNotNull { parseProvider(providers.getJSONObject(it)) }.toList()
    }

    fun hasLink(text: String): Boolean = URL_PATTERN.containsMatchIn(text)

    fun clean(text: String, providers: List<Provider>): String = URL_PATTERN.replace(text) { match ->
        val url = trimTrailing(match.value)
        cleanUrl(url, providers, 0) + match.value.substring(url.length)
    }

    private fun parseProvider(item: JSONObject): Provider? = try {
        Provider(
            urlPattern = regex(item.getString("urlPattern")),
            exceptions = regexes(item, "exceptions"),
            redirections = regexes(item, "redirections"),
            rawRules = regexes(item, "rawRules"),
            params = regexes(item, "rules") + regexes(item, "referralMarketing")
        )
    } catch (e: PatternSyntaxException) {
        null
    }

    private fun regexes(item: JSONObject, key: String): List<Regex> {
        val array = item.optJSONArray(key) ?: return emptyList()
        return List(array.length()) { regex(array.getString(it)) }
    }

    private fun regex(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private fun trimTrailing(url: String): String {
        val trimmed = url.trimEnd { it in TRAILING_PUNCTUATION }
        val unbalanced = trimmed.endsWith(')') && trimmed.count { it == '(' } < trimmed.count { it == ')' }
        return if (unbalanced) trimTrailing(trimmed.dropLast(1)) else trimmed
    }

    private fun cleanUrl(url: String, providers: List<Provider>, depth: Int): String {
        val matching = providers.filter { it.matches(url) }
        val target = matching.asSequence()
            .flatMap { it.redirections }
            .firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1)?.let(::decode) }
        if (target != null && depth < MAX_REDIRECTS) return cleanUrl(target, providers, depth + 1)
        val stripped = matching.flatMap { it.rawRules }.fold(url) { current, rule -> rule.replace(current, "") }
        return removeParams(stripped, matching.flatMap { it.params })
    }

    private fun decode(value: String): String? = try {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
            .takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun removeParams(url: String, params: List<Regex>): String {
        val fragmentStart = url.indexOf('#').takeIf { it >= 0 } ?: url.length
        val beforeFragment = url.substring(0, fragmentStart)
        val queryStart = beforeFragment.indexOf('?')
        if (queryStart < 0) return url
        val kept = beforeFragment.substring(queryStart + 1).split('&')
            .filter { pair -> pair.isNotEmpty() && params.none { it.matches(pair.substringBefore('=')) } }
        val query = if (kept.isEmpty()) "" else kept.joinToString("&", prefix = "?")
        return beforeFragment.substring(0, queryStart) + query + url.substring(fragmentStart)
    }
}
