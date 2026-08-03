package com.arslan.customanimator.utils

import android.content.Context

class WifiNotesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("wifi_notes_prefs", Context.MODE_PRIVATE)


    fun setNote(ssid: String, note: String) {
        val editor = prefs.edit()
        if (note.isBlank()) editor.remove(key(ssid)) else editor.putString(key(ssid), note.trim())
        editor.apply()
    }

    fun getAllNotes(): Map<String, String> {
        return prefs.all.entries
            .filter { it.key.startsWith(PREFIX) && it.value is String }
            .associate { it.key.removePrefix(PREFIX) to it.value as String }
    }


    private fun key(ssid: String) = PREFIX + ssid

    private companion object {
        const val PREFIX = "note_"
    }
}
