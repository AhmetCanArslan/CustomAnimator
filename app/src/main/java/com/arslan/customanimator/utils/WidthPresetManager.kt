package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.WidthPreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WidthPresetManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("width_presets", Context.MODE_PRIVATE)
    private val presetsKey = "width_presets_list"

    fun savePreset(name: String, widthDp: Int): Boolean {
        return try {
            val presets = getAllPresetsJson()
            val preset = WidthPreset(
                id = UUID.randomUUID().toString(),
                name = name,
                widthDp = widthDp
            )

            val presetJson = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("widthDp", preset.widthDp)
            }

            presets.put(presetJson)
            sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAllPresets(): List<WidthPreset> {
        return try {
            val presets = getAllPresetsJson()
            val list = mutableListOf<WidthPreset>()
            for (i in 0 until presets.length()) {
                val json = presets.getJSONObject(i)
                list.add(
                    WidthPreset(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        widthDp = json.getInt("widthDp")
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
            var foundIndex = -1
            for (i in 0 until presets.length()) {
                if (presets.getJSONObject(i).getString("id") == id) {
                    foundIndex = i
                    break
                }
            }

            if (foundIndex >= 0) {
                val newPresets = JSONArray()
                for (i in 0 until presets.length()) {
                    if (i != foundIndex) {
                        newPresets.put(presets.getJSONObject(i))
                    }
                }
                sharedPreferences.edit().putString(presetsKey, newPresets.toString()).apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
