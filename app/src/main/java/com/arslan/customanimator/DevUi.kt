package com.arslan.customanimator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.components.AppSwitch
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.InfoNoticeManager

@Composable
internal fun DevSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

internal fun LazyListScope.cardSection(titleRes: Int, vararg rows: @Composable () -> Unit) {
    item {
        Column {
            DevSectionTitle(stringResource(titleRes))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    cardRows(*rows)
    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}

internal fun LazyListScope.cardRows(vararg rows: @Composable () -> Unit) {
    items(rows.size, contentType = { CARD_ROW_CONTENT_TYPE }) { index ->
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = AppShapes.segment(index, rows.size),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (index > 0) SEGMENT_GAP else 0.dp)
        ) {
            Column {
                rows[index]()
            }
        }
    }
}

private const val CARD_ROW_CONTENT_TYPE = "cardRow"
private val SEGMENT_GAP = 2.dp

@Composable
internal fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
internal fun NavigationRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun QuickActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isRunning, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall,)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes()) {
            Text(
                text = if (isRunning) stringResource(R.string.working) else stringResource(R.string.apply_settings),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun rememberInfoNoticeVisible(dismissKey: String): MutableState<Boolean> {
    val context = LocalContext.current
    return remember(dismissKey) {
        mutableStateOf(!InfoNoticeManager.isDismissed(context, dismissKey))
    }
}

@Composable
internal fun InfoNote(text: String, dismissKey: String) {
    val context = LocalContext.current
    val visible = rememberInfoNoticeVisible(dismissKey)
    if (!visible.value) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        InfoDismissButton(
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismiss = {
                InfoNoticeManager.dismiss(context, dismissKey)
                visible.value = false
            }
        )
    }
}

@Composable
internal fun InfoCard(
    dismissKey: String,
    texts: List<String>,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val context = LocalContext.current
    val visible = rememberInfoNoticeVisible(dismissKey)
    if (!visible.value) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = AppShapes.card
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = contentColor
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                texts.forEach { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
            InfoDismissButton(
                tint = contentColor,
                onDismiss = {
                    InfoNoticeManager.dismiss(context, dismissKey)
                    visible.value = false
                }
            )
        }
    }
}

@Composable
internal fun InfoDismissButton(tint: androidx.compose.ui.graphics.Color, onDismiss: () -> Unit) {
    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp), shapes = IconButtonDefaults.shapes()) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.close),
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 340.dp
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = icon, size = 42.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!stacked) {
                    Button(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes()) {
                        Text(text = buttonLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            if (stacked) {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.End),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = buttonLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun CompileFilterOption(
    selected: Boolean,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun RotationSelector(
    currentRotation: Int,
    enabled: Boolean,
    onRotationSelected: (Int) -> Unit
) {
    val rotations = listOf(
        0 to stringResource(R.string.rotation_portrait),
        1 to stringResource(R.string.rotation_landscape_90),
        2 to stringResource(R.string.rotation_reverse_portrait),
        3 to stringResource(R.string.rotation_landscape_270)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.rotation_orientation).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        rotations.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { onRotationSelected(value) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentRotation == value,
                    onClick = { if (enabled) onRotationSelected(value) },
                    enabled = enabled
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun BusyDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
                Text(message)
            }
        }
    )
}
