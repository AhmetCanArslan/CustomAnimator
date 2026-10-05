package com.arslan.customanimator

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutActivityInfo
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutHandlerInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ShortcutMaker

private const val DISABLED_ROW_ALPHA = 0.45f

@Composable
internal fun ShortcutStepHost(state: ShortcutsState, hasShizukuPermission: Boolean) {
    when (val step = state.step) {
        ShortcutStep.Idle -> Unit
        is ShortcutStep.PickApp -> AppPickerSheet(
            apps = state.apps,
            onPick = state::onAppPicked,
            onDismiss = state::dismiss
        )
        is ShortcutStep.PickActivity -> ActivityPickerSheet(
            step = step,
            hasShizukuPermission = hasShizukuPermission,
            isRoot = state.isRoot,
            onPick = { activity -> state.onActivityPicked(step.app, activity) },
            onDismiss = state::dismiss
        )
        is ShortcutStep.PickHandler -> HandlerPickerSheet(
            step = step,
            onPick = state::onHandlerPicked,
            onDismiss = state::dismiss
        )
        is ShortcutStep.Confirm -> ConfirmShortcutDialog(
            step = step,
            onConfirm = state::confirm,
            onDismiss = state::dismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = AppShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun PickerSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = AppShapes.field
    )
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .alpha(if (enabled) 1f else DISABLED_ROW_ALPHA)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
internal fun PickerMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun AppPickerSheet(
    apps: List<InstalledAppInfo>,
    onPick: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }
    PickerSheet(title = stringResource(R.string.shortcut_pick_app), subtitle = null, onDismiss = onDismiss) {
        item {
            PickerSearchField(query, { query = it }, stringResource(R.string.search_apps))
        }
        if (apps.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item { PickerMessage(stringResource(R.string.no_apps_found)) }
        }
        items(filtered, key = { it.packageName }) { app ->
            PickerRow(title = app.label, subtitle = app.packageName, onClick = { onPick(app) }) {
                AppIcon(icon = app.icon, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun ActivityPickerSheet(
    step: ShortcutStep.PickActivity,
    hasShizukuPermission: Boolean,
    isRoot: Boolean,
    onPick: (ShortcutActivityInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(step.activities, query) {
        step.activities.filter { it.name.contains(query, ignoreCase = true) || it.label.contains(query, ignoreCase = true) }
    }
    PickerSheet(
        title = step.app.label,
        subtitle = stringResource(R.string.shortcut_activity_note),
        onDismiss = onDismiss
    ) {
        item {
            PickerSearchField(query, { query = it }, stringResource(R.string.shortcut_search_activities))
        }
        if (filtered.isEmpty()) {
            item { PickerMessage(stringResource(R.string.shortcut_no_activities)) }
        }
        items(filtered, key = { it.name }) { activity ->
            PickerRow(
                title = activity.label,
                subtitle = activity.name.substringAfterLast('.'),
                enabled = activity.isLaunchable(hasShizukuPermission, isRoot),
                onClick = { onPick(activity) },
                trailing = { ActivityAccessPill(activity) }
            ) {
                AppIcon(icon = step.app.icon, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun ActivityAccessPill(activity: ShortcutActivityInfo) {
    if (activity.needsRoot) {
        StatusPill(text = stringResource(R.string.shortcut_pill_root), tone = StatusTone.WARNING)
    } else if (activity.needsShizuku) {
        StatusPill(text = stringResource(R.string.shortcut_pill_shizuku), tone = StatusTone.INFO)
    }
}

@Composable
private fun HandlerPickerSheet(
    step: ShortcutStep.PickHandler,
    onPick: (ShortcutHandlerInfo?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    val selected = if (step.editing) step.entry.selectedHandler() else step.defaultHandler
    PickerSheet(
        title = stringResource(R.string.shortcut_open_with),
        subtitle = step.entry.label,
        onDismiss = onDismiss
    ) {
        item {
            ToggleRow(
                title = stringResource(R.string.shortcut_remember_handler),
                subtitle = step.entry.mimeType,
                checked = rememberChoice,
                onCheckedChange = { rememberChoice = it }
            )
        }
        item {
            PickerRow(
                title = stringResource(R.string.shortcut_system_default),
                subtitle = stringResource(R.string.shortcut_system_default_desc),
                selected = selected == null,
                onClick = { onPick(null, rememberChoice) }
            ) {
                IconBadge(icon = Icons.Filled.Android, size = 40.dp)
            }
        }
        if (step.handlers.isEmpty()) {
            item { PickerMessage(stringResource(R.string.shortcut_no_handlers)) }
        }
        items(step.handlers, key = { it.packageName + "/" + it.activityName }) { handler ->
            PickerRow(
                title = handler.label,
                subtitle = handler.packageName,
                selected = handler.component() == selected,
                onClick = { onPick(handler, rememberChoice) },
                trailing = {
                    if (handler.component() == step.defaultHandler) {
                        StatusPill(text = stringResource(R.string.shortcut_pill_default), tone = StatusTone.ACTIVE)
                    }
                }
            ) {
                AppIcon(icon = handler.icon, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun ConfirmShortcutDialog(
    step: ShortcutStep.Confirm,
    onConfirm: (ShortcutEntry, Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val entry = step.entry
    var label by remember(entry.id) { mutableStateOf(entry.label) }
    var value by remember(entry.id) { mutableStateOf(entry.inputValue()) }
    var icon by remember(entry.id) { mutableStateOf(ShortcutIconImage(step.icon, entry.legacyIcon)) }
    var showIconPicker by remember(entry.id) { mutableStateOf(false) }
    val valueLabelRes = entry.valueLabelRes()
    val isValid = label.isNotBlank() && (valueLabelRes == null || value.isNotBlank())

    if (showIconPicker) {
        ShortcutIconPickerSheet(
            targetPackage = entry.packageName,
            current = icon,
            onPick = {
                icon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            ShortcutIcon(
                bitmap = icon.bitmap,
                legacy = icon.legacy,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { showIconPicker = true }
            )
        },
        title = {
            Text(stringResource(if (step.editing) R.string.shortcut_edit_title else R.string.shortcut_new_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showIconPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.shortcut_change_icon))
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.shortcut_name)) },
                    singleLine = true,
                    shape = AppShapes.field,
                    modifier = Modifier.fillMaxWidth()
                )
                if (valueLabelRes != null) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(stringResource(valueLabelRes)) },
                        shape = AppShapes.field,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(entry.withInput(label, value).copy(legacyIcon = icon.legacy), icon.bitmap) },
                enabled = isValid
            ) {
                Text(stringResource(if (step.editing) R.string.save else R.string.shortcut_add_to_home))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun ShortcutActivityInfo.isLaunchable(hasShizukuPermission: Boolean, isRoot: Boolean): Boolean = when {
    needsRoot -> isRoot
    needsShizuku -> hasShizukuPermission
    else -> true
}

private fun ShortcutEntry.selectedHandler() =
    packageName?.let { owner -> activityName?.let { android.content.ComponentName(owner, it) } }

private fun ShortcutEntry.inputValue(): String = when (type) {
    ShortcutType.LINK -> uri
    ShortcutType.COMMAND -> command
    else -> null
}.orEmpty()

private fun ShortcutEntry.valueLabelRes(): Int? = when (type) {
    ShortcutType.LINK -> R.string.shortcut_link_field
    ShortcutType.COMMAND -> R.string.shortcut_command_field
    else -> null
}

private fun ShortcutEntry.withInput(label: String, value: String): ShortcutEntry = when (type) {
    ShortcutType.LINK -> copy(label = label.trim(), uri = ShortcutMaker.normalizeLink(value))
    ShortcutType.COMMAND -> copy(label = label.trim(), command = value.trim())
    else -> copy(label = label.trim())
}
