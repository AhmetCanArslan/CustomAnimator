package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONArray

abstract class SelectedAppsManager(protected val context: Context, prefsName: String) {

    private val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val selectedKey = "selected_packages"

    private val unsafePackages: Set<String> by lazy {
        InstalledAppsProvider.getUnsafeToKillPackages(context)
    }

    private fun sanitize(packages: Set<String>): Set<String> {
        return packages.filterTo(mutableSetOf()) { it !in unsafePackages }
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
