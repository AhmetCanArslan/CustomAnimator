package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.DividerSoft
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.utils.LiveUpdatesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdatesScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current

    val permissionRefresh = rememberPermissionRefresh()
    val requestPermission = rememberPermissionRequester(onChanged = { permissionRefresh.intValue++ })
    val permissions = key(permissionRefresh.intValue) { permissionEntries(LIVE_UPDATES_PERMISSIONS) }

    var isEnabled by remember { mutableStateOf(LiveUpdatesManager.isEnabled(context)) }
    var isProgressEnabled by remember { mutableStateOf(LiveUpdatesManager.isProgressEnabled(context)) }
    var isNavigationEnabled by remember { mutableStateOf(LiveUpdatesManager.isNavigationEnabled(context)) }
    var isHideOriginalEnabled by remember { mutableStateOf(LiveUpdatesManager.isHideOriginalEnabled(context)) }
    var isCodesEnabled by remember { mutableStateOf(LiveUpdatesManager.isCodesEnabled(context)) }
    var navigationApps by remember { mutableStateOf(LiveUpdatesManager.getNavigationApps(context)) }

    val appList = rememberAppListState(selectedPackages = navigationApps)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.live_updates),
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
            if (!LiveUpdatesManager.isSupported()) {
                item { UnsupportedCard(stringResource(R.string.live_updates_unsupported)) }
                return@LazyColumn
            }

            item {
                Text(
                    text = stringResource(R.string.live_updates_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                PermissionSection(
                    title = stringResource(R.string.permissions_title),
                    entries = permissions,
                    onGrant = requestPermission
                )
            }

            item {
                AppCard(contentPadding = 16.dp, highlighted = isEnabled) {
                    ToggleRow(
                        title = stringResource(R.string.live_updates_enable),
                        subtitle = stringResource(R.string.live_updates_enable_desc),
                        icon = Icons.Filled.Upcoming,
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            LiveUpdatesManager.setEnabled(context, it)
                        }
                    )
                    DividerSoft()
                    ToggleRow(
                        title = stringResource(R.string.live_updates_progress),
                        subtitle = stringResource(R.string.live_updates_progress_desc),
                        icon = Icons.Filled.Downloading,
                        checked = isProgressEnabled,
                        enabled = isEnabled,
                        onCheckedChange = {
                            isProgressEnabled = it
                            LiveUpdatesManager.setProgressEnabled(context, it)
                        }
                    )
                    ToggleRow(
                        title = stringResource(R.string.live_updates_navigation),
                        subtitle = stringResource(R.string.live_updates_navigation_desc),
                        icon = Icons.Filled.Navigation,
                        checked = isNavigationEnabled,
                        enabled = isEnabled,
                        onCheckedChange = {
                            isNavigationEnabled = it
                            LiveUpdatesManager.setNavigationEnabled(context, it)
                        }
                    )
                    ToggleRow(
                        title = stringResource(R.string.live_updates_codes),
                        subtitle = stringResource(R.string.live_updates_codes_desc),
                        icon = Icons.Filled.Pin,
                        checked = isCodesEnabled,
                        enabled = isEnabled,
                        onCheckedChange = {
                            isCodesEnabled = it
                            LiveUpdatesManager.setCodesEnabled(context, it)
                        }
                    )
                    ToggleRow(
                        title = stringResource(R.string.live_updates_hide_original),
                        subtitle = stringResource(R.string.live_updates_hide_original_desc),
                        icon = Icons.Filled.NotificationsOff,
                        checked = isHideOriginalEnabled,
                        enabled = isEnabled,
                        onCheckedChange = {
                            isHideOriginalEnabled = it
                            LiveUpdatesManager.setHideOriginalEnabled(context, it)
                        }
                    )
                }
            }

            if (isNavigationEnabled) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.live_updates_navigation_apps),
                        subtitle = stringResource(R.string.live_updates_navigation_apps_desc)
                    )
                }

                appList(appList) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in navigationApps,
                        onCheckedChange = {
                            navigationApps = LiveUpdatesManager.toggleNavigationApp(context, app.packageName)
                        }
                    )
                }
            }
        }
    }
}
