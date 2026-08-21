package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.arslan.customanimator.widget.GameModeWidgetProvider
import org.json.JSONArray

object GameModeController {

    private const val PREFS_NAME = "game_mode_prefs"
    private const val KEY_ACTIVE = "active"
    private const val KEY_TOUCHED_APPS = "touched_apps"
    private const val KEY_TOUCHED_GAMES = "touched_games"
    private const val KEY_BACKGROUND_LIMIT = "previous_background_limit"
    private const val KEY_MASTER_SYNC = "previous_master_sync"
    private const val KEY_MIN_REFRESH_RATE = "previous_min_refresh_rate"
    private const val KEY_HWUI_APPLIED = "hwui_applied"
    private const val KEY_PREVIOUS_TEXTURE_CACHE = "previous_texture_cache"
    private const val KEY_PREVIOUS_FORCE_GPU = "previous_force_gpu"

    private const val GAME_TEXTURE_CACHE_MB = 96

    private const val MIN_REFRESH_RATE_KEY = "min_refresh_rate"
    private const val NO_SAVED_REFRESH_RATE = -1f

    data class Result(val succeeded: Boolean)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun canApply(context: Context): Boolean = ShizukuHelper.hasShizukuPermission()

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean): Result {
        val result = if (active) enable(context) else disable(context)
        GameModeWidgetProvider.updateAll(context.applicationContext)
        return result
    }

    private fun enable(context: Context): Result {
        val appContext = context.applicationContext
        if (!canApply(appContext)) return Result(false)

        val resolver = appContext.contentResolver
        val games = GameModeManager(appContext).getSelectedPackages()
        val targets = GameModeTargets.resolve(appContext, games)

        var restricted = 0
        fun record(success: Boolean) {
            if (success) restricted++
        }

        val boostHwui = games.isNotEmpty()
        val previousTextureCache = if (boostHwui) HwuiTweaksManager.getTextureCacheSize() else 0
        val previousForceGpu = if (boostHwui) HwuiTweaksManager.isForceGpuRenderingEnabled() else false

        prefs(appContext).edit()
            .putBoolean(KEY_HWUI_APPLIED, boostHwui)
            .putInt(KEY_PREVIOUS_TEXTURE_CACHE, previousTextureCache)
            .putBoolean(KEY_PREVIOUS_FORCE_GPU, previousForceGpu)
            .putBoolean(KEY_BACKGROUND_LIMIT, DeveloperOptionsManager.isBackgroundProcessLimitEnabled(resolver))
            .putBoolean(KEY_MASTER_SYNC, isMasterSyncEnabled())
            .putFloat(KEY_MIN_REFRESH_RATE, currentMinRefreshRate(resolver))
            .putString(KEY_TOUCHED_APPS, encode(targets))
            .putString(KEY_TOUCHED_GAMES, encode(games.toList()))
            .putBoolean(KEY_ACTIVE, true)
            .apply()

        targets.forEach { packageName ->
            record(setStandbyBucket(packageName, "restricted"))
            record(setBackgroundOps(packageName, "ignore"))
        }

        games.forEach { packageName -> setGameMode(packageName, "performance") }

        if (boostHwui) {
            HwuiTweaksManager.setTextureCacheSize(GAME_TEXTURE_CACHE_MB)
            HwuiTweaksManager.setForceGpuRendering(true)
            games.forEach { packageName -> forceStop(packageName) }
        }

        DeveloperOptionsManager.setBackgroundProcessLimit(appContext, resolver, true)
        setMasterSync(false)
        CaffeineManager.setActive(appContext, resolver, true)
        peakRefreshRate(appContext)?.let { rate -> writeMinRefreshRate(rate.toString()) }

        DeveloperOptionsManager.clearAllAppCaches()
        ShizukuHelper.executeShellCommand(arrayOf("am", "compact", "all", "full"))
        val fixedPerformanceEnabled = setFixedPerformanceMode(true)

        return Result((targets.isEmpty() || restricted > 0) && fixedPerformanceEnabled)
    }

    private fun disable(context: Context): Result {
        val appContext = context.applicationContext
        if (!canApply(appContext)) return Result(false)

        val resolver = appContext.contentResolver
        val prefs = prefs(appContext)
        val targets = decode(prefs.getString(KEY_TOUCHED_APPS, null))
        val games = decode(prefs.getString(KEY_TOUCHED_GAMES, null))

        var restored = 0
        fun record(success: Boolean) {
            if (success) restored++
        }

        targets.forEach { packageName ->
            record(setStandbyBucket(packageName, "active"))
            record(setBackgroundOps(packageName, "allow"))
        }

        games.forEach { packageName -> setGameMode(packageName, "standard") }

        if (prefs.getBoolean(KEY_HWUI_APPLIED, false)) {
            HwuiTweaksManager.setTextureCacheSize(prefs.getInt(KEY_PREVIOUS_TEXTURE_CACHE, HwuiTweaksManager.TEXTURE_CACHE_DEFAULT))
            HwuiTweaksManager.setForceGpuRendering(prefs.getBoolean(KEY_PREVIOUS_FORCE_GPU, false))
        }

        DeveloperOptionsManager.setBackgroundProcessLimit(
            appContext, resolver, prefs.getBoolean(KEY_BACKGROUND_LIMIT, false)
        )
        setMasterSync(prefs.getBoolean(KEY_MASTER_SYNC, true))
        CaffeineManager.setActive(appContext, resolver, false)

        val previousRate = prefs.getFloat(KEY_MIN_REFRESH_RATE, NO_SAVED_REFRESH_RATE)
        if (previousRate <= 0f) {
            ShizukuHelper.executeShellCommand(arrayOf("settings", "delete", "system", MIN_REFRESH_RATE_KEY))
        } else {
            writeMinRefreshRate(previousRate.toString())
        }

        val fixedPerformanceDisabled = setFixedPerformanceMode(false)

        prefs.edit()
            .remove(KEY_TOUCHED_APPS)
            .remove(KEY_TOUCHED_GAMES)
            .remove(KEY_BACKGROUND_LIMIT)
            .remove(KEY_MASTER_SYNC)
            .remove(KEY_MIN_REFRESH_RATE)
            .remove(KEY_HWUI_APPLIED)
            .remove(KEY_PREVIOUS_TEXTURE_CACHE)
            .remove(KEY_PREVIOUS_FORCE_GPU)
            .putBoolean(KEY_ACTIVE, false)
            .apply()

        return Result((targets.isEmpty() || restored > 0) && fixedPerformanceDisabled)
    }

    private fun forceStop(packageName: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("am", "force-stop", packageName))

    private fun setStandbyBucket(packageName: String, bucket: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("am", "set-standby-bucket", packageName, bucket))

    private fun setBackgroundOps(packageName: String, mode: String): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "appops", "set", packageName, "RUN_ANY_IN_BACKGROUND", mode)
        )

    private fun setGameMode(packageName: String, mode: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "game", "mode", mode, packageName))
    }

    private fun setFixedPerformanceMode(enabled: Boolean): Boolean =
        ShizukuHelper.executeShellCommand(
            arrayOf("cmd", "power", "set-fixed-performance-mode-enabled", enabled.toString())
        )

    private fun isMasterSyncEnabled(): Boolean = try {
        ContentResolver.getMasterSyncAutomatically()
    } catch (e: Exception) {
        true
    }

    private fun setMasterSync(enabled: Boolean): Boolean = try {
        ContentResolver.setMasterSyncAutomatically(enabled)
        true
    } catch (e: Exception) {
        false
    }

    private fun currentMinRefreshRate(resolver: ContentResolver): Float = try {
        Settings.System.getFloat(resolver, MIN_REFRESH_RATE_KEY, NO_SAVED_REFRESH_RATE)
    } catch (e: Exception) {
        NO_SAVED_REFRESH_RATE
    }

    private fun writeMinRefreshRate(value: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("settings", "put", "system", MIN_REFRESH_RATE_KEY, value))

    private fun peakRefreshRate(context: Context): Float? {
        return try {
            val display = context.getSystemService(DisplayManager::class.java)
                ?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
            display.supportedModes.maxOfOrNull { it.refreshRate }?.takeIf { it > 0f }
        } catch (e: Exception) {
            null
        }
    }

    private fun encode(packages: List<String>): String {
        val array = JSONArray()
        packages.forEach { array.put(it) }
        return array.toString()
    }

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
