package com.riyadmondol2006.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.riyadmondol2006.customanimator.utils.GameModeController
import com.riyadmondol2006.customanimator.utils.GameModeExclusionManager
import com.riyadmondol2006.customanimator.utils.GameModeManager
import com.riyadmondol2006.customanimator.utils.GameModeTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.riyadmondol2006.customanimator.utils.LocalShizukuPermission
import com.riyadmondol2006.customanimator.ui.components.AppCard
import com.riyadmondol2006.customanimator.ui.components.ConnectedChoiceGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeScreenContent(
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { GameModeManager(context) }
    val exclusionManager = remember { GameModeExclusionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedGames by remember { mutableStateOf(manager.getSelectedPackages()) }
    var excludedApps by remember { mutableStateOf(exclusionManager.getSelectedPackages()) }
    var editingExclusions by remember { mutableStateOf(false) }
    val listedPackages = if (editingExclusions) excludedApps else selectedGames
    var isBusy by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(GameModeController.isActive(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isBusy) isActive = GameModeController.isActive(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var targetCount by remember { mutableStateOf(0) }

    val appList = rememberAppListState(selectedPackages = listedPackages)

    LaunchedEffect(selectedGames, excludedApps, appList.isLoading) {
        if (!appList.isLoading) {
            targetCount = withContext(Dispatchers.IO) {
                GameModeTargets.resolve(context, selectedGames, excludedApps).size
            }
        }
    }

    val toggle: (Boolean) -> Unit = { enable ->
        isBusy = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { GameModeController.setActive(context, enable) }
            isActive = GameModeController.isActive(context)
            isBusy = false
            android.widget.Toast.makeText(
                context,
                resources.getString(
                    when {
                        !result.succeeded -> R.string.game_mode_failed_toast
                        enable -> R.string.game_mode_enabled_toast
                        else -> R.string.game_mode_disabled_toast
                    }
                ),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.game_mode_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                AppCard(contentPadding = 0.dp) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isActive) {
                                stringResource(R.string.game_mode_status_active)
                            } else {
                                stringResource(R.string.game_mode_status_inactive)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.game_mode_target_count, targetCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                when {
                                    !hasShizukuPermission -> Toast.makeText(
                                        context,
                                        resources.getString(R.string.game_mode_needs_shizuku),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isActive -> toggle(false)
                                    selectedGames.isEmpty() -> Toast.makeText(
                                        context,
                                        resources.getString(R.string.game_mode_select_games_first),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    else -> toggle(true)
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(
                                text = when {
                                    isBusy -> stringResource(R.string.working)
                                    isActive -> stringResource(R.string.game_mode_turn_off)
                                    else -> stringResource(R.string.game_mode_turn_on)
                                }
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(
                    dismissKey = "game_mode_info",
                    texts = listOf(
                        stringResource(R.string.game_mode_info),
                        stringResource(R.string.game_mode_fixed_performance)
                    )
                )
            }

            item {
                GameModeListSwitch(
                    editingExclusions = editingExclusions,
                    onEditingExclusionsChange = { editingExclusions = it }
                )
            }

            item {
                Text(
                    text = when {
                        editingExclusions && excludedApps.isEmpty() -> stringResource(R.string.game_mode_select_excluded)
                        editingExclusions -> stringResource(R.string.game_mode_excluded_count, excludedApps.size)
                        selectedGames.isEmpty() -> stringResource(R.string.game_mode_select_games)
                        else -> stringResource(R.string.game_mode_games_count, selectedGames.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            appList(appList) { app ->
                AppRow(
                    app = app,
                    checked = listedPackages.contains(app.packageName),
                    onCheckedChange = {
                        if (editingExclusions) {
                            excludedApps = exclusionManager.togglePackage(app.packageName)
                        } else {
                            selectedGames = manager.togglePackage(app.packageName)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameModeListSwitch(editingExclusions: Boolean, onEditingExclusionsChange: (Boolean) -> Unit) {
    ConnectedChoiceGroup(
        options = listOf(
            false to stringResource(R.string.game_mode_list_games),
            true to stringResource(R.string.game_mode_list_excluded)
        ),
        selected = editingExclusions,
        onSelect = onEditingExclusionsChange
    )
}
