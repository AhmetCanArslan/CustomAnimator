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
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.AppShortcutInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DISABLED_ROW_ALPHA = 0.45f

@Composable
internal fun ShortcutStepHost(state: ShortcutsState, hasShizukuPermission: Boolean, onSource: (ShortcutTile) -> Unit) {
    when (val step = state.step) {
        ShortcutStep.Idle -> Unit
        is ShortcutStep.PickAction -> ActionPickerSheet(
            step = step,
            current = state.current,
            hasShizukuPermission = hasShizukuPermission,
            onSource = onSource,
            onPick = state::onActionPicked,
            onDismiss = state::dismiss
        )
        is ShortcutStep.EnterValue -> ActionValueDialog(
            entry = step.entry,
            onSave = state::onValueEntered,
            onDismiss = state::dismiss
        )
        ShortcutStep.PickScreen -> ScreenPickerSheet(
            onPick = state::onScreenPicked,
            onDismiss = state::dismiss
        )
        is ShortcutStep.PickApp -> AppPickerSheet(
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
        is ShortcutStep.PickAppShortcut -> AppShortcutPickerSheet(
            step = step,
            onPick = { shortcut -> state.onAppShortcutPicked(step.app, shortcut, step.icons[shortcut.id]) },
            onDismiss = state::dismiss
        )
        is ShortcutStep.PickHandler -> HandlerPickerSheet(
            step = step,
            onPick = state::onHandlerPicked,
            onDismiss = state::dismiss
        )
        is ShortcutStep.Confirm -> ConfirmShortcutDialog(
            step = step,
            canAddWidget = state.canAddWidget,
            onConfirm = state::confirm,
            onTest = state::launch,
            onDismiss = state::dismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerSheet(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AppShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
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
internal fun PickerRow(
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
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
internal fun AppPickerSheet(
    onPick: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val appList = rememberAppListState()
    PickerSheet(title = stringResource(R.string.shortcut_pick_app), subtitle = null, onDismiss = onDismiss) {
        appList(appList) { app ->
            PickerRow(title = app.label, subtitle = app.packageName, onClick = { onPick(app) }) {
                AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun ScreenPickerSheet(
    onPick: (SearchEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val screens = remember { screenShortcutEntries() }
    PickerSheet(title = stringResource(R.string.shortcut_pick_screen), subtitle = null, onDismiss = onDismiss) {
        items(screens, key = { it.target.key }) { screen ->
            PickerRow(
                title = stringResource(screen.titleRes),
                subtitle = screen.descriptionRes?.let { stringResource(it) }.orEmpty(),
                onClick = { onPick(screen) }
            ) {
                IconBadge(icon = screen.icon, size = 40.dp)
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
            AppSearchBar(
                searchQuery = query,
                onSearchQueryChange = { query = it },
                showFilterCheckbox = false,
                placeholder = stringResource(R.string.shortcut_search_activities)
            )
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
                AppIcon(packageName = step.app.packageName, modifier = Modifier.size(40.dp))
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
private fun AppShortcutPickerSheet(
    step: ShortcutStep.PickAppShortcut,
    onPick: (AppShortcutInfo) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(
        title = step.app.label,
        subtitle = stringResource(R.string.shortcut_app_shortcut_note),
        onDismiss = onDismiss
    ) {
        if (step.shortcuts.isEmpty()) {
            item { PickerMessage(stringResource(R.string.shortcut_no_app_shortcuts)) }
        }
        items(step.shortcuts, key = { it.id }) { shortcut ->
            PickerRow(
                title = shortcut.label,
                subtitle = shortcut.id,
                onClick = { onPick(shortcut) },
                trailing = {
                    if (shortcut.needsShizuku) {
                        StatusPill(text = stringResource(R.string.shortcut_pill_shizuku), tone = StatusTone.INFO)
                    }
                }
            ) {
                val icon = step.icons[shortcut.id]
                if (icon == null) {
                    AppIcon(packageName = step.app.packageName, modifier = Modifier.size(40.dp))
                } else {
                    ShortcutIcon(bitmap = icon.bitmap, legacy = icon.legacy, modifier = Modifier.size(40.dp))
                }
            }
        }
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
    canAddWidget: Boolean,
    onConfirm: (ShortcutEntry, Bitmap, Boolean) -> Unit,
    onTest: (ShortcutEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val entry = step.entry
    var label by remember(entry.id) { mutableStateOf(entry.label) }
    var value by remember(entry.id) { mutableStateOf(entry.inputValue()) }
    var icon by remember(entry.id) { mutableStateOf(ShortcutIconImage(step.icon, entry.legacyIcon)) }
    var showIconPicker by remember(entry.id) { mutableStateOf(false) }
    var rewarded by remember(entry.id) { mutableStateOf(false) }
    var choosingPlacement by remember(entry.id) { mutableStateOf(false) }
    val valueLabelRes = entry.valueLabelRes()
    val isValid = label.isNotBlank() && (valueLabelRes == null || value.isNotBlank())
    val context = LocalContext.current
    val isAdFree by rememberIsAdFree()
    val needsAd = !step.editing && !isAdFree && !rewarded
    val save: (Boolean) -> Unit = { asWidget ->
        onConfirm(entry.withInput(label, value).copy(legacyIcon = icon.legacy), icon.bitmap, asWidget)
    }
    val proceed = {
        if (!step.editing && canAddWidget) choosingPlacement = true else save(false)
    }

    if (choosingPlacement) {
        ShortcutPlacementDialog(onPick = save, onDismiss = { choosingPlacement = false })
        return
    }

    if (showIconPicker) {
        val originalIcons by produceState(initialValue = emptyList<ShortcutIconImage>(), entry.id) {
            this.value = withContext(Dispatchers.IO) { ShortcutMaker.originalIcons(context, entry) }
        }
        ShortcutIconPickerSheet(
            targetPackage = entry.packageName,
            type = entry.type,
            current = icon,
            originals = originalIcons,
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
                OutlinedButton(onClick = { showIconPicker = true }, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
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
                onClick = {
                    if (needsAd) {
                        requestReward(context) {
                            rewarded = true
                            proceed()
                        }
                    } else {
                        proceed()
                    }
                },
                enabled = isValid,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(confirmLabelRes(step.editing, needsAd)))
            }
        },
        dismissButton = {
            TextButton(onClick = { onTest(entry.withInput(label, value)) }, enabled = isValid, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.shortcut_test))
            }
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ShortcutPlacementDialog(onPick: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shortcut_add_to_home)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PickerRow(title = stringResource(R.string.shortcut_place_shortcut), subtitle = "", onClick = { onPick(false) }) {
                    IconBadge(icon = Icons.AutoMirrored.Filled.AddToHomeScreen, size = 40.dp)
                }
                PickerRow(title = stringResource(R.string.shortcut_place_widget), subtitle = "", onClick = { onPick(true) }) {
                    IconBadge(icon = Icons.Filled.Widgets, size = 40.dp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun confirmLabelRes(editing: Boolean, needsAd: Boolean): Int = when {
    editing -> R.string.save
    needsAd -> R.string.shortcut_watch_ad_add
    else -> R.string.shortcut_add_to_home
}

private fun ShortcutActivityInfo.isLaunchable(hasShizukuPermission: Boolean, isRoot: Boolean): Boolean = when {
    needsRoot -> isRoot
    needsShizuku -> hasShizukuPermission
    else -> true
}

private fun ShortcutEntry.selectedHandler() =
    packageName?.let { owner -> activityName?.let { android.content.ComponentName(owner, it) } }

internal fun ShortcutEntry.inputValue(): String = when (type) {
    ShortcutType.LINK -> uri
    ShortcutType.COMMAND -> command
    else -> null
}.orEmpty()

internal fun ShortcutEntry.valueLabelRes(): Int? = when (type) {
    ShortcutType.LINK -> R.string.shortcut_link_field
    ShortcutType.COMMAND -> R.string.shortcut_command_field
    else -> null
}

internal fun ShortcutEntry.withInput(label: String, value: String): ShortcutEntry = when (type) {
    ShortcutType.LINK -> copy(label = label.trim(), uri = ShortcutMaker.normalizeLink(value))
    ShortcutType.COMMAND -> copy(label = label.trim(), command = value.trim())
    else -> copy(label = label.trim())
}
