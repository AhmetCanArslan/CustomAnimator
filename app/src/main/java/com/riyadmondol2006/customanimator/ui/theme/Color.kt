package com.riyadmondol2006.customanimator.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import com.riyadmondol2006.customanimator.R

/**
 * The app's own colour identity. Every palette except [WALLPAPER] is generated from a fixed seed,
 * so the app keeps its look regardless of the wallpaper.
 */
enum class AppPalette(val seed: Long, val labelRes: Int) {
    AURORA(0xFF5B5BD6, R.string.palette_aurora),
    OCEAN(0xFF0E8A9A, R.string.palette_ocean),
    EMBER(0xFFE0603A, R.string.palette_ember),
    MEADOW(0xFF4E8F3A, R.string.palette_meadow),
    GRAPHITE(0xFF607080, R.string.palette_graphite),
    WALLPAPER(0xFF5B5BD6, R.string.palette_wallpaper);

    val isAvailable: Boolean
        get() = this != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    companion object {
        val DEFAULT = AURORA

        fun fromName(name: String?): AppPalette =
            entries.firstOrNull { it.name == name && it.isAvailable } ?: DEFAULT
    }
}

private fun AppPalette.dynamicScheme(dark: Boolean): DynamicScheme {
    val hct = Hct.fromInt(seed.toInt())
    return when (this) {
        AppPalette.EMBER -> SchemeVibrant(hct, dark, 0.0)
        AppPalette.GRAPHITE -> SchemeMonochrome(hct, dark, 0.0)
        else -> SchemeTonalSpot(hct, dark, 0.0)
    }
}

fun AppPalette.colorScheme(dark: Boolean): ColorScheme {
    val s = dynamicScheme(dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(s.primary),
        onPrimary = Color(s.onPrimary),
        primaryContainer = Color(s.primaryContainer),
        onPrimaryContainer = Color(s.onPrimaryContainer),
        inversePrimary = Color(s.inversePrimary),
        secondary = Color(s.secondary),
        onSecondary = Color(s.onSecondary),
        secondaryContainer = Color(s.secondaryContainer),
        onSecondaryContainer = Color(s.onSecondaryContainer),
        tertiary = Color(s.tertiary),
        onTertiary = Color(s.onTertiary),
        tertiaryContainer = Color(s.tertiaryContainer),
        onTertiaryContainer = Color(s.onTertiaryContainer),
        background = Color(s.background),
        onBackground = Color(s.onBackground),
        surface = Color(s.surface),
        onSurface = Color(s.onSurface),
        surfaceVariant = Color(s.surfaceVariant),
        onSurfaceVariant = Color(s.onSurfaceVariant),
        surfaceTint = Color(s.surfaceTint),
        inverseSurface = Color(s.inverseSurface),
        inverseOnSurface = Color(s.inverseOnSurface),
        error = Color(s.error),
        onError = Color(s.onError),
        errorContainer = Color(s.errorContainer),
        onErrorContainer = Color(s.onErrorContainer),
        outline = Color(s.outline),
        outlineVariant = Color(s.outlineVariant),
        scrim = Color(s.scrim),
        surfaceBright = Color(s.surfaceBright),
        surfaceContainer = Color(s.surfaceContainer),
        surfaceContainerHigh = Color(s.surfaceContainerHigh),
        surfaceContainerHighest = Color(s.surfaceContainerHighest),
        surfaceContainerLow = Color(s.surfaceContainerLow),
        surfaceContainerLowest = Color(s.surfaceContainerLowest),
        surfaceDim = Color(s.surfaceDim)
    )
}

/** Pushes surfaces to true black for OLED panels while keeping the tonal containers distinguishable. */
fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0F),
    surfaceContainer = Color(0xFF121218),
    surfaceContainerHigh = Color(0xFF1A1A21),
    surfaceContainerHighest = Color(0xFF23232B)
)

val SuccessLight = Color(0xFF1B6B3A)
val SuccessDark = Color(0xFF7DDC9F)
val SuccessContainerLight = Color(0xFFB4F1C8)
val SuccessContainerDark = Color(0xFF0A4021)
val WarningLight = Color(0xFF8A5100)
val WarningDark = Color(0xFFFFB870)
val WarningContainerLight = Color(0xFFFFDDB8)
val WarningContainerDark = Color(0xFF5C3400)
val InfoLight = Color(0xFF00468C)
val InfoDark = Color(0xFF9FCAFF)
val InfoContainerLight = Color(0xFFCFE4FF)
val InfoContainerDark = Color(0xFF00305E)
