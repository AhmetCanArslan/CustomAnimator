package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import org.json.JSONArray

class AutoForceStopManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("auto_force_stop", Context.MODE_PRIVATE)
    private val selectedKey = "selected_packages"

    private fun launcherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return try {
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitize(packages: Set<String>): Set<String> {
        val launcherPackage = launcherPackageName()
        return packages.filterTo(mutableSetOf()) {
            it != context.packageName && it != launcherPackage
        }
    }

    fun getSelectedPackages(): Set<String> {
        return try {
            val jsonString = sharedPreferences.getString(selectedKey, "[]") ?: "[]"
            val array = JSONArray(jsonString)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
            sanitize(set)
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun setSelectedPackages(packages: Set<String>) {
        val sanitized = sanitize(packages)
        val array = JSONArray()
        sanitized.forEach { array.put(it) }
        sharedPreferences.edit().putString(selectedKey, array.toString()).apply()
    }

    fun isPackageSelected(packageName: String): Boolean {
        return getSelectedPackages().contains(packageName)
    }

    fun togglePackage(packageName: String): Set<String> {
        val current = getSelectedPackages().toMutableSet()
        if (!current.add(packageName)) {
            current.remove(packageName)
        }
        setSelectedPackages(current)
        return getSelectedPackages()
    }
}
