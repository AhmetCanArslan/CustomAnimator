package com.arslan.customanimator

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.service.ForegroundAppWatcherService
import com.arslan.customanimator.utils.AppIconBitmaps
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.LocalShizukuAvailable
import com.arslan.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoForceStopScreen(
    onBack: () -> Unit,
    isShizukuAvailable: Boolean = LocalShizukuAvailable.current,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { AutoForceStopManager(context) }

    var selectedPackages by remember { mutableStateOf(manager.getSelectedPackages()) }
    val appList = rememberAppListState(selectedPackages = selectedPackages)

    val requestNotificationPermission = rememberNotificationPermissionRequest()

    val syncServiceState: () -> Unit = {
        if (selectedPackages.isNotEmpty()) requestNotificationPermission()
        ForegroundAppWatcherService.sync(context)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.auto_force_stop),
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
            item(key = "info") {
                InfoCard(
                    dismissKey = "auto_force_stop_info",
                    texts = listOf(stringResource(R.string.close_apps_info)),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    text = if (selectedPackages.isNotEmpty() && hasShizukuPermission) {
                        stringResource(R.string.auto_force_stop_status_active, selectedPackages.size)
                    } else if (selectedPackages.isNotEmpty()) {
                        stringResource(R.string.auto_force_stop_status_paused)
                    } else {
                        stringResource(R.string.auto_force_stop_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            appList(appList) { app ->
                AppRow(
                    app = app,
                    checked = selectedPackages.contains(app.packageName),
                    onCheckedChange = {
                        selectedPackages = manager.togglePackage(app.packageName)
                        syncServiceState()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AppSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showSelectedOnly: Boolean = false,
    onShowSelectedOnlyChange: (Boolean) -> Unit = {},
    showFilterCheckbox: Boolean = true,
    placeholder: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder ?: stringResource(R.string.search_apps)) },
            singleLine = true,
            leadingIcon = { AnimatedSearchIcon(focused) },
            interactionSource = interactionSource,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, shapes = IconButtonDefaults.shapes()) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            shape = RoundedCornerShape(12.dp)
        )
        if (showFilterCheckbox) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showSelectedOnly,
                    onCheckedChange = onShowSelectedOnlyChange
                )
                Text(
                    text = stringResource(R.string.show_selected_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedSearchIcon(focused: Boolean) {
    val progress by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "searchIconFocus"
    )
    val tint by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "searchIconTint"
    )
    Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.graphicsLayer {
            val scale = 1f + progress * SEARCH_ICON_FOCUS_SCALE
            scaleX = scale
            scaleY = scale
            rotationZ = progress * SEARCH_ICON_FOCUS_ROTATION
        }
    )
}

private const val SEARCH_ICON_FOCUS_SCALE = 0.15f
private const val SEARCH_ICON_FOCUS_ROTATION = -20f

@Composable
fun WarningCard(message: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.align(Alignment.End), shapes = ButtonDefaults.shapes()) {
                Text(actionLabel, style = MaterialTheme.typography.bodySmall,)
            }
        }
    }
}

@Composable
fun AppRow(
    app: InstalledAppInfo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AppCard(contentPadding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf(AppIconBitmaps.cached(packageName)) }
    LaunchedEffect(packageName) {
        if (bitmap == null) bitmap = AppIconBitmaps.load(context, packageName)
    }
    AppIconImage(bitmap, modifier)
}

@Composable
fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Box(modifier = modifier)
        return
    }
    var bitmap by remember(icon) { mutableStateOf(AppIconBitmaps.cached(icon)) }
    LaunchedEffect(icon) {
        if (bitmap == null) bitmap = AppIconBitmaps.load(icon)
    }
    AppIconImage(bitmap, modifier)
}

@Composable
private fun AppIconImage(bitmap: ImageBitmap?, modifier: Modifier) {
    if (bitmap == null) {
        Box(modifier = modifier)
    } else {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    }
}
