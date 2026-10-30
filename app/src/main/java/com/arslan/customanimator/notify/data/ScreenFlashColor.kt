package com.arslan.customanimator.notify.data

enum class ScreenFlashColor(val colorArgb: Long, val displayNameRes: Int) {
    RED(0xFFFF1744, com.arslan.customanimator.R.string.pn_flash_screen_color_red),
    GREEN(0xFF00E676, com.arslan.customanimator.R.string.pn_flash_screen_color_green),
    BLUE(0xFF2979FF, com.arslan.customanimator.R.string.pn_flash_screen_color_blue),
    YELLOW(0xFFFFEA00, com.arslan.customanimator.R.string.pn_flash_screen_color_yellow),
    ORANGE(0xFFFF9100, com.arslan.customanimator.R.string.pn_flash_screen_color_orange),
    PURPLE(0xFFD500F9, com.arslan.customanimator.R.string.pn_flash_screen_color_purple),
    CYAN(0xFF00E5FF, com.arslan.customanimator.R.string.pn_flash_screen_color_cyan),
    MAGENTA(0xFFF50057, com.arslan.customanimator.R.string.pn_flash_screen_color_magenta),
    WHITE(0xFFFFFFFF, com.arslan.customanimator.R.string.pn_flash_screen_color_white),
    PINK(0xFFFF80AB, com.arslan.customanimator.R.string.pn_flash_screen_color_pink),
    LIME(0xFFB2FF59, com.arslan.customanimator.R.string.pn_flash_screen_color_lime),
    TEAL(0xFF1DE9B6, com.arslan.customanimator.R.string.pn_flash_screen_color_teal);

    companion object {
        fun fromName(name: String?): ScreenFlashColor =
            entries.firstOrNull { it.name == name } ?: RED
    }
}
