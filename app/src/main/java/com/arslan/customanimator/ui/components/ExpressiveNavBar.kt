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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(AppShapes.chip)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            NavBarCell(item = item, modifier = Modifier.weight(if (item.selected) 1.9f else 1f))
        }
    }
}

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
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription,
            tint = content,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale)
        )
        AnimatedVisibility(
            visible = item.selected,
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
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
