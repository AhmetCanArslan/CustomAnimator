package com.riyadmondol2006.customanimator.service

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.PickerRow
import com.riyadmondol2006.customanimator.PickerSheet
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.data.SharedItem
import com.riyadmondol2006.customanimator.data.ShortcutFileInfo
import com.riyadmondol2006.customanimator.ui.components.IconBadge
import com.riyadmondol2006.customanimator.ui.theme.AppShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShareSheetState(
    val item: SharedItem,
    val hasLink: Boolean,
    val isCleaning: Boolean = false,
    val cleaned: String? = null
)

@Composable
fun ShareActionsSheet(
    state: ShareSheetState,
    onDescribe: () -> ShortcutFileInfo,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onCleanLink: () -> Unit,
    onCopyCleaned: (String) -> Unit,
    onShareCleaned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val file by produceState<ShortcutFileInfo?>(null, state.item) {
        value = withContext(Dispatchers.IO) { onDescribe() }
    }

    PickerSheet(title = stringResource(R.string.share_actions), subtitle = null, onDismiss = onDismiss) {
        item(key = "preview") { SharePreview(item = state.item, file = file) }
        item(key = "gap") { Spacer(modifier = Modifier.height(8.dp)) }
        if (state.hasLink) {
            item(key = "clean") {
                AnimatedContent(targetState = state.cleaned, label = "cleanLink") { cleaned ->
                    if (cleaned == null) {
                        ShareActionRow(
                            title = stringResource(R.string.link_cleaner_label),
                            icon = Icons.Filled.AutoFixHigh,
                            onClick = onCleanLink,
                            enabled = !state.isCleaning,
                            trailing = { if (state.isCleaning) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                        )
                    } else {
                        CleanedLinkCard(cleaned = cleaned, onCopy = { onCopyCleaned(cleaned) }, onShare = { onShareCleaned(cleaned) })
                    }
                }
            }
        }
        item(key = "copy") {
            ShareActionRow(title = stringResource(R.string.copy_to_clipboard), icon = Icons.Filled.ContentCopy, onClick = onCopy)
        }
        item(key = "save") {
            ShareActionRow(title = stringResource(R.string.save_to_device), icon = Icons.Filled.SaveAlt, onClick = onSave)
        }
    }
}

@Composable
private fun SharePreview(item: SharedItem, file: ShortcutFileInfo?) {
    val isText = item.stream == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = if (isText) Icons.AutoMirrored.Filled.Notes else Icons.AutoMirrored.Filled.InsertDriveFile,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isText) item.text.orEmpty() else file?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isText) 4 else 2,
                overflow = TextOverflow.Ellipsis
            )
            file?.mimeType?.takeUnless { isText }?.let { mime ->
                Text(
                    text = mime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShareActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    PickerRow(title = title, subtitle = "", onClick = onClick, enabled = enabled, trailing = trailing) {
        IconBadge(icon = icon, size = 40.dp)
    }
}

@Composable
private fun CleanedLinkCard(cleaned: String, onCopy: () -> Unit, onShare: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoFixHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.link_cleaner_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = cleaned,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.link_cleaner_copy), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(onClick = onShare, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.link_cleaner_share), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text = stringResource(R.string.link_cleaner_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
