package com.arslan.customanimator.utils

import android.content.SharedPreferences
import org.json.JSONArray

fun SharedPreferences.getJsonArray(key: String): JSONArray =
    try {
        JSONArray(getString(key, "[]") ?: "[]")
    } catch (e: Exception) {
        JSONArray()
    }
