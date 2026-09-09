package com.arslan.customanimator

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.ToggleTile
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.ExpandableCard
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.TileFaceIcon
import com.arslan.customanimator.ui.components.TileIconPickerDialog
import com.arslan.customanimator.utils.TerminalTileIcons
import com.arslan.customanimator.utils.TileCatalog
import com.arslan.customanimator.utils.ToggleTileManager
import com.arslan.customanimator.utils.ToggleTilePresets
import com.arslan.customanimator.utils.ToggleTileSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QsTilesScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState(),
    onOpenTab: (HomeTab) -> Unit = {},
    onOpenProfiles: () -> Unit = {},
    onOpenSoundTile: () -> Unit = {},
    onOpenScreenshotActions: () -> Unit = {}
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val manager = remember { ToggleTileManager(context) }
    val scope = rememberCoroutineScope()

    var refreshToken by remember { mutableIntStateOf(0) }
    var toggleTiles by remember { mutableStateOf(emptyList<ToggleTile>()) }
    var activeTiles by remember { mutableStateOf(emptyList<TileCatalog.Entry>()) }
    var appTilesOff by remember { mutableStateOf(emptyList<TileCatalog.Entry>()) }
    var editingTile by remember { mutableStateOf<ToggleTile?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(hasShizukuPermission, refreshToken) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(
                manager.getTiles(),
                TileCatalog.activeTiles(context),
                TileCatalog.disabledAppTiles(context)
            )
        }
        toggleTiles = loaded.first
        activeTiles = loaded.second
        appTilesOff = loaded.third
    }

    val reload: () -> Unit = { refreshToken++ }

    val runAndReload: ((Context) -> Unit) -> Unit = { action ->
        scope.launch {
            withContext(Dispatchers.IO) { action(context) }
            reload()
        }
    }

    val addPreset: (ToggleTilePresets.Preset) -> Unit = { preset ->
        val added = manager.addTile(
            presetKey = preset.key,
            label = resources.getString(preset.nameRes),
            onCommand = preset.onCommand,
            offCommand = preset.offCommand,
            readCommand = preset.readCommand,
            onValue = preset.onValue,
            iconKey = preset.iconKey,
            collapsePanel = true,
            showToast = true
        )
        if (added == null) {
            Toast.makeText(context, R.string.qs_tiles_no_free_slot, Toast.LENGTH_SHORT).show()
        } else {
            reload()
            ToggleTileSlots.requestAddTile(context, added.slot, added.label, added.iconKey)
            maybeShowInterstitial(context)
        }
    }

    val settingsFor: (TileCatalog.Entry) -> (() -> Unit)? = { entry ->
        when (entry.group) {
            TileCatalog.Group.TOGGLE -> {
                { editingTile = toggleTiles.firstOrNull { it.id == entry.id } }
            }
            TileCatalog.Group.ANIMATION -> {
                { onOpenTab(HomeTab.ANIMATION) }
            }
            TileCatalog.Group.WIDTH -> {
                { onOpenTab(HomeTab.WIDTH) }
            }
            TileCatalog.Group.TERMINAL -> {
                { onOpenTab(HomeTab.TERMINAL) }
            }
            TileCatalog.Group.PROFILE -> onOpenProfiles
            TileCatalog.Group.APP -> when (entry.id) {
                "sound" -> onOpenSoundTile
                "screenshot" -> onOpenScreenshotActions
                else -> null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.qs_tiles),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
            if (!hasShizukuPermission) {
                item { QsTilesShizukuCard(onOpenSetup = openSetup) }
            }

            activeTilesSection(
                tiles = activeTiles,
                settingsFor = settingsFor,
                onAddToPanel = { entry -> entry.addToPanel(context) },
                onRemove = { entry -> runAndReload(entry.remove) }
            )

            availableTilesSection(
                appTilesOff = appTilesOff,
                toggleTiles = toggleTiles,
                expandedCategory = expandedCategory,
                onExpandedChange = { expandedCategory = it },
                onAddAppTile = { entry -> runAndReload(entry.addToPanel) },
                onAddPreset = addPreset,
                onAddCustom = { showCustomDialog = true }
            )
        }
    }

    editingTile?.let { tile ->
        ToggleTileDialog(
            initial = tile,
            onDismiss = { editingTile = null },
            onConfirm = { updated ->
                manager.updateTile(updated)
                reload()
                editingTile = null
            },
            onDelete = {
                manager.removeTile(tile.id)
                reload()
                editingTile = null
            }
        )
    }

    if (showCustomDialog) {
        ToggleTileDialog(
            initial = null,
            onDismiss = { showCustomDialog = false },
            onConfirm = { draft ->
                val added = manager.addTile(
                    presetKey = ToggleTilePresets.CUSTOM_KEY,
                    label = draft.label,
                    onCommand = draft.onCommand,
                    offCommand = draft.offCommand,
                    readCommand = draft.readCommand,
                    onValue = draft.onValue,
                    iconKey = draft.iconKey,
                    collapsePanel = draft.collapsePanel,
                    showToast = draft.showToast
                )
                if (added == null) {
                    Toast.makeText(context, R.string.qs_tiles_no_free_slot, Toast.LENGTH_SHORT).show()
                } else {
                    reload()
                    ToggleTileSlots.requestAddTile(context, added.slot, added.label, added.iconKey)
                }
                showCustomDialog = false
            },
            onDelete = null
        )
    }
}

private fun LazyListScope.activeTilesSection(
    tiles: List<TileCatalog.Entry>,
    settingsFor: (TileCatalog.Entry) -> (() -> Unit)?,
    onAddToPanel: (TileCatalog.Entry) -> Unit,
    onRemove: (TileCatalog.Entry) -> Unit
) {
    item {
        SectionHeader(
            title = stringResource(R.string.qs_tiles_placed_section),
            subtitle = stringResource(R.string.qs_tiles_placed_section_desc)
        )
    }

    if (tiles.isEmpty()) {
        item {
            AppCard {
                Text(
                    text = stringResource(R.string.qs_tiles_placed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    item {
        AppCard(contentPadding = 4.dp) {
            tiles.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                TileRow(
                    face = entry.face,
                    title = entry.label,
                    subtitle = stringResource(entry.subtitleRes),
                    onSettings = settingsFor(entry),
                    onAdd = { onAddToPanel(entry) }.takeIf { TileCatalog.canRequestAdd() },
                    onRemove = { onRemove(entry) }
                )
            }
        }
    }

    if (!TileCatalog.canRequestAdd()) {
        item {
            Text(
                text = stringResource(R.string.qs_tiles_builtin_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LazyListScope.availableTilesSection(
    appTilesOff: List<TileCatalog.Entry>,
    toggleTiles: List<ToggleTile>,
    expandedCategory: Int?,
    onExpandedChange: (Int?) -> Unit,
    onAddAppTile: (TileCatalog.Entry) -> Unit,
    onAddPreset: (ToggleTilePresets.Preset) -> Unit,
    onAddCustom: () -> Unit
) {
    item {
        SectionHeader(
            title = stringResource(R.string.qs_tiles_available_section),
            subtitle = stringResource(R.string.qs_tiles_available_section_desc)
        )
    }

    if (appTilesOff.isNotEmpty()) {
        item {
            AppCard(contentPadding = 4.dp) {
                appTilesOff.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                    TileRow(
                        face = entry.face,
                        title = entry.label,
                        subtitle = stringResource(entry.subtitleRes),
                        onSettings = null,
                        onAdd = { onAddAppTile(entry) },
                        onRemove = null
                    )
                }
            }
        }
    }

    presetCatalogItems(
        toggleTiles = toggleTiles,
        expandedCategory = expandedCategory,
        onExpandedChange = onExpandedChange,
        onAddPreset = onAddPreset
    )

    item {
        AppCard(contentPadding = 4.dp) {
            TileRow(
                face = TileCatalog.Face.Drawable(
                    TerminalTileIcons.resFor(TerminalTileIcons.DEFAULT_KEY)
                ),
                title = stringResource(R.string.qs_tiles_add_custom),
                subtitle = stringResource(R.string.qs_tiles_add_custom_desc),
                onSettings = null,
                onAdd = onAddCustom,
                onRemove = null
            )
        }
    }
}

private fun LazyListScope.presetCatalogItems(
    toggleTiles: List<ToggleTile>,
    expandedCategory: Int?,
    onExpandedChange: (Int?) -> Unit,
    onAddPreset: (ToggleTilePresets.Preset) -> Unit
) {
    ToggleTilePresets.categories.forEach { category ->
        item(key = "cat-${category.titleRes}") {
            val available = category.presets.filter { preset ->
                toggleTiles.none { it.presetKey == preset.key }
            }
            if (available.isNotEmpty()) {
                ExpandableCard(
                    title = stringResource(category.titleRes),
                    subtitle = stringResource(R.string.qs_tiles_category_count, available.size),
                    expanded = expandedCategory == category.titleRes,
                    onExpandedChange = { expanded ->
                        onExpandedChange(if (expanded) category.titleRes else null)
                    }
                ) {
                    available.forEach { preset ->
                        TileRow(
                            face = TileCatalog.Face.Drawable(TerminalTileIcons.resFor(preset.iconKey)),
                            title = stringResource(preset.nameRes),
                            subtitle = stringResource(preset.descriptionRes),
                            onSettings = null,
                            onAdd = { onAddPreset(preset) },
                            onRemove = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QsTilesShizukuCard(onOpenSetup: () -> Unit) {
    AppCard(onClick = onOpenSetup) {
        Text(
            text = stringResource(R.string.qs_tiles_needs_shizuku_title),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.qs_tiles_needs_shizuku_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TileFaceBadge(face: TileCatalog.Face) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        when (face) {
            is TileCatalog.Face.Drawable -> Icon(
                painter = painterResource(face.res),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            is TileCatalog.Face.Number -> TileFaceIcon(
                iconKey = null,
                numberText = face.text,
                size = 24.dp
            )
        }
    }
}

@Composable
private fun TileRow(
    face: TileCatalog.Face,
    title: String,
    subtitle: String,
    onSettings: (() -> Unit)?,
    onAdd: (() -> Unit)?,
    onRemove: (() -> Unit)?
) {
    val rowClick = onSettings ?: onAdd
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (rowClick != null) Modifier.clickable(onClick = rowClick) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TileFaceBadge(face)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onSettings != null) {
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.qs_tiles_action_settings)
                )
            }
        }
        if (onAdd != null) {
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.qs_tiles_add_to_panel)
                )
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.qs_tiles_remove)
                )
            }
        }
    }
}

@Composable
private fun ToggleTileDialog(
    initial: ToggleTile?,
    onDismiss: () -> Unit,
    onConfirm: (ToggleTile) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var onCommand by remember { mutableStateOf(initial?.onCommand ?: "") }
    var offCommand by remember { mutableStateOf(initial?.offCommand ?: "") }
    var readCommand by remember { mutableStateOf(initial?.readCommand ?: "") }
    var onValue by remember { mutableStateOf(initial?.onValue ?: "") }
    var iconKey by remember {
        mutableStateOf(TerminalTileIcons.canonicalKey(initial?.iconKey ?: TerminalTileIcons.DEFAULT_KEY))
    }
    var collapsePanel by remember { mutableStateOf(initial?.collapsePanel ?: true) }
    var showToast by remember { mutableStateOf(initial?.showToast ?: true) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val canSave = label.isNotBlank() && onCommand.isNotBlank() && offCommand.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.qs_tiles_dialog_new else R.string.qs_tiles_dialog_edit
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.qs_tiles_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = onCommand,
                    onValueChange = { onCommand = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_on_command)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = offCommand,
                    onValueChange = { offCommand = it },
                    label = { Text(stringResource(R.string.qs_tiles_field_off_command)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showIconPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.qs_tiles_pick_icon))
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(
                        stringResource(
                            if (showAdvanced) {
                                R.string.qs_tiles_advanced_hide
                            } else {
                                R.string.qs_tiles_advanced_show
                            }
                        )
                    )
                }
                if (showAdvanced) {
                    ToggleTileAdvancedFields(
                        readCommand = readCommand,
                        onReadCommandChange = { readCommand = it },
                        onValue = onValue,
                        onOnValueChange = { onValue = it },
                        collapsePanel = collapsePanel,
                        onCollapsePanelChange = { collapsePanel = it },
                        showToast = showToast,
                        onShowToastChange = { showToast = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onConfirm(
                        ToggleTile(
                            id = initial?.id ?: "",
                            presetKey = initial?.presetKey ?: ToggleTilePresets.CUSTOM_KEY,
                            label = label.trim(),
                            onCommand = onCommand.trim(),
                            offCommand = offCommand.trim(),
                            readCommand = readCommand.trim(),
                            onValue = onValue.trim(),
                            iconKey = iconKey,
                            slot = initial?.slot ?: -1,
                            collapsePanel = collapsePanel,
                            showToast = showToast
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.qs_tiles_remove))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )

    if (showIconPicker) {
        TileIconPickerDialog(
            selectedKey = iconKey,
            onDismiss = { showIconPicker = false },
            onSelect = {
                iconKey = it
                showIconPicker = false
            }
        )
    }
}

@Composable
private fun ToggleTileAdvancedFields(
    readCommand: String,
    onReadCommandChange: (String) -> Unit,
    onValue: String,
    onOnValueChange: (String) -> Unit,
    collapsePanel: Boolean,
    onCollapsePanelChange: (Boolean) -> Unit,
    showToast: Boolean,
    onShowToastChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = readCommand,
        onValueChange = onReadCommandChange,
        label = { Text(stringResource(R.string.qs_tiles_field_read_command)) },
        supportingText = { Text(stringResource(R.string.qs_tiles_field_read_command_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = onValue,
        onValueChange = onOnValueChange,
        label = { Text(stringResource(R.string.qs_tiles_field_on_value)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    ToggleTileSwitchRow(
        title = stringResource(R.string.qs_tiles_collapse_panel),
        checked = collapsePanel,
        onCheckedChange = onCollapsePanelChange
    )
    ToggleTileSwitchRow(
        title = stringResource(R.string.qs_tiles_show_toast),
        checked = showToast,
        onCheckedChange = onShowToastChange
    )
}

@Composable
private fun ToggleTileSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
