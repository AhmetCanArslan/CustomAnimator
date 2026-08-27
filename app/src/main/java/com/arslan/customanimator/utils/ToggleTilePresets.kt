package com.arslan.customanimator.utils

import androidx.annotation.StringRes
import com.arslan.customanimator.R

object ToggleTilePresets {

    const val CUSTOM_KEY = "custom"

    data class Preset(
        val key: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val iconKey: String,
        val onCommand: String,
        val offCommand: String,
        val readCommand: String,
        val onValue: String
    )

    data class Category(@StringRes val titleRes: Int, val presets: List<Preset>)

    private fun setting(
        key: String,
        nameRes: Int,
        descriptionRes: Int,
        iconKey: String,
        namespace: String,
        settingKey: String,
        onValue: String,
        offValue: String
    ) = Preset(
        key = key,
        nameRes = nameRes,
        descriptionRes = descriptionRes,
        iconKey = iconKey,
        onCommand = "settings put $namespace $settingKey $onValue",
        offCommand = "settings put $namespace $settingKey $offValue",
        readCommand = "settings get $namespace $settingKey",
        onValue = onValue
    )

    val categories: List<Category> = listOf(
        Category(
            R.string.qs_tiles_cat_display,
            listOf(
                Preset(
                    key = "grayscale",
                    nameRes = R.string.qs_toggle_grayscale,
                    descriptionRes = R.string.qs_toggle_grayscale_desc,
                    iconKey = "contrast",
                    onCommand = "settings put secure accessibility_display_daltonizer_enabled 1 && settings put secure accessibility_display_daltonizer 0",
                    offCommand = "settings put secure accessibility_display_daltonizer_enabled 0 && settings put secure accessibility_display_daltonizer -1",
                    readCommand = "settings get secure accessibility_display_daltonizer_enabled",
                    onValue = "1"
                ),
                setting(
                    "color_inversion", R.string.qs_toggle_color_inversion, R.string.qs_toggle_color_inversion_desc,
                    "palette", "secure", "accessibility_display_inversion_enabled", "1", "0"
                ),
                Preset(
                    key = "dark_theme",
                    nameRes = R.string.qs_toggle_dark_theme,
                    descriptionRes = R.string.qs_toggle_dark_theme_desc,
                    iconKey = "dark_mode",
                    onCommand = "cmd uimode night yes",
                    offCommand = "cmd uimode night no",
                    readCommand = "settings get secure ui_night_mode",
                    onValue = "2"
                ),
                setting(
                    "night_light", R.string.qs_toggle_night_light, R.string.qs_toggle_night_light_desc,
                    "light_mode", "secure", "night_display_activated", "1", "0"
                ),
                setting(
                    "adaptive_brightness", R.string.qs_toggle_adaptive_brightness, R.string.qs_toggle_adaptive_brightness_desc,
                    "brightness_6", "system", "screen_brightness_mode", "1", "0"
                ),
                setting(
                    "auto_rotate", R.string.qs_toggle_auto_rotate, R.string.qs_toggle_auto_rotate_desc,
                    "screen_rotation", "system", "accelerometer_rotation", "1", "0"
                ),
                setting(
                    "stay_awake", R.string.qs_toggle_stay_awake, R.string.qs_toggle_stay_awake_desc,
                    "battery_charging_full", "global", "stay_on_while_plugged_in", "7", "0"
                )
            )
        ),
        Category(
            R.string.qs_tiles_cat_network,
            listOf(
                Preset(
                    key = "private_dns",
                    nameRes = R.string.qs_toggle_private_dns,
                    descriptionRes = R.string.qs_toggle_private_dns_desc,
                    iconKey = "dns",
                    onCommand = "settings put global private_dns_mode opportunistic",
                    offCommand = "settings put global private_dns_mode off",
                    readCommand = "settings get global private_dns_mode",
                    onValue = "opportunistic"
                ),
                Preset(
                    key = "wifi",
                    nameRes = R.string.qs_toggle_wifi,
                    descriptionRes = R.string.qs_toggle_wifi_desc,
                    iconKey = "wifi",
                    onCommand = "svc wifi enable",
                    offCommand = "svc wifi disable",
                    readCommand = "settings get global wifi_on",
                    onValue = "1"
                ),
                Preset(
                    key = "mobile_data",
                    nameRes = R.string.qs_toggle_mobile_data,
                    descriptionRes = R.string.qs_toggle_mobile_data_desc,
                    iconKey = "signal_cellular_alt",
                    onCommand = "svc data enable",
                    offCommand = "svc data disable",
                    readCommand = "settings get global mobile_data",
                    onValue = "1"
                ),
                Preset(
                    key = "bluetooth",
                    nameRes = R.string.qs_toggle_bluetooth,
                    descriptionRes = R.string.qs_toggle_bluetooth_desc,
                    iconKey = "bluetooth",
                    onCommand = "svc bluetooth enable",
                    offCommand = "svc bluetooth disable",
                    readCommand = "settings get global bluetooth_on",
                    onValue = "1"
                ),
                Preset(
                    key = "nfc",
                    nameRes = R.string.qs_toggle_nfc,
                    descriptionRes = R.string.qs_toggle_nfc_desc,
                    iconKey = "nfc",
                    onCommand = "svc nfc enable",
                    offCommand = "svc nfc disable",
                    readCommand = "",
                    onValue = ""
                ),
                Preset(
                    key = "airplane_mode",
                    nameRes = R.string.qs_toggle_airplane_mode,
                    descriptionRes = R.string.qs_toggle_airplane_mode_desc,
                    iconKey = "airplanemode_active",
                    onCommand = "settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true",
                    offCommand = "settings put global airplane_mode_on 0 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false",
                    readCommand = "settings get global airplane_mode_on",
                    onValue = "1"
                ),
                setting(
                    "wifi_scanning", R.string.qs_toggle_wifi_scanning, R.string.qs_toggle_wifi_scanning_desc,
                    "network_check", "global", "wifi_scan_always_enabled", "1", "0"
                ),
                setting(
                    "ble_scanning", R.string.qs_toggle_ble_scanning, R.string.qs_toggle_ble_scanning_desc,
                    "cast", "global", "ble_scan_always_enabled", "1", "0"
                ),
                setting(
                    "wireless_debugging", R.string.qs_toggle_wireless_debugging, R.string.qs_toggle_wireless_debugging_desc,
                    "lan", "global", "adb_wifi_enabled", "1", "0"
                ),
                setting(
                    "usb_debugging", R.string.qs_toggle_usb_debugging, R.string.qs_toggle_usb_debugging_desc,
                    "usb", "global", "adb_enabled", "1", "0"
                ),
                setting(
                    "location", R.string.qs_toggle_location, R.string.qs_toggle_location_desc,
                    "public", "secure", "location_mode", "3", "0"
                )
            )
        ),
        Category(
            R.string.qs_tiles_cat_performance,
            listOf(
                setting(
                    "dont_keep_activities", R.string.qs_toggle_dont_keep_activities, R.string.qs_toggle_dont_keep_activities_desc,
                    "delete_sweep", "global", "always_finish_activities", "1", "0"
                ),
                Preset(
                    key = "gpu_profile_bars",
                    nameRes = R.string.qs_toggle_gpu_profile_bars,
                    descriptionRes = R.string.qs_toggle_gpu_profile_bars_desc,
                    iconKey = "dashboard",
                    onCommand = "setprop debug.hwui.profile visual_bars",
                    offCommand = "setprop debug.hwui.profile false",
                    readCommand = "getprop debug.hwui.profile",
                    onValue = "visual_bars"
                ),
                Preset(
                    key = "overdraw",
                    nameRes = R.string.qs_toggle_overdraw,
                    descriptionRes = R.string.qs_toggle_overdraw_desc,
                    iconKey = "layers",
                    onCommand = "setprop debug.hwui.overdraw show",
                    offCommand = "setprop debug.hwui.overdraw false",
                    readCommand = "getprop debug.hwui.overdraw",
                    onValue = "show"
                ),
                Preset(
                    key = "layout_bounds",
                    nameRes = R.string.qs_toggle_layout_bounds,
                    descriptionRes = R.string.qs_toggle_layout_bounds_desc,
                    iconKey = "square",
                    onCommand = "setprop debug.layout true && service call activity 1599295570",
                    offCommand = "setprop debug.layout false && service call activity 1599295570",
                    readCommand = "getprop debug.layout",
                    onValue = "true"
                ),
                Preset(
                    key = "disable_hw_overlays",
                    nameRes = R.string.qs_toggle_disable_hw_overlays,
                    descriptionRes = R.string.qs_toggle_disable_hw_overlays_desc,
                    iconKey = "hardware",
                    onCommand = "service call SurfaceFlinger 1008 i32 1",
                    offCommand = "service call SurfaceFlinger 1008 i32 0",
                    readCommand = "",
                    onValue = ""
                ),
                setting(
                    "show_taps", R.string.qs_toggle_show_taps, R.string.qs_toggle_show_taps_desc,
                    "touch_app", "system", "show_touches", "1", "0"
                ),
                setting(
                    "pointer_location", R.string.qs_toggle_pointer_location, R.string.qs_toggle_pointer_location_desc,
                    "gesture", "system", "pointer_location", "1", "0"
                ),
                setting(
                    "freeform_windows", R.string.qs_toggle_freeform_windows, R.string.qs_toggle_freeform_windows_desc,
                    "space_dashboard", "global", "enable_freeform_support", "1", "0"
                ),
                setting(
                    "desktop_mode", R.string.qs_toggle_desktop_mode, R.string.qs_toggle_desktop_mode_desc,
                    "monitor", "global", "force_desktop_mode_on_external_displays", "1", "0"
                ),
                setting(
                    "force_rtl", R.string.qs_toggle_force_rtl, R.string.qs_toggle_force_rtl_desc,
                    "sort", "global", "debug.force_rtl", "1", "0"
                )
            )
        ),
        Category(
            R.string.qs_tiles_cat_system,
            listOf(
                Preset(
                    key = "do_not_disturb",
                    nameRes = R.string.qs_toggle_do_not_disturb,
                    descriptionRes = R.string.qs_toggle_do_not_disturb_desc,
                    iconKey = "do_not_disturb_on",
                    onCommand = "cmd notification set_dnd priority",
                    offCommand = "cmd notification set_dnd off",
                    readCommand = "settings get global zen_mode",
                    onValue = "1"
                ),
                setting(
                    "heads_up", R.string.qs_toggle_heads_up, R.string.qs_toggle_heads_up_desc,
                    "notifications", "global", "heads_up_notifications_enabled", "1", "0"
                ),
                setting(
                    "touch_haptics", R.string.qs_toggle_touch_haptics, R.string.qs_toggle_touch_haptics_desc,
                    "vibration", "system", "haptic_feedback_enabled", "1", "0"
                ),
                setting(
                    "charging_sounds", R.string.qs_toggle_charging_sounds, R.string.qs_toggle_charging_sounds_desc,
                    "volume_up", "global", "charging_sounds_enabled", "1", "0"
                )
            )
        )
    )

    val all: List<Preset> = categories.flatMap { it.presets }

    fun byKey(key: String): Preset? = all.firstOrNull { it.key == key }
}
