package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.ForegroundAppWatcherService
import com.arslan.customanimator.utils.AppThreadingConfig
import com.arslan.customanimator.utils.AppThreadingManager
import com.arslan.customanimator.utils.CpuTopology
import com.arslan.customanimator.utils.ThreadAffinityMode
import com.arslan.customanimator.utils.ThreadPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.ConnectedChoiceGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThreadingScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val manager = remember { AppThreadingManager(context) }

    var configs by remember { mutableStateOf(manager.getConfigs()) }
    var coreSummary by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var priorityDenied by remember { mutableStateOf(false) }

    val requestNotificationPermission = rememberNotificationPermissionRequest()

    val syncServiceState: (Boolean) -> Unit = { hasConfigs ->
        if (hasConfigs) requestNotificationPermission()
        ForegroundAppWatcherService.sync(context)
    }

    LaunchedEffect(hasShizukuPermission) {
        if (hasShizukuPermission) {
            coreSummary = withContext(Dispatchers.IO) {
                Triple(CpuTopology.coreCount, CpuTopology.bigCoreCount(), CpuTopology.littleCoreCount())
            }
        }
    }

    val appList = rememberAppListState(selectedPackages = configs.keys)

    val updateConfig: (String, AppThreadingConfig) -> Unit = { packageName, config ->
        configs = if (config.isDefault) configs - packageName else configs + (packageName to config)
        val hasConfigs = configs.isNotEmpty()
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                manager.setConfig(packageName, config)
                if (config.isDefault) null else manager.apply(packageName, config)
            }
            if (result?.priorityDenied == true) priorityDenied = true
            syncServiceState(hasConfigs)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.app_threading),
                onBack = onBack,
                scrollBehavior = scrollBehavior
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
            item {
                InfoCard(
                    dismissKey = "app_threading_disclaimer",
                    texts = listOf(stringResource(R.string.app_threading_disclaimer))
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

            if (configs.isNotEmpty()) {
                item {
                    Text(
                        text = if (hasShizukuPermission) {
                            stringResource(R.string.app_threading_status_active, configs.size)
                        } else {
                            stringResource(R.string.app_threading_status_paused)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            if (priorityDenied) {
                item {
                    WarningCard(
                        message = stringResource(R.string.app_threading_priority_denied),
                        actionLabel = stringResource(R.string.app_threading_priority_use_normal),
                        onAction = {
                            configs.filterValues { it.priority == ThreadPriority.HIGH }
                                .forEach { (packageName, config) ->
                                    updateConfig(packageName, config.copy(priority = ThreadPriority.NORMAL))
                                }
                            priorityDenied = false
                        }
                    )
                }
            }

            appList(appList) { app ->
                ThreadingRow(
                    app = app,
                    config = configs[app.packageName] ?: AppThreadingConfig(),
                    enabled = hasShizukuPermission,
                    onConfigChange = { config -> updateConfig(app.packageName, config) }
                )
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
    AppCard(contentPadding = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
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
            ConnectedChoiceGroup(
                options = listOf(
                    ThreadAffinityMode.ALL to stringResource(R.string.app_threading_cores_all),
                    ThreadAffinityMode.BIG to stringResource(R.string.app_threading_cores_big),
                    ThreadAffinityMode.LITTLE to stringResource(R.string.app_threading_cores_little)
                ),
                selected = config.affinity,
                onSelect = { onConfigChange(config.copy(affinity = it)) },
                enabled = enabled
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_threading_priority_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            ConnectedChoiceGroup(
                options = listOf(
                    ThreadPriority.HIGH to stringResource(R.string.app_threading_priority_high),
                    ThreadPriority.NORMAL to stringResource(R.string.app_threading_priority_normal),
                    ThreadPriority.LOW to stringResource(R.string.app_threading_priority_low)
                ),
                selected = config.priority,
                onSelect = { onConfigChange(config.copy(priority = it)) },
                enabled = enabled
            )
        }
    }
}
