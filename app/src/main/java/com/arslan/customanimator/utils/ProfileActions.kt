package com.arslan.customanimator.utils

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import com.arslan.customanimator.R

object ProfileActions {

    data class DevAction(
        val key: String,
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val needsShizuku: Boolean,
        val available: () -> Boolean = { true },
        val read: (Context) -> Boolean,
        val write: (Context, Boolean) -> Boolean
    )

    data class BatteryToggle(
        val key: String,
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val read: (Context) -> Boolean,
        val write: (Context, Boolean) -> Boolean
    )

    val devActions: List<DevAction> = listOf(
        DevAction(
            "adb", R.string.usb_debugging, R.string.usb_debugging_desc, false,
            read = { DeveloperOptionsManager.isAdbEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setAdbEnabled(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "adb_wifi", R.string.wireless_debugging, R.string.wireless_debugging_desc, false,
            available = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R },
            read = { DeveloperOptionsManager.isAdbWifiEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setAdbWifiEnabled(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "dont_keep_activities", R.string.dont_keep_activities, R.string.dont_keep_activities_desc, false,
            read = { DeveloperOptionsManager.isAlwaysFinishActivitiesEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setAlwaysFinishActivities(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "limit_background_processes", R.string.limit_background_processes,
            R.string.limit_background_processes_desc, false,
            read = { DeveloperOptionsManager.isBackgroundProcessLimitEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setBackgroundProcessLimit(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "sensors_off", R.string.sensors_off, R.string.sensors_off_desc, true,
            read = { DeveloperOptionsManager.isSensorsOffEnabled() },
            write = { _, v -> DeveloperOptionsManager.setSensorsOff(v) }
        ),
        DevAction(
            "show_touches", R.string.show_taps, R.string.show_taps_desc, false,
            read = { DeveloperOptionsManager.isShowTouchesEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setShowTouches(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "pointer_location", R.string.pointer_location, R.string.pointer_location_desc, false,
            read = { DeveloperOptionsManager.isPointerLocationEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setPointerLocation(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "layout_bounds", R.string.show_layout_bounds, R.string.show_layout_bounds_desc, true,
            read = { DeveloperOptionsManager.isLayoutBoundsEnabled() },
            write = { _, v -> DeveloperOptionsManager.setLayoutBounds(v) }
        ),
        DevAction(
            "gpu_profiling", R.string.gpu_profiling, R.string.gpu_profiling_desc, true,
            read = { DeveloperOptionsManager.isGpuProfilingEnabled() },
            write = { _, v -> DeveloperOptionsManager.setGpuProfiling(v) }
        ),
        DevAction(
            "force_rtl", R.string.force_rtl, R.string.force_rtl_desc, false,
            read = { DeveloperOptionsManager.isForceRtlEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setForceRtl(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "disable_keyboard_animation", R.string.disable_keyboard_animation,
            R.string.disable_keyboard_animation_desc, false,
            read = { DeveloperOptionsManager.isFancyImeAnimationsDisabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setFancyImeAnimations(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "clock_seconds", R.string.show_clock_seconds, R.string.show_clock_seconds_desc, false,
            read = { DeveloperOptionsManager.isClockSecondsEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setClockSeconds(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "auto_rotation", R.string.profile_dev_auto_rotation, R.string.profile_dev_auto_rotation_desc, false,
            read = { DeveloperOptionsManager.isAutoRotationEnabled(it.contentResolver) },
            write = { ctx, v -> DeveloperOptionsManager.setAutoRotation(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "high_volume_warning", R.string.remove_high_volume_warning,
            R.string.remove_high_volume_warning_desc, false,
            read = { DeveloperOptionsManager.isHighVolumeWarningDisabled(it) },
            write = { ctx, v -> DeveloperOptionsManager.setHighVolumeWarningDisabled(ctx, ctx.contentResolver, v) }
        ),
        DevAction(
            "clear_all_app_caches", R.string.clear_all_app_caches, R.string.clear_all_app_caches_desc, true,
            read = { true },
            write = { _, _ -> DeveloperOptionsManager.clearAllAppCaches() }
        ),
        DevAction(
            "close_background_apps", R.string.close_background_apps, R.string.close_background_apps_desc, true,
            read = { true },
            write = { context, _ -> DeveloperOptionsManager.forceStopBackgroundApps(context) }
        ),
        DevAction(
            "compile_all_apps", R.string.compile_all_apps, R.string.compile_all_apps_desc, true,
            read = { true },
            write = { ctx, _ -> DeveloperOptionsManager.compileAllApps(CompileFilterManager.getFilter(ctx)) }
        ),
        DevAction(
            "restart_system_ui", R.string.restart_system_ui, R.string.restart_system_ui_desc, true,
            read = { true },
            write = { _, _ -> DeveloperOptionsManager.restartSystemUi() }
        ),
        DevAction(
            "reset_rotation", R.string.reset_rotation, R.string.reset_rotation_desc, true,
            read = { true },
            write = { context, _ ->
                DeveloperOptionsManager.resetRotation(context, context.contentResolver)
            }
        )
    )

    val batteryToggles: List<BatteryToggle> = listOf(
        BatteryToggle(
            BatteryTweaksManager.KEY_APP_STANDBY, R.string.bt_app_standby, R.string.bt_app_standby_desc,
            read = { BatteryTweaksManager.getGlobalInt(it.contentResolver, BatteryTweaksManager.KEY_APP_STANDBY, 1) == 1 },
            write = { ctx, v ->
                BatteryTweaksManager.putGlobal(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_APP_STANDBY, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_ADAPTIVE_BATTERY, R.string.bt_adaptive_battery, R.string.bt_adaptive_battery_desc,
            read = {
                BatteryTweaksManager.getGlobalInt(it.contentResolver, BatteryTweaksManager.KEY_ADAPTIVE_BATTERY, 1) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putGlobal(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_ADAPTIVE_BATTERY, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_APP_AUTO_RESTRICTION, R.string.bt_app_auto_restriction,
            R.string.bt_app_auto_restriction_desc,
            read = {
                BatteryTweaksManager.getGlobalInt(
                    it.contentResolver, BatteryTweaksManager.KEY_APP_AUTO_RESTRICTION, 1
                ) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putGlobal(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_APP_AUTO_RESTRICTION, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_CACHED_APPS_FREEZER, R.string.bt_cached_freezer, R.string.bt_cached_freezer_desc,
            read = {
                BatteryTweaksManager.getGlobalString(
                    it.contentResolver, BatteryTweaksManager.KEY_CACHED_APPS_FREEZER
                ) != "disabled"
            },
            write = { ctx, v ->
                BatteryTweaksManager.putGlobal(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_CACHED_APPS_FREEZER,
                    if (v) "enabled" else "disabled"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_BLE_SCAN_ALWAYS, R.string.bt_ble_scan, R.string.bt_ble_scan_desc,
            read = {
                BatteryTweaksManager.getGlobalInt(it.contentResolver, BatteryTweaksManager.KEY_BLE_SCAN_ALWAYS, 0) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putGlobal(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_BLE_SCAN_ALWAYS, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_ADAPTIVE_CHARGING, R.string.bt_adaptive_charging, R.string.bt_adaptive_charging_desc,
            read = {
                BatteryTweaksManager.getSecureInt(it.contentResolver, BatteryTweaksManager.KEY_ADAPTIVE_CHARGING, 0) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putSecure(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_ADAPTIVE_CHARGING, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_CHARGING_SOUNDS, R.string.bt_charging_sounds, R.string.bt_charging_sounds_desc,
            read = {
                BatteryTweaksManager.getSecureInt(it.contentResolver, BatteryTweaksManager.KEY_CHARGING_SOUNDS, 1) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putSecure(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_CHARGING_SOUNDS, if (v) "1" else "0"
                )
            }
        ),
        BatteryToggle(
            BatteryTweaksManager.KEY_CHARGING_VIBRATION, R.string.bt_charging_vibration,
            R.string.bt_charging_vibration_desc,
            read = {
                BatteryTweaksManager.getSecureInt(it.contentResolver, BatteryTweaksManager.KEY_CHARGING_VIBRATION, 1) == 1
            },
            write = { ctx, v ->
                BatteryTweaksManager.putSecure(
                    ctx, ctx.contentResolver, BatteryTweaksManager.KEY_CHARGING_VIBRATION, if (v) "1" else "0"
                )
            }
        )
    )

    fun devAction(key: String): DevAction? = devActions.firstOrNull { it.key == key }

    fun batteryToggle(key: String): BatteryToggle? = batteryToggles.firstOrNull { it.key == key }
}
