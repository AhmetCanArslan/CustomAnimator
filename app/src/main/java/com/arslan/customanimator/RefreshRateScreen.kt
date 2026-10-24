package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.ForegroundAppWatcherService
import com.arslan.customanimator.utils.PerAppRefreshRateManager
import com.arslan.customanimator.utils.RefreshRateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RefreshRateScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
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

    var overrides by remember { mutableStateOf(manager.getOverrides()) }
    var editingApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    val appList = rememberAppListState(selectedPackages = overrides.keys)

    LaunchedEffect(hasShizukuPermission) {
        val loadedMin = withContext(Dispatchers.IO) { RefreshRateManager.getMinRate(context.contentResolver) }
        val loadedPeak = withContext(Dispatchers.IO) { RefreshRateManager.getPeakRate(context.contentResolver) }
        minRate = loadedMin
        peakRate = loadedPeak
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

    val requestNotificationPermission = rememberNotificationPermissionRequest()

    val syncServiceState: () -> Unit = {
        if (overrides.isNotEmpty()) requestNotificationPermission()
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.refresh_rate),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (overrides.isNotEmpty()) {
                        TextButton(onClick = {
                            manager.clearAll()
                            overrides = manager.getOverrides()
                            syncServiceState()
                        }, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.per_app_refresh_rate_clear_all))
                        }
                    }
                }
            )
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
                AppCard(contentPadding = 0.dp) {
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

            item(key = "per_app_status") {
                Text(
                    text = if (overrides.isNotEmpty() && hasShizukuPermission) {
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

            appList(appList) { app ->
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
    AppCard(contentPadding = 0.dp) {
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
    AppCard(contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
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
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp), shapes = IconButtonDefaults.shapes()) {
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
            TextButton(onClick = { onConfirm(selected) }, enabled = selected != null, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (currentRate != null) {
                    TextButton(onClick = { onConfirm(null) }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.per_app_refresh_rate_reset))
                    }
                }
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
