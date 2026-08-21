package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.PerAppWidthService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.PerAppWidthManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppWidthScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { PerAppWidthManager(context) }

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var overrides by remember { mutableStateOf(manager.getOverrides()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }
    var hasUsageAccess by remember { mutableStateOf(UsageAccessHelper.hasUsageAccess(context)) }
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

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        isLoading = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = UsageAccessHelper.hasUsageAccess(context)
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
            PerAppWidthService.start(context)
        } else {
            PerAppWidthService.stop(context)
        }
    }

    editingApp?.let { app ->
        PerAppWidthDialog(
            app = app,
            currentWidth = overrides[app.packageName],
            onDismiss = { editingApp = null },
            onConfirm = { widthDp ->
                manager.setWidth(app.packageName, widthDp)
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
                        stringResource(R.string.per_app_width),
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
                            Text(stringResource(R.string.per_app_width_clear_all))
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
                    dismissKey = "per_app_width_disclaimer",
                    texts = listOf(stringResource(R.string.per_app_width_disclaimer))
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

            if (!hasUsageAccess) {
                item {
                    WarningCard(
                        message = stringResource(R.string.per_app_width_needs_usage_access),
                        actionLabel = stringResource(R.string.open_usage_access_settings),
                        onAction = { UsageAccessHelper.openUsageAccessSettings(context) }
                    )
                }
            }

            item {
                Text(
                    text = if (overrides.isNotEmpty() && hasShizukuPermission && hasUsageAccess) {
                        stringResource(R.string.per_app_width_status_active, overrides.size)
                    } else if (overrides.isNotEmpty()) {
                        stringResource(R.string.per_app_width_status_paused)
                    } else {
                        stringResource(R.string.per_app_width_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLoading && apps.isNotEmpty()) {
                item {
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
                    PerAppWidthRow(
                        app = app,
                        widthDp = overrides[app.packageName],
                        onClick = { editingApp = app },
                        onClear = {
                            manager.setWidth(app.packageName, null)
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

@Composable
private fun PerAppWidthRow(
    app: InstalledAppInfo,
    widthDp: Int?,
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
                    text = if (widthDp != null) {
                        stringResource(R.string.per_app_width_value, widthDp)
                    } else {
                        stringResource(R.string.per_app_width_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (widthDp != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (widthDp != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.per_app_width_reset)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun PerAppWidthDialog(
    app: InstalledAppInfo,
    currentWidth: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val context = LocalContext.current
    val deviceWidth = SettingsManager.getSmallestWidth(context)
    var value by remember { mutableStateOf(currentWidth?.toString() ?: deviceWidth.toString()) }
    val parsed = value.toIntOrNull()
    val isValid = parsed != null && parsed in PerAppWidthManager.MIN_WIDTH..PerAppWidthManager.MAX_WIDTH
    val previewDensity = if (isValid) SettingsManager.densityForSmallestWidth(context, parsed!!) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.per_app_width_dialog_desc, deviceWidth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.per_app_width_label)) },
                    singleLine = true,
                    isError = value.isNotEmpty() && !isValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(
                        R.string.per_app_width_range,
                        PerAppWidthManager.MIN_WIDTH,
                        PerAppWidthManager.MAX_WIDTH
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                previewDensity?.let { density ->
                    Text(
                        text = stringResource(R.string.per_app_width_density_preview, density),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsed) }, enabled = isValid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (currentWidth != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text(stringResource(R.string.per_app_width_reset))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
