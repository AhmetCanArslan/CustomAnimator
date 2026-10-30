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
import com.riyadmondol2006.customanimator.utils.DozeWhitelistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.riyadmondol2006.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar
import com.riyadmondol2006.customanimator.utils.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DozeWhitelistScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var whitelisted by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(hasShizukuPermission) {
        if (hasShizukuPermission) {
            whitelisted = withContext(Dispatchers.IO) { DozeWhitelistManager.getWhitelistedPackages() }
        }
    }

    val appList = rememberAppListState(selectedPackages = whitelisted)

    val onToggle: (String, Boolean) -> Unit = { packageName, checked ->
        val previous = whitelisted
        whitelisted = if (checked) previous + packageName else previous - packageName
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                DozeWhitelistManager.setWhitelisted(packageName, checked)
            }
            if (success) {
                whitelisted = withContext(Dispatchers.IO) { DozeWhitelistManager.getWhitelistedPackages() }
            } else {
                whitelisted = previous
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.doze_whitelist),
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

            appList(appList) { app ->
                AppRow(
                    app = app,
                    checked = whitelisted.contains(app.packageName),
                    onCheckedChange = { checked -> onToggle(app.packageName, checked) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
