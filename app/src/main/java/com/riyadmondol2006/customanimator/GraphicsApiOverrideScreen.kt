package com.riyadmondol2006.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.data.InstalledAppInfo
import com.riyadmondol2006.customanimator.utils.DeveloperOptionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.riyadmondol2006.customanimator.utils.LocalShizukuPermission
import com.riyadmondol2006.customanimator.utils.LocalWriteSecureSettings
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar
import com.riyadmondol2006.customanimator.ui.components.AppCard
import com.riyadmondol2006.customanimator.ui.components.ConnectedChoiceGroup
import com.riyadmondol2006.customanimator.utils.ShizukuHelper

private const val DRIVER_NATIVE = "native"
private const val DRIVER_ANGLE = "angle"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsApiOverrideScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    hasWriteSecureSettings: Boolean = LocalWriteSecureSettings.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val actionsEnabled = hasShizukuPermission || hasWriteSecureSettings

    var selections by remember {
        mutableStateOf(DeveloperOptionsManager.getAngleDriverSelections(contentResolver))
    }

    val appList = rememberAppListState()

    val onDriverSelected: (String, String?) -> Unit = { packageName, driver ->
        val previous = selections
        selections = if (driver == null) previous - packageName else previous + (packageName to driver)
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                DeveloperOptionsManager.setAngleDriverSelection(context, contentResolver, packageName, driver)
            }
            if (!success) {
                selections = previous
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.graphics_api_override),
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
                    dismissKey = "graphics_api_override_disclaimer",
                    texts = listOf(stringResource(R.string.graphics_api_override_disclaimer))
                )
            }

            if (!actionsEnabled) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.graphics_api_override_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            appList(appList) { app ->
                DriverSelectionRow(
                    app = app,
                    selectedDriver = selections[app.packageName],
                    enabled = actionsEnabled,
                    onDriverSelected = { driver -> onDriverSelected(app.packageName, driver) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverSelectionRow(
    app: InstalledAppInfo,
    selectedDriver: String?,
    enabled: Boolean,
    onDriverSelected: (String?) -> Unit
) {
    AppCard(contentPadding = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConnectedChoiceGroup(
                options = listOf(
                    null to stringResource(R.string.driver_default),
                    DRIVER_NATIVE to stringResource(R.string.driver_native),
                    DRIVER_ANGLE to stringResource(R.string.driver_angle)
                ),
                selected = selectedDriver,
                onSelect = onDriverSelected,
                enabled = enabled
            )
        }
    }
}
