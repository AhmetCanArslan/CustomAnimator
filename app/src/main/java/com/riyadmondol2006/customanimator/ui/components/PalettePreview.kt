package com.riyadmondol2006.customanimator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.ui.theme.AppShapes
import com.riyadmondol2006.customanimator.utils.PaletteStyle
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

fun previewScheme(seed: Int, style: PaletteStyle, dark: Boolean, contrast: Double): DynamicScheme {
    val hct = Hct.fromInt(seed)
    return when (style) {
        PaletteStyle.TONAL_SPOT -> SchemeTonalSpot(hct, dark, contrast)
        PaletteStyle.VIBRANT -> SchemeVibrant(hct, dark, contrast)
        PaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, dark, contrast)
        PaletteStyle.SPRITZ -> SchemeNeutral(hct, dark, contrast)
        PaletteStyle.RAINBOW -> SchemeRainbow(hct, dark, contrast)
        PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, dark, contrast)
        PaletteStyle.CONTENT -> SchemeContent(hct, dark, contrast)
        PaletteStyle.MONOCHROMATIC -> SchemeMonochrome(hct, dark, contrast)
    }
}

@Composable
fun PaletteDots(scheme: DynamicScheme, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceContainerHighest).forEach {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(scheme.surfaceContainerLow))
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(it))
            )
        }
    }
}

@Composable
fun PalettePreview(scheme: DynamicScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(Color(scheme.surface))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.iconBadge)
                    .background(Color(scheme.primaryContainer)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(scheme.onPrimaryContainer))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewLine(Color(scheme.onSurface), 0.7f, 10.dp)
                PreviewLine(Color(scheme.onSurfaceVariant), 0.45f, 8.dp)
            }
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 26.dp)
                    .clip(CircleShape)
                    .background(Color(scheme.primary)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(scheme.onPrimary))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                scheme.primaryContainer to scheme.onPrimaryContainer,
                scheme.secondaryContainer to scheme.onSecondaryContainer,
                scheme.tertiaryContainer to scheme.onTertiaryContainer
            ).forEach { (container, content) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Color(container)),
                    contentAlignment = Alignment.Center
                ) {
                    PreviewLine(Color(content), 0.5f, 6.dp)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceContainerHighest, scheme.outline)
                .forEach { Box(modifier = Modifier.weight(1f).height(28.dp).background(Color(it))) }
        }
    }
}

@Composable
private fun PreviewLine(color: Color, fraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(color)
    )
}
