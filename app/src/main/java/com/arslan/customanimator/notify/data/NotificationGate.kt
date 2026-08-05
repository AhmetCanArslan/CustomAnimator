package com.arslan.customanimator.notify.data

object NotificationGate {

    fun shouldProcess(
        isServiceEnabled: Boolean,
        isOwnServiceNotification: Boolean,
        isGroupSummary: Boolean,
        title: String,
        body: String,
    ): Boolean {
        if (!isServiceEnabled) return false
        if (isOwnServiceNotification) return false
        if (isGroupSummary) return false
        if (title.isBlank() && body.isBlank()) return false
        return true
    }
}
