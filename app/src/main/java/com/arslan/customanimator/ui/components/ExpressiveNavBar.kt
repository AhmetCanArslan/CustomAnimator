package com.arslan.customanimator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OUTER_PADDING, vertical = 8.dp)
            .clip(AppShapes.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = INNER_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)
    ) {
        items.forEach { item ->
            NavBarCell(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val OUTER_PADDING = 12.dp
private val INNER_PADDING = 6.dp
private val CELL_SPACING = 2.dp
private val ICON_SIZE = 22.dp

@Composable
private fun NavBarCell(
    item: NavBarItem,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.durationFast),
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (item.selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.durationFast),
        label = "navContent"
    )

    Column(
        modifier = modifier
            .clip(AppShapes.chip)
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = item.onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription,
            tint = content,
            modifier = Modifier.size(ICON_SIZE)
        )
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
