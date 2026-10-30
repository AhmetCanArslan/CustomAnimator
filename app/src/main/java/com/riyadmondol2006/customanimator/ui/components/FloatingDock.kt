package com.riyadmondol2006.customanimator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.ui.theme.pressScale
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Immutable
data class NavBarItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

@Immutable
data class NavBarItems(val items: List<NavBarItem>)

/**
 * Floating pill-shaped dock at the bottom of the home screen. Unselected destinations are
 * icon-only; the selected one expands into a labelled pill. Scrolls when the items don't fit.
 */
@Composable
fun FloatingDock(
    items: NavBarItems,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var revealedOnce by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val animate = revealedOnce
        revealedOnce = true
        val info = snapshotFlow { listState.layoutInfo }
            .first { it.visibleItemsInfo.isNotEmpty() }
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        if (item == null) {
            if (animate) listState.animateScrollToItem(selectedIndex) else listState.requestScrollToItem(selectedIndex)
            return@LaunchedEffect
        }
        val delta = revealScrollDelta(
            info.viewportStartOffset + info.beforeContentPadding,
            info.viewportEndOffset - info.afterContentPadding,
            item
        )
        if (delta == 0f) return@LaunchedEffect
        if (animate) {
            listState.animateScrollBy(delta)
        } else {
            listState.requestScrollToItem(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset + delta.roundToInt()
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = 8.dp,
            tonalElevation = 3.dp
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.height(DOCK_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                items(items.items.size) { index ->
                    DockCell(
                        item = items.items[index],
                        selected = index == selectedIndex,
                        onClick = {
                            if (index != selectedIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            onSelect(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockCell(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "dockContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "dockContent"
    )

    Row(
        modifier = Modifier
            .height(CELL_HEIGHT)
            .widthIn(min = CELL_HEIGHT)
            .pressScale(interactionSource, 0.88f)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = item.contentDescription
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(ICON_SIZE)
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
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
    }
}

private fun revealScrollDelta(start: Int, end: Int, item: LazyListItemInfo): Float {
    return when {
        item.offset < start -> (item.offset - start).toFloat()
        item.offset + item.size > end -> (item.offset + item.size - end).toFloat()
        else -> 0f
    }
}

private val DOCK_HEIGHT = 64.dp
private val CELL_HEIGHT = 52.dp
private val ICON_SIZE = 24.dp
