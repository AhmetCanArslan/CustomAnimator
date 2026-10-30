package com.riyadmondol2006.customanimator.notify.data

object NotificationGate {

    fun shouldProcess(
        isServiceEnabled: Boolean,
        isOwnServiceNotification: Boolean,
    ): Boolean {
        if (!isServiceEnabled) return false
        if (isOwnServiceNotification) return false
        return true
    }
}
