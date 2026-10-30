package com.arslan.customanimator.utils

import com.arslan.customanimator.data.PresetTileConfig
import org.json.JSONObject

object PresetTileJson {

    const val MAX_TILE_SLOTS = 5

    const val KEY = "tile"

    fun write(target: JSONObject, config: PresetTileConfig?) {
        if (config == null) return
        target.put(KEY, JSONObject().apply {
            put("slot", config.slot)
            put("label", config.label)
            put("showToast", config.showToast)
            put("collapsePanel", config.collapsePanel)
        })
    }

    fun read(source: JSONObject): PresetTileConfig? {
        val json = source.optJSONObject(KEY) ?: return null
        val config = PresetTileConfig(
            slot = json.optInt("slot", -1),
            label = json.optString("label"),
            showToast = json.optBoolean("showToast", true),
            collapsePanel = json.optBoolean("collapsePanel", true)
        )
        return config.takeIf { it.slot in 0 until MAX_TILE_SLOTS }
    }

    fun firstFreeSlot(takenSlots: Collection<Int>): Int? {
        val taken = takenSlots.toSet()
        return (0 until MAX_TILE_SLOTS).firstOrNull { it !in taken }
    }
}
