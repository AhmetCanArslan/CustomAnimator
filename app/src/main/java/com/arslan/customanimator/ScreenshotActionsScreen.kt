package com.arslan.customanimator

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.screenshot.ScreenshotOverlay
import com.arslan.customanimator.screenshot.ScreenshotPermissions
import com.arslan.customanimator.screenshot.ScreenshotPrefs
import com.arslan.customanimator.service.ScreenshotWatcherService
import com.arslan.customanimator.ui.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotActionsScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val prefs = remember { ScreenshotPrefs(context) }

    var permRefresh by remember { mutableIntStateOf(0) }
    val hasImages = remember(permRefresh) { ScreenshotPermissions.hasImagesPermission(context) }
    val hasNotif = remember(permRefresh) { ScreenshotPermissions.hasNotificationPermission(context) }
    val hasOverlayPerm = remember(permRefresh) { ScreenshotPermissions.hasOverlayPermission(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var watcherEnabled by remember { mutableStateOf(prefs.watcherEnabled) }
    var overlayEnabled by remember { mutableStateOf(prefs.overlayEnabled) }
    var delaySeconds by remember { mutableIntStateOf(prefs.notificationDelaySeconds) }
    var notifShowCopy by remember { mutableStateOf(prefs.notificationShowCopy) }
    var notifShowDelete by remember { mutableStateOf(prefs.notificationShowDelete) }
    var notifShowPreview by remember { mutableStateOf(prefs.notificationShowPreview) }
    var overlayShowCopy by remember { mutableStateOf(prefs.overlayShowCopy) }
    var overlayShowDelete by remember { mutableStateOf(prefs.overlayShowDelete) }
    var overlayTimeout by remember { mutableIntStateOf(prefs.overlayTimeoutSeconds) }
    var overlayX by remember { mutableIntStateOf(prefs.overlayX) }
    var overlayY by remember { mutableIntStateOf(prefs.overlayY) }

    val canWatch = hasImages && hasNotif
    val canOverlay = hasImages && hasOverlayPerm

    val tester = remember { ScreenshotOverlay(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { tester.hide() } }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permRefresh++ }

    val imagesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permRefresh++
        if (!granted) {
            runCatching {
                context.startActivity(ScreenshotPermissions.appSettingsIntent(context))
            }
        }
    }

    val setWatcher: (Boolean) -> Unit = { on ->
        watcherEnabled = on
        prefs.watcherEnabled = on
        ScreenshotWatcherService.sync(context)
    }

    val setOverlay: (Boolean) -> Unit = { on ->
        overlayEnabled = on
        prefs.overlayEnabled = on
        ScreenshotWatcherService.sync(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screenshot_actions),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.screenshot_actions_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { ScreenshotSectionTitle(stringResource(R.string.screenshot_section_permissions)) }

            item {
                ScreenshotCard {
                    ScreenshotPermissionRow(
                        label = stringResource(R.string.screenshot_perm_images),
                        granted = hasImages,
                        onGrant = { imagesPermLauncher.launch(ScreenshotPermissions.imagesPermission) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    ScreenshotPermissionRow(
                        label = stringResource(R.string.screenshot_perm_notifications),
                        granted = hasNotif,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    ScreenshotPermissionRow(
                        label = stringResource(R.string.screenshot_perm_overlay),
                        granted = hasOverlayPerm,
                        onGrant = {
                            runCatching {
                                context.startActivity(
                                    ScreenshotPermissions.overlayPermissionIntent(context)
                                )
                            }
                        }
                    )
                }
            }

            item { ScreenshotSectionTitle(stringResource(R.string.screenshot_section_tile)) }

            item {
                ScreenshotCard {
                    ScreenshotInfoRow(
                        icon = Icons.Filled.ContentCopy,
                        title = stringResource(R.string.screenshot_tile_label),
                        description = stringResource(R.string.screenshot_tile_hint)
                    )
                }
            }

            item { ScreenshotSectionTitle(stringResource(R.string.screenshot_section_notification)) }

            item {
                ScreenshotCard {
                    ScreenshotToggleRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.screenshot_watcher_switch),
                        description = if (canWatch) {
                            stringResource(R.string.screenshot_notification_hint)
                        } else {
                            stringResource(R.string.screenshot_watcher_needs_perms)
                        },
                        checked = watcherEnabled,
                        enabled = canWatch,
                        onCheckedChange = setWatcher
                    )
                    AnimatedVisibility(visible = watcherEnabled) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ScreenshotGroupLabel(stringResource(R.string.screenshot_group_buttons))
                            ScreenshotToggleRow(
                                icon = Icons.Filled.ContentCopy,
                                title = stringResource(R.string.screenshot_notif_show_copy),
                                description = stringResource(R.string.screenshot_action_copy_delete),
                                checked = notifShowCopy,
                                onCheckedChange = {
                                    notifShowCopy = it
                                    prefs.notificationShowCopy = it
                                }
                            )
                            ScreenshotToggleRow(
                                icon = Icons.Filled.Delete,
                                title = stringResource(R.string.screenshot_notif_show_delete),
                                description = stringResource(R.string.screenshot_action_delete),
                                checked = notifShowDelete,
                                onCheckedChange = {
                                    notifShowDelete = it
                                    prefs.notificationShowDelete = it
                                }
                            )
                            ScreenshotToggleRow(
                                icon = Icons.Filled.Image,
                                title = stringResource(R.string.screenshot_notif_show_preview),
                                description = stringResource(R.string.screenshot_alert_title),
                                checked = notifShowPreview,
                                onCheckedChange = {
                                    notifShowPreview = it
                                    prefs.notificationShowPreview = it
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ScreenshotGroupLabel(stringResource(R.string.screenshot_group_timing))
                            ScreenshotChipRow(
                                label = stringResource(R.string.screenshot_delay_label),
                                options = ScreenshotPrefs.DELAY_OPTIONS,
                                selected = delaySeconds,
                                labelFor = { stringResource(R.string.screenshot_delay_value, it) },
                                onSelect = {
                                    delaySeconds = it
                                    prefs.notificationDelaySeconds = it
                                }
                            )
                        }
                    }
                }
            }

            item { ScreenshotSectionTitle(stringResource(R.string.screenshot_section_overlay)) }

            item {
                ScreenshotCard {
                    ScreenshotToggleRow(
                        icon = Icons.Filled.Layers,
                        title = stringResource(R.string.screenshot_overlay_switch),
                        description = if (canOverlay) {
                            stringResource(R.string.screenshot_overlay_hint)
                        } else {
                            stringResource(R.string.screenshot_overlay_needs_perms)
                        },
                        checked = overlayEnabled,
                        enabled = canOverlay,
                        onCheckedChange = setOverlay
                    )
                    AnimatedVisibility(visible = overlayEnabled) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ScreenshotGroupLabel(stringResource(R.string.screenshot_group_buttons))
                            ScreenshotToggleRow(
                                icon = Icons.Filled.ContentCopy,
                                title = stringResource(R.string.screenshot_overlay_show_copy),
                                description = stringResource(R.string.screenshot_action_copy_delete),
                                checked = overlayShowCopy,
                                onCheckedChange = {
                                    overlayShowCopy = it
                                    prefs.overlayShowCopy = it
                                }
                            )
                            ScreenshotToggleRow(
                                icon = Icons.Filled.Delete,
                                title = stringResource(R.string.screenshot_overlay_show_delete),
                                description = stringResource(R.string.screenshot_action_delete),
                                checked = overlayShowDelete,
                                onCheckedChange = {
                                    overlayShowDelete = it
                                    prefs.overlayShowDelete = it
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ScreenshotGroupLabel(stringResource(R.string.screenshot_group_timing))
                            ScreenshotChipRow(
                                label = stringResource(R.string.screenshot_overlay_timeout_label),
                                options = ScreenshotPrefs.OVERLAY_TIMEOUT_OPTIONS,
                                selected = overlayTimeout,
                                labelFor = {
                                    stringResource(R.string.screenshot_overlay_timeout_value, it)
                                },
                                onSelect = {
                                    overlayTimeout = it
                                    prefs.overlayTimeoutSeconds = it
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            ScreenshotGroupLabel(stringResource(R.string.screenshot_group_position))
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                ScreenshotPositionSlider(
                                    label = stringResource(R.string.screenshot_overlay_x_label),
                                    value = overlayX,
                                    onChange = {
                                        overlayX = it
                                        prefs.overlayX = it
                                    }
                                )
                                ScreenshotPositionSlider(
                                    label = stringResource(R.string.screenshot_overlay_y_label),
                                    value = overlayY,
                                    onChange = {
                                        overlayY = it
                                        prefs.overlayY = it
                                    }
                                )
                                Button(
                                    onClick = {
                                        tester.hide()
                                        tester.show(
                                            overlayX,
                                            overlayY,
                                            overlayShowCopy,
                                            overlayShowDelete,
                                            { tester.hide() },
                                            { tester.hide() }
                                        )
                                        mainHandler.postDelayed({ tester.hide() }, 1500L)
                                    },
                                    enabled = hasOverlayPerm,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(R.string.screenshot_overlay_test),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { ScreenshotSectionTitle(stringResource(R.string.screenshot_section_about)) }

            item {
                ScreenshotCard {
                    Text(
                        text = stringResource(R.string.screenshot_delete_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenshotGroupLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    )
}

@Composable
private fun ScreenshotInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScreenshotChipRow(
    label: String,
    options: List<Int>,
    selected: Int,
    labelFor: @Composable (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            labelFor(option),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ScreenshotSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ScreenshotCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(content = content)
    }
}

@Composable
private fun ScreenshotToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ScreenshotPermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (granted) stringResource(R.string.screenshot_granted)
                else stringResource(R.string.screenshot_not_granted),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!granted) {
            Button(onClick = onGrant) {
                Text(
                    stringResource(R.string.screenshot_grant),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ScreenshotPositionSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.screenshot_overlay_position_value, label, value),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..ScreenshotPrefs.OVERLAY_POSITION_MAX.toFloat()
        )
    }
}
