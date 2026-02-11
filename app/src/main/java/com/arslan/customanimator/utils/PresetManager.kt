package com.arslan.customanimator.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.arslan.customanimator.data.AnimatorPreset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PresetManager(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences("animator_presets", Context.MODE_PRIVATE)
    private val presetsKey = "presets_list"
    
    fun savePreset(
        name: String,
        windowScale: Float,
        transitionScale: Float,
        animatorScale: Float
    ): Boolean {
        return try {
            val presets = getAllPresetsJson()
            val preset = AnimatorPreset(
                id = UUID.randomUUID().toString(),
                name = name,
                windowAnimationScale = windowScale,
                transitionAnimationScale = transitionScale,
                animatorDurationScale = animatorScale
            )
            
            val presetJson = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("windowAnimationScale", preset.windowAnimationScale.toString())
                put("transitionAnimationScale", preset.transitionAnimationScale.toString())
                put("animatorDurationScale", preset.animatorDurationScale.toString())
            }
            
            presets.put(presetJson)
            sharedPreferences.edit().putString(presetsKey, presets.toString()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getPreset(id: String): AnimatorPreset? {
        return try {
            val presets = getAllPresetsJson()
            for (i in 0 until presets.length()) {
                val json = presets.getJSONObject(i)
                if (json.getString("id") == id) {
                    return AnimatorPreset(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        windowAnimationScale = json.getString("windowAnimationScale").toFloat(),
                        transitionAnimationScale = json.getString("transitionAnimationScale").toFloat(),
                        animatorDurationScale = json.getString("animatorDurationScale").toFloat()
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getAllPresets(): List<AnimatorPreset> {
        return try {
            val presets = getAllPresetsJson()
            val list = mutableListOf<AnimatorPreset>()
            for (i in 0 until presets.length()) {
                val json = presets.getJSONObject(i)
                list.add(
                    AnimatorPreset(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        windowAnimationScale = json.getString("windowAnimationScale").toFloat(),
                        transitionAnimationScale = json.getString("transitionAnimationScale").toFloat(),
                        animatorDurationScale = json.getString("animatorDurationScale").toFloat()
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
                // JSONArray doesn't have a direct remove method for older API levels
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
    
    fun updatePreset(
        id: String,
        name: String,
        windowScale: Float,
        transitionScale: Float,
        animatorScale: Float
    ): Boolean {
        return try {
            if (deletePreset(id)) {
                savePreset(name, windowScale, transitionScale, animatorScale)
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
