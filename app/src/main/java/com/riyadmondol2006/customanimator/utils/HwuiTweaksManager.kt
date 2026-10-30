package com.riyadmondol2006.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

object HwuiTweaksManager {

    const val RENDERER_DEFAULT = "default"
    const val RENDERER_SKIA_GL = "skiagl"
    const val RENDERER_SKIA_VK = "skiavk"

    const val TEXTURE_CACHE_DEFAULT = 0

    val textureCacheOptions = listOf(TEXTURE_CACHE_DEFAULT, 24, 48, 72, 96)

    private const val PROP_RENDERER = "debug.hwui.renderer"
    private const val PROP_OVERDRAW = "debug.hwui.overdraw"
    private const val PROP_DIRTY_REGIONS = "debug.hwui.show_dirty_regions"
    private const val PROP_TEXTURE_CACHE = "debug.hwui.texture_cache_size"
    private const val PROP_LAYER_CACHE = "debug.hwui.layer_cache_size"

    private const val PREFS_NAME = "hwui_tweaks"
    private const val KEY_HW_OVERLAYS_DISABLED = "hw_overlays_disabled"
    private const val KEY_HW_OVERLAYS_BOOT = "hw_overlays_boot_count"
    private const val UNKNOWN_BOOT = -1

    private const val SF_DISABLE_OVERLAYS_TXN = "1008"
    private const val SYSPROPS_TRANSACTION = "1599295570"

    private fun getProp(name: String): String {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("getprop", name))
        return if (result.isSuccess) result.output.trim() else ""
    }

    private fun setProp(name: String, value: String): Boolean {
        if (!ShizukuHelper.executeShellCommand(arrayOf("setprop", name, value))) return false
        ShizukuHelper.executeShellCommand(arrayOf("service", "call", "activity", SYSPROPS_TRANSACTION))
        return true
    }

    fun getRenderer(): String {
        return when (val value = getProp(PROP_RENDERER)) {
            RENDERER_SKIA_GL, RENDERER_SKIA_VK -> value
            else -> RENDERER_DEFAULT
        }
    }

    fun setRenderer(renderer: String): Boolean {
        return setProp(PROP_RENDERER, if (renderer == RENDERER_DEFAULT) "" else renderer)
    }

    fun isOverdrawDebugEnabled(): Boolean = getProp(PROP_OVERDRAW) == "show"

    fun setOverdrawDebug(enabled: Boolean): Boolean {
        return setProp(PROP_OVERDRAW, if (enabled) "show" else "false")
    }

    fun isDirtyRegionsEnabled(): Boolean = getProp(PROP_DIRTY_REGIONS) == "true"

    fun setDirtyRegions(enabled: Boolean): Boolean {
        return setProp(PROP_DIRTY_REGIONS, if (enabled) "true" else "false")
    }

    fun getTextureCacheSize(): Int {
        return getProp(PROP_TEXTURE_CACHE).toFloatOrNull()?.toInt() ?: TEXTURE_CACHE_DEFAULT
    }

    fun setTextureCacheSize(sizeMb: Int): Boolean {
        if (sizeMb == TEXTURE_CACHE_DEFAULT) {
            val texture = setProp(PROP_TEXTURE_CACHE, "")
            val layer = setProp(PROP_LAYER_CACHE, "")
            return texture && layer
        }
        val texture = setProp(PROP_TEXTURE_CACHE, sizeMb.toString())
        val layer = setProp(PROP_LAYER_CACHE, (sizeMb / 2).coerceAtLeast(8).toString())
        return texture && layer
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun areHwOverlaysDisabled(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_HW_OVERLAYS_DISABLED, false)) return false
        val appliedAt = prefs.getInt(KEY_HW_OVERLAYS_BOOT, UNKNOWN_BOOT)
        val current = bootCount(context.contentResolver)
        if (appliedAt == UNKNOWN_BOOT || current == UNKNOWN_BOOT || current == appliedAt) return true
        prefs.edit().putBoolean(KEY_HW_OVERLAYS_DISABLED, false).remove(KEY_HW_OVERLAYS_BOOT).apply()
        return false
    }

    private fun bootCount(contentResolver: ContentResolver): Int {
        return try {
            Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, UNKNOWN_BOOT)
        } catch (e: Exception) {
            UNKNOWN_BOOT
        }
    }

    fun setHwOverlaysDisabled(context: Context, disabled: Boolean): Boolean {
        val applied = ShizukuHelper.executeShellCommand(
            arrayOf("service", "call", "SurfaceFlinger", SF_DISABLE_OVERLAYS_TXN, "i32", if (disabled) "1" else "0")
        )
        if (applied) {
            prefs(context).edit()
                .putBoolean(KEY_HW_OVERLAYS_DISABLED, disabled)
                .putInt(KEY_HW_OVERLAYS_BOOT, bootCount(context.contentResolver))
                .apply()
        }
        return applied
    }

    fun resetAll(context: Context): Boolean {
        val renderer = setRenderer(RENDERER_DEFAULT)
        val overdraw = setOverdrawDebug(false)
        val dirty = setDirtyRegions(false)
        val cache = setTextureCacheSize(TEXTURE_CACHE_DEFAULT)
        val overlays = setHwOverlaysDisabled(context, false)
        return renderer && overdraw && dirty && cache && overlays
    }
}
