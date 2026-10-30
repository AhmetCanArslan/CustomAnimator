package com.arslan.customanimator

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.ui.theme.horizontalPagerTransition
import com.arslan.customanimator.utils.IconChangerManager
import com.arslan.customanimator.utils.LocalShizukuPermission
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.AppSlider
import com.arslan.customanimator.ui.components.AppCard

private const val ADAPTIVE_LAYER_ZOOM = 1.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconChangerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember { IconChangerState(context, coroutineScope) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(state::onImagePicked)
    }

    val appList = rememberAppListState(include = { !it.isSystemApp })

    LaunchedEffect(state.installs) {
        if (state.installs > 0) appList.reload(context)
    }

    BackHandler(enabled = state.app != null, onBack = state::clearApp)

    IconChangerOverlays(state)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.icon_changer),
                onBack = { if (state.app == null) onBack() else state.clearApp() },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state.app,
            contentKey = { it?.packageName },
            transitionSpec = { horizontalPagerTransition(forward = targetState != null) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding(),
            label = "icon changer step"
        ) { app ->
            val image = state.image
            if (app == null || image == null) {
                IconChangerAppList(
                    appList = appList,
                    listState = listState,
                    hasShizukuPermission = hasShizukuPermission,
                    onOpenSetup = openSetup,
                    onPick = state::onAppPicked
                )
            } else {
                IconChangerEditor(
                    state = state,
                    app = app,
                    image = image,
                    canInstall = hasShizukuPermission,
                    onPickImage = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
    }
}

@Composable
private fun IconChangerAppList(
    appList: AppListState,
    listState: LazyListState,
    hasShizukuPermission: Boolean,
    onOpenSetup: () -> Unit,
    onPick: (InstalledAppInfo) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!IconChangerManager.isSupported()) {
            item { UnsupportedCard(stringResource(R.string.icon_changer_unsupported)) }
            return@LazyColumn
        }

        if (!hasShizukuPermission) {
            item {
                SetupNudgeCard(
                    message = stringResource(R.string.developer_needs_shizuku),
                    onOpenSetup = onOpenSetup
                )
            }
        }

        item {
            InfoCard(
                dismissKey = "icon_changer_info",
                texts = listOf(
                    stringResource(R.string.icon_changer_info),
                    stringResource(R.string.icon_changer_warning)
                )
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
private fun IconChangerEditor(
    state: IconChangerState,
    app: InstalledAppInfo,
    image: ShortcutIconImage,
    canInstall: Boolean,
    onPickImage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DevSectionTitle(stringResource(R.string.icon_changer_app_section)) }

        item { IconChangerAppCard(app = app, onClick = state::clearApp) }

        item { DevSectionTitle(stringResource(R.string.icon_changer_icon_section)) }

        item { IconChangerDesignCard(state = state, app = app, image = image, onPickImage = onPickImage) }

        item { IconChangerActions(state = state, enabled = canInstall) }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IconChangerOverlays(state: IconChangerState) {
    val image = state.image
    if (state.sheet == IconChangerSheet.ICONS && image != null) {
        ShortcutIconPickerSheet(
            targetPackage = state.app?.packageName,
            type = ShortcutType.APP,
            current = image,
            onPick = state::onIconPicked,
            onDismiss = { state.sheet = IconChangerSheet.NONE }
        )
    }
    state.confirmation?.let { IconChangerConfirmDialog(it, onConfirm = state::confirm, onDismiss = state::dismissConfirmation) }
    state.progress?.let { IconChangerProgressDialog(it) }
}

@Composable
private fun IconChangerAppCard(app: InstalledAppInfo, onClick: () -> Unit) {
    AppCard(contentPadding = 0.dp) {
        PickerRow(title = app.label, subtitle = app.packageName, onClick = onClick) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun IconChangerDesignCard(
    state: IconChangerState,
    app: InstalledAppInfo,
    image: ShortcutIconImage,
    onPickImage: () -> Unit
) {
    AppCard(contentPadding = 0.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = app.packageName, modifier = Modifier.size(64.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconChangerPreview(image = image, scale = state.scale, background = state.background)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state.sheet = IconChangerSheet.ICONS }, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.icon_changer_choose_icon), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = onPickImage, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.icon_changer_from_gallery), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (image.legacy) {
                IconChangerLayerControls(state)
            }
        }
    }
}

@Composable
private fun IconChangerLayerControls(state: IconChangerState) {
    Text(
        text = stringResource(R.string.icon_changer_background),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    IconColorRow(
        selected = state.background,
        onSelect = { state.background = it },
        colors = IconChangerManager.backgroundColors
    )
    Text(
        text = stringResource(R.string.icon_changer_size),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppSlider(
        value = state.scale,
        onValueChange = { state.scale = it },
        valueRange = IconChangerManager.MIN_SCALE..IconChangerManager.MAX_SCALE
    )
}

@Composable
private fun IconChangerPreview(image: ShortcutIconImage, scale: Float, background: Int) {
    val layer = remember(image, scale) { IconChangerManager.foregroundLayer(image, scale).asImageBitmap() }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(background))
    ) {
        Image(
            bitmap = layer,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(ADAPTIVE_LAYER_ZOOM)
        )
    }
}

@Composable
private fun IconChangerActions(state: IconChangerState, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = state::askToApply,
            enabled = enabled && state.canApply,
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(stringResource(R.string.icon_changer_apply))
        }
        OutlinedButton(
            onClick = state::export,
            enabled = enabled && state.canApply,
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(stringResource(R.string.debloater_export_apk))
        }
    }
}

@Composable
private fun IconChangerConfirmDialog(
    confirmation: IconChangerConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (confirmation.erasureAcknowledged) {
        IconChangerEraseDialog(confirmation, onConfirm = onConfirm, onDismiss = onDismiss)
        return
    }
    val messageRes = if (confirmation.keepsData) R.string.icon_changer_confirm_keep else R.string.icon_changer_confirm_erase
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.icon_changer_confirm_title)) },
        text = { Text(stringResource(messageRes, confirmation.app.label)) },
        confirmButton = {
            Button(onClick = onConfirm, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.icon_changer_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun IconChangerEraseDialog(
    confirmation: IconChangerConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.icon_changer_erase_title)) },
        text = { Text(stringResource(R.string.icon_changer_erase_message, confirmation.app.label)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.icon_changer_erase_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun IconChangerProgressDialog(progress: IconChangerProgress) {
    val messageRes = when (progress) {
        IconChangerProgress.REBUILDING -> R.string.icon_changer_step_rebuilding
        IconChangerProgress.INSTALLING -> R.string.icon_changer_step_installing
    }
    BusyDialog(stringResource(messageRes))
}
