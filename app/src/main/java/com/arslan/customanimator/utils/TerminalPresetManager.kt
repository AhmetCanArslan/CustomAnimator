package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.TerminalPreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TerminalPresetManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("terminal_presets", Context.MODE_PRIVATE)
    private val presetsKey = "presets_list"

    fun savePreset(name: String, command: String): Boolean {
        return try {
            val presets = getAllPresetsJson()
            presets.put(toJson(TerminalPreset(UUID.randomUUID().toString(), name, command)))
            sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAllPresets(): List<TerminalPreset> {
        return try {
            val presets = getAllPresetsJson()
            val list = mutableListOf<TerminalPreset>()
            for (i in 0 until presets.length()) {
                val json = presets.getJSONObject(i)
                list.add(
                    TerminalPreset(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        command = json.getString("command")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deletePreset(id: String): Boolean {
        return try {
            val presets = getAllPresetsJson()
            val remaining = JSONArray()
            var found = false
            for (i in 0 until presets.length()) {
                val json = presets.getJSONObject(i)
                if (json.getString("id") == id) {
                    found = true
                } else {
                    remaining.put(json)
                }
            }

            if (found) {
                sharedPreferences.edit().putString(presetsKey, remaining.toString()).apply()
            }
            found
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Rewrites the entry in place so editing keeps its id and its position in the list. */
    fun updatePreset(id: String, name: String, command: String): Boolean {
        return try {
            val presets = getAllPresetsJson()
            var found = false
            for (i in 0 until presets.length()) {
                if (presets.getJSONObject(i).getString("id") == id) {
                    presets.put(i, toJson(TerminalPreset(id, name, command)))
                    found = true
                    break
                }
            }

            if (found) {
                sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            }
            found
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun toJson(preset: TerminalPreset): JSONObject {
        return JSONObject().apply {
            put("id", preset.id)
            put("name", preset.name)
            put("command", preset.command)
        }
    }

    private fun getAllPresetsJson(): JSONArray {
        return try {
            val presetStr = sharedPreferences.getString(presetsKey, "[]") ?: "[]"
            JSONArray(presetStr)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
