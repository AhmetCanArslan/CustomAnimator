package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager

object GameModeTargets {

    private val SECURE_COMPONENT_KEYS = listOf(
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        "enabled_notification_listeners",
        "assistant",
        "voice_interaction_service"
    )

    fun resolve(context: Context, games: Set<String>): List<String> {
        val protectedPackages = protectedPackages(context) + games
        return InstalledAppsProvider.getLaunchableApps(context)
            .filterNot { it.isSystemApp }
            .map { it.packageName }
            .filterNot { it in protectedPackages }
            .filterNot { isPersistent(context.packageManager, it) }
    }

    fun protectedPackages(context: Context): Set<String> {
        val protectedPackages = InstalledAppsProvider.getUnsafeToKillPackages(context).toMutableSet()
        protectedPackages += defaultHandlers(context)
        SECURE_COMPONENT_KEYS.forEach { key -> protectedPackages += componentOwners(context, key) }
        return protectedPackages
    }

    private fun defaultHandlers(context: Context): Set<String> {
        val handlers = mutableSetOf<String>()
        try {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { handlers += it }
        } catch (e: Exception) {
        }
        try {
            Telephony.Sms.getDefaultSmsPackage(context)?.let { handlers += it }
        } catch (e: Exception) {
        }
        return handlers
    }

    private fun componentOwners(context: Context, secureKey: String): Set<String> {
        val raw = try {
            Settings.Secure.getString(context.contentResolver, secureKey)
        } catch (e: Exception) {
            null
        } ?: return emptySet()

        return raw.split(':')
            .mapNotNull { entry -> entry.substringBefore('/').takeIf { it.isNotBlank() } }
            .toSet()
    }

    private fun isPersistent(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            info.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        } catch (e: Exception) {
            false
        }
    }
}
