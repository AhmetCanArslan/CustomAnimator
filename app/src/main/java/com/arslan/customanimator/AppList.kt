package com.arslan.customanimator

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.utils.AppIconBitmaps
import com.arslan.customanimator.utils.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PREWARMED_ICONS = 12
private const val PRELOAD_DELAY_MS = 1200L

@Stable
class AppListState internal constructor(private val include: (InstalledAppInfo) -> Boolean) {
    private var loaded by mutableStateOf(InstalledAppsProvider.cachedLaunchableApps())
    internal var selectedPackages by mutableStateOf<Set<String>?>(null)
    var query by mutableStateOf("")
    var showSelectedOnly by mutableStateOf(false)

    val isLoading: Boolean get() = loaded == null

    val apps: List<InstalledAppInfo> by derivedStateOf { loaded.orEmpty().filter(include) }

    val visibleApps: List<InstalledAppInfo> by derivedStateOf {
        val selected = selectedPackages.takeIf { showSelectedOnly }
        apps.filter { app ->
            (selected == null || app.packageName in selected) &&
                (app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true))
        }
    }

    suspend fun reload(context: Context) {
        val fresh = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        if (loaded == null) {
            fresh.filter(include).take(PREWARMED_ICONS).forEach { app -> AppIconBitmaps.load(context, app.packageName) }
        }
        loaded = fresh
    }
}

@Composable
fun rememberAppListState(
    selectedPackages: Set<String>? = null,
    include: (InstalledAppInfo) -> Boolean = { true }
): AppListState {
    val context = LocalContext.current
    val state = remember { AppListState(include) }
    SideEffect { state.selectedPackages = selectedPackages }
    LaunchedEffect(state) { state.reload(context) }
    return state
}

@Composable
fun PreloadInstalledApps() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(PRELOAD_DELAY_MS)
        withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
    }
}

fun LazyListScope.appList(
    state: AppListState,
    searchPlaceholder: String? = null,
    row: @Composable (InstalledAppInfo) -> Unit
) {
    if (state.isLoading) {
        item(key = "app_list_loading") {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }
    item(key = "app_list_search") {
        AppSearchBar(
            searchQuery = state.query,
            onSearchQueryChange = { state.query = it },
            showSelectedOnly = state.showSelectedOnly,
            onShowSelectedOnlyChange = { state.showSelectedOnly = it },
            showFilterCheckbox = state.selectedPackages != null,
            placeholder = searchPlaceholder
        )
    }
    if (state.visibleApps.isEmpty()) {
        item(key = "app_list_empty") {
            Text(
                stringResource(R.string.no_apps_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    items(state.visibleApps, key = { it.packageName }) { app -> row(app) }
}
