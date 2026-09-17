package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
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
import com.arslan.customanimator.data.DebloatState
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.DebloatCatalog
import com.arslan.customanimator.utils.DebloatManager
import com.arslan.customanimator.utils.DebloatRemoteList
import com.arslan.customanimator.utils.InfoNoticeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission

private const val DEBLOAT_LIST_DISMISS_KEY = "debloater_list_card"

private data class DebloatFilters(
    val risk: DebloatCatalog.Risk? = null,
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
    val listCardVisible = rememberInfoNoticeVisible(DEBLOAT_LIST_DISMISS_KEY)

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

    val updateList: () -> Unit = {
        scope.launch {
            isWorking = true
            val count = withContext(Dispatchers.IO) { DebloatRemoteList.download(context) }
            listCount = DebloatRemoteList.entryCount(context)
            listUpdatedAt = DebloatRemoteList.updatedAtMs(context)
            if (count > 0) reload()
            isWorking = false
            android.widget.Toast.makeText(
                context,
                if (count > 0) {
                    resources.getString(R.string.debloater_list_done, count)
                } else {
                    resources.getString(R.string.debloater_list_failed)
                },
                android.widget.Toast.LENGTH_LONG
            ).show()
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
                },
                actions = {
                    DebloatOverflowMenu(
                        enabled = !isWorking,
                        onUpdateList = {
                            InfoNoticeManager.restore(context, DEBLOAT_LIST_DISMISS_KEY)
                            listCardVisible.value = true
                            updateList()
                        }
                    )
                }
            )
        },
        bottomBar = {
            Column {
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
                BannerAdView()
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "info") {
                InfoCard(
                    dismissKey = "debloater_info",
                    texts = listOf(
                        stringResource(R.string.debloater_info_scope),
                        stringResource(R.string.debloater_info_listing),
                        stringResource(R.string.debloater_info_reboot)
                    ),
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

            if (listCardVisible.value) {
                item(key = "list") {
                    DebloatListCard(
                        entryCount = listCount,
                        updatedAtMs = listUpdatedAt,
                        enabled = !isWorking,
                        onUpdate = updateList,
                        onDismiss = {
                            InfoNoticeManager.dismiss(context, DEBLOAT_LIST_DISMISS_KEY)
                            listCardVisible.value = false
                        }
                    )
                }
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
        .filter { filters.state == null || it.state == filters.state }
        .filter { filters.showSystem || !it.isSystemApp }
        .filter { !filters.hideProtected || !it.isProtected }
        .filter { !filters.changedOnly || it.isChangedByApp }
        .filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
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
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
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
private fun DebloatOverflowMenu(enabled: Boolean, onUpdateList: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.pn_cd_more_options)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            enabled = enabled,
            text = { Text(stringResource(R.string.debloater_list_menu)) },
            leadingIcon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
            onClick = {
                expanded = false
                onUpdateList()
            }
        )
    }
}

@Composable
private fun DebloatListCard(
    entryCount: Int,
    updatedAtMs: Long,
    enabled: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatted = remember(updatedAtMs) {
        if (updatedAtMs <= 0L) {
            ""
        } else {
            java.text.DateFormat.getDateInstance().format(java.util.Date(updatedAtMs))
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.debloater_list_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = if (entryCount > 0) {
                    stringResource(R.string.debloater_list_ready, entryCount, formatted)
                } else {
                    stringResource(R.string.debloater_list_missing)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.debloater_list_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onUpdate, enabled = enabled) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (entryCount > 0) R.string.debloater_list_update else R.string.debloater_list_download
                    )
                )
            }
        }
    }
}

@Composable
private fun DebloatFilterRows(filters: DebloatFilters, onChange: (DebloatFilters) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.risk == null,
                    onClick = { onChange(filters.copy(risk = null)) },
                    label = { Text(stringResource(R.string.debloater_filter_all)) }
                )
            }
            items(DebloatCatalog.Risk.entries.toList()) { risk ->
                FilterChip(
                    selected = filters.risk == risk,
                    onClick = { onChange(filters.copy(risk = risk)) },
                    label = { Text(stringResource(riskLabel(risk))) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.state == null,
                    onClick = { onChange(filters.copy(state = null)) },
                    label = { Text(stringResource(R.string.debloater_filter_all)) }
                )
            }
            items(DebloatState.entries.toList()) { state ->
                FilterChip(
                    selected = filters.state == state,
                    onClick = { onChange(filters.copy(state = state)) },
                    label = { Text(stringResource(stateLabel(state))) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                DebloatToggleChip(
                    selected = filters.showSystem,
                    label = stringResource(R.string.debloater_show_system),
                    onClick = { onChange(filters.copy(showSystem = !filters.showSystem)) }
                )
            }
            item {
                DebloatToggleChip(
                    selected = filters.hideProtected,
                    label = stringResource(R.string.debloater_hide_protected),
                    onClick = { onChange(filters.copy(hideProtected = !filters.hideProtected)) }
                )
            }
            item {
                DebloatToggleChip(
                    selected = filters.changedOnly,
                    label = stringResource(R.string.debloater_changed_only),
                    onClick = { onChange(filters.copy(changedOnly = !filters.changedOnly)) }
                )
            }
        }
    }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(text = stringResource(riskLabel(app.risk)), tone = riskTone(app.risk))
                    if (app.state != DebloatState.ACTIVE) {
                        StatusPill(text = stringResource(stateLabel(app.state)), tone = StatusTone.WARNING)
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
    val icon by produceState(app.icon, app.packageName) {
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
            StatusPill(text = stringResource(riskLabel(app.risk)), tone = riskTone(app.risk))
            StatusPill(text = stringResource(stateLabel(app.state)), tone = stateTone(app.state))
            if (app.isSystemApp) {
                StatusPill(text = stringResource(R.string.debloater_system_app), tone = StatusTone.NEUTRAL)
            }
        }
        Text(text = stringResource(riskDescription(app.risk)), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = app.description ?: stringResource(groupDescription(app.group)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                if (apps.any { it.risk == DebloatCatalog.Risk.UNSAFE }) {
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

private fun riskLabel(risk: DebloatCatalog.Risk): Int = when (risk) {
    DebloatCatalog.Risk.RECOMMENDED -> R.string.debloater_risk_recommended
    DebloatCatalog.Risk.ADVANCED -> R.string.debloater_risk_advanced
    DebloatCatalog.Risk.EXPERT -> R.string.debloater_risk_expert
    DebloatCatalog.Risk.UNSAFE -> R.string.debloater_risk_unsafe
}

private fun riskDescription(risk: DebloatCatalog.Risk): Int = when (risk) {
    DebloatCatalog.Risk.RECOMMENDED -> R.string.debloater_risk_recommended_desc
    DebloatCatalog.Risk.ADVANCED -> R.string.debloater_risk_advanced_desc
    DebloatCatalog.Risk.EXPERT -> R.string.debloater_risk_expert_desc
    DebloatCatalog.Risk.UNSAFE -> R.string.debloater_risk_unsafe_desc
}

private fun riskTone(risk: DebloatCatalog.Risk): StatusTone = when (risk) {
    DebloatCatalog.Risk.RECOMMENDED -> StatusTone.ACTIVE
    DebloatCatalog.Risk.ADVANCED -> StatusTone.INFO
    DebloatCatalog.Risk.EXPERT -> StatusTone.WARNING
    DebloatCatalog.Risk.UNSAFE -> StatusTone.DANGER
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

private fun groupDescription(group: DebloatCatalog.Group): Int = when (group) {
    DebloatCatalog.Group.GOOGLE -> R.string.debloater_group_google
    DebloatCatalog.Group.ASSISTANT -> R.string.debloater_group_assistant
    DebloatCatalog.Group.OEM -> R.string.debloater_group_oem
    DebloatCatalog.Group.CARRIER -> R.string.debloater_group_carrier
    DebloatCatalog.Group.ADS -> R.string.debloater_group_ads
    DebloatCatalog.Group.DIAGNOSTICS -> R.string.debloater_group_diagnostics
    DebloatCatalog.Group.PRINTING -> R.string.debloater_group_printing
    DebloatCatalog.Group.AR -> R.string.debloater_group_ar
    DebloatCatalog.Group.ACCESSIBILITY -> R.string.debloater_group_accessibility
    DebloatCatalog.Group.BACKUP -> R.string.debloater_group_backup
    DebloatCatalog.Group.PARTNER -> R.string.debloater_group_partner
    DebloatCatalog.Group.THEMES -> R.string.debloater_group_themes
    DebloatCatalog.Group.BROWSER -> R.string.debloater_group_browser
    DebloatCatalog.Group.MEDIA -> R.string.debloater_group_media
    DebloatCatalog.Group.PAYMENT -> R.string.debloater_group_payment
    DebloatCatalog.Group.SYSTEM -> R.string.debloater_group_system
}
