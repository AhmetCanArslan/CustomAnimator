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
import com.arslan.customanimator.utils.DozeWhitelistManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DozeWhitelistScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var whitelisted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }

    LaunchedEffect(hasShizukuPermission) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        if (hasShizukuPermission) {
            whitelisted = withContext(Dispatchers.IO) { DozeWhitelistManager.getWhitelistedPackages() }
        }
        isLoading = false
    }

    val filteredApps by remember(apps, searchQuery, showSelectedOnly, whitelisted) {
        derivedStateOf {
            apps.filter { app ->
                (!showSelectedOnly || whitelisted.contains(app.packageName)) &&
                    (searchQuery.isBlank() ||
                        app.label.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val onToggle: (String, Boolean) -> Unit = { packageName, checked ->
        val previous = whitelisted
        whitelisted = if (checked) previous + packageName else previous - packageName
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                DozeWhitelistManager.setWhitelisted(packageName, checked)
            }
            if (success) {
                whitelisted = withContext(Dispatchers.IO) { DozeWhitelistManager.getWhitelistedPackages() }
                maybeShowInterstitial(context)
            } else {
                whitelisted = previous
                Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.doze_whitelist),
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
                InfoCard(
                    dismissKey = "doze_whitelist_disclaimer",
                    texts = listOf(stringResource(R.string.doze_whitelist_disclaimer))
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
                    text = stringResource(R.string.doze_whitelist_count, whitelisted.size),
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
                        checked = whitelisted.contains(app.packageName),
                        onCheckedChange = { checked -> onToggle(app.packageName, checked) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
