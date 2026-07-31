package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.AutoForceStopService
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.PermissionDisablerManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPermissionDisablerScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val manager = remember { PermissionDisablerManager(context) }
    val otherManager = remember { AutoForceStopManager(context) }

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedPackages by remember { mutableStateOf(manager.getSelectedPackages()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasUsageAccess by remember { mutableStateOf(UsageAccessHelper.hasUsageAccess(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }
    val filteredApps by remember(apps, searchQuery, showSelectedOnly, selectedPackages) {
        derivedStateOf {
            apps
                .filter {
                    searchQuery.isBlank() ||
                        it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
                .filter { !showSelectedOnly || selectedPackages.contains(it.packageName) }
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        isLoading = false
    }

    // Re-check usage access whenever the screen resumes to the composition
    // (e.g. after returning from the Usage Access settings screen).
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

    val prerequisitesMet = hasShizukuPermission && hasUsageAccess

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless; service still runs without a visible notification if denied */ }

    val syncServiceState: () -> Unit = {
        if (selectedPackages.isNotEmpty() && prerequisitesMet) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            AutoForceStopService.start(context)
        } else if (otherManager.getSelectedPackages().isEmpty()) {
            AutoForceStopService.stop(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.auto_permission_disabler),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.close_apps_info),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!hasShizukuPermission) {
                item {
                    WarningCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        actionLabel = stringResource(R.string.grant_shizuku_permission),
                        onAction = { ShizukuHelper.requestShizukuPermission(context) }
                    )
                }
            }

            if (!hasUsageAccess) {
                item {
                    WarningCard(
                        message = stringResource(R.string.auto_permission_disabler_needs_usage_access),
                        actionLabel = stringResource(R.string.open_usage_access_settings),
                        onAction = { UsageAccessHelper.openUsageAccessSettings(context) }
                    )
                }
            }

            item {
                Text(
                    text = if (selectedPackages.isNotEmpty() && prerequisitesMet) {
                        stringResource(R.string.auto_permission_disabler_status_active, selectedPackages.size)
                    } else if (selectedPackages.isNotEmpty()) {
                        stringResource(R.string.auto_permission_disabler_status_paused)
                    } else {
                        stringResource(R.string.auto_permission_disabler_desc)
                    },
                    fontSize = 12.sp,
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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = selectedPackages.contains(app.packageName),
                        onCheckedChange = {
                            selectedPackages = manager.togglePackage(app.packageName)
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
