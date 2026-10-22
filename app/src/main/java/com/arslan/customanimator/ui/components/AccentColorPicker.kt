package com.arslan.customanimator.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.R
import com.arslan.customanimator.ui.theme.AccentSeedColors
import com.arslan.customanimator.ui.theme.LocalThemeController

private val RainbowColors = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsvColor(it, 1f, 1f) }

private const val HEX_LENGTH = 6

@Composable
fun AccentColorSelector() {
    val controller = LocalThemeController.current
    val seed = controller.seedColor
    var showCustom by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_color),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = accentDescription(seed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccentSwatch(
                brush = SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                icon = Icons.Filled.AutoAwesome,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                selected = seed == null,
                label = stringResource(autoAccentLabel()),
                onClick = { controller.updateSeedColor(null) }
            )
            AccentSeedColors.forEach { color ->
                AccentSwatch(
                    brush = SolidColor(Color(color)),
                    icon = null,
                    iconTint = contentColorOn(color),
                    selected = seed == color,
                    label = hexOf(color),
                    onClick = { controller.updateSeedColor(color) }
                )
            }
            CustomAccentSwatch(seed = seed, onClick = { showCustom = true })
        }
    }
    if (showCustom) {
        AccentColorDialog(
            initial = seed ?: AccentSeedColors.first(),
            onDismiss = { showCustom = false },
            onConfirm = {
                controller.updateSeedColor(it)
                showCustom = false
            }
        )
    }
}

@Composable
private fun accentDescription(seed: Int?): String =
    if (seed == null) stringResource(autoAccentLabel()) else hexOf(seed)

private fun autoAccentLabel(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.accent_wallpaper else R.string.accent_default

@Composable
private fun CustomAccentSwatch(seed: Int?, onClick: () -> Unit) {
    val custom = seed?.takeIf { it !in AccentSeedColors }
    AccentSwatch(
        brush = if (custom == null) Brush.sweepGradient(RainbowColors) else SolidColor(Color(custom)),
        icon = Icons.Filled.Colorize,
        iconTint = custom?.let { contentColorOn(it) } ?: Color.White,
        selected = custom != null,
        label = stringResource(R.string.accent_custom),
        onClick = onClick
    )
}

@Composable
private fun AccentSwatch(
    brush: Brush,
    icon: ImageVector?,
    iconTint: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val shown = if (selected) Icons.Filled.Check else icon
        if (shown != null) {
            Icon(
                imageVector = shown,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AccentColorDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val start = remember(initial) { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    var hexText by remember { mutableStateOf(hexOf(initial).drop(1)) }
    val color = hsvColor(hue, saturation, brightness)
    val syncHex: () -> Unit = { hexText = hexOf(color.toArgb()).drop(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accent_custom)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(color)
                )
                GradientSlider(
                    label = stringResource(R.string.accent_hue),
                    value = hue,
                    range = 0f..360f,
                    brush = Brush.horizontalGradient(RainbowColors),
                    onValueChange = { hue = it; syncHex() }
                )
                GradientSlider(
                    label = stringResource(R.string.accent_saturation),
                    value = saturation,
                    range = 0f..1f,
                    brush = Brush.horizontalGradient(listOf(hsvColor(hue, 0f, brightness), hsvColor(hue, 1f, brightness))),
                    onValueChange = { saturation = it; syncHex() }
                )
                GradientSlider(
                    label = stringResource(R.string.accent_brightness),
                    value = brightness,
                    range = 0f..1f,
                    brush = Brush.horizontalGradient(listOf(Color.Black, hsvColor(hue, saturation, 1f))),
                    onValueChange = { brightness = it; syncHex() }
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text.filter { it.isLetterOrDigit() }.uppercase().take(HEX_LENGTH)
                        parseHex(hexText)?.let { parsed ->
                            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(parsed, it) }
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    label = { Text(stringResource(R.string.accent_hex)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    isError = parseHex(hexText) == null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(color.toArgb()) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.apply_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun GradientSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

private fun hsvColor(hue: Float, saturation: Float, brightness: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))

private fun contentColorOn(color: Int): Color =
    if (Color(color).luminance() > 0.5f) Color.Black else Color.White

private fun hexOf(color: Int): String = "#%06X".format(color and 0xFFFFFF)

private fun parseHex(text: String): Int? =
    if (text.length == HEX_LENGTH) text.toIntOrNull(16)?.let { it or 0xFF000000.toInt() } else null
