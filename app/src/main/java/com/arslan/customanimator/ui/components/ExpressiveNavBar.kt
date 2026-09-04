package com.arslan.customanimator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.Motion

@Immutable
data class NavBarItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

@Immutable
data class NavBarItems(val items: List<NavBarItem>)

@Composable
fun ExpressiveTopNavBar(
    items: NavBarItems,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val info = snapshotFlow { listState.layoutInfo }
            .first { it.visibleItemsInfo.isNotEmpty() }
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        if (item == null) {
            listState.animateScrollToItem(selectedIndex)
            return@LaunchedEffect
        }
        val delta = revealScrollDelta(info.viewportStartOffset + info.beforeContentPadding,
            info.viewportEndOffset - info.afterContentPadding, item)
        if (delta != 0f) listState.animateScrollBy(delta)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OUTER_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
        contentPadding = PaddingValues(horizontal = INNER_PADDING)
    ) {
        items(items.items.size) { index ->
            TopNavBarCell(
                item = items.items[index],
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun TopNavBarCell(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(Motion.durationFast),
        label = "topNavContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.durationFast),
        label = "topNavContent"
    )

    Row(
        modifier = Modifier
            .clip(AppShapes.chip)
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription,
            tint = content,
            modifier = Modifier.size(ICON_SIZE)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun revealScrollDelta(start: Int, end: Int, item: LazyListItemInfo): Float {
    return when {
        item.offset < start -> (item.offset - start).toFloat()
        item.offset + item.size > end -> (item.offset + item.size - end).toFloat()
        else -> 0f
    }
}

private val OUTER_PADDING = 12.dp
private val INNER_PADDING = 6.dp
private val CELL_SPACING = 8.dp
private val ICON_SIZE = 22.dp
