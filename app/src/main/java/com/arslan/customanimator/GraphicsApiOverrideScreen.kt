package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DRIVER_NATIVE = "native"
private const val DRIVER_ANGLE = "angle"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsApiOverrideScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selections by remember {
        mutableStateOf(DeveloperOptionsManager.getAngleDriverSelections(contentResolver))
    }

    val filteredApps by remember(apps, searchQuery) {
        derivedStateOf {
            apps.filter {
                searchQuery.isBlank() ||
                    it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        isLoading = false
    }

    val onDriverSelected: (String, String?) -> Unit = { packageName, driver ->
        val previous = selections
        selections = if (driver == null) previous - packageName else previous + (packageName to driver)
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                DeveloperOptionsManager.setAngleDriverSelection(context, contentResolver, packageName, driver)
            }
            if (!success) {
                selections = previous
                Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.graphics_api_override),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.graphics_api_override_disclaimer),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!hasShizukuPermission) {
                item {
                    WarningCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        actionLabel = stringResource(R.string.grant_shizuku_permission),
                        onAction = { ShizukuHelper.requestShizukuPermission(context) }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.graphics_api_override_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLoading && apps.isNotEmpty()) {
                item {
                    AppSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        showFilterCheckbox = false
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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    DriverSelectionRow(
                        app = app,
                        selectedDriver = selections[app.packageName],
                        enabled = hasShizukuPermission,
                        onDriverSelected = { driver -> onDriverSelected(app.packageName, driver) }
                    )
                }
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(icon = app.icon, modifier = Modifier.size(36.dp))
                Text(
                    text = app.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedDriver == null,
                    onClick = { onDriverSelected(null) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text(stringResource(R.string.driver_default), fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = selectedDriver == DRIVER_NATIVE,
                    onClick = { onDriverSelected(DRIVER_NATIVE) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text(stringResource(R.string.driver_native), fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = selectedDriver == DRIVER_ANGLE,
                    onClick = { onDriverSelected(DRIVER_ANGLE) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text(stringResource(R.string.driver_angle), fontSize = 11.sp)
                }
            }
        }
    }
}
