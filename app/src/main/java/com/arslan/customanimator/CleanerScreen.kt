package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.CloseAppsExclusionManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CleanerAction { CLEAR_CACHES, CLOSE_APPS }

@Composable
fun CleanerScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    onNavigateToCloseAppsExclusions: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var runningAction by remember { mutableStateOf<CleanerAction?>(null) }
    var showClearCachesConfirm by remember { mutableStateOf(false) }
    var showCloseAppsConfirm by remember { mutableStateOf(false) }

    val gate = rememberRewardGate("cleaner")
    val actionsEnabled = hasShizukuPermission && runningAction == null && gate.unlocked

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                RewardGateCard(
                    gate = gate,
                    description = stringResource(R.string.reward_gate_cleaner_desc)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        QuickActionRow(
                            icon = Icons.Filled.CleaningServices,
                            title = stringResource(R.string.clear_all_app_caches),
                            description = stringResource(R.string.clear_all_app_caches_desc),
                            enabled = actionsEnabled,
                            isRunning = runningAction == CleanerAction.CLEAR_CACHES,
                            onClick = { showClearCachesConfirm = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        QuickActionRow(
                            icon = Icons.Filled.StopCircle,
                            title = stringResource(R.string.close_background_apps),
                            description = stringResource(R.string.close_background_apps_desc),
                            enabled = actionsEnabled,
                            isRunning = runningAction == CleanerAction.CLOSE_APPS,
                            onClick = { showCloseAppsConfirm = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Block,
                            title = stringResource(R.string.close_apps_exclusions),
                            description = stringResource(R.string.close_apps_exclusions_desc),
                            enabled = gate.unlocked,
                            onClick = onNavigateToCloseAppsExclusions
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showClearCachesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCachesConfirm = false },
            title = { Text(stringResource(R.string.clear_caches_confirm_title)) },
            text = { Text(stringResource(R.string.clear_caches_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showClearCachesConfirm = false
                    runningAction = CleanerAction.CLEAR_CACHES
                    coroutineScope.launch {
                        val success = withContext(Dispatchers.IO) { DeveloperOptionsManager.clearAllAppCaches() }
                        runningAction = null
                        Toast.makeText(
                            context,
                            context.getString(if (success) R.string.action_succeeded else R.string.action_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text(stringResource(R.string.clear_all_app_caches))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearCachesConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCloseAppsConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseAppsConfirm = false },
            title = { Text(stringResource(R.string.close_apps_confirm_title)) },
            text = { Text(stringResource(R.string.close_apps_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showCloseAppsConfirm = false
                    runningAction = CleanerAction.CLOSE_APPS
                    coroutineScope.launch {
                        val closedCount = withContext(Dispatchers.IO) {
                            val skip = CloseAppsExclusionManager(context).getSelectedPackages() +
                                InstalledAppsProvider.getUnsafeToKillPackages(context)
                            InstalledAppsProvider.getLaunchableApps(context)
                                .filterNot { skip.contains(it.packageName) }
                                .count { DeveloperOptionsManager.forceStopApp(it.packageName) }
                        }
                        runningAction = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.apps_closed_count, closedCount),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text(stringResource(R.string.close_background_apps))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCloseAppsConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
