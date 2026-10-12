package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.utils.CloseAppsExclusionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseAppsExclusionsScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val manager = remember { CloseAppsExclusionManager(context) }

    var excludedPackages by remember { mutableStateOf(manager.getSelectedPackages()) }

    val appList = rememberAppListState(selectedPackages = excludedPackages)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.close_apps_exclusions),
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
            item {
                InfoCard(
                    dismissKey = "close_apps_exclusions_info",
                    texts = listOf(stringResource(R.string.close_apps_info)),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            item {
                Text(
                    text = if (excludedPackages.isEmpty()) {
                        stringResource(R.string.close_apps_exclusions_desc)
                    } else {
                        stringResource(R.string.close_apps_exclusions_count, excludedPackages.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            appList(appList) { app ->
                AppRow(
                    app = app,
                    checked = excludedPackages.contains(app.packageName),
                    onCheckedChange = { excludedPackages = manager.togglePackage(app.packageName) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
