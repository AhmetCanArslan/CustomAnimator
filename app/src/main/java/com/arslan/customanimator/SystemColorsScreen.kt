package com.arslan.customanimator

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.AccentColorSelector
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.ConnectedChoiceGroup
import com.arslan.customanimator.ui.components.PaletteDots
import com.arslan.customanimator.ui.components.PalettePreview
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.ui.components.previewScheme
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.PaletteStyle
import com.arslan.customanimator.utils.SystemColorsManager
import com.arslan.customanimator.utils.SystemContrast
import com.arslan.customanimator.utils.SystemDarkMode
import com.arslan.customanimator.utils.SystemPalette
import com.materialkolor.scheme.DynamicScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.ShizukuHelper

private const val PALETTE_SETTLE_MS = 600L

private class SystemColorsState {
    var palette by mutableStateOf(SystemPalette.WALLPAPER_DEFAULT)
    var appliedPalette = SystemPalette.WALLPAPER_DEFAULT
    var pendingPalette: Job? = null
    var contrast by mutableStateOf(SystemContrast.STANDARD)
    var darkMode by mutableStateOf<SystemDarkMode?>(null)
    var wallpaperColors by mutableStateOf(emptyList<Int>())
    var recentColors by mutableStateOf(emptyList<Int>())
    var isLoading by mutableStateOf(true)

    suspend fun load(context: Context) {
        withContext(Dispatchers.IO) {
            val palette = SystemColorsManager.getPalette()
            val contrast = if (SystemColorsManager.supportsContrast()) SystemColorsManager.getContrast() else SystemContrast.STANDARD
            val darkMode = SystemColorsManager.getDarkMode(context)
            val wallpaperColors = if (SystemColorsManager.isSupported()) SystemColorsManager.wallpaperColors(context) else emptyList()
            val recentColors = SystemColorsManager.recentColors(context)
            withContext(Dispatchers.Main) {
                this@SystemColorsState.palette = palette
                appliedPalette = palette
                this@SystemColorsState.contrast = contrast
                this@SystemColorsState.darkMode = darkMode
                this@SystemColorsState.wallpaperColors = wallpaperColors
                this@SystemColorsState.recentColors = recentColors
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemColorsScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember { SystemColorsState() }
    val enabled = hasShizukuPermission && !state.isLoading

    LaunchedEffect(hasShizukuPermission) { state.load(context) }

    val showFailure: () -> Unit = {
        Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
    }
    val writePalette: (SystemPalette) -> Unit = { palette ->
        coroutineScope.runAction(context, { SystemColorsManager.applyPalette(context, palette) }) { success ->
            if (success) state.appliedPalette = palette
            if (!success && state.palette == palette) {
                state.palette = state.appliedPalette
                showFailure()
            }
            palette.seedColor?.takeIf { success && !palette.fromWallpaper }?.let {
                SystemColorsManager.rememberColor(context, it)
                state.recentColors = SystemColorsManager.recentColors(context)
            }
        }
    }
    val applyPalette: (SystemPalette) -> Unit = { palette ->
        state.palette = palette
        state.pendingPalette?.cancel()
        state.pendingPalette = coroutineScope.launch {
            delay(PALETTE_SETTLE_MS)
            if (palette != state.appliedPalette) writePalette(palette)
        }
    }
    val applyContrast: (SystemContrast) -> Unit = applyContrast@{ contrast ->
        if (contrast == state.contrast) return@applyContrast
        val previous = state.contrast
        state.contrast = contrast
        coroutineScope.runAction(context, { SystemColorsManager.applyContrast(context, contrast) }) { success ->
            if (!success) {
                state.contrast = previous
                showFailure()
            }
        }
    }
    val applyDarkMode: (SystemDarkMode) -> Unit = { mode ->
        val previous = state.darkMode
        state.darkMode = mode
        coroutineScope.runAction(context, { SystemColorsManager.applyDarkMode(mode) }) { success ->
            if (!success) {
                state.darkMode = previous
                showFailure()
            }
        }
    }
    val restore: () -> Unit = {
        coroutineScope.runAction(context, { SystemColorsManager.restoreBaseline(context) }) { success ->
            if (success) {
                coroutineScope.launch { state.load(context) }
            } else {
                showFailure()
            }
        }
    }

    val dark = when (state.darkMode) {
        SystemDarkMode.DARK -> true
        SystemDarkMode.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    val fallbackSeed = MaterialTheme.colorScheme.primary.toArgb()
    val previewSeed = state.palette.seedColor ?: state.wallpaperColors.firstOrNull() ?: fallbackSeed
    val contrastLevel = state.contrast.level.toDouble()
    val styles = remember { PaletteStyle.available() }
    val schemes = remember(previewSeed, dark, contrastLevel) {
        PaletteStyle.entries.associateWith { previewScheme(previewSeed, it, dark, contrastLevel) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.system_colors),
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
            if (!SystemColorsManager.isSupported()) {
                item { UnsupportedCard(stringResource(R.string.system_colors_unsupported)) }
                return@LazyColumn
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
                    text = stringResource(R.string.system_colors_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { PalettePreview(schemes.getValue(state.palette.style)) }

            item {
                AppCard(contentPadding = 0.dp) {
                    AccentColorSelector(
                        palette = state.palette,
                        wallpaperColors = state.wallpaperColors,
                        recentColors = state.recentColors,
                        enabled = enabled,
                        onSelect = applyPalette
                    )
                }
            }

            if (SystemColorsManager.supportsStyles()) {
                cardSection(
                    R.string.system_colors_style,
                    *styles.map { style ->
                        @Composable {
                            PaletteStyleRow(
                                style = style,
                                scheme = schemes.getValue(style),
                                selected = state.palette.style == style,
                                enabled = enabled,
                                onClick = { applyPalette(state.palette.copy(style = style)) }
                            )
                        }
                    }.toTypedArray()
                )
            }

            if (SystemColorsManager.supportsContrast()) {
                item {
                    ChoiceCard(stringResource(R.string.system_colors_contrast)) {
                        ConnectedChoiceGroup(
                            options = SystemContrast.entries.map { it to stringResource(it.labelRes) },
                            selected = state.contrast,
                            enabled = enabled,
                            onSelect = applyContrast
                        )
                    }
                }
            }

            item {
                ChoiceCard(stringResource(R.string.system_colors_dark_theme)) {
                    ConnectedChoiceGroup(
                        options = SystemDarkMode.entries.map { it to stringResource(it.labelRes) },
                        selected = state.darkMode,
                        enabled = enabled,
                        onSelect = { mode -> mode?.let(applyDarkMode) }
                    )
                }
            }

            item {
                AppCard(contentPadding = 0.dp) {
                    ActionRow(
                        icon = Icons.Filled.SettingsBackupRestore,
                        title = stringResource(R.string.system_colors_restore),
                        description = stringResource(R.string.system_colors_restore_desc),
                        buttonLabel = stringResource(R.string.reset),
                        enabled = enabled,
                        onClick = restore
                    )
                }
            }
        }
    }
}

private fun CoroutineScope.runAction(
    context: Context,
    action: () -> Boolean,
    onResult: (Boolean) -> Unit
) {
    launch {
        val success = withContext(Dispatchers.IO) { action() }
        onResult(success)
        if (success) maybeShowInterstitial(context)
    }
}

@Composable
private fun ChoiceCard(title: String, content: @Composable () -> Unit) {
    AppCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun PaletteStyleRow(
    style: PaletteStyle,
    scheme: DynamicScheme,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(
            text = stringResource(style.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        PaletteDots(scheme = scheme, modifier = Modifier.padding(end = 8.dp))
    }
}
