package com.arslan.customanimator

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutActivityInfo
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.theme.horizontalPagerTransition
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.ShortcutMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class ActivityManagerApp(val app: InstalledAppInfo, val activities: List<ShortcutActivityInfo>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityManagerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val appList = rememberAppListState()
    var selected by remember { mutableStateOf<ActivityManagerApp?>(null) }
    var isRoot by remember { mutableStateOf(false) }

    LaunchedEffect(hasShizukuPermission) {
        isRoot = withContext(Dispatchers.IO) { hasShizukuPermission && ShizukuHelper.isRunningAsRoot() }
    }

    val onAppPicked: (InstalledAppInfo) -> Unit = { app ->
        coroutineScope.launch {
            selected = withContext(Dispatchers.IO) {
                ActivityManagerApp(app, ShortcutMaker.listActivities(context, app.packageName))
            }
        }
    }
    val onActivityPicked: (InstalledAppInfo, ShortcutActivityInfo) -> Unit = { app, activity ->
        val entry = ShortcutMaker.activityEntry(activity.name, app.packageName, activity)
        coroutineScope.launch {
            val launched = withContext(Dispatchers.IO) { ShortcutMaker.launch(context, entry) }
            if (!launched) {
                Toast.makeText(context, resources.getString(ShortcutMaker.failureMessageRes(entry)), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = selected != null) { selected = null }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.activity_manager),
                onBack = { if (selected == null) onBack() else selected = null },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selected,
            contentKey = { it?.app?.packageName },
            transitionSpec = { horizontalPagerTransition(forward = targetState != null) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding(),
            label = "activity manager step"
        ) { picked ->
            if (picked == null) {
                ActivityManagerAppList(appList = appList, listState = listState, onPick = onAppPicked)
            } else {
                ActivityManagerActivities(
                    picked = picked,
                    hasShizukuPermission = hasShizukuPermission,
                    isRoot = isRoot,
                    onAppClick = { selected = null },
                    onPick = { activity -> onActivityPicked(picked.app, activity) }
                )
            }
        }
    }
}

@Composable
private fun ActivityManagerAppList(
    appList: AppListState,
    listState: LazyListState,
    onPick: (InstalledAppInfo) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            InfoCard(
                dismissKey = "activity_manager_info",
                texts = listOf(stringResource(R.string.activity_manager_info))
            )
        }

        appList(appList) { app ->
            PickerRow(title = app.label, subtitle = app.packageName, onClick = { onPick(app) }) {
                AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActivityManagerActivities(
    picked: ActivityManagerApp,
    hasShizukuPermission: Boolean,
    isRoot: Boolean,
    onAppClick: () -> Unit,
    onPick: (ShortcutActivityInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "activity_app") {
            PickerRow(
                title = picked.app.label,
                subtitle = picked.app.packageName,
                selected = true,
                onClick = onAppClick
            ) {
                AppIcon(packageName = picked.app.packageName, modifier = Modifier.size(40.dp))
            }
        }

        item(key = "activity_note") {
            PickerMessage(stringResource(R.string.shortcut_activity_note))
        }

        activityList(
            app = picked.app,
            activities = picked.activities,
            query = query,
            onQueryChange = { query = it },
            hasShizukuPermission = hasShizukuPermission,
            isRoot = isRoot,
            onPick = onPick
        )

        item(key = "activity_bottom") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
