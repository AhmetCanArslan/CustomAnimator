package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.PresetTileConfig
import com.arslan.customanimator.data.WidthPreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WidthPresetManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences("width_presets", Context.MODE_PRIVATE)
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
                        widthDp = json.getInt("widthDp"),
                        tile = PresetTileJson.read(json)
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
                WidthTileSlots.sync(appContext, this)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setTileConfig(id: String, config: PresetTileConfig?): Boolean {
        return try {
            val presets = getAllPresetsJson()
            var index = -1
            for (i in 0 until presets.length()) {
                if (presets.getJSONObject(i).getString("id") == id) {
                    index = i
                    break
                }
            }
            if (index < 0) return false

            val resolved = when {
                config == null -> null
                config.slot in 0 until PresetTileJson.MAX_TILE_SLOTS -> config
                else -> {
                    val free = firstFreeSlot(excludingPresetId = id) ?: return false
                    config.copy(slot = free)
                }
            }

            val json = presets.getJSONObject(index)
            json.remove(PresetTileJson.KEY)
            PresetTileJson.write(json, resolved)
            presets.put(index, json)
            sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            WidthTileSlots.sync(appContext, this)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPresetForSlot(slot: Int): WidthPreset? =
        getAllPresets().firstOrNull { it.tile?.slot == slot }

    fun firstFreeSlot(excludingPresetId: String? = null): Int? =
        PresetTileJson.firstFreeSlot(
            getAllPresets().filter { it.id != excludingPresetId }.mapNotNull { it.tile?.slot }
        )

    private fun getAllPresetsJson(): JSONArray {
        return try {
            val presetStr = sharedPreferences.getString(presetsKey, "[]") ?: "[]"
            JSONArray(presetStr)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
