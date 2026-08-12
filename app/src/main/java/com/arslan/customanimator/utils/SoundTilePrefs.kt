package com.arslan.customanimator.utils

import android.content.Context

enum class SoundTileAction {
    VOLUME_PANEL,
    MEDIA_OUTPUT,
    NONE
}

class SoundTilePrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var clickAction: SoundTileAction
        get() = read(KEY_CLICK, SoundTileAction.VOLUME_PANEL)
        set(value) = sp.edit().putString(KEY_CLICK, value.name).apply()

    var longPressAction: SoundTileAction
        get() = read(KEY_LONG_PRESS, SoundTileAction.MEDIA_OUTPUT)
        set(value) = sp.edit().putString(KEY_LONG_PRESS, value.name).apply()

    var collapseDelayMs: Int
        get() = sp.getInt(KEY_COLLAPSE_DELAY, DEFAULT_COLLAPSE_DELAY)
        set(value) = sp.edit().putInt(KEY_COLLAPSE_DELAY, value).apply()

    private fun read(key: String, fallback: SoundTileAction): SoundTileAction {
        val raw = sp.getString(key, null) ?: return fallback
        return runCatching { SoundTileAction.valueOf(raw) }.getOrDefault(fallback)
    }

    companion object {
        private const val NAME = "sound_tile_prefs"
        private const val KEY_CLICK = "click_action"
        private const val KEY_LONG_PRESS = "long_press_action"
        private const val KEY_COLLAPSE_DELAY = "collapse_delay_ms"

        const val COLLAPSE_NEVER = -1
        const val DEFAULT_COLLAPSE_DELAY = 500

        val COLLAPSE_DELAY_OPTIONS = listOf(COLLAPSE_NEVER, 0, 300, 500, 1000)
    }
}
