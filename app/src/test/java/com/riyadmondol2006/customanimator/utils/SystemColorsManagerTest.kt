package com.riyadmondol2006.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SystemColorsManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val presetPalette = SystemPalette(0xFF1E88E5.toInt(), false, null, PaletteStyle.VIBRANT)

    @Test
    fun presetPaletteRoundTripsThroughTheSetting() {
        val json = SystemColorsManager.withPalette(null, presetPalette, 42L)
        assertEquals(presetPalette, SystemColorsManager.paletteOf(json))
        val settings = JSONObject(json)
        assertEquals("preset", settings.getString("android.theme.customization.color_source"))
        assertEquals("1E88E5", settings.getString("android.theme.customization.accent_color"))
        assertEquals(42L, settings.getLong("_applied_timestamp"))
    }

    @Test
    fun wallpaperColorKeepsItsIndex() {
        val palette = SystemPalette(0xFF43A047.toInt(), true, 2, PaletteStyle.EXPRESSIVE)
        val json = SystemColorsManager.withPalette(null, palette, 1L)
        assertEquals(palette, SystemColorsManager.paletteOf(json))
        assertEquals("home_wallpaper", JSONObject(json).getString("android.theme.customization.color_source"))
    }

    @Test
    fun automaticWallpaperColorsDropTheSeed() {
        val preset = SystemColorsManager.withPalette(null, presetPalette, 1L)
        val json = SystemColorsManager.withPalette(preset, SystemPalette.WALLPAPER_DEFAULT, 2L)
        val settings = JSONObject(json)
        assertFalse(settings.has("android.theme.customization.system_palette"))
        assertFalse(settings.has("android.theme.customization.color_index"))
        assertEquals(SystemPalette.WALLPAPER_DEFAULT, SystemColorsManager.paletteOf(json))
    }

    @Test
    fun otherCustomizationsAreKept() {
        val existing = """{"android.theme.customization.font":"com.android.theme.font.notoserifsource"}"""
        val json = SystemColorsManager.withPalette(existing, presetPalette, 1L)
        assertEquals("com.android.theme.font.notoserifsource", JSONObject(json).getString("android.theme.customization.font"))
    }

    @Test
    @Config(sdk = [31])
    fun androidTwelveWritesNoStyle() {
        val json = SystemColorsManager.withPalette(null, presetPalette, 1L)
        assertFalse(JSONObject(json).has("android.theme.customization.theme_style"))
    }

    @Test
    fun unreadableSettingFallsBackToWallpaper() {
        assertEquals(SystemPalette.WALLPAPER_DEFAULT, SystemColorsManager.paletteOf("not json"))
        assertEquals(PaletteStyle.TONAL_SPOT, PaletteStyle.fromKey("UNKNOWN"))
    }

    @Test
    fun contrastSnapsToTheClosestLevel() {
        assertEquals(SystemContrast.STANDARD, SystemContrast.closestTo(0.1f))
        assertEquals(SystemContrast.MEDIUM, SystemContrast.closestTo(0.4f))
        assertEquals(SystemContrast.HIGH, SystemContrast.closestTo(0.9f))
    }

    @Test
    fun recentColorsKeepNewestFirstAndSkipPresets() {
        SystemColorsManager.rememberColor(context, 0xFF123456.toInt())
        SystemColorsManager.rememberColor(context, SystemColorsManager.presets.first())
        SystemColorsManager.rememberColor(context, 0xFFABCDEF.toInt())
        SystemColorsManager.rememberColor(context, 0xFF123456.toInt())
        assertEquals(listOf(0xFF123456.toInt(), 0xFFABCDEF.toInt()), SystemColorsManager.recentColors(context))
    }

    @Test
    fun failsSafelyWithoutShizuku() {
        assertEquals(SystemPalette.WALLPAPER_DEFAULT, SystemColorsManager.getPalette())
        assertFalse(SystemColorsManager.applyPalette(context, presetPalette))
        assertFalse(SystemColorsManager.applyContrast(context, SystemContrast.HIGH))
        assertFalse(SystemColorsManager.applyDarkMode(SystemDarkMode.DARK))
        assertTrue(SystemColorsManager.restoreBaseline(context))
        assertNull(SystemColorsManager.recentColors(context).firstOrNull())
    }
}
