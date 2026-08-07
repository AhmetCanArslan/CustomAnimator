package com.arslan.customanimator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.Motion

data class NavBarItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val contentDescription: String = label
)

@Composable
fun ExpressiveNavBar(
    items: List<NavBarItem>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = MaterialTheme.typography.labelMedium
        val selectedLabel = items.firstOrNull { it.selected }?.label.orEmpty()

        val showLabel = remember(maxWidth, items.size, selectedLabel, labelStyle, density) {
            if (selectedLabel.isEmpty()) {
                false
            } else {
                val labelPx = textMeasurer.measure(selectedLabel, labelStyle).size.width
                val innerWidthPx = with(density) {
                    (maxWidth - OUTER_PADDING * 2 - INNER_PADDING * 2).toPx()
                }
                val spacingPx = with(density) { (CELL_SPACING * (items.size - 1)).toPx() }
                val selectedWeight = SELECTED_WEIGHT / (items.size - 1 + SELECTED_WEIGHT)
                val selectedCellPx = (innerWidthPx - spacingPx) * selectedWeight
                val iconAreaPx = with(density) { (ICON_SIZE + LABEL_GAP + CELL_PADDING * 2).toPx() }
                labelPx + iconAreaPx <= selectedCellPx
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OUTER_PADDING, vertical = 8.dp)
                .clip(AppShapes.chip)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = INNER_PADDING, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavBarCell(
                    item = item,
                    showLabel = showLabel,
                    modifier = Modifier.weight(if (item.selected) SELECTED_WEIGHT else 1f)
                )
            }
        }
    }
}

private val OUTER_PADDING = 12.dp
private val INNER_PADDING = 6.dp
private val CELL_SPACING = 2.dp
private val CELL_PADDING = 6.dp
private val ICON_SIZE = 22.dp
private val LABEL_GAP = 6.dp
private const val SELECTED_WEIGHT = 1.9f

@Composable
private fun NavBarCell(
    item: NavBarItem,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.durationMedium, easing = Motion.emphasizedEasing),
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.durationMedium),
        label = "navContent"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (item.selected) 1.08f else 1f,
        animationSpec = Motion.bouncy(),
        label = "navIconScale"
    )

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(AppShapes.chip)
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = item.onClick
            )
            .padding(horizontal = CELL_PADDING),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription,
            tint = content,
            modifier = Modifier
                .size(ICON_SIZE)
                .scale(iconScale)
        )
        AnimatedVisibility(
            visible = item.selected && showLabel,
            modifier = Modifier.weight(1f, fill = false),
            enter = expandHorizontally(Motion.snappy()) + fadeIn(tween(Motion.durationMedium)),
            exit = shrinkHorizontally(Motion.snappy()) + fadeOut(tween(Motion.durationFast))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
