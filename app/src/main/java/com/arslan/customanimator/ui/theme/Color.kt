package com.arslan.customanimator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeFidelity

val MaterialLightColorScheme = lightColorScheme()

val MaterialDarkColorScheme = darkColorScheme()

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

val AccentSeedColors = listOf(
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

fun seedColorScheme(seed: Int, dark: Boolean): ColorScheme {
    val scheme = SchemeFidelity(Hct.fromInt(seed), dark, 0.0)
    return ColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        inversePrimary = Color(scheme.inversePrimary),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceTint = Color(scheme.surfaceTint),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        surfaceDim = Color(scheme.surfaceDim),
        primaryFixed = Color(scheme.primaryFixed),
        primaryFixedDim = Color(scheme.primaryFixedDim),
        onPrimaryFixed = Color(scheme.onPrimaryFixed),
        onPrimaryFixedVariant = Color(scheme.onPrimaryFixedVariant),
        secondaryFixed = Color(scheme.secondaryFixed),
        secondaryFixedDim = Color(scheme.secondaryFixedDim),
        onSecondaryFixed = Color(scheme.onSecondaryFixed),
        onSecondaryFixedVariant = Color(scheme.onSecondaryFixedVariant),
        tertiaryFixed = Color(scheme.tertiaryFixed),
        tertiaryFixedDim = Color(scheme.tertiaryFixedDim),
        onTertiaryFixed = Color(scheme.onTertiaryFixed),
        onTertiaryFixedVariant = Color(scheme.onTertiaryFixedVariant)
    )
}
