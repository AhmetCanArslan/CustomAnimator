package com.arslan.customanimator.notify.data

object RuleMatcher {

    fun matches(
        rule: NotificationRule,
        packageName: String,
        title: String,
        text: String,
        bigText: String,
    ): Boolean {
        if (!rule.packageNames.contains(packageName)) return false

        val searchBody = "$title $text $bigText".lowercase()
        val titleLower = title.lowercase()
        val bodyLower = "$text $bigText".lowercase()

        return (rule.keywords.isEmpty() || rule.keywords.any { searchBody.contains(it.lowercase()) }) &&
            (rule.titleKeywords.isEmpty() || rule.titleKeywords.any { titleLower.contains(it.lowercase()) }) &&
            (rule.bodyKeywords.isEmpty() || rule.bodyKeywords.any { bodyLower.contains(it.lowercase()) })
    }

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
