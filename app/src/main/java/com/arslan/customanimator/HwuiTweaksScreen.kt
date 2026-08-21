package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.HwuiTweaksManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HwuiTweaksScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var renderer by remember { mutableStateOf(HwuiTweaksManager.RENDERER_DEFAULT) }
    var overdrawDebug by remember { mutableStateOf(false) }
    var dirtyRegions by remember { mutableStateOf(false) }
    var forceGpu by remember { mutableStateOf(false) }
    var textureCache by remember { mutableIntStateOf(HwuiTweaksManager.TEXTURE_CACHE_DEFAULT) }
    var showTextureCacheMenu by remember { mutableStateOf(false) }
    var hwOverlaysDisabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(hasShizukuPermission) {
        if (hasShizukuPermission) {
            withContext(Dispatchers.IO) {
                val currentRenderer = HwuiTweaksManager.getRenderer()
                val currentOverdraw = HwuiTweaksManager.isOverdrawDebugEnabled()
                val currentDirty = HwuiTweaksManager.isDirtyRegionsEnabled()
                val currentForceGpu = HwuiTweaksManager.isForceGpuRenderingEnabled()
                val currentTextureCache = HwuiTweaksManager.getTextureCacheSize()
                val currentOverlays = HwuiTweaksManager.areHwOverlaysDisabled(context)
                withContext(Dispatchers.Main) {
                    renderer = currentRenderer
                    overdrawDebug = currentOverdraw
                    dirtyRegions = currentDirty
                    forceGpu = currentForceGpu
                    textureCache = currentTextureCache
                    hwOverlaysDisabled = currentOverlays
                }
            }
        }
        isLoading = false
    }

    val applyToggle: (Boolean, (Boolean) -> Unit, () -> Boolean) -> Unit = { newValue, setState, action ->
        setState(newValue)
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            if (success) {
                maybeShowInterstitial(context)
            } else {
                setState(!newValue)
                Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.hwui_tweaks),
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
                    text = stringResource(R.string.hwui_tweaks_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { DevSectionTitle(stringResource(R.string.hwui_section_renderer)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.hwui_renderer),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.hwui_renderer_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(
                                HwuiTweaksManager.RENDERER_DEFAULT to R.string.hwui_renderer_default,
                                HwuiTweaksManager.RENDERER_SKIA_GL to R.string.hwui_renderer_gl,
                                HwuiTweaksManager.RENDERER_SKIA_VK to R.string.hwui_renderer_vk
                            )
                            options.forEachIndexed { index, (value, labelRes) ->
                                SegmentedButton(
                                    selected = renderer == value,
                                    enabled = hasShizukuPermission && !isLoading,
                                    onClick = {
                                        val previous = renderer
                                        renderer = value
                                        coroutineScope.launch {
                                            val success = withContext(Dispatchers.IO) {
                                                HwuiTweaksManager.setRenderer(value)
                                            }
                                            if (!success) {
                                                renderer = previous
                                                Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                ) {
                                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        InfoNote(text = stringResource(R.string.hwui_restart_note), dismissKey = "hwui_restart")
                    }
                }
            }

            item { DevSectionTitle(stringResource(R.string.hwui_section_cache)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.hwui_texture_cache),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.hwui_texture_cache_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showTextureCacheMenu = true },
                                enabled = hasShizukuPermission && !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.card
                            ) {
                                Text(
                                    text = textureCacheLabel(textureCache),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = showTextureCacheMenu,
                                onDismissRequest = { showTextureCacheMenu = false }
                            ) {
                                HwuiTweaksManager.textureCacheOptions.forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text(textureCacheLabel(size)) },
                                        trailingIcon = {
                                            if (textureCache == size) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        onClick = {
                                            showTextureCacheMenu = false
                                            if (size == textureCache) return@DropdownMenuItem
                                            val previous = textureCache
                                            textureCache = size
                                            coroutineScope.launch {
                                                val success = withContext(Dispatchers.IO) {
                                                    HwuiTweaksManager.setTextureCacheSize(size)
                                                }
                                                if (!success) {
                                                    textureCache = previous
                                                    Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { DevSectionTitle(stringResource(R.string.hwui_section_rendering)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        ToggleRow(
                            icon = Icons.Filled.Brush,
                            title = stringResource(R.string.hwui_force_gpu),
                            description = stringResource(R.string.hwui_force_gpu_desc),
                            checked = forceGpu,
                            enabled = hasShizukuPermission && !isLoading,
                            onCheckedChange = { newValue ->
                                applyToggle(newValue, { forceGpu = it }) {
                                    HwuiTweaksManager.setForceGpuRendering(newValue)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.Layers,
                            title = stringResource(R.string.hwui_disable_overlays),
                            description = stringResource(R.string.hwui_disable_overlays_desc),
                            checked = hwOverlaysDisabled,
                            enabled = hasShizukuPermission && !isLoading,
                            onCheckedChange = { newValue ->
                                applyToggle(newValue, { hwOverlaysDisabled = it }) {
                                    HwuiTweaksManager.setHwOverlaysDisabled(context, newValue)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.GridOn,
                            title = stringResource(R.string.hwui_overdraw),
                            description = stringResource(R.string.hwui_overdraw_desc),
                            checked = overdrawDebug,
                            enabled = hasShizukuPermission && !isLoading,
                            onCheckedChange = { newValue ->
                                applyToggle(newValue, { overdrawDebug = it }) {
                                    HwuiTweaksManager.setOverdrawDebug(newValue)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ToggleRow(
                            icon = Icons.Filled.Speed,
                            title = stringResource(R.string.hwui_dirty_regions),
                            description = stringResource(R.string.hwui_dirty_regions_desc),
                            checked = dirtyRegions,
                            enabled = hasShizukuPermission && !isLoading,
                            onCheckedChange = { newValue ->
                                applyToggle(newValue, { dirtyRegions = it }) {
                                    HwuiTweaksManager.setDirtyRegions(newValue)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ActionRow(
                            icon = Icons.Filled.RestartAlt,
                            title = stringResource(R.string.hwui_reset),
                            description = stringResource(R.string.hwui_reset_desc),
                            buttonLabel = stringResource(R.string.reset),
                            enabled = hasShizukuPermission && !isLoading,
                            onClick = {
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) { HwuiTweaksManager.resetAll(context) }
                                    renderer = HwuiTweaksManager.RENDERER_DEFAULT
                                    overdrawDebug = false
                                    dirtyRegions = false
                                    forceGpu = false
                                    textureCache = HwuiTweaksManager.TEXTURE_CACHE_DEFAULT
                                    hwOverlaysDisabled = false
                                    Toast.makeText(
                                        context,
                                        context.getString(if (success) R.string.action_succeeded else R.string.action_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ActionRow(
                            icon = Icons.Filled.Memory,
                            title = stringResource(R.string.restart_system_ui),
                            description = stringResource(R.string.hwui_restart_systemui_desc),
                            buttonLabel = stringResource(R.string.restart),
                            enabled = hasShizukuPermission,
                            onClick = {
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        com.arslan.customanimator.utils.DeveloperOptionsManager.restartSystemUi()
                                    }
                                    if (!success) {
                                        Toast.makeText(context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun textureCacheLabel(size: Int): String {
    return if (size == HwuiTweaksManager.TEXTURE_CACHE_DEFAULT) {
        stringResource(R.string.hwui_renderer_default)
    } else {
        stringResource(R.string.hwui_megabytes, size)
    }
}
