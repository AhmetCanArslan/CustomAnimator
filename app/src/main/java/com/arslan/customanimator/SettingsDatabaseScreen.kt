package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.ConnectedChoiceGroup
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.clearFocusOnTapOutside
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.SettingEntry
import com.arslan.customanimator.utils.SettingsDatabaseManager
import com.arslan.customanimator.utils.SettingsNamespace
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface SettingsDialog {
    data object Add : SettingsDialog
    data class Edit(val entry: SettingEntry) : SettingsDialog
    data class Delete(val entry: SettingEntry) : SettingsDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDatabaseScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var namespace by rememberSaveable { mutableStateOf(SettingsNamespace.SYSTEM) }
    var query by rememberSaveable { mutableStateOf("") }
    var entries by remember { mutableStateOf(emptyList<SettingEntry>()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    LaunchedEffect(hasShizukuPermission, namespace, reloadToken) {
        if (!hasShizukuPermission) {
            entries = emptyList()
            loadFailed = false
            return@LaunchedEffect
        }
        isLoading = true
        val loaded = withContext(Dispatchers.IO) { SettingsDatabaseManager.list(namespace) }
        entries = loaded.orEmpty()
        loadFailed = loaded == null
        isLoading = false
    }

    val applyChange: (List<SettingEntry>, () -> Boolean) -> Unit = { updated, action ->
        val previous = entries
        val target = namespace
        entries = updated
        dialog = null
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            if (!success) {
                if (namespace == target) entries = previous
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onSave: (String, String) -> Unit = { key, value ->
        val target = namespace
        applyChange(withEntry(entries, SettingEntry(key, value))) { SettingsDatabaseManager.put(target, key, value) }
    }
    val onDelete: (SettingEntry) -> Unit = { entry ->
        val target = namespace
        applyChange(entries.filterNot { it.key == entry.key }) { SettingsDatabaseManager.delete(target, entry.key) }
    }

    val filtered = remember(entries, query) { SettingsDatabaseManager.filter(entries, query) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.settings_database),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    SettingsDatabaseActions(
                        enabled = hasShizukuPermission && !isLoading,
                        onRefresh = { reloadToken++ },
                        onAdd = { dialog = SettingsDialog.Add }
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "settings_database_info") {
                InfoCard(
                    dismissKey = "settings_database_info",
                    texts = listOf(stringResource(R.string.settings_database_info))
                )
            }

            if (!hasShizukuPermission) {
                item(key = "settings_database_setup") {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item(key = "settings_database_namespace") {
                ConnectedChoiceGroup(
                    options = SettingsNamespace.entries.map { it to stringResource(it.labelRes()) },
                    selected = namespace,
                    onSelect = { namespace = it },
                    enabled = !isLoading
                )
            }

            item(key = "settings_database_search") {
                AppSearchBar(
                    searchQuery = query,
                    onSearchQueryChange = { query = it },
                    showFilterCheckbox = false,
                    placeholder = stringResource(R.string.settings_database_search)
                )
            }

            settingsEntries(
                entries = filtered,
                isLoading = isLoading,
                loadFailed = loadFailed,
                hasShizukuPermission = hasShizukuPermission,
                onClick = { dialog = SettingsDialog.Edit(it) }
            )

            item(key = "settings_database_bottom") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    SettingsDatabaseDialogs(
        dialog = dialog,
        existingKeys = remember(entries) { entries.mapTo(HashSet()) { it.key } },
        onDismiss = { dialog = null },
        onSave = onSave,
        onRequestDelete = { dialog = SettingsDialog.Delete(it) },
        onDelete = onDelete
    )
}

private fun SettingsNamespace.labelRes(): Int = when (this) {
    SettingsNamespace.SYSTEM -> R.string.settings_database_system
    SettingsNamespace.SECURE -> R.string.settings_database_secure
    SettingsNamespace.GLOBAL -> R.string.settings_database_global
}

private fun withEntry(entries: List<SettingEntry>, entry: SettingEntry): List<SettingEntry> =
    (entries.filterNot { it.key == entry.key } + entry).sortedBy { it.key.lowercase() }

@Composable
private fun SettingsDatabaseActions(enabled: Boolean, onRefresh: () -> Unit, onAdd: () -> Unit) {
    IconButton(onClick = onRefresh, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
        Icon(imageVector = Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
    }
    IconButton(onClick = onAdd, enabled = enabled, shapes = IconButtonDefaults.shapes()) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.settings_database_add))
    }
}

private fun LazyListScope.settingsEntries(
    entries: List<SettingEntry>,
    isLoading: Boolean,
    loadFailed: Boolean,
    hasShizukuPermission: Boolean,
    onClick: (SettingEntry) -> Unit
) {
    when {
        isLoading -> item(key = "settings_database_loading") {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        loadFailed -> item(key = "settings_database_failed") {
            UnsupportedCard(stringResource(R.string.settings_database_load_failed))
        }
        entries.isEmpty() && hasShizukuPermission -> item(key = "settings_database_empty") {
            PickerMessage(stringResource(R.string.settings_database_empty))
        }
        else -> itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
            SettingEntryRow(entry = entry, index = index, count = entries.size, onClick = { onClick(entry) })
        }
    }
}

@Composable
private fun SettingEntryRow(entry: SettingEntry, index: Int, count: Int, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = AppShapes.segment(index, count),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (index > 0) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = entry.key, style = MaterialTheme.typography.titleSmall)
            Text(
                text = entry.value.ifEmpty { stringResource(R.string.settings_database_empty_value) },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsDatabaseDialogs(
    dialog: SettingsDialog?,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onRequestDelete: (SettingEntry) -> Unit,
    onDelete: (SettingEntry) -> Unit
) {
    when (dialog) {
        SettingsDialog.Add -> SettingEditDialog(
            entry = null,
            existingKeys = existingKeys,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = null
        )
        is SettingsDialog.Edit -> SettingEditDialog(
            entry = dialog.entry,
            existingKeys = existingKeys,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = { onRequestDelete(dialog.entry) }
        )
        is SettingsDialog.Delete -> SettingDeleteDialog(
            entry = dialog.entry,
            onDismiss = onDismiss,
            onConfirm = { onDelete(dialog.entry) }
        )
        null -> Unit
    }
}

@Composable
private fun SettingEditDialog(
    entry: SettingEntry?,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var key by rememberSaveable { mutableStateOf(entry?.key.orEmpty()) }
    var value by rememberSaveable { mutableStateOf(entry?.value.orEmpty()) }
    val trimmedKey = key.trim()
    val keyValid = SettingsDatabaseManager.isValidKey(trimmedKey)

    AlertDialog(
        modifier = Modifier.clearFocusOnTapOutside(),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (entry == null) R.string.settings_database_add else R.string.settings_database_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.settings_database_key)) },
                    enabled = entry == null,
                    singleLine = true,
                    isError = key.isNotEmpty() && !keyValid,
                    supportingText = keySupportingText(entry == null, key.isNotEmpty() && !keyValid, trimmedKey in existingKeys),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.settings_database_value)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(trimmedKey, value) },
                enabled = keyValid && value != entry?.value,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

private fun keySupportingText(adding: Boolean, invalid: Boolean, exists: Boolean): (@Composable () -> Unit)? {
    val messageRes = when {
        !adding -> null
        invalid -> R.string.settings_database_key_invalid
        exists -> R.string.settings_database_key_exists
        else -> null
    }
    return messageRes?.let { res -> { Text(stringResource(res)) } }
}

@Composable
private fun SettingDeleteDialog(entry: SettingEntry, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_database_delete_title)) },
        text = { Text(stringResource(R.string.settings_database_delete_message, entry.key)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
