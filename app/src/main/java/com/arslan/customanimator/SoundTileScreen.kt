package com.arslan.customanimator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.SoundTileAction
import com.arslan.customanimator.utils.SoundTileActions
import com.arslan.customanimator.utils.SoundTilePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundTileScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val prefs = remember { SoundTilePrefs(context) }
    val coroutineScope = rememberCoroutineScope()

    var clickAction by remember { mutableStateOf(prefs.clickAction) }
    var longPressAction by remember { mutableStateOf(prefs.longPressAction) }
    var collapseDelay by remember { mutableIntStateOf(prefs.collapseDelayMs) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.sound_tile),
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
                    text = stringResource(R.string.sound_tile_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { SoundSectionTitle(stringResource(R.string.sound_tile_section_actions)) }

            item {
                SoundCard {
                    SoundInfoRow(
                        icon = Icons.Filled.VolumeUp,
                        title = stringResource(R.string.sound_tile_label),
                        description = stringResource(R.string.sound_tile_hint)
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    SoundActionRow(
                        label = stringResource(R.string.sound_tile_click_action),
                        selected = clickAction,
                        onSelect = {
                            clickAction = it
                            prefs.clickAction = it
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    SoundActionRow(
                        label = stringResource(R.string.sound_tile_long_press_action),
                        selected = longPressAction,
                        onSelect = {
                            longPressAction = it
                            prefs.longPressAction = it
                        }
                    )
                }
            }

            item { SoundSectionTitle(stringResource(R.string.sound_tile_section_collapse)) }

            item {
                SoundCard {
                    SoundCollapseRow(
                        selected = collapseDelay,
                        onSelect = {
                            collapseDelay = it
                            prefs.collapseDelayMs = it
                        }
                    )
                }
            }

            item {
                SoundCard {
                    SoundInfoRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.sound_tile_shizuku_note),
                        description = if (hasShizukuPermission) {
                            stringResource(R.string.screenshot_granted)
                        } else {
                            stringResource(R.string.screenshot_not_granted)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sound_tile_test),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { SoundTileActions.showVolumePanel(context) }) {
                            Text(
                                stringResource(R.string.sound_tile_action_volume_panel),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            enabled = hasShizukuPermission,
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    SoundTileActions.openMediaOutputDialog()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cast,
                                contentDescription = stringResource(R.string.sound_tile_action_media_output),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun soundActionLabel(action: SoundTileAction): String = when (action) {
    SoundTileAction.VOLUME_PANEL -> stringResource(R.string.sound_tile_action_volume_panel)
    SoundTileAction.MEDIA_OUTPUT -> stringResource(R.string.sound_tile_action_media_output)
    SoundTileAction.NONE -> stringResource(R.string.sound_tile_action_none)
}

@Composable
private fun SoundActionRow(
    label: String,
    selected: SoundTileAction,
    onSelect: (SoundTileAction) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        FlowChips {
            SoundTileAction.entries.forEach { action ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = {
                        Text(
                            soundActionLabel(action),
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
private fun SoundCollapseRow(selected: Int, onSelect: (Int) -> Unit) {
    var input by remember(selected) {
        mutableStateOf(if (selected >= 0) selected.toString() else "")
    }
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        Text(
            text = stringResource(R.string.sound_tile_collapse_label),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowChips {
            SoundTilePrefs.COLLAPSE_DELAY_OPTIONS.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            text = when (option) {
                                SoundTilePrefs.COLLAPSE_NEVER ->
                                    stringResource(R.string.sound_tile_collapse_never)
                                0 -> stringResource(R.string.sound_tile_collapse_immediate)
                                else -> stringResource(R.string.sound_tile_collapse_delay, option)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(5)
                input = digits
                val parsed = digits.toIntOrNull()
                if (parsed != null) {
                    onSelect(parsed.coerceAtMost(SoundTilePrefs.COLLAPSE_DELAY_MAX))
                }
            },
            label = { Text(stringResource(R.string.sound_tile_collapse_custom)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.sound_tile_collapse_custom_hint,
                        SoundTilePrefs.COLLAPSE_DELAY_MAX
                    )
                )
            },
            singleLine = true,
            enabled = selected != SoundTilePrefs.COLLAPSE_NEVER,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun SoundSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SoundCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(content = content)
    }
}

@Composable
private fun SoundInfoRow(icon: ImageVector, title: String, description: String) {
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
