package com.riyadmondol2006.customanimator

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riyadmondol2006.customanimator.screenshot.ScreenshotFolder
import com.riyadmondol2006.customanimator.screenshot.ScreenshotOverlay
import com.riyadmondol2006.customanimator.screenshot.ScreenshotPrefs
import com.riyadmondol2006.customanimator.service.ScreenshotWatcherService
import com.riyadmondol2006.customanimator.utils.SaveToDeviceManager
import kotlin.math.roundToInt
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar
import com.riyadmondol2006.customanimator.ui.components.AppSlider
import com.riyadmondol2006.customanimator.ui.components.AppCard
import com.riyadmondol2006.customanimator.ui.components.AppSwitch
import com.riyadmondol2006.customanimator.ui.components.IconBadge

private val TIMING_SLIDER_PADDING = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotActionsScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val prefs = remember { ScreenshotPrefs(context) }

    val permissionRefresh = rememberPermissionRefresh()
    val requestPermission = rememberPermissionRequester(onChanged = { permissionRefresh.intValue++ })
    val permissions = key(permissionRefresh.intValue) { permissionEntries(SCREENSHOT_PERMISSIONS) }
    val granted = permissions.filter { it.granted }.map { it.permission }
    val folder = remember(permissionRefresh.intValue) { ScreenshotFolder.tree(context) }
    val hasNotif = AppPermission.POST_NOTIFICATIONS in granted
    val hasOverlayPerm = AppPermission.OVERLAY in granted

    var watcherEnabled by remember { mutableStateOf(prefs.watcherEnabled) }
    var overlayEnabled by remember { mutableStateOf(prefs.overlayEnabled) }
    var delaySeconds by remember { mutableIntStateOf(prefs.notificationDelaySeconds) }
    var autoDismissSeconds by remember { mutableIntStateOf(prefs.notificationAutoDismissSeconds) }
    var notifShowCopy by remember { mutableStateOf(prefs.notificationShowCopy) }
    var notifShowDelete by remember { mutableStateOf(prefs.notificationShowDelete) }
    var notifShowPreview by remember { mutableStateOf(prefs.notificationShowPreview) }
    var overlayShowCopy by remember { mutableStateOf(prefs.overlayShowCopy) }
    var overlayShowDelete by remember { mutableStateOf(prefs.overlayShowDelete) }
    var overlayTimeout by remember { mutableIntStateOf(prefs.overlayTimeoutSeconds) }
    var showConfirmationToasts by remember { mutableStateOf(prefs.showConfirmationToasts) }
    var overlayX by remember { mutableIntStateOf(prefs.overlayX) }
    var overlayY by remember { mutableIntStateOf(prefs.overlayY) }

    val canWatch = folder != null && hasNotif
    val canOverlay = folder != null && hasOverlayPerm

    val tester = remember { ScreenshotOverlay(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { tester.hide() } }

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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.screenshot_actions),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.screenshot_actions_desc),
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

            if (folder != null) {
                item {
                    ScreenshotCard {
                        ScreenshotInfoRow(
                            icon = Icons.Filled.Folder,
                            title = stringResource(R.string.screenshot_perm_folder),
                            description = stringResource(
                                R.string.screenshot_folder_current,
                                SaveToDeviceManager.folderLabel(folder)
                            ),
                            modifier = Modifier.clickable {
                                requestPermission(AppPermission.SCREENSHOT_FOLDER)
                            }
                        )
                    }
                }
            }

            item { LabelSectionTitle(stringResource(R.string.screenshot_section_tile)) }

            item {
                ScreenshotCard {
                    ScreenshotInfoRow(
                        icon = Icons.Filled.ContentCopy,
                        title = stringResource(R.string.screenshot_tile_label),
                        description = stringResource(R.string.screenshot_tile_hint)
                    )
                }
            }

            item { LabelSectionTitle(stringResource(R.string.screenshot_section_feedback)) }

            item {
                ScreenshotCard {
                    ScreenshotToggleRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.screenshot_toast_switch),
                        description = stringResource(R.string.screenshot_toast_switch_desc),
                        checked = showConfirmationToasts,
                        onCheckedChange = {
                            showConfirmationToasts = it
                            prefs.showConfirmationToasts = it
                        }
                    )
                }
            }

            item { LabelSectionTitle(stringResource(R.string.screenshot_section_notification)) }

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
                            ScreenshotSlider(
                                label = stringResource(R.string.screenshot_delay_label),
                                valueText = stringResource(R.string.screenshot_delay_value, delaySeconds),
                                value = delaySeconds,
                                range = 0..ScreenshotPrefs.DELAY_MAX,
                                onChange = {
                                    delaySeconds = it
                                    prefs.notificationDelaySeconds = it
                                },
                                modifier = TIMING_SLIDER_PADDING
                            )
                            ScreenshotSlider(
                                label = stringResource(R.string.screenshot_auto_dismiss_label),
                                valueText = if (autoDismissSeconds == ScreenshotPrefs.AUTO_DISMISS_OFF) {
                                    stringResource(R.string.screenshot_auto_dismiss_off)
                                } else {
                                    stringResource(R.string.screenshot_auto_dismiss_value, autoDismissSeconds)
                                },
                                value = autoDismissSeconds,
                                range = ScreenshotPrefs.AUTO_DISMISS_OFF..ScreenshotPrefs.AUTO_DISMISS_MAX,
                                onChange = {
                                    autoDismissSeconds = it
                                    prefs.notificationAutoDismissSeconds = it
                                },
                                modifier = TIMING_SLIDER_PADDING
                            )
                        }
                    }
                }
            }

            item { LabelSectionTitle(stringResource(R.string.screenshot_section_overlay)) }

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
                            ScreenshotSlider(
                                label = stringResource(R.string.screenshot_overlay_timeout_label),
                                valueText = stringResource(R.string.screenshot_overlay_timeout_value, overlayTimeout),
                                value = overlayTimeout,
                                range = ScreenshotPrefs.OVERLAY_TIMEOUT_MIN..ScreenshotPrefs.OVERLAY_TIMEOUT_MAX,
                                onChange = {
                                    overlayTimeout = it
                                    prefs.overlayTimeoutSeconds = it
                                },
                                modifier = TIMING_SLIDER_PADDING
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
                                ScreenshotSlider(
                                    label = stringResource(
                                        R.string.screenshot_overlay_position_value,
                                        stringResource(R.string.screenshot_overlay_x_label),
                                        overlayX
                                    ),
                                    value = overlayX,
                                    range = 0..ScreenshotPrefs.OVERLAY_POSITION_MAX,
                                    onChange = {
                                        overlayX = it
                                        prefs.overlayX = it
                                    }
                                )
                                ScreenshotSlider(
                                    label = stringResource(
                                        R.string.screenshot_overlay_position_value,
                                        stringResource(R.string.screenshot_overlay_y_label),
                                        overlayY
                                    ),
                                    value = overlayY,
                                    range = 0..ScreenshotPrefs.OVERLAY_POSITION_MAX,
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
                                    modifier = Modifier.fillMaxWidth(),
                                    shapes = ButtonDefaults.shapes()
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
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 36.dp)
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
private fun ScreenshotCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(contentPadding = 0.dp) {
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
        IconBadge(icon = icon, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ScreenshotSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
        AppSlider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}
