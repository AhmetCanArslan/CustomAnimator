package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.ForegroundAppWatcherService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.PerAppRefreshRateManager
import com.arslan.customanimator.utils.RefreshRateManager
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RefreshRateScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val manager = remember { PerAppRefreshRateManager(context) }

    val supportedRates = remember { RefreshRateManager.getSupportedRates(context) }
    var activeRate by remember { mutableStateOf(RefreshRateManager.getActiveRate(context)) }
    var minRate by remember { mutableStateOf<Float?>(null) }
    var peakRate by remember { mutableStateOf<Float?>(null) }

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var overrides by remember { mutableStateOf(manager.getOverrides()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }
    var hasUsageAccess by rememberUsageAccessState(hasShizukuPermission)
    var editingApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    val filteredApps by remember(apps, searchQuery, showSelectedOnly, overrides) {
        derivedStateOf {
            apps
                .filter {
                    searchQuery.isBlank() ||
                        it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
                .filter { !showSelectedOnly || overrides.containsKey(it.packageName) }
        }
    }

    LaunchedEffect(hasShizukuPermission) {
        val loadedApps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        val loadedMin = withContext(Dispatchers.IO) { RefreshRateManager.getMinRate(context.contentResolver) }
        val loadedPeak = withContext(Dispatchers.IO) { RefreshRateManager.getPeakRate(context.contentResolver) }
        apps = loadedApps
        minRate = loadedMin
        peakRate = loadedPeak
        isLoading = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                activeRate = RefreshRateManager.getActiveRate(context)
                minRate = RefreshRateManager.getMinRate(context.contentResolver)
                peakRate = RefreshRateManager.getPeakRate(context.contentResolver)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val syncServiceState: () -> Unit = {
        if (overrides.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        ForegroundAppWatcherService.sync(context)
    }

    val applyGlobalRate: (Float?, Boolean) -> Unit = { rate, isPeak ->
        val previousMin = minRate
        val previousPeak = peakRate
        if (isPeak) peakRate = rate else minRate = rate
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                if (isPeak) RefreshRateManager.setPeakRate(rate) else RefreshRateManager.setMinRate(rate)
            }
            if (success) {
                activeRate = RefreshRateManager.getActiveRate(context)
                maybeShowInterstitial(context)
            } else {
                minRate = previousMin
                peakRate = previousPeak
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    editingApp?.let { app ->
        RefreshRateDialog(
            app = app,
            currentRate = overrides[app.packageName],
            supportedRates = supportedRates,
            onDismiss = { editingApp = null },
            onConfirm = { rate ->
                manager.setRate(app.packageName, rate)
                overrides = manager.getOverrides()
                editingApp = null
                syncServiceState()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.refresh_rate),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    if (overrides.isNotEmpty()) {
                        TextButton(onClick = {
                            manager.clearAll()
                            overrides = manager.getOverrides()
                            syncServiceState()
                        }) {
                            Text(stringResource(R.string.per_app_refresh_rate_clear_all))
                        }
                    }
                }
            )
        },
        bottomBar = { BannerAdView() }
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
                    dismissKey = "refresh_rate_disclaimer",
                    texts = listOf(stringResource(R.string.refresh_rate_disclaimer))
                )
            }

            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item(key = "current") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.refresh_rate_current_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = activeRate?.let {
                                stringResource(R.string.refresh_rate_active, RefreshRateManager.formatRate(it))
                            } ?: stringResource(R.string.refresh_rate_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                R.string.refresh_rate_supported,
                                if (supportedRates.isEmpty()) {
                                    stringResource(R.string.refresh_rate_unknown)
                                } else {
                                    supportedRates.joinToString(", ") { RefreshRateManager.formatRate(it) }
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.refresh_rate_system_values,
                                minRate?.let { RefreshRateManager.formatRate(it) }
                                    ?: stringResource(R.string.refresh_rate_default),
                                peakRate?.let { RefreshRateManager.formatRate(it) }
                                    ?: stringResource(R.string.refresh_rate_default)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (supportedRates.isNotEmpty()) {
                item(key = "min") {
                    RefreshRateSelector(
                        title = stringResource(R.string.refresh_rate_min_title),
                        description = stringResource(R.string.refresh_rate_min_desc),
                        rates = supportedRates,
                        selected = minRate,
                        enabled = hasShizukuPermission,
                        onSelect = { applyGlobalRate(it, false) }
                    )
                }

                item(key = "peak") {
                    RefreshRateSelector(
                        title = stringResource(R.string.refresh_rate_peak_title),
                        description = stringResource(R.string.refresh_rate_peak_desc),
                        rates = supportedRates,
                        selected = peakRate,
                        enabled = hasShizukuPermission,
                        onSelect = { applyGlobalRate(it, true) }
                    )
                }
            }

            item(key = "per_app_header") {
                Text(
                    text = stringResource(R.string.per_app_refresh_rate),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!hasUsageAccess) {
                item {
                    WarningCard(
                        message = stringResource(R.string.per_app_refresh_rate_needs_usage_access),
                        actionLabel = stringResource(R.string.open_usage_access_settings),
                        onAction = { UsageAccessHelper.openUsageAccessSettings(context) }
                    )
                }
            }

            item(key = "per_app_status") {
                Text(
                    text = if (overrides.isNotEmpty() && hasShizukuPermission && hasUsageAccess) {
                        stringResource(R.string.per_app_refresh_rate_status_active, overrides.size)
                    } else if (overrides.isNotEmpty()) {
                        stringResource(R.string.per_app_refresh_rate_status_paused)
                    } else {
                        stringResource(R.string.per_app_refresh_rate_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLoading && apps.isNotEmpty()) {
                item(key = "search") {
                    AppSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        showSelectedOnly = showSelectedOnly,
                        onShowSelectedOnlyChange = { showSelectedOnly = it }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_apps_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    PerAppRefreshRateRow(
                        app = app,
                        rate = overrides[app.packageName],
                        onClick = { editingApp = app },
                        onClear = {
                            manager.setRate(app.packageName, null)
                            overrides = manager.getOverrides()
                            syncServiceState()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RefreshRateSelector(
    title: String,
    description: String,
    rates: List<Float>,
    selected: Float?,
    enabled: Boolean,
    onSelect: (Float?) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected == null,
                    onClick = { onSelect(null) },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.refresh_rate_default)) }
                )
                rates.forEach { rate ->
                    FilterChip(
                        selected = selected != null && kotlin.math.abs(selected - rate) < 0.5f,
                        onClick = { onSelect(rate) },
                        enabled = enabled,
                        label = {
                            Text(
                                stringResource(
                                    R.string.refresh_rate_value,
                                    RefreshRateManager.formatRate(rate)
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerAppRefreshRateRow(
    app: InstalledAppInfo,
    rate: Float?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = app.icon, modifier = Modifier.size(36.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (rate != null) {
                        stringResource(R.string.refresh_rate_value, RefreshRateManager.formatRate(rate))
                    } else {
                        stringResource(R.string.per_app_refresh_rate_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rate != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (rate != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.per_app_refresh_rate_reset)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RefreshRateDialog(
    app: InstalledAppInfo,
    currentRate: Float?,
    supportedRates: List<Float>,
    onDismiss: () -> Unit,
    onConfirm: (Float?) -> Unit
) {
    var selected by remember { mutableStateOf(currentRate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.per_app_refresh_rate_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (supportedRates.isEmpty()) {
                    Text(
                        text = stringResource(R.string.refresh_rate_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        supportedRates.forEach { rate ->
                            FilterChip(
                                selected = selected != null && kotlin.math.abs(selected!! - rate) < 0.5f,
                                onClick = { selected = rate },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.refresh_rate_value,
                                            RefreshRateManager.formatRate(rate)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected != null) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (currentRate != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(stringResource(R.string.per_app_refresh_rate_reset))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
