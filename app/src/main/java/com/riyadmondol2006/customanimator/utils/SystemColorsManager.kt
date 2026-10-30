package com.riyadmondol2006.customanimator.utils

import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import com.riyadmondol2006.customanimator.R
import org.json.JSONObject
import kotlin.math.abs

enum class PaletteStyle(val key: String, val labelRes: Int, val minSdk: Int) {
    TONAL_SPOT("TONAL_SPOT", R.string.palette_style_tonal_spot, Build.VERSION_CODES.TIRAMISU),
    VIBRANT("VIBRANT", R.string.palette_style_vibrant, Build.VERSION_CODES.TIRAMISU),
    EXPRESSIVE("EXPRESSIVE", R.string.palette_style_expressive, Build.VERSION_CODES.TIRAMISU),
    SPRITZ("SPRITZ", R.string.palette_style_spritz, Build.VERSION_CODES.TIRAMISU),
    RAINBOW("RAINBOW", R.string.palette_style_rainbow, Build.VERSION_CODES.TIRAMISU),
    FRUIT_SALAD("FRUIT_SALAD", R.string.palette_style_fruit_salad, Build.VERSION_CODES.TIRAMISU),
    CONTENT("CONTENT", R.string.palette_style_content, Build.VERSION_CODES.TIRAMISU),
    MONOCHROMATIC("MONOCHROMATIC", R.string.palette_style_monochrome, Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

    companion object {
        fun fromKey(key: String?): PaletteStyle = entries.firstOrNull { it.key == key } ?: TONAL_SPOT

        fun available(): List<PaletteStyle> = entries.filter { Build.VERSION.SDK_INT >= it.minSdk }
    }
}

enum class SystemContrast(val level: Float, val labelRes: Int) {
    STANDARD(0f, R.string.system_colors_contrast_standard),
    MEDIUM(0.5f, R.string.system_colors_contrast_medium),
    HIGH(1f, R.string.system_colors_contrast_high);

    companion object {
        fun closestTo(level: Float): SystemContrast = entries.minBy { abs(it.level - level) }
    }
}

enum class SystemDarkMode(val command: String, val nightMode: Int, val labelRes: Int) {
    LIGHT("no", UiModeManager.MODE_NIGHT_NO, R.string.theme_light),
    DARK("yes", UiModeManager.MODE_NIGHT_YES, R.string.theme_dark),
    AUTO("auto", UiModeManager.MODE_NIGHT_AUTO, R.string.system_colors_dark_auto);

    companion object {
        fun fromNightMode(mode: Int): SystemDarkMode? = entries.firstOrNull { it.nightMode == mode }
    }
}

data class SystemPalette(
    val seedColor: Int?,
    val fromWallpaper: Boolean,
    val colorIndex: Int?,
    val style: PaletteStyle
) {
    companion object {
        val WALLPAPER_DEFAULT = SystemPalette(null, true, null, PaletteStyle.TONAL_SPOT)
    }
}

object SystemColorsManager {

    private const val STATE_PREFS = "system_colors_state"
    private const val PREFS_NAME = "system_colors_prefs"
    private const val KEY_RECENT_COLORS = "recent_colors"
    private const val BASELINE_SAVED_PREFIX = "baseline_saved_"
    private const val BASELINE_PREFIX = "baseline_"
    private const val MAX_RECENT_COLORS = 6

    private const val OVERLAY_SETTING = "theme_customization_overlay_packages"
    private const val CONTRAST_SETTING = "contrast_level"
    private const val COLOR_SOURCE = "android.theme.customization.color_source"
    private const val SYSTEM_PALETTE = "android.theme.customization.system_palette"
    private const val ACCENT_COLOR = "android.theme.customization.accent_color"
    private const val COLOR_INDEX = "android.theme.customization.color_index"
    private const val THEME_STYLE = "android.theme.customization.theme_style"
    private const val APPLIED_TIMESTAMP = "_applied_timestamp"
    private const val SOURCE_PRESET = "preset"
    private const val SOURCE_WALLPAPER = "home_wallpaper"
    private const val UNSET_OUTPUT = "null"

    val presets = listOf(
        0xFF6750A4.toInt(),
        0xFF3F51B5.toInt(),
        0xFF1E88E5.toInt(),
        0xFF00897B.toInt(),
        0xFF43A047.toInt(),
        0xFFC0CA33.toInt(),
        0xFFFFB300.toInt(),
        0xFFF4511E.toInt(),
        0xFFE53935.toInt(),
        0xFFD81B60.toInt(),
        0xFF8E24AA.toInt(),
        0xFF6D4C41.toInt()
    )

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun supportsStyles(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun supportsContrast(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun getPalette(): SystemPalette = readSetting(OVERLAY_SETTING)?.let { paletteOf(it) } ?: SystemPalette.WALLPAPER_DEFAULT

    fun applyPalette(context: Context, palette: SystemPalette): Boolean {
        if (!ShizukuHelper.hasShizukuPermission()) return false
        val current = readSetting(OVERLAY_SETTING)
        saveBaseline(context, OVERLAY_SETTING, current)
        return writeSetting(OVERLAY_SETTING, withPalette(current, palette, System.currentTimeMillis()))
    }

    fun getContrast(): SystemContrast =
        SystemContrast.closestTo(readSetting(CONTRAST_SETTING)?.toFloatOrNull() ?: 0f)

    fun applyContrast(context: Context, contrast: SystemContrast): Boolean {
        if (!ShizukuHelper.hasShizukuPermission()) return false
        saveBaseline(context, CONTRAST_SETTING, readSetting(CONTRAST_SETTING))
        return writeSetting(CONTRAST_SETTING, contrast.level.toString())
    }

    fun getDarkMode(context: Context): SystemDarkMode? {
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return null
        return SystemDarkMode.fromNightMode(uiModeManager.nightMode)
    }

    fun applyDarkMode(mode: SystemDarkMode): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("cmd", "uimode", "night", mode.command))

    @RequiresApi(Build.VERSION_CODES.S)
    fun wallpaperColors(context: Context): List<Int> {
        val colors = runCatching {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull() ?: return emptyList()
        return listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
            .map { it.toArgb() or 0xFF000000.toInt() }
            .distinct()
    }

    fun recentColors(context: Context): List<Int> =
        prefs(context).getString(KEY_RECENT_COLORS, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull(16)?.toInt() }
            .orEmpty()

    fun rememberColor(context: Context, color: Int) {
        if (color in presets) return
        val updated = (listOf(color) + recentColors(context).filter { it != color }).take(MAX_RECENT_COLORS)
        prefs(context).edit()
            .putString(KEY_RECENT_COLORS, updated.joinToString(",") { "%08X".format(it) })
            .apply()
    }

    fun restoreBaseline(context: Context): Boolean =
        listOf(OVERLAY_SETTING, CONTRAST_SETTING).map { restoreSetting(context, it) }.all { it }

    fun paletteOf(json: String): SystemPalette {
        val settings = parse(json)
        val fromWallpaper = settings.optString(COLOR_SOURCE) != SOURCE_PRESET
        val seed = settings.optString(SYSTEM_PALETTE).removePrefix("#").toLongOrNull(16)?.toInt()?.or(0xFF000000.toInt())
        val index = if (settings.has(COLOR_INDEX)) settings.optInt(COLOR_INDEX) else null
        return SystemPalette(
            seedColor = seed?.takeIf { !fromWallpaper || index != null },
            fromWallpaper = fromWallpaper,
            colorIndex = index,
            style = PaletteStyle.fromKey(settings.optString(THEME_STYLE))
        )
    }

    fun withPalette(json: String?, palette: SystemPalette, timestamp: Long): String {
        val settings = parse(json)
        settings.put(COLOR_SOURCE, if (palette.fromWallpaper) SOURCE_WALLPAPER else SOURCE_PRESET)
        settings.put(APPLIED_TIMESTAMP, timestamp)
        if (supportsStyles()) settings.put(THEME_STYLE, palette.style.key)
        putOrRemove(settings, COLOR_INDEX, palette.colorIndex)
        val hex = palette.seedColor?.let { "%06X".format(it and 0xFFFFFF) }
        putOrRemove(settings, SYSTEM_PALETTE, hex)
        putOrRemove(settings, ACCENT_COLOR, hex)
        return settings.toString()
    }

    private fun putOrRemove(settings: JSONObject, key: String, value: Any?) {
        if (value == null) settings.remove(key) else settings.put(key, value)
    }

    private fun restoreSetting(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(BASELINE_SAVED_PREFIX + key, false)) return true
        val baseline = prefs.getString(BASELINE_PREFIX + key, null)
        val restored = if (baseline == null) {
            ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "secure", key))
        } else {
            writeSetting(key, baseline)
        }
        if (restored) prefs.edit().remove(BASELINE_SAVED_PREFIX + key).remove(BASELINE_PREFIX + key).apply()
        return restored
    }

    private fun saveBaseline(context: Context, key: String, current: String?) {
        val prefs = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(BASELINE_SAVED_PREFIX + key, false)) return
        prefs.edit().putBoolean(BASELINE_SAVED_PREFIX + key, true).putString(BASELINE_PREFIX + key, current).commit()
    }

    private fun readSetting(key: String): String? {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("settings", "get", "secure", key))
        val output = result.output.trim()
        return if (result.isSuccess && output.isNotEmpty() && output != UNSET_OUTPUT) output else null
    }

    private fun writeSetting(key: String, value: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "secure", key, value))

    private fun parse(json: String?): JSONObject =
        json?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
