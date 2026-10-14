package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arslan.customanimator.data.DebloatAppInfo
import com.arslan.customanimator.data.DebloatGroup
import com.arslan.customanimator.data.DebloatRisk
import com.arslan.customanimator.data.DebloatState
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.DebloatManager
import com.arslan.customanimator.utils.DebloatRemoteList
import com.arslan.customanimator.utils.InfoNoticeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission

private const val LIST_CARD_DISMISS_KEY = "debloater_list_card"

private data class DebloatFilters(
    val risk: DebloatRisk? = null,
    val state: DebloatState? = null,
    val showSystem: Boolean = true,
    val hideProtected: Boolean = false,
    val changedOnly: Boolean = false
)

private data class DebloatConfirmation(
    val apps: List<DebloatAppInfo>,
    val disableOnly: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebloaterScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<DebloatAppInfo>>(emptyList()) }
    var changedCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(DebloatFilters()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var detailApp by remember { mutableStateOf<DebloatAppInfo?>(null) }
    var confirmation by remember { mutableStateOf<DebloatConfirmation?>(null) }
    var confirmRestoreAll by remember { mutableStateOf(false) }
    var listCount by remember { mutableIntStateOf(DebloatRemoteList.entryCount(context)) }
    var listUpdatedAt by remember { mutableLongStateOf(DebloatRemoteList.updatedAtMs(context)) }
    var listFailed by remember { mutableStateOf(false) }

    val reload: suspend () -> Unit = {
        val loaded = withContext(Dispatchers.IO) { DebloatManager.loadApps(context) }
        val changed = withContext(Dispatchers.IO) { DebloatManager.changedPackageCount(context) }
        apps = loaded
        changedCount = changed
        selected = emptySet()
        isLoading = false
    }

    LaunchedEffect(hasShizukuPermission) {
        isLoading = true
        reload()
    }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { DebloatRemoteList.download(context) }
        listFailed = result == DebloatRemoteList.DownloadResult.FAILED
        if (!listFailed) {
            listCount = DebloatRemoteList.entryCount(context)
            listUpdatedAt = DebloatRemoteList.updatedAtMs(context)
        }
        if (result == DebloatRemoteList.DownloadResult.UPDATED) reload()
    }

    val visibleApps by remember(apps, searchQuery, filters) {
        derivedStateOf { filterApps(apps, searchQuery, filters) }
    }

    val runAction: (List<DebloatAppInfo>, Boolean) -> Unit = { targets, disableOnly ->
        scope.launch {
            isWorking = true
            val done = withContext(Dispatchers.IO) { applyAction(context, targets, disableOnly) }
            reload()
            isWorking = false
            reportResult(context, done, targets.size)
        }
    }

    val exportApk: (DebloatAppInfo) -> Unit = { app ->
        scope.launch {
            isWorking = true
            val destination = withContext(Dispatchers.IO) { DebloatManager.exportApk(app) }
            isWorking = false
            android.widget.Toast.makeText(
                context,
                destination?.let { resources.getString(R.string.debloater_export_done, it) }
                    ?: resources.getString(R.string.debloater_export_failed),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val restoreApp: (DebloatAppInfo) -> Unit = { app ->
        scope.launch {
            isWorking = true
            val done = withContext(Dispatchers.IO) { DebloatManager.restore(context, app) }
            reload()
            isWorking = false
            reportResult(context, if (done) 1 else 0, 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.debloater),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                DebloatSelectionBar(
                    count = selected.size,
                    enabled = !isWorking,
                    onClear = { selected = emptySet() },
                    onRemove = {
                        confirmation = DebloatConfirmation(apps.filter { selected.contains(it.packageName) }, false)
                    },
                    onDisable = {
                        confirmation = DebloatConfirmation(apps.filter { selected.contains(it.packageName) }, true)
                    }
                )
            }
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
            item(key = "info") {
                InfoCard(
                    dismissKey = "debloater_info",
                    texts = listOf(stringResource(R.string.debloater_info)),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (!hasShizukuPermission) {
                item(key = "nudge") {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item(key = "summary") {
                DebloatSummaryCard(
                    changedCount = changedCount,
                    enabled = hasShizukuPermission && !isWorking,
                    onRestoreAll = { confirmRestoreAll = true }
                )
            }

            item(key = "list") {
                DebloatListCard(entryCount = listCount, updatedAtMs = listUpdatedAt, failed = listFailed)
            }

            item(key = "search") {
                AppSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    showFilterCheckbox = false,
                    placeholder = stringResource(R.string.debloater_search_hint)
                )
            }

            item(key = "filters") {
                DebloatFilterRows(filters = filters, onChange = { filters = it })
            }

            if (isLoading || isWorking) {
                item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (visibleApps.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.debloater_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(visibleApps, key = { it.packageName }) { app ->
                    DebloatRow(
                        app = app,
                        selected = selected.contains(app.packageName),
                        onSelectedChange = { checked ->
                            selected = if (checked) selected + app.packageName else selected - app.packageName
                        },
                        onClick = { detailApp = app }
                    )
                }
            }

            item(key = "spacer") { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    detailApp?.let { app ->
        DebloatDetailDialog(
            app = app,
            onDismiss = { detailApp = null },
            onRemove = {
                detailApp = null
                confirmation = DebloatConfirmation(listOf(app), false)
            },
            onDisable = {
                detailApp = null
                confirmation = DebloatConfirmation(listOf(app), true)
            },
            onRestore = {
                detailApp = null
                restoreApp(app)
            },
            onExport = {
                detailApp = null
                exportApk(app)
            }
        )
    }

    confirmation?.let { pending ->
        DebloatConfirmDialog(
            confirmation = pending,
            onDismiss = { confirmation = null },
            onConfirm = {
                confirmation = null
                runAction(pending.apps, pending.disableOnly)
            }
        )
    }

    if (confirmRestoreAll) {
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            title = { Text(stringResource(R.string.debloater_restore_all_title)) },
            text = { Text(stringResource(R.string.debloater_restore_all_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRestoreAll = false
                        scope.launch {
                            isWorking = true
                            val restored = withContext(Dispatchers.IO) { DebloatManager.restoreAll(context) }
                            reload()
                            isWorking = false
                            android.widget.Toast.makeText(
                                context,
                                resources.getString(R.string.debloater_restore_all_done, restored),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.debloater_restore_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun filterApps(
    apps: List<DebloatAppInfo>,
    query: String,
    filters: DebloatFilters
): List<DebloatAppInfo> {
    return apps
        .filter { filters.risk == null || it.risk == filters.risk }
        .filter { matchesState(it, filters) }
        .filter { filters.showSystem || !it.isSystemApp }
        .filter { !filters.hideProtected || !it.isProtected }
        .filter { !filters.changedOnly || it.isChangedByApp }
        .filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
}

private fun matchesState(app: DebloatAppInfo, filters: DebloatFilters): Boolean {
    if (filters.state != null) return app.state == filters.state
    return filters.changedOnly || app.state != DebloatState.REMOVED
}

private fun applyAction(
    context: android.content.Context,
    targets: List<DebloatAppInfo>,
    disableOnly: Boolean
): Int {
    var done = 0
    targets.forEach { app ->
        val ok = if (disableOnly) DebloatManager.disable(context, app) else DebloatManager.remove(context, app)
        if (ok) done++
    }
    return done
}

private fun reportResult(context: android.content.Context, done: Int, total: Int) {
    if (done == 0) {
        android.widget.Toast.makeText(context, R.string.action_failed, android.widget.Toast.LENGTH_LONG).show()
        return
    }
    android.widget.Toast.makeText(
        context,
        context.getString(R.string.debloater_result, done, total - done),
        android.widget.Toast.LENGTH_LONG
    ).show()
    maybeShowInterstitial(context)
}

@Composable
private fun DebloatSelectionBar(
    count: Int,
    enabled: Boolean,
    onClear: () -> Unit,
    onRemove: () -> Unit,
    onDisable: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier.imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debloater_selected, count),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear, enabled = enabled) {
                Text(stringResource(R.string.debloater_clear_selection))
            }
            TextButton(onClick = onDisable, enabled = enabled) {
                Text(stringResource(R.string.debloater_action_disable))
            }
            Button(onClick = onRemove, enabled = enabled) {
                Text(stringResource(R.string.debloater_action_remove))
            }
        }
    }
}

@Composable
private fun DebloatSummaryCard(changedCount: Int, enabled: Boolean, onRestoreAll: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (changedCount == 0) {
                    stringResource(R.string.debloater_status_clean)
                } else {
                    stringResource(R.string.debloater_status, changedCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (changedCount > 0) {
                FilledTonalButton(onClick = onRestoreAll, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.debloater_restore_all))
                }
            }
        }
    }
}

@Composable
private fun DebloatListCard(entryCount: Int, updatedAtMs: Long, failed: Boolean) {
    val context = LocalContext.current
    val visible = rememberInfoNoticeVisible(LIST_CARD_DISMISS_KEY)
    if (entryCount > 0 && !visible.value) return
    val formatted = remember(updatedAtMs) {
        if (updatedAtMs <= 0L) {
            ""
        } else {
            java.text.DateFormat.getDateInstance().format(java.util.Date(updatedAtMs))
        }
    }
    val status = when {
        entryCount > 0 -> stringResource(R.string.debloater_list_ready, entryCount, formatted)
        failed -> stringResource(R.string.debloater_list_failed)
        else -> stringResource(R.string.debloater_list_missing)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.debloater_list_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entryCount == 0 && failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = stringResource(R.string.debloater_list_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entryCount > 0) {
                InfoDismissButton(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onDismiss = {
                        InfoNoticeManager.dismiss(context, LIST_CARD_DISMISS_KEY)
                        visible.value = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebloatFilterRows(filters: DebloatFilters, onChange: (DebloatFilters) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        DebloatMenuChip(
            title = stringResource(R.string.debloater_filter_risk),
            options = DebloatRisk.entries,
            selected = filters.risk,
            labelOf = ::riskLabel,
            onSelect = { onChange(filters.copy(risk = it)) }
        )
        DebloatMenuChip(
            title = stringResource(R.string.debloater_filter_state),
            options = DebloatState.entries,
            selected = filters.state,
            labelOf = ::stateLabel,
            onSelect = { onChange(filters.copy(state = it)) }
        )
        DebloatToggleChip(
            selected = filters.showSystem,
            label = stringResource(R.string.debloater_show_system),
            onClick = { onChange(filters.copy(showSystem = !filters.showSystem)) }
        )
        DebloatToggleChip(
            selected = filters.hideProtected,
            label = stringResource(R.string.debloater_hide_protected),
            onClick = { onChange(filters.copy(hideProtected = !filters.hideProtected)) }
        )
        DebloatToggleChip(
            selected = filters.changedOnly,
            label = stringResource(R.string.debloater_changed_only),
            onClick = { onChange(filters.copy(changedOnly = !filters.changedOnly)) }
        )
        if (filters != DebloatFilters()) {
            AssistChip(
                onClick = { onChange(DebloatFilters()) },
                label = { Text(stringResource(R.string.reset)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun <T> DebloatMenuChip(
    title: String,
    options: List<T>,
    selected: T?,
    labelOf: (T) -> Int,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selected?.let { stringResource(labelOf(it)) } ?: title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DebloatMenuItem(
                label = stringResource(R.string.debloater_filter_all),
                checked = selected == null,
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            options.forEach { option ->
                DebloatMenuItem(
                    label = stringResource(labelOf(option)),
                    checked = selected == option,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun DebloatMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = if (checked) {
            { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        }
    )
}

@Composable
private fun DebloatToggleChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun DebloatRow(
    app: DebloatAppInfo,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebloatAppIcon(app = app, modifier = Modifier.size(36.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.risk != null || app.state != DebloatState.ACTIVE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        app.risk?.let { StatusPill(text = stringResource(riskLabel(it)), tone = riskTone(it)) }
                        if (app.state != DebloatState.ACTIVE) {
                            StatusPill(text = stringResource(stateLabel(app.state)), tone = StatusTone.WARNING)
                        }
                    }
                }
            }
            if (app.isProtected) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.debloater_protected),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Checkbox(checked = selected, onCheckedChange = onSelectedChange)
            }
        }
    }
}

@Composable
private fun DebloatAppIcon(app: DebloatAppInfo, modifier: Modifier) {
    val context = LocalContext.current
    val icon by produceState(DebloatManager.cachedIcon(app.packageName), app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { DebloatManager.loadIcon(context, app) }
        }
    }
    AppIcon(icon = icon, modifier = modifier)
}

@Composable
private fun DebloatDetailDialog(
    app: DebloatAppInfo,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onDisable: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = { Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = { DebloatDetailBody(app = app, onExport = onExport) },
        confirmButton = {
            if (app.state == DebloatState.ACTIVE) {
                if (!app.isProtected) {
                    Button(onClick = onRemove) { Text(stringResource(R.string.debloater_action_remove)) }
                }
            } else {
                Button(onClick = onRestore) { Text(stringResource(R.string.debloater_action_restore)) }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                if (app.state == DebloatState.ACTIVE && !app.isProtected) {
                    TextButton(onClick = onDisable) { Text(stringResource(R.string.debloater_action_disable)) }
                }
            }
        }
    )
}

@Composable
private fun DebloatDetailBody(app: DebloatAppInfo, onExport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            app.risk?.let { StatusPill(text = stringResource(riskLabel(it)), tone = riskTone(it)) }
            StatusPill(text = stringResource(stateLabel(app.state)), tone = stateTone(app.state))
            if (app.isSystemApp) {
                StatusPill(text = stringResource(R.string.debloater_system_app), tone = StatusTone.NEUTRAL)
            }
        }
        app.risk?.let {
            Text(text = stringResource(riskDescription(it)), style = MaterialTheme.typography.bodyMedium)
        }
        (app.description ?: app.group?.let { stringResource(groupDescription(it)) })?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.debloater_export_apk))
        }
        if (app.isProtected) {
            Text(
                text = stringResource(R.string.debloater_protected_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!app.isRestorable) {
            Text(
                text = stringResource(R.string.debloater_not_restorable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DebloatConfirmDialog(
    confirmation: DebloatConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val apps = confirmation.apps
    val single = apps.singleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (confirmation.disableOnly && single != null) {
                    stringResource(R.string.debloater_confirm_disable_title, single.label)
                } else {
                    stringResource(R.string.debloater_confirm_remove_title, apps.size)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (confirmation.disableOnly) {
                        stringResource(R.string.debloater_confirm_disable_message)
                    } else {
                        stringResource(R.string.debloater_confirm_remove_message)
                    }
                )
                if (apps.any { it.risk == DebloatRisk.UNSAFE }) {
                    Text(
                        text = stringResource(R.string.debloater_confirm_unsafe),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!confirmation.disableOnly && apps.any { !it.isRestorable }) {
                    Text(
                        text = stringResource(R.string.debloater_confirm_unrestorable),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = apps.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    stringResource(
                        if (confirmation.disableOnly) {
                            R.string.debloater_action_disable
                        } else {
                            R.string.debloater_action_remove
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun riskLabel(risk: DebloatRisk): Int = when (risk) {
    DebloatRisk.RECOMMENDED -> R.string.debloater_risk_recommended
    DebloatRisk.ADVANCED -> R.string.debloater_risk_advanced
    DebloatRisk.EXPERT -> R.string.debloater_risk_expert
    DebloatRisk.UNSAFE -> R.string.debloater_risk_unsafe
}

private fun riskDescription(risk: DebloatRisk): Int = when (risk) {
    DebloatRisk.RECOMMENDED -> R.string.debloater_risk_recommended_desc
    DebloatRisk.ADVANCED -> R.string.debloater_risk_advanced_desc
    DebloatRisk.EXPERT -> R.string.debloater_risk_expert_desc
    DebloatRisk.UNSAFE -> R.string.debloater_risk_unsafe_desc
}

private fun riskTone(risk: DebloatRisk): StatusTone = when (risk) {
    DebloatRisk.RECOMMENDED -> StatusTone.ACTIVE
    DebloatRisk.ADVANCED -> StatusTone.INFO
    DebloatRisk.EXPERT -> StatusTone.WARNING
    DebloatRisk.UNSAFE -> StatusTone.DANGER
}

private fun stateLabel(state: DebloatState): Int = when (state) {
    DebloatState.ACTIVE -> R.string.debloater_state_active
    DebloatState.DISABLED -> R.string.debloater_state_disabled
    DebloatState.REMOVED -> R.string.debloater_state_removed
}

private fun stateTone(state: DebloatState): StatusTone = when (state) {
    DebloatState.ACTIVE -> StatusTone.ACTIVE
    DebloatState.DISABLED -> StatusTone.WARNING
    DebloatState.REMOVED -> StatusTone.DANGER
}

private fun groupDescription(group: DebloatGroup): Int = when (group) {
    DebloatGroup.GOOGLE -> R.string.debloater_group_google
    DebloatGroup.OEM -> R.string.debloater_group_oem
    DebloatGroup.CARRIER -> R.string.debloater_group_carrier
    DebloatGroup.PARTNER -> R.string.debloater_group_partner
    DebloatGroup.SYSTEM -> R.string.debloater_group_system
}
