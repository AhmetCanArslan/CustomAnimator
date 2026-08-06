package com.arslan.customanimator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Azure10 = Color(0xFF001945)
private val Azure20 = Color(0xFF002C6E)
private val Azure30 = Color(0xFF00429B)
private val Azure40 = Color(0xFF0B57D0)
private val Azure70 = Color(0xFF7BA7FF)
private val Azure80 = Color(0xFFA8C5FF)
private val Azure90 = Color(0xFFD6E2FF)

private val Slate10 = Color(0xFF0D1B33)
private val Slate20 = Color(0xFF23304A)
private val Slate30 = Color(0xFF3A4762)
private val Slate40 = Color(0xFF525F7B)
private val Slate80 = Color(0xFFBAC7E4)
private val Slate90 = Color(0xFFD9E3FF)

private val Coral10 = Color(0xFF3A0018)
private val Coral20 = Color(0xFF5E0029)
private val Coral30 = Color(0xFF85003C)
private val Coral40 = Color(0xFFAD1152)
private val Coral80 = Color(0xFFFFB0C3)
private val Coral90 = Color(0xFFFFD9E1)

private val Neutral4 = Color(0xFF0A0B10)
private val Neutral6 = Color(0xFF0F1116)
private val Neutral10 = Color(0xFF14161C)
private val Neutral12 = Color(0xFF181A20)
private val Neutral17 = Color(0xFF22242B)
private val Neutral20 = Color(0xFF292B31)
private val Neutral22 = Color(0xFF2D2F36)
private val Neutral24 = Color(0xFF313339)
private val Neutral90 = Color(0xFFE1E2E9)
private val Neutral94 = Color(0xFFEFF0F7)
private val Neutral96 = Color(0xFFF5F6FD)
private val Neutral98 = Color(0xFFFAFAFF)
private val Neutral100 = Color(0xFFFFFFFF)

private val NeutralVariant30 = Color(0xFF43474E)
private val NeutralVariant50 = Color(0xFF73777F)
private val NeutralVariant60 = Color(0xFF8D9199)
private val NeutralVariant80 = Color(0xFFC3C7CF)
private val NeutralVariant90 = Color(0xFFDFE2EB)

private val Error10 = Color(0xFF410002)
private val Error20 = Color(0xFF690005)
private val Error30 = Color(0xFF93000A)
private val Error40 = Color(0xFFBA1A1A)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)

val BrandLightColorScheme = lightColorScheme(
    primary = Azure40,
    onPrimary = Neutral100,
    primaryContainer = Azure90,
    onPrimaryContainer = Azure10,
    inversePrimary = Azure80,
    secondary = Slate40,
    onSecondary = Neutral100,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Coral40,
    onTertiary = Neutral100,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Azure40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral96,
    surfaceDim = Color(0xFFDCDCE4),
    surfaceBright = Neutral98,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Color(0xFFE9EAF1),
    surfaceContainerHighest = Neutral90,
    error = Error40,
    onError = Neutral100,
    errorContainer = Error90,
    onErrorContainer = Error10,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color(0xFF000000)
)

val BrandDarkColorScheme = darkColorScheme(
    primary = Azure80,
    onPrimary = Azure20,
    primaryContainer = Azure30,
    onPrimaryContainer = Azure90,
    inversePrimary = Azure40,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Coral80,
    onTertiary = Coral20,
    tertiaryContainer = Coral30,
    onTertiaryContainer = Coral90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Azure80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    surfaceDim = Neutral6,
    surfaceBright = Color(0xFF383A41),
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral12,
    surfaceContainer = Neutral17,
    surfaceContainerHigh = Neutral22,
    surfaceContainerHighest = Neutral24,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color(0xFF000000)
)

val SuccessLight = Color(0xFF1B6B3A)
val SuccessDark = Color(0xFF7DDC9F)
val SuccessContainerLight = Color(0xFFB4F1C8)
val SuccessContainerDark = Color(0xFF0A4021)
val WarningLight = Color(0xFF8A5100)
val WarningDark = Color(0xFFFFB870)
val WarningContainerLight = Color(0xFFFFDDB8)
val WarningContainerDark = Color(0xFF5C3400)
