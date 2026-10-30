package com.arslan.customanimator.notify.data

object RuleMatcher {

    class Content(rawTitle: String, rawText: String, rawBigText: String) {
        val title: String by lazy(LazyThreadSafetyMode.NONE) { rawTitle.lowercase() }
        val body: String by lazy(LazyThreadSafetyMode.NONE) { "$rawText $rawBigText".lowercase() }
        val all: String by lazy(LazyThreadSafetyMode.NONE) { "$rawTitle $rawText $rawBigText".lowercase() }
    }

    fun matches(
        rule: NotificationRule,
        packageName: String,
        title: String,
        text: String,
        bigText: String,
    ): Boolean = rule.packageNames.contains(packageName) && matchesKeywords(rule, Content(title, text, bigText))

    fun matchesKeywords(rule: NotificationRule, content: Content): Boolean =
        (rule.keywords.isEmpty() || rule.keywords.any { content.all.contains(it.lowercase()) }) &&
            (rule.titleKeywords.isEmpty() || rule.titleKeywords.any { content.title.contains(it.lowercase()) }) &&
            (rule.bodyKeywords.isEmpty() || rule.bodyKeywords.any { content.body.contains(it.lowercase()) })

    fun shouldExecute(
        rule: NotificationRule,
        isThrottled: Boolean,
        isVibration: Boolean,
        isSilent: Boolean,
        isDND: Boolean,
    ): Boolean {
        if (rule.preventMultipleNotifications && isThrottled) return false
        if (isVibration && !rule.applyOnVibration) return false
        if (isSilent && !rule.applyOnSilent) return false
        if (isDND && !rule.applyOnDND) return false
        return true
    }
}
