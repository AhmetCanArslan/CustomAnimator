package com.riyadmondol2006.customanimator.notify

import com.riyadmondol2006.customanimator.notify.data.IgnoreMatcher
import com.riyadmondol2006.customanimator.notify.data.IgnoreRule
import com.riyadmondol2006.customanimator.notify.data.IgnoreType
import com.riyadmondol2006.customanimator.notify.data.NotificationRule
import com.riyadmondol2006.customanimator.notify.data.RuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMatchingTest {

    private fun rule(
        packages: List<String> = listOf("com.example.chat"),
        keywords: List<String> = emptyList(),
        titleKeywords: List<String> = emptyList(),
        bodyKeywords: List<String> = emptyList()
    ) = NotificationRule(
        packageNames = packages,
        appNames = listOf("Chat"),
        keywords = keywords,
        titleKeywords = titleKeywords,
        bodyKeywords = bodyKeywords,
        actions = emptyList()
    )

    @Test
    fun ruleOnlyMatchesItsOwnPackages() {
        assertTrue(RuleMatcher.matches(rule(), "com.example.chat", "Ann", "hi", ""))
        assertFalse(RuleMatcher.matches(rule(), "com.other.app", "Ann", "hi", ""))
    }

    @Test
    fun keywordMatchingIsCaseInsensitiveAcrossTitleAndBody() {
        val keywordRule = rule(keywords = listOf("URGENT"))
        assertTrue(RuleMatcher.matches(keywordRule, "com.example.chat", "urgent ping", "", ""))
        assertTrue(RuleMatcher.matches(keywordRule, "com.example.chat", "", "", "Urgent later"))
        assertFalse(RuleMatcher.matches(keywordRule, "com.example.chat", "hello", "world", ""))
    }

    @Test
    fun titleAndBodyKeywordsMustBothMatch() {
        val strict = rule(titleKeywords = listOf("bank"), bodyKeywords = listOf("code"))
        assertTrue(RuleMatcher.matches(strict, "com.example.chat", "Bank", "your code is 1234", ""))
        assertFalse(RuleMatcher.matches(strict, "com.example.chat", "Bank", "hello", ""))
        assertFalse(RuleMatcher.matches(strict, "com.example.chat", "Friend", "your code is 1234", ""))
    }

    @Test
    fun executionRespectsModeSwitchesAndThrottling() {
        val base = rule()
        assertTrue(RuleMatcher.shouldExecute(base, isThrottled = false, isVibration = false, isSilent = false, isDND = false))
        assertTrue(RuleMatcher.shouldExecute(base, isThrottled = true, isVibration = false, isSilent = false, isDND = false))

        val throttled = base.copy(preventMultipleNotifications = true)
        assertFalse(RuleMatcher.shouldExecute(throttled, isThrottled = true, isVibration = false, isSilent = false, isDND = false))

        val noVibration = base.copy(applyOnVibration = false)
        assertFalse(RuleMatcher.shouldExecute(noVibration, isThrottled = false, isVibration = true, isSilent = false, isDND = false))

        val noDnd = base.copy(applyOnDND = false)
        assertFalse(RuleMatcher.shouldExecute(noDnd, isThrottled = false, isVibration = false, isSilent = false, isDND = true))
    }

    @Test
    fun onlyFiltersMarkedAsBlockingRemoveTheNotification() {
        val muted = IgnoreRule(type = IgnoreType.APP, packageName = "com.example.chat")
        val blocking = IgnoreRule(
            type = IgnoreType.TITLE,
            packageName = "com.example.shop",
            matchValue = "sale",
            blocksNotification = true
        )
        val rules = listOf(muted, blocking)
        fun matching(packageName: String, title: String) =
            rules.filter { IgnoreMatcher.matches(it, packageName, title, "") }
        assertEquals(listOf(muted), matching("com.example.chat", "hello"))
        assertFalse(matching("com.example.chat", "hello").any { it.blocksNotification })
        assertTrue(matching("com.example.shop", "Big SALE today").any { it.blocksNotification })
        assertTrue(matching("com.example.shop", "Your order shipped").isEmpty())
    }

    @Test
    fun ignoreRulesCoverAppTitleBodyAndCombinations() {
        val appRule = IgnoreRule(type = IgnoreType.APP, packageName = "com.example.chat")
        assertTrue(IgnoreMatcher.matches(appRule, "com.example.chat", "anything", "anything"))
        assertFalse(IgnoreMatcher.matches(appRule, "com.other.app", "anything", "anything"))

        val titleRule = IgnoreRule(type = IgnoreType.TITLE, packageName = "com.example.chat", matchValue = "spam")
        assertTrue(IgnoreMatcher.matches(titleRule, "com.example.chat", "SPAM alert", ""))
        assertFalse(IgnoreMatcher.matches(titleRule, "com.example.chat", "real message", ""))

        val bothRule = IgnoreRule(
            type = IgnoreType.TITLE_AND_BODY,
            packageName = "com.example.chat",
            matchValue = "promo",
            matchValue2 = "discount"
        )
        assertTrue(IgnoreMatcher.matches(bothRule, "com.example.chat", "Promo", "big discount"))
        assertFalse(IgnoreMatcher.matches(bothRule, "com.example.chat", "Promo", "nothing here"))
    }

    @Test
    fun brokenRegexNeverCrashesMatching() {
        val broken = IgnoreRule(
            type = IgnoreType.TITLE,
            packageName = "com.example.chat",
            matchValue = "([unclosed",
            isRegex = true
        )
        assertFalse(IgnoreMatcher.matches(broken, "com.example.chat", "([unclosed", ""))
    }

    @Test
    fun blankMatchValuesNeverIgnoreEverything() {
        val blank = IgnoreRule(type = IgnoreType.TITLE, packageName = "com.example.chat", matchValue = "")
        assertFalse(IgnoreMatcher.matches(blank, "com.example.chat", "whatever", ""))
    }
}
