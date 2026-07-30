package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.TerminalPreset
import com.arslan.customanimator.data.TerminalTileConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TerminalPresetManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences("terminal_presets", Context.MODE_PRIVATE)
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
                list.add(fromJson(presets.getJSONObject(i)))
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
                TerminalTileSlots.sync(appContext, this)
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
                val existing = presets.getJSONObject(i)
                if (existing.getString("id") == id) {
                    val tile = fromJson(existing).tile
                    presets.put(i, toJson(TerminalPreset(id, name, command, tile)))
                    found = true
                    break
                }
            }

            if (found) {
                sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
                TerminalTileSlots.sync(appContext, this)
            }
            found
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setTileConfig(id: String, config: TerminalTileConfig?): Boolean {
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
                config.slot in 0 until MAX_TILE_SLOTS -> config
                else -> {
                    val free = firstFreeSlot(excludingPresetId = id) ?: return false
                    config.copy(slot = free)
                }
            }

            val current = fromJson(presets.getJSONObject(index))
            presets.put(index, toJson(current.copy(tile = resolved)))
            sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            TerminalTileSlots.sync(appContext, this)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPresetForSlot(slot: Int): TerminalPreset? =
        getAllPresets().firstOrNull { it.tile?.slot == slot }

    fun firstFreeSlot(excludingPresetId: String? = null): Int? {
        val taken = getAllPresets()
            .filter { it.id != excludingPresetId }
            .mapNotNull { it.tile?.slot }
            .toSet()
        return (0 until MAX_TILE_SLOTS).firstOrNull { it !in taken }
    }

    private fun toJson(preset: TerminalPreset): JSONObject {
        return JSONObject().apply {
            put("id", preset.id)
            put("name", preset.name)
            put("command", preset.command)
            preset.tile?.let { tile ->
                put("tile", JSONObject().apply {
                    put("slot", tile.slot)
                    put("label", tile.label)
                    put("iconKey", tile.iconKey)
                    put("showToast", tile.showToast)
                    put("collapsePanel", tile.collapsePanel)
                })
            }
        }
    }

    private fun fromJson(json: JSONObject): TerminalPreset {
        val tile = json.optJSONObject("tile")?.let {
            TerminalTileConfig(
                slot = it.optInt("slot", -1),
                label = it.optString("label"),
                iconKey = it.optString("iconKey", TerminalTileIcons.DEFAULT_KEY),
                showToast = it.optBoolean("showToast", true),
                collapsePanel = it.optBoolean("collapsePanel", true)
            )
        }?.takeIf { it.slot in 0 until MAX_TILE_SLOTS }

        return TerminalPreset(
            id = json.getString("id"),
            name = json.getString("name"),
            command = json.getString("command"),
            tile = tile
        )
    }

    private fun getAllPresetsJson(): JSONArray {
        return try {
            val presetStr = sharedPreferences.getString(presetsKey, "[]") ?: "[]"
            JSONArray(presetStr)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    companion object {
        const val MAX_TILE_SLOTS = 5
    }
}
