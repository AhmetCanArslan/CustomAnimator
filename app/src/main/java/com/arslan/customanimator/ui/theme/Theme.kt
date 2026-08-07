package com.arslan.customanimator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.view.WindowCompat
import com.arslan.customanimator.utils.SettingsManager

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemeController(
    initialMode: ThemeMode,
    private val onModeChange: (ThemeMode) -> Unit
) {
    var mode by mutableStateOf(initialMode)
        private set

    fun updateMode(value: ThemeMode) {
        mode = value
        onModeChange(value)
    }
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("ThemeController not provided")
}

data class ExtendedColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessLight,
        onSuccessContainer = SuccessLight,
        successContainer = SuccessContainerLight,
        warning = WarningLight,
        onWarningContainer = WarningLight,
        warningContainer = WarningContainerLight
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(
        durationMillis = Motion.durationSlow,
        easing = Motion.emphasizedEasing
    )
    @Composable
    fun anim(color: Color, label: String): Color =
        animateColorAsState(targetValue = color, animationSpec = spec, label = label).value
    return target.copy(
        primary = anim(target.primary, "primary"),
        onPrimary = anim(target.onPrimary, "onPrimary"),
        primaryContainer = anim(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = anim(target.onPrimaryContainer, "onPrimaryContainer"),
        secondary = anim(target.secondary, "secondary"),
        onSecondary = anim(target.onSecondary, "onSecondary"),
        secondaryContainer = anim(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = anim(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = anim(target.tertiary, "tertiary"),
        tertiaryContainer = anim(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = anim(target.onTertiaryContainer, "onTertiaryContainer"),
        background = anim(target.background, "background"),
        onBackground = anim(target.onBackground, "onBackground"),
        surface = anim(target.surface, "surface"),
        onSurface = anim(target.onSurface, "onSurface"),
        surfaceVariant = anim(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = anim(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceContainerLowest = anim(target.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = anim(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = anim(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = anim(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = anim(target.surfaceContainerHighest, "surfaceContainerHighest"),
        outline = anim(target.outline, "outline"),
        outlineVariant = anim(target.outlineVariant, "outlineVariant")
    )
}

@Composable
fun CustomAnimatorTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val controller = remember {
        ThemeController(
            initialMode = SettingsManager.getThemeMode(context),
            onModeChange = { SettingsManager.setThemeMode(context, it) }
        )
    }

    val darkTheme = when (controller.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val useDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val targetScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> MaterialDarkColorScheme
        else -> MaterialLightColorScheme
    }

    val colorScheme = if (LocalInspectionMode.current) targetScheme else animateScheme(targetScheme)

    val extendedColors = if (darkTheme) {
        ExtendedColors(
            success = SuccessDark,
            onSuccessContainer = SuccessDark,
            successContainer = SuccessContainerDark,
            warning = WarningDark,
            onWarningContainer = WarningDark,
            warningContainer = WarningContainerDark
        )
    } else {
        ExtendedColors(
            success = SuccessLight,
            onSuccessContainer = Color(0xFF00210C),
            successContainer = SuccessContainerLight,
            warning = WarningLight,
            onWarningContainer = Color(0xFF2C1600),
            warningContainer = WarningContainerLight
        )
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalThemeController provides controller,
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
