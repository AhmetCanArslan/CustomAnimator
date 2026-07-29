package com.arslan.customanimator.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RevokedPermissionsStore(context: Context) {

    private val prefs = context.getSharedPreferences("auto_permission_disabler_state", Context.MODE_PRIVATE)
    private val key = "revoked_map"

    private fun readMap(): JSONObject {
        return try {
            JSONObject(prefs.getString(key, "{}") ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeMap(map: JSONObject) {
        prefs.edit().putString(key, map.toString()).apply()
    }

    @Synchronized
    fun recordRevoked(packageName: String, permissions: List<String>) {
        val map = readMap()
        val array = JSONArray()
        permissions.forEach { array.put(it) }
        map.put(packageName, array)
        writeMap(map)
    }

    @Synchronized
    fun getRevoked(packageName: String): List<String> {
        return try {
            val array = readMap().optJSONArray(packageName) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getAllRevoked(): Map<String, List<String>> {
        return try {
            val map = readMap()
            val result = mutableMapOf<String, List<String>>()
            map.keys().forEach { pkg ->
                val array = map.optJSONArray(pkg) ?: return@forEach
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) list.add(array.getString(i))
                result[pkg] = list
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove(key).apply()
    }

    @Synchronized
    fun clearRevoked(packageName: String) {
        val map = readMap()
        map.remove(packageName)
        writeMap(map)
    }
}
