package com.riyadmondol2006.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.service.ForegroundAppWatcherService
import com.riyadmondol2006.customanimator.utils.PermissionDisablerManager
import com.riyadmondol2006.customanimator.utils.LocalShizukuAvailable
import com.riyadmondol2006.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPermissionDisablerScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean = LocalShizukuAvailable.current,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { PermissionDisablerManager(context) }

    var selectedPackages by remember { mutableStateOf(manager.getSelectedPackages()) }
    val appList = rememberAppListState(selectedPackages = selectedPackages)

    val requestNotificationPermission = rememberNotificationPermissionRequest()

    val syncServiceState: () -> Unit = {
        if (selectedPackages.isNotEmpty()) requestNotificationPermission()
        ForegroundAppWatcherService.sync(context)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.auto_permission_disabler),
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
            item(key = "info") {
                InfoCard(
                    dismissKey = "auto_permission_disabler_info",
                    texts = listOf(stringResource(R.string.close_apps_info)),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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

            item {
                Text(
                    text = if (selectedPackages.isNotEmpty() && hasShizukuPermission) {
                        stringResource(R.string.auto_permission_disabler_status_active, selectedPackages.size)
                    } else if (selectedPackages.isNotEmpty()) {
                        stringResource(R.string.auto_permission_disabler_status_paused)
                    } else {
                        stringResource(R.string.auto_permission_disabler_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            appList(appList) { app ->
                AppRow(
                    app = app,
                    checked = selectedPackages.contains(app.packageName),
                    onCheckedChange = {
                        selectedPackages = manager.togglePackage(app.packageName)
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
