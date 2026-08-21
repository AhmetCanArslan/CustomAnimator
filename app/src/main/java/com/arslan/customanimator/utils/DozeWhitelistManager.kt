package com.arslan.customanimator.utils

data class DozeWhitelistEntry(val packageName: String, val isSystem: Boolean)

object DozeWhitelistManager {

    fun getWhitelist(): List<DozeWhitelistEntry> {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("dumpsys", "deviceidle", "whitelist"))
        if (!result.isSuccess) return emptyList()
        return result.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(',') }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size < 2) return@mapNotNull null
                val source = parts[0].trim()
                val packageName = parts[1].trim()
                if (packageName.isEmpty()) return@mapNotNull null
                DozeWhitelistEntry(packageName, source.startsWith("system"))
            }
            .distinctBy { it.packageName }
            .toList()
    }

    fun getWhitelistedPackages(): Set<String> = getWhitelist().mapTo(mutableSetOf()) { it.packageName }

    fun add(packageName: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "deviceidle", "whitelist", "+$packageName"))
    }

    fun remove(packageName: String): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "deviceidle", "whitelist", "-$packageName"))
    }

    fun setWhitelisted(packageName: String, whitelisted: Boolean): Boolean {
        return if (whitelisted) add(packageName) else remove(packageName)
    }
}
