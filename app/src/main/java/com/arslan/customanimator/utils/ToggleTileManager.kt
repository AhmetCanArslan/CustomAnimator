package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.ToggleTile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ToggleTileManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences("toggle_tiles", Context.MODE_PRIVATE)

    fun getTiles(): List<ToggleTile> {
        return try {
            val array = readArray()
            val list = mutableListOf<ToggleTile>()
            for (i in 0 until array.length()) {
                list.add(fromJson(array.getJSONObject(i)))
            }
            list.filter { it.slot in 0 until MAX_TILE_SLOTS }.sortedBy { it.slot }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getTileForSlot(slot: Int): ToggleTile? = getTiles().firstOrNull { it.slot == slot }

    fun getTileForPreset(presetKey: String): ToggleTile? =
        getTiles().firstOrNull { it.presetKey == presetKey && presetKey != ToggleTilePresets.CUSTOM_KEY }

    fun firstFreeSlot(excludingId: String? = null): Int? {
        val taken = getTiles().filter { it.id != excludingId }.map { it.slot }.toSet()
        return (0 until MAX_TILE_SLOTS).firstOrNull { it !in taken }
    }

    fun addTile(
        presetKey: String,
        label: String,
        onCommand: String,
        offCommand: String,
        readCommand: String,
        onValue: String,
        iconKey: String,
        collapsePanel: Boolean,
        showToast: Boolean
    ): ToggleTile? {
        val slot = firstFreeSlot() ?: return null
        val tile = ToggleTile(
            id = UUID.randomUUID().toString(),
            presetKey = presetKey,
            label = label,
            onCommand = onCommand,
            offCommand = offCommand,
            readCommand = readCommand,
            onValue = onValue,
            iconKey = TerminalTileIcons.canonicalKey(iconKey),
            slot = slot,
            collapsePanel = collapsePanel,
            showToast = showToast
        )
        val array = readArray()
        array.put(toJson(tile))
        persist(array)
        return tile
    }

    fun updateTile(tile: ToggleTile): Boolean {
        val array = readArray()
        for (i in 0 until array.length()) {
            if (array.getJSONObject(i).optString("id") == tile.id) {
                array.put(i, toJson(tile))
                persist(array)
                return true
            }
        }
        return false
    }

    fun removeTile(id: String): Boolean {
        val array = readArray()
        val remaining = JSONArray()
        var found = false
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            if (json.optString("id") == id) {
                found = true
            } else {
                remaining.put(json)
            }
        }
        if (found) {
            sharedPreferences.edit().remove(stateKey(id)).apply()
            persist(remaining)
        }
        return found
    }

    fun getStoredState(id: String): Boolean = sharedPreferences.getBoolean(stateKey(id), false)

    fun setStoredState(id: String, active: Boolean) {
        sharedPreferences.edit().putBoolean(stateKey(id), active).apply()
    }

    fun resolveState(tile: ToggleTile): Boolean {
        if (tile.readCommand.isBlank()) return getStoredState(tile.id)
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", tile.readCommand))
        if (!result.isSuccess) return getStoredState(tile.id)
        val active = result.output.trim().lines().firstOrNull()?.trim() == tile.onValue.trim()
        setStoredState(tile.id, active)
        return active
    }

    fun apply(tile: ToggleTile, active: Boolean): Boolean {
        val command = if (active) tile.onCommand else tile.offCommand
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", command))
        if (result.isSuccess) {
            setStoredState(tile.id, active)
        }
        return result.isSuccess
    }

    private fun stateKey(id: String) = "state_$id"

    private fun persist(array: JSONArray) {
        sharedPreferences.edit().putString(TILES_KEY, array.toString()).apply()
        ToggleTileSlots.sync(appContext, this)
    }

    private fun readArray(): JSONArray {
        return try {
            JSONArray(sharedPreferences.getString(TILES_KEY, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun toJson(tile: ToggleTile): JSONObject = JSONObject().apply {
        put("id", tile.id)
        put("presetKey", tile.presetKey)
        put("label", tile.label)
        put("onCommand", tile.onCommand)
        put("offCommand", tile.offCommand)
        put("readCommand", tile.readCommand)
        put("onValue", tile.onValue)
        put("iconKey", tile.iconKey)
        put("slot", tile.slot)
        put("collapsePanel", tile.collapsePanel)
        put("showToast", tile.showToast)
    }

    private fun fromJson(json: JSONObject): ToggleTile = ToggleTile(
        id = json.getString("id"),
        presetKey = json.optString("presetKey", ToggleTilePresets.CUSTOM_KEY),
        label = json.optString("label"),
        onCommand = json.optString("onCommand"),
        offCommand = json.optString("offCommand"),
        readCommand = json.optString("readCommand"),
        onValue = json.optString("onValue"),
        iconKey = TerminalTileIcons.canonicalKey(json.optString("iconKey", TerminalTileIcons.DEFAULT_KEY)),
        slot = json.optInt("slot", -1),
        collapsePanel = json.optBoolean("collapsePanel", true),
        showToast = json.optBoolean("showToast", true)
    )

    companion object {
        const val MAX_TILE_SLOTS = 12
        private const val TILES_KEY = "tiles_list"
    }
}
