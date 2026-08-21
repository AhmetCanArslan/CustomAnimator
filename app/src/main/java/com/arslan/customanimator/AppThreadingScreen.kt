package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.AppThreadingConfig
import com.arslan.customanimator.utils.AppThreadingManager
import com.arslan.customanimator.utils.CpuTopology
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.ThreadAffinityMode
import com.arslan.customanimator.utils.ThreadPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThreadingScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val manager = remember { AppThreadingManager(context) }

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var configs by remember { mutableStateOf(manager.getConfigs()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }
    var coreSummary by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    val isAdFree by rememberIsAdFree()

    LaunchedEffect(hasShizukuPermission) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        if (hasShizukuPermission) {
            coreSummary = withContext(Dispatchers.IO) {
                Triple(CpuTopology.coreCount, CpuTopology.bigCoreCount(), CpuTopology.littleCoreCount())
            }
        }
        isLoading = false
    }

    val filteredApps by remember(apps, searchQuery, showSelectedOnly, configs) {
        derivedStateOf {
            apps.filter { app ->
                (!showSelectedOnly || configs.containsKey(app.packageName)) &&
                    (searchQuery.isBlank() ||
                        app.label.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val updateConfig: (String, AppThreadingConfig) -> Unit = { packageName, config ->
        configs = if (config.isDefault) configs - packageName else configs + (packageName to config)
        coroutineScope.launch {
            withContext(Dispatchers.IO) { manager.setConfig(packageName, config) }
        }
    }

    val applyAll: () -> Unit = {
        coroutineScope.launch {
            val applied = withContext(Dispatchers.IO) { manager.applyAll() }
            Toast.makeText(
                context,
                context.getString(R.string.app_threading_applied, applied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_threading),
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.app_threading_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            coreSummary?.let { (total, big, little) ->
                item {
                    Text(
                        text = stringResource(R.string.app_threading_cores, total, big, little),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.app_threading_reapply),
                    description = stringResource(R.string.app_threading_reapply_desc),
                    buttonLabel = stringResource(
                        if (isAdFree) R.string.apply_settings else R.string.app_threading_apply_with_ad
                    ),
                    enabled = hasShizukuPermission && configs.isNotEmpty(),
                    onClick = {
                        if (isAdFree) applyAll() else requestReward(context) { applyAll() }
                    }
                )
                if (hasShizukuPermission && configs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.app_threading_apply_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                }
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
                    ThreadingRow(
                        app = app,
                        config = configs[app.packageName] ?: AppThreadingConfig(),
                        enabled = hasShizukuPermission,
                        onConfigChange = { config -> updateConfig(app.packageName, config) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadingRow(
    app: InstalledAppInfo,
    config: AppThreadingConfig,
    enabled: Boolean,
    onConfigChange: (AppThreadingConfig) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(icon = app.icon, modifier = Modifier.size(36.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_threading_cores_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThreadAffinityMode.ALL to R.string.app_threading_cores_all,
                    ThreadAffinityMode.BIG to R.string.app_threading_cores_big,
                    ThreadAffinityMode.LITTLE to R.string.app_threading_cores_little
                )
                options.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = config.affinity == mode,
                        enabled = enabled,
                        onClick = { onConfigChange(config.copy(affinity = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_threading_priority_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThreadPriority.HIGH to R.string.app_threading_priority_high,
                    ThreadPriority.NORMAL to R.string.app_threading_priority_normal,
                    ThreadPriority.LOW to R.string.app_threading_priority_low
                )
                options.forEachIndexed { index, (priority, labelRes) ->
                    SegmentedButton(
                        selected = config.priority == priority,
                        enabled = enabled,
                        onClick = { onConfigChange(config.copy(priority = priority)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
