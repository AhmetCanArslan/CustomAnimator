package com.arslan.customanimator.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arslan.customanimator.R

object TerminalTileIcons {

    const val DEFAULT_KEY = "terminal"

    data class Category(@StringRes val titleRes: Int, val keys: List<String>)

    val categories: List<Category> = listOf(
        Category(
            R.string.terminal_tile_cat_terminal,
            listOf(
                "terminal", "code", "data_object", "dns", "api", "bug_report", "build",
                "construction", "handyman", "developer_mode", "developer_board",
                "integration_instructions", "http", "webhook", "token", "tag"
            )
        ),
        Category(
            R.string.terminal_tile_cat_power,
            listOf(
                "bolt", "flash_on", "speed", "rocket_launch", "electric_bolt", "battery_full",
                "battery_charging_full", "battery_saver", "power_settings_new",
                "energy_savings_leaf", "whatshot", "local_fire_department", "thermostat",
                "ac_unit", "timer", "timelapse", "hourglass_bottom"
            )
        ),
        Category(
            R.string.terminal_tile_cat_system,
            listOf(
                "settings", "settings_suggest", "tune", "memory", "storage", "sd_storage",
                "hardware", "cable", "router", "dashboard", "space_dashboard", "widgets",
                "extension", "layers", "schema", "account_tree"
            )
        ),
        Category(
            R.string.terminal_tile_cat_apps,
            listOf(
                "apps", "android", "app_settings_alt", "inventory_2", "folder", "folder_open",
                "archive", "backup", "cloud_upload", "cloud_download", "cached", "sync", "restore",
                "restart_alt", "refresh", "update", "system_update", "install_mobile",
                "delete_sweep", "cleaning_services", "delete", "recycling"
            )
        ),
        Category(
            R.string.terminal_tile_cat_network,
            listOf(
                "wifi", "wifi_off", "signal_cellular_alt", "bluetooth", "network_check", "lan",
                "public", "vpn_key", "vpn_lock", "airplanemode_active", "nfc", "cast", "usb"
            )
        ),
        Category(
            R.string.terminal_tile_cat_display,
            listOf(
                "dark_mode", "light_mode", "brightness_6", "contrast", "palette", "wallpaper",
                "animation", "movie", "videocam", "photo_camera", "screenshot", "screen_rotation",
                "smartphone", "tablet_android", "tv", "monitor", "vibration", "volume_up",
                "volume_off", "notifications", "notifications_off", "do_not_disturb_on"
            )
        ),
        Category(
            R.string.terminal_tile_cat_security,
            listOf(
                "lock", "lock_open", "shield", "security", "gpp_good", "privacy_tip",
                "fingerprint", "key", "password", "admin_panel_settings", "visibility",
                "visibility_off", "block", "report", "warning"
            )
        ),
        Category(
            R.string.terminal_tile_cat_actions,
            listOf(
                "play_arrow", "stop", "pause", "skip_next", "bookmark", "star", "favorite", "flag",
                "push_pin", "check_circle", "cancel", "add", "remove", "edit", "search",
                "filter_alt", "sort", "send", "launch", "open_in_new", "touch_app", "gesture",
                "mouse", "keyboard"
            )
        ),
        Category(
            R.string.terminal_tile_cat_labels,
            listOf(
                "circle", "square", "hexagon", "pentagon", "label", "local_offer", "numbers",
                "abc"
            )
        )
    )

    val keys: List<String> = categories.flatMap { it.keys }

    private val icons: Map<String, Int> = mapOf(
        "terminal" to R.drawable.ic_tile_terminal,
        "code" to R.drawable.ic_tile_code,
        "data_object" to R.drawable.ic_tile_data_object,
        "dns" to R.drawable.ic_tile_dns,
        "api" to R.drawable.ic_tile_api,
        "bug_report" to R.drawable.ic_tile_bug_report,
        "build" to R.drawable.ic_tile_build,
        "construction" to R.drawable.ic_tile_construction,
        "handyman" to R.drawable.ic_tile_handyman,
        "developer_mode" to R.drawable.ic_tile_developer_mode,
        "developer_board" to R.drawable.ic_tile_developer_board,
        "integration_instructions" to R.drawable.ic_tile_integration_instructions,
        "http" to R.drawable.ic_tile_http,
        "webhook" to R.drawable.ic_tile_webhook,
        "token" to R.drawable.ic_tile_token,
        "tag" to R.drawable.ic_tile_tag,
        "bolt" to R.drawable.ic_tile_bolt,
        "flash_on" to R.drawable.ic_tile_flash_on,
        "speed" to R.drawable.ic_tile_speed,
        "rocket_launch" to R.drawable.ic_tile_rocket_launch,
        "electric_bolt" to R.drawable.ic_tile_electric_bolt,
        "battery_full" to R.drawable.ic_tile_battery_full,
        "battery_charging_full" to R.drawable.ic_tile_battery_charging_full,
        "battery_saver" to R.drawable.ic_tile_battery_saver,
        "power_settings_new" to R.drawable.ic_tile_power_settings_new,
        "energy_savings_leaf" to R.drawable.ic_tile_energy_savings_leaf,
        "whatshot" to R.drawable.ic_tile_whatshot,
        "local_fire_department" to R.drawable.ic_tile_local_fire_department,
        "thermostat" to R.drawable.ic_tile_thermostat,
        "ac_unit" to R.drawable.ic_tile_ac_unit,
        "timer" to R.drawable.ic_tile_timer,
        "timelapse" to R.drawable.ic_tile_timelapse,
        "hourglass_bottom" to R.drawable.ic_tile_hourglass_bottom,
        "settings" to R.drawable.ic_tile_settings,
        "settings_suggest" to R.drawable.ic_tile_settings_suggest,
        "tune" to R.drawable.ic_tile_tune,
        "memory" to R.drawable.ic_tile_memory,
        "storage" to R.drawable.ic_tile_storage,
        "sd_storage" to R.drawable.ic_tile_sd_storage,
        "hardware" to R.drawable.ic_tile_hardware,
        "cable" to R.drawable.ic_tile_cable,
        "router" to R.drawable.ic_tile_router,
        "dashboard" to R.drawable.ic_tile_dashboard,
        "space_dashboard" to R.drawable.ic_tile_space_dashboard,
        "widgets" to R.drawable.ic_tile_widgets,
        "extension" to R.drawable.ic_tile_extension,
        "layers" to R.drawable.ic_tile_layers,
        "schema" to R.drawable.ic_tile_schema,
        "account_tree" to R.drawable.ic_tile_account_tree,
        "apps" to R.drawable.ic_tile_apps,
        "android" to R.drawable.ic_tile_android,
        "app_settings_alt" to R.drawable.ic_tile_app_settings_alt,
        "inventory_2" to R.drawable.ic_tile_inventory_2,
        "folder" to R.drawable.ic_tile_folder,
        "folder_open" to R.drawable.ic_tile_folder_open,
        "archive" to R.drawable.ic_tile_archive,
        "backup" to R.drawable.ic_tile_backup,
        "cloud_upload" to R.drawable.ic_tile_cloud_upload,
        "cloud_download" to R.drawable.ic_tile_cloud_download,
        "cached" to R.drawable.ic_tile_cached,
        "sync" to R.drawable.ic_tile_sync,
        "restore" to R.drawable.ic_tile_restore,
        "restart_alt" to R.drawable.ic_tile_restart_alt,
        "refresh" to R.drawable.ic_tile_refresh,
        "update" to R.drawable.ic_tile_update,
        "system_update" to R.drawable.ic_tile_system_update,
        "install_mobile" to R.drawable.ic_tile_install_mobile,
        "delete_sweep" to R.drawable.ic_tile_delete_sweep,
        "cleaning_services" to R.drawable.ic_tile_cleaning_services,
        "delete" to R.drawable.ic_tile_delete,
        "recycling" to R.drawable.ic_tile_recycling,
        "wifi" to R.drawable.ic_tile_wifi,
        "wifi_off" to R.drawable.ic_tile_wifi_off,
        "signal_cellular_alt" to R.drawable.ic_tile_signal_cellular_alt,
        "bluetooth" to R.drawable.ic_tile_bluetooth,
        "network_check" to R.drawable.ic_tile_network_check,
        "lan" to R.drawable.ic_tile_lan,
        "public" to R.drawable.ic_tile_public,
        "vpn_key" to R.drawable.ic_tile_vpn_key,
        "vpn_lock" to R.drawable.ic_tile_vpn_lock,
        "airplanemode_active" to R.drawable.ic_tile_airplanemode_active,
        "nfc" to R.drawable.ic_tile_nfc,
        "cast" to R.drawable.ic_tile_cast,
        "usb" to R.drawable.ic_tile_usb,
        "dark_mode" to R.drawable.ic_tile_dark_mode,
        "light_mode" to R.drawable.ic_tile_light_mode,
        "brightness_6" to R.drawable.ic_tile_brightness_6,
        "contrast" to R.drawable.ic_tile_contrast,
        "palette" to R.drawable.ic_tile_palette,
        "wallpaper" to R.drawable.ic_tile_wallpaper,
        "animation" to R.drawable.ic_tile_animation,
        "movie" to R.drawable.ic_tile_movie,
        "videocam" to R.drawable.ic_tile_videocam,
        "photo_camera" to R.drawable.ic_tile_photo_camera,
        "screenshot" to R.drawable.ic_tile_screenshot,
        "screen_rotation" to R.drawable.ic_tile_screen_rotation,
        "smartphone" to R.drawable.ic_tile_smartphone,
        "tablet_android" to R.drawable.ic_tile_tablet_android,
        "tv" to R.drawable.ic_tile_tv,
        "monitor" to R.drawable.ic_tile_monitor,
        "vibration" to R.drawable.ic_tile_vibration,
        "volume_up" to R.drawable.ic_tile_volume_up,
        "volume_off" to R.drawable.ic_tile_volume_off,
        "notifications" to R.drawable.ic_tile_notifications,
        "notifications_off" to R.drawable.ic_tile_notifications_off,
        "do_not_disturb_on" to R.drawable.ic_tile_do_not_disturb_on,
        "lock" to R.drawable.ic_tile_lock,
        "lock_open" to R.drawable.ic_tile_lock_open,
        "shield" to R.drawable.ic_tile_shield,
        "security" to R.drawable.ic_tile_security,
        "gpp_good" to R.drawable.ic_tile_gpp_good,
        "privacy_tip" to R.drawable.ic_tile_privacy_tip,
        "fingerprint" to R.drawable.ic_tile_fingerprint,
        "key" to R.drawable.ic_tile_key,
        "password" to R.drawable.ic_tile_password,
        "admin_panel_settings" to R.drawable.ic_tile_admin_panel_settings,
        "visibility" to R.drawable.ic_tile_visibility,
        "visibility_off" to R.drawable.ic_tile_visibility_off,
        "block" to R.drawable.ic_tile_block,
        "report" to R.drawable.ic_tile_report,
        "warning" to R.drawable.ic_tile_warning,
        "play_arrow" to R.drawable.ic_tile_play_arrow,
        "stop" to R.drawable.ic_tile_stop,
        "pause" to R.drawable.ic_tile_pause,
        "skip_next" to R.drawable.ic_tile_skip_next,
        "bookmark" to R.drawable.ic_tile_bookmark,
        "star" to R.drawable.ic_tile_star,
        "favorite" to R.drawable.ic_tile_favorite,
        "flag" to R.drawable.ic_tile_flag,
        "push_pin" to R.drawable.ic_tile_push_pin,
        "check_circle" to R.drawable.ic_tile_check_circle,
        "cancel" to R.drawable.ic_tile_cancel,
        "add" to R.drawable.ic_tile_add,
        "remove" to R.drawable.ic_tile_remove,
        "edit" to R.drawable.ic_tile_edit,
        "search" to R.drawable.ic_tile_search,
        "filter_alt" to R.drawable.ic_tile_filter_alt,
        "sort" to R.drawable.ic_tile_sort,
        "send" to R.drawable.ic_tile_send,
        "launch" to R.drawable.ic_tile_launch,
        "open_in_new" to R.drawable.ic_tile_open_in_new,
        "touch_app" to R.drawable.ic_tile_touch_app,
        "gesture" to R.drawable.ic_tile_gesture,
        "mouse" to R.drawable.ic_tile_mouse,
        "keyboard" to R.drawable.ic_tile_keyboard,
        "circle" to R.drawable.ic_tile_circle,
        "square" to R.drawable.ic_tile_square,
        "hexagon" to R.drawable.ic_tile_hexagon,
        "pentagon" to R.drawable.ic_tile_pentagon,
        "label" to R.drawable.ic_tile_label,
        "local_offer" to R.drawable.ic_tile_local_offer,
        "numbers" to R.drawable.ic_tile_numbers,
        "abc" to R.drawable.ic_tile_abc
    )

    private val legacyKeys: Map<String, String> = mapOf(
        "play" to "play_arrow",
        "power" to "power_settings_new"
    )

    @DrawableRes
    fun resFor(key: String): Int {
        val resolved = legacyKeys[key] ?: key
        return icons[resolved] ?: icons.getValue(DEFAULT_KEY)
    }

    fun canonicalKey(key: String): String {
        val resolved = legacyKeys[key] ?: key
        return if (resolved in icons) resolved else DEFAULT_KEY
    }
}
