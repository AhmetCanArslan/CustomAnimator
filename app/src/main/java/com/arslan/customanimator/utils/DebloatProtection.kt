package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager

object DebloatProtection {

    private val CRITICAL_SHARED_UIDS = setOf(
        "android.uid.system",
        "android.uid.systemui",
        "android.uid.phone",
        "android.uid.bluetooth",
        "android.uid.nfc",
        "android.uid.se",
        "android.uid.shell",
        "android.uid.networkstack",
        "android.uid.telephony",
        "android.media",
        "com.android.providers.telephony",
        "com.samsung.android.providers.telephony"
    )

    fun protectedPackages(context: Context): Set<String> {
        val blocked = InstalledAppsProvider.getUnsafeToKillPackages(context).toMutableSet()
        blocked.add(context.packageName)
        defaultDialer(context)?.let { blocked.add(it) }
        defaultSms(context)?.let { blocked.add(it) }
        return blocked
    }

    data class Target(
        val packageName: String,
        val uid: Int,
        val flags: Int,
        val sharedUserId: String?
    )

    fun isProtected(target: Target, blocked: Set<String>): Boolean {
        if (target.packageName in blocked) return true
        if (target.uid in 0 until Process.FIRST_APPLICATION_UID) return true
        if (target.sharedUserId in CRITICAL_SHARED_UIDS) return true
        return (target.flags and ApplicationInfo.FLAG_PERSISTENT) != 0
    }

    private fun defaultDialer(context: Context): String? {
        return try {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }

    private fun defaultSms(context: Context): String? {
        return try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) {
            null
        }
    }
}
