package com.arslan.customanimator

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.HeroCard
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.utils.LocalShizukuPermission

private const val ADAPTIVE_ICON_ZOOM = 1.5f
private const val DISABLED_TILE_ALPHA = 0.5f
private const val IMAGE_MIME = "image/*"
private const val ANY_MIME = "*/*"

private enum class ShortcutTile(val icon: ImageVector, val titleRes: Int, val descriptionRes: Int) {
    APP(Icons.Filled.Apps, R.string.shortcut_tile_app, R.string.shortcut_tile_app_desc),
    ACTIVITY(Icons.AutoMirrored.Filled.Launch, R.string.shortcut_tile_activity, R.string.shortcut_tile_activity_desc),
    FILE(Icons.AutoMirrored.Filled.InsertDriveFile, R.string.shortcut_tile_file, R.string.shortcut_tile_file_desc),
    PHOTO(Icons.Filled.Image, R.string.shortcut_tile_photo, R.string.shortcut_tile_photo_desc),
    LINK(Icons.Filled.Link, R.string.shortcut_tile_link, R.string.shortcut_tile_link_desc),
    COMMAND(Icons.Filled.Terminal, R.string.shortcut_tile_command, R.string.shortcut_tile_command_desc)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember { ShortcutsState(context, coroutineScope) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(state::onFilePicked)
    }

    LaunchedEffect(Unit) {
        RewardedAds.preload(context)
    }

    LaunchedEffect(hasShizukuPermission) {
        state.load(hasShizukuPermission)
    }

    val onTileClick: (ShortcutTile) -> Unit = { tile ->
        when (tile) {
            ShortcutTile.APP -> state.startAppPicker(forActivity = false)
            ShortcutTile.ACTIVITY -> state.startAppPicker(forActivity = true)
            ShortcutTile.FILE -> filePicker.launch(arrayOf(ANY_MIME))
            ShortcutTile.PHOTO -> filePicker.launch(arrayOf(IMAGE_MIME))
            ShortcutTile.LINK -> state.startBlank(ShortcutType.LINK)
            ShortcutTile.COMMAND -> if (hasShizukuPermission) state.startBlank(ShortcutType.COMMAND) else openSetup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.shortcuts),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroCard(
                    title = stringResource(R.string.shortcuts_hero_title),
                    subtitle = stringResource(R.string.shortcuts_desc)
                ) {
                    StatusPill(
                        text = stringResource(
                            if (hasShizukuPermission) R.string.shortcuts_shizuku_ready else R.string.shortcuts_shizuku_optional
                        ),
                        tone = if (hasShizukuPermission) StatusTone.ACTIVE else StatusTone.NEUTRAL
                    )
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.shortcuts_create))
            }

            items(ShortcutTile.entries.chunked(2), key = { row -> row.first().name }) { row ->
                ShortcutTileRow(
                    tiles = row,
                    hasShizukuPermission = hasShizukuPermission,
                    onClick = onTileClick
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.shortcuts_saved),
                    trailing = {
                        if (state.shortcuts.isNotEmpty()) StatusPill(text = state.shortcuts.size.toString())
                    }
                )
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.shortcuts.isEmpty()) {
                item {
                    ShortcutsEmptyCard()
                }
            } else {
                items(state.shortcuts, key = { it.id }) { entry ->
                    ShortcutRow(
                        entry = entry,
                        icon = state.icons[entry.id],
                        onLaunch = { state.launch(entry) },
                        onEdit = { state.edit(entry) },
                        onPin = { state.pinAgain(entry) },
                        onChangeHandler = { state.changeHandler(entry) },
                        onDelete = { state.delete(entry) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    ShortcutStepHost(state = state, hasShizukuPermission = hasShizukuPermission)
}

@Composable
private fun ShortcutTileRow(
    tiles: List<ShortcutTile>,
    hasShizukuPermission: Boolean,
    onClick: (ShortcutTile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.forEach { tile ->
            ShortcutTileCard(
                tile = tile,
                enabled = tile != ShortcutTile.COMMAND || hasShizukuPermission,
                onClick = { onClick(tile) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ShortcutTileCard(
    tile: ShortcutTile,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tile.ordinal % 3) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    AppCard(
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_TILE_ALPHA),
        onClick = onClick,
        contentPadding = 16.dp
    ) {
        IconBadge(icon = tile.icon, containerColor = container, contentColor = content)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(tile.titleRes),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(tile.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShortcutsEmptyCard() {
    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(
                icon = Icons.Filled.AddToHomeScreen,
                size = 52.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.shortcuts_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    entry: ShortcutEntry,
    icon: Bitmap?,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onChangeHandler: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(onClick = onLaunch, contentPadding = 14.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShortcutIcon(bitmap = icon, legacy = entry.legacyIcon, modifier = Modifier.size(48.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = entry.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(entry.typeLabelRes()) + " · " + entry.target(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.needsShizuku) {
                    StatusPill(text = stringResource(R.string.shortcut_pill_shizuku), tone = StatusTone.INFO)
                }
            }
            ShortcutRowMenu(
                showChangeHandler = entry.type == ShortcutType.FILE,
                onLaunch = onLaunch,
                onEdit = onEdit,
                onPin = onPin,
                onChangeHandler = onChangeHandler,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun ShortcutRowMenu(
    showChangeHandler: Boolean,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onChangeHandler: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val select: (() -> Unit) -> Unit = { action ->
        expanded = false
        action()
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.shortcut_more_options)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ShortcutMenuItem(Icons.Filled.PlayArrow, R.string.shortcut_launch) { select(onLaunch) }
            ShortcutMenuItem(Icons.Filled.Edit, R.string.shortcut_edit) { select(onEdit) }
            ShortcutMenuItem(Icons.Filled.AddToHomeScreen, R.string.shortcut_add_to_home) { select(onPin) }
            if (showChangeHandler) {
                ShortcutMenuItem(Icons.AutoMirrored.Filled.OpenInNew, R.string.shortcut_change_app) {
                    select(onChangeHandler)
                }
            }
            ShortcutMenuItem(Icons.Filled.Delete, R.string.delete) { select(onDelete) }
        }
    }
}

@Composable
private fun ShortcutMenuItem(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        onClick = onClick
    )
}

@Composable
internal fun ShortcutIcon(bitmap: Bitmap?, modifier: Modifier = Modifier, legacy: Boolean = false) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    if (image != null && legacy) {
        Image(bitmap = image, contentDescription = null, modifier = modifier)
        return
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(ADAPTIVE_ICON_ZOOM)
            )
        }
    }
}

private fun ShortcutEntry.typeLabelRes(): Int = when (type) {
    ShortcutType.APP -> R.string.shortcut_tile_app
    ShortcutType.ACTIVITY -> R.string.shortcut_tile_activity
    ShortcutType.FILE -> R.string.shortcut_tile_file
    ShortcutType.LINK -> R.string.shortcut_tile_link
    ShortcutType.COMMAND -> R.string.shortcut_tile_command
    ShortcutType.FOLDER -> R.string.shortcut_tile_folder
}

private fun ShortcutEntry.target(): String = when (type) {
    ShortcutType.APP -> packageName
    ShortcutType.ACTIVITY -> activityName?.substringAfterLast('.')
    ShortcutType.FILE -> mimeType
    ShortcutType.LINK -> uri
    ShortcutType.COMMAND -> command
    ShortcutType.FOLDER -> uri?.let { Uri.parse(it).lastPathSegment }
}.orEmpty()
