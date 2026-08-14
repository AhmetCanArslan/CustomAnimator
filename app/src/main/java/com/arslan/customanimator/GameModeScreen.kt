package com.arslan.customanimator

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.GameModeController
import com.arslan.customanimator.utils.GameModeManager
import com.arslan.customanimator.utils.GameModeTargets
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { GameModeManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedGames by remember { mutableStateOf(manager.getSelectedPackages()) }
    var isLoading by remember { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(GameModeController.isActive(context)) }
    var targetCount by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }

    val filteredApps by remember(apps, searchQuery, showSelectedOnly, selectedGames) {
        derivedStateOf {
            apps
                .filter {
                    searchQuery.isBlank() ||
                        it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
                .filter { !showSelectedOnly || selectedGames.contains(it.packageName) }
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        isLoading = false
    }

    LaunchedEffect(selectedGames, isLoading) {
        if (!isLoading) {
            targetCount = withContext(Dispatchers.IO) {
                GameModeTargets.resolve(context, selectedGames).size
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
                context.getString(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.game_mode),
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
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.game_mode_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
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
                            onClick = { toggle(!isActive) },
                            enabled = hasShizukuPermission && !isBusy && (isActive || selectedGames.isNotEmpty()),
                            modifier = Modifier.fillMaxWidth()
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = AppShapes.card
                ) {
                    Text(
                        text = stringResource(R.string.game_mode_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                Text(
                    text = if (selectedGames.isEmpty()) {
                        stringResource(R.string.game_mode_select_games)
                    } else {
                        stringResource(R.string.game_mode_games_count, selectedGames.size)
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
                    AppRow(
                        app = app,
                        checked = selectedGames.contains(app.packageName),
                        onCheckedChange = { selectedGames = manager.togglePackage(app.packageName) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
