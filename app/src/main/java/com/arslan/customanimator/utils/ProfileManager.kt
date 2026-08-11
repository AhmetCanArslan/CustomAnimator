package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.Profile
import com.arslan.customanimator.data.ProfileAnimation
import com.arslan.customanimator.data.ProfileBattery
import com.arslan.customanimator.data.ProfileTileConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileManager(context: Context) {

    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val profilesKey = "profiles_list"

    fun getAllProfiles(): List<Profile> {
        return try {
            val array = getArray()
            val list = mutableListOf<Profile>()
            for (i in 0 until array.length()) {
                list.add(fromJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getProfile(id: String): Profile? = getAllProfiles().firstOrNull { it.id == id }

    fun newId(): String = UUID.randomUUID().toString()

    fun saveProfile(profile: Profile): Boolean {
        return try {
            val array = getArray()
            var index = -1
            for (i in 0 until array.length()) {
                if (array.getJSONObject(i).getString("id") == profile.id) {
                    index = i
                    break
                }
            }

            val resolved = resolveTileSlot(profile)
            val json = toJson(resolved)
            if (index >= 0) array.put(index, json) else array.put(json)
            persist(array)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteProfile(id: String): Boolean {
        return try {
            val array = getArray()
            val remaining = JSONArray()
            var found = false
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                if (json.getString("id") == id) found = true else remaining.put(json)
            }
            if (found) persist(remaining)
            found
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getProfileForSlot(slot: Int): Profile? =
        getAllProfiles().firstOrNull { it.tile?.slot == slot }

    fun firstFreeSlot(excludingProfileId: String? = null): Int? {
        val taken = getAllProfiles()
            .filter { it.id != excludingProfileId }
            .mapNotNull { it.tile?.slot }
            .toSet()
        return (0 until MAX_TILE_SLOTS).firstOrNull { it !in taken }
    }

    private fun resolveTileSlot(profile: Profile): Profile {
        val tile = profile.tile ?: return profile
        if (tile.slot in 0 until MAX_TILE_SLOTS) return profile
        val free = firstFreeSlot(excludingProfileId = profile.id) ?: return profile.copy(tile = null)
        return profile.copy(tile = tile.copy(slot = free))
    }

    private fun persist(array: JSONArray) {
        sharedPreferences.edit().putString(profilesKey, array.toString()).apply()
        ProfileTileSlots.sync(appContext, this)
    }

    private fun toJson(profile: Profile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("iconKey", profile.iconKey)
        profile.animation?.let {
            put("animation", JSONObject().apply {
                put("window", it.windowAnimationScale.toString())
                put("transition", it.transitionAnimationScale.toString())
                put("animator", it.animatorDurationScale.toString())
            })
        }
        profile.smallestWidthDp?.let { put("smallestWidthDp", it) }
        profile.battery?.let { battery ->
            put("battery", JSONObject().apply {
                battery.saverPresetId?.let { put("saverPreset", it) }
                battery.dozePresetId?.let { put("dozePreset", it) }
                battery.batterySaverOn?.let { put("batterySaverOn", it) }
                battery.triggerLevel?.let { put("triggerLevel", it) }
                battery.automaticPowerSaveMode?.let { put("automaticPowerSaveMode", it) }
                battery.sticky?.let { put("sticky", it) }
                battery.stickyAutoDisable?.let { put("stickyAutoDisable", it) }
                battery.stickyAutoDisableLevel?.let { put("stickyAutoDisableLevel", it) }
                if (battery.policy.isNotEmpty()) {
                    put("policy", JSONObject().apply {
                        battery.policy.forEach { (key, value) -> put(key, value) }
                    })
                }
                if (battery.toggles.isNotEmpty()) {
                    put("toggles", JSONObject().apply {
                        battery.toggles.forEach { (key, value) -> put(key, value) }
                    })
                }
            })
        }
        if (profile.developer.isNotEmpty()) {
            put("developer", JSONObject().apply {
                profile.developer.forEach { (key, value) -> put(key, value) }
            })
        }
        profile.tile?.let { tile ->
            put("tile", JSONObject().apply {
                put("slot", tile.slot)
                put("label", tile.label)
                put("showToast", tile.showToast)
                put("collapsePanel", tile.collapsePanel)
            })
        }
    }

    private fun fromJson(json: JSONObject): Profile {
        val animation = json.optJSONObject("animation")?.let {
            ProfileAnimation(
                windowAnimationScale = it.optString("window", "1").toFloatOrNull() ?: 1f,
                transitionAnimationScale = it.optString("transition", "1").toFloatOrNull() ?: 1f,
                animatorDurationScale = it.optString("animator", "1").toFloatOrNull() ?: 1f
            )
        }

        val battery = json.optJSONObject("battery")?.let { obj ->
            ProfileBattery(
                saverPresetId = obj.optString("saverPreset").takeIf { it.isNotBlank() },
                dozePresetId = obj.optString("dozePreset").takeIf { it.isNotBlank() },
                batterySaverOn = if (obj.has("batterySaverOn")) obj.optBoolean("batterySaverOn") else null,
                triggerLevel = if (obj.has("triggerLevel")) obj.optInt("triggerLevel") else null,
                automaticPowerSaveMode = if (obj.has("automaticPowerSaveMode")) {
                    obj.optInt("automaticPowerSaveMode")
                } else {
                    null
                },
                sticky = if (obj.has("sticky")) obj.optBoolean("sticky") else null,
                stickyAutoDisable = if (obj.has("stickyAutoDisable")) {
                    obj.optBoolean("stickyAutoDisable")
                } else {
                    null
                },
                stickyAutoDisableLevel = if (obj.has("stickyAutoDisableLevel")) {
                    obj.optInt("stickyAutoDisableLevel")
                } else {
                    null
                },
                policy = obj.optJSONObject("policy")?.let { readStringMap(it) } ?: emptyMap(),
                toggles = obj.optJSONObject("toggles")?.let { readBooleanMap(it) } ?: emptyMap()
            )
        }?.takeIf { !it.isEmpty }

        val developer = json.optJSONObject("developer")?.let { readBooleanMap(it) } ?: emptyMap()

        val tile = json.optJSONObject("tile")?.let {
            ProfileTileConfig(
                slot = it.optInt("slot", -1),
                label = it.optString("label"),
                showToast = it.optBoolean("showToast", true),
                collapsePanel = it.optBoolean("collapsePanel", true)
            )
        }?.takeIf { it.slot in 0 until MAX_TILE_SLOTS }

        return Profile(
            id = json.getString("id"),
            name = json.optString("name"),
            iconKey = TerminalTileIcons.canonicalKey(json.optString("iconKey", DEFAULT_ICON_KEY)),
            animation = animation,
            smallestWidthDp = if (json.has("smallestWidthDp")) json.optInt("smallestWidthDp") else null,
            battery = battery,
            developer = developer,
            tile = tile
        )
    }

    private fun readBooleanMap(json: JSONObject): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.optBoolean(key)
        }
        return map
    }

    private fun readStringMap(json: JSONObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.optString(key)
        }
        return map
    }

    private fun getArray(): JSONArray {
        return try {
            JSONArray(sharedPreferences.getString(profilesKey, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }

    companion object {
        const val MAX_TILE_SLOTS = 5
        const val DEFAULT_ICON_KEY = "tune"
    }
}
