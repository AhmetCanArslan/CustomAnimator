package com.arslan.customanimator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.data.Profile
import com.arslan.customanimator.data.ProfileAnimation
import com.arslan.customanimator.data.ProfileBattery
import com.arslan.customanimator.data.ProfileTileConfig
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.utils.BatteryTweaksManager
import com.arslan.customanimator.utils.ProfileActions
import com.arslan.customanimator.utils.ProfileManager
import com.arslan.customanimator.utils.TerminalTileIcons
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ProfileManager(context) }
    val existing = remember(profileId) { profileId?.let { manager.getProfile(it) } }
    val listState = rememberLazyListState()

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember {
        mutableStateOf(existing?.iconKey ?: ProfileManager.DEFAULT_ICON_KEY)
    }
    var showIconPicker by remember { mutableStateOf(false) }

    var animationEnabled by remember { mutableStateOf(existing?.animation != null) }
    var windowScale by remember { mutableStateOf(existing?.animation?.windowAnimationScale ?: 0.5f) }
    var transitionScale by remember { mutableStateOf(existing?.animation?.transitionAnimationScale ?: 0.5f) }
    var animatorScale by remember { mutableStateOf(existing?.animation?.animatorDurationScale ?: 0.5f) }

    var widthEnabled by remember { mutableStateOf(existing?.smallestWidthDp != null) }
    var widthReset by remember { mutableStateOf((existing?.smallestWidthDp ?: 1) <= 0) }
    var widthText by remember {
        mutableStateOf(existing?.smallestWidthDp?.takeIf { it > 0 }?.toString() ?: "411")
    }

    var batteryEnabled by remember { mutableStateOf(existing?.battery != null) }
    var saverPresetId by remember { mutableStateOf(existing?.battery?.saverPresetId) }
    var dozePresetId by remember { mutableStateOf(existing?.battery?.dozePresetId) }
    var saverStateIncluded by remember { mutableStateOf(existing?.battery?.batterySaverOn != null) }
    var saverStateOn by remember { mutableStateOf(existing?.battery?.batterySaverOn ?: false) }
    var triggerIncluded by remember { mutableStateOf(existing?.battery?.triggerLevel != null) }
    var triggerLevel by remember { mutableStateOf((existing?.battery?.triggerLevel ?: 15).toFloat()) }
    val batteryToggles = remember {
        mutableStateMapOf<String, Boolean>().apply {
            existing?.battery?.toggles?.forEach { (key, value) -> put(key, value) }
        }
    }

    var developerEnabled by remember { mutableStateOf(existing?.developer?.isNotEmpty() == true) }
    val devToggles = remember {
        mutableStateMapOf<String, Boolean>().apply {
            existing?.developer?.forEach { (key, value) -> put(key, value) }
        }
    }

    var tileLabel by remember { mutableStateOf(existing?.tile?.label ?: "") }
    var tileToast by remember { mutableStateOf(existing?.tile?.showToast ?: true) }
    var tileCollapse by remember { mutableStateOf(existing?.tile?.collapsePanel ?: true) }

    val freeSlot = remember(profileId) { manager.firstFreeSlot(excludingProfileId = profileId) }
    val tileSlot = existing?.tile?.slot ?: freeSlot

    val trimmedName = name.trim()
    val buildProfile: () -> Profile = {
        val battery = if (batteryEnabled) {
            ProfileBattery(
                saverPresetId = saverPresetId,
                dozePresetId = dozePresetId,
                batterySaverOn = if (saverStateIncluded) saverStateOn else null,
                triggerLevel = if (triggerIncluded) triggerLevel.roundToInt() else null,
                toggles = batteryToggles.toMap()
            ).takeIf { !it.isEmpty }
        } else {
            null
        }

        Profile(
            id = existing?.id ?: manager.newId(),
            name = trimmedName,
            iconKey = iconKey,
            animation = if (animationEnabled) {
                ProfileAnimation(windowScale, transitionScale, animatorScale)
            } else {
                null
            },
            smallestWidthDp = if (widthEnabled) {
                if (widthReset) 0 else widthText.toIntOrNull()?.coerceIn(200, 2000)
            } else {
                null
            },
            battery = battery,
            developer = if (developerEnabled) devToggles.toMap() else emptyMap(),
            tile = tileSlot?.let { slot ->
                ProfileTileConfig(
                    slot = slot,
                    label = tileLabel.trim().ifBlank { trimmedName },
                    showToast = tileToast,
                    collapsePanel = tileCollapse
                )
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.profile_editor_new else R.string.profile_editor_edit
                        ),
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (trimmedName.isEmpty()) return@ExtendedFloatingActionButton
                    manager.saveProfile(buildProfile())
                    onSaved()
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.save)) }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(TerminalTileIcons.resFor(iconKey)),
                                contentDescription = stringResource(R.string.profile_icon),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.profile_name)) },
                                singleLine = true,
                                isError = trimmedName.isEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showIconPicker = true }) {
                        Text(stringResource(R.string.profile_icon_change))
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_section_animation)) }
            item {
                SectionCard(
                    title = stringResource(R.string.profile_include_animation),
                    description = stringResource(R.string.profile_include_animation_desc),
                    enabled = animationEnabled,
                    onEnabledChange = { animationEnabled = it }
                ) {
                    ScaleSlider(
                        title = stringResource(R.string.window_animation_scale),
                        value = windowScale,
                        onValueChange = { windowScale = it }
                    )
                    ScaleSlider(
                        title = stringResource(R.string.transition_animation_scale),
                        value = transitionScale,
                        onValueChange = { transitionScale = it }
                    )
                    ScaleSlider(
                        title = stringResource(R.string.animator_duration_scale),
                        value = animatorScale,
                        onValueChange = { animatorScale = it }
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_section_width)) }
            item {
                SectionCard(
                    title = stringResource(R.string.profile_include_width),
                    description = stringResource(R.string.profile_include_width_desc),
                    enabled = widthEnabled,
                    onEnabledChange = { widthEnabled = it }
                ) {
                    ProfileSwitchRow(
                        title = stringResource(R.string.profile_width_reset),
                        description = stringResource(R.string.profile_width_reset_desc),
                        checked = widthReset,
                        onCheckedChange = { widthReset = it }
                    )
                    if (!widthReset) {
                        OutlinedTextField(
                            value = widthText,
                            onValueChange = { input ->
                                widthText = input.filter { it.isDigit() }.take(4)
                            },
                            label = { Text(stringResource(R.string.profile_width_dp)) },
                            supportingText = { Text(stringResource(R.string.profile_width_dp_helper)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_section_battery)) }
            item {
                SectionCard(
                    title = stringResource(R.string.profile_include_battery),
                    description = stringResource(R.string.profile_include_battery_desc),
                    enabled = batteryEnabled,
                    onEnabledChange = { batteryEnabled = it }
                ) {
                    PresetDropdown(
                        title = stringResource(R.string.bt_section_presets),
                        selectedId = saverPresetId,
                        options = BatteryTweaksManager.saverPresets.map { it.id to stringResource(it.titleRes) },
                        onSelected = { saverPresetId = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    PresetDropdown(
                        title = stringResource(R.string.bt_section_doze),
                        selectedId = dozePresetId,
                        options = BatteryTweaksManager.dozePresets.map { it.id to stringResource(it.titleRes) },
                        onSelected = { dozePresetId = it }
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    IncludeSwitchRow(
                        title = stringResource(R.string.bt_low_power),
                        description = stringResource(R.string.bt_low_power_desc),
                        included = saverStateIncluded,
                        value = saverStateOn,
                        onIncludedChange = { saverStateIncluded = it },
                        onValueChange = { saverStateOn = it }
                    )
                    IncludeRow(
                        title = stringResource(R.string.bt_trigger_level),
                        description = stringResource(R.string.bt_trigger_level_desc),
                        included = triggerIncluded,
                        onIncludedChange = { triggerIncluded = it }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${triggerLevel.roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = triggerLevel,
                                onValueChange = { triggerLevel = it },
                                valueRange = 0f..75f,
                                steps = 14
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.bt_section_background),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ProfileActions.batteryToggles.forEach { toggle ->
                        IncludeSwitchRow(
                            title = stringResource(toggle.titleRes),
                            description = stringResource(toggle.descriptionRes),
                            included = batteryToggles.containsKey(toggle.key),
                            value = batteryToggles[toggle.key] ?: false,
                            onIncludedChange = { included ->
                                if (included) {
                                    batteryToggles[toggle.key] = toggle.read(context)
                                } else {
                                    batteryToggles.remove(toggle.key)
                                }
                            },
                            onValueChange = { batteryToggles[toggle.key] = it }
                        )
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_section_developer)) }
            item {
                SectionCard(
                    title = stringResource(R.string.profile_include_developer),
                    description = stringResource(R.string.profile_include_developer_desc),
                    enabled = developerEnabled,
                    onEnabledChange = { developerEnabled = it }
                ) {
                    ProfileActions.devActions.filter { it.available() }.forEach { action ->
                        IncludeSwitchRow(
                            title = stringResource(action.titleRes),
                            description = stringResource(action.descriptionRes),
                            included = devToggles.containsKey(action.key),
                            value = devToggles[action.key] ?: false,
                            onIncludedChange = { included ->
                                if (included) {
                                    devToggles[action.key] = runCatching { action.read(context) }
                                        .getOrDefault(false)
                                } else {
                                    devToggles.remove(action.key)
                                }
                            },
                            onValueChange = { devToggles[action.key] = it }
                        )
                    }
                }
            }

            item { SectionHeader(title = stringResource(R.string.profile_section_tile)) }
            item {
                AppCard {
                    OutlinedTextField(
                        value = tileLabel,
                        onValueChange = { tileLabel = it },
                        label = { Text(stringResource(R.string.terminal_tile_label)) },
                        supportingText = { Text(stringResource(R.string.terminal_tile_label_helper)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ProfileSwitchRow(
                        title = stringResource(R.string.terminal_tile_toast),
                        description = stringResource(R.string.preset_tile_toast_description),
                        checked = tileToast,
                        onCheckedChange = { tileToast = it }
                    )
                    ProfileSwitchRow(
                        title = stringResource(R.string.terminal_tile_collapse),
                        description = stringResource(R.string.preset_tile_collapse_description),
                        checked = tileCollapse,
                        onCheckedChange = { tileCollapse = it }
                    )
                }

                if (tileSlot == null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.terminal_tile_slots_full,
                            ProfileManager.MAX_TILE_SLOTS
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showIconPicker) {
        ProfileIconPickerDialog(
            selectedKey = iconKey,
            onDismiss = { showIconPicker = false },
            onSelected = {
                iconKey = it
                showIconPicker = false
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    enabledToggleAllowed: Boolean = true,
    content: @Composable () -> Unit
) {
    AppCard(highlighted = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = enabledToggleAllowed
            )
        }
        AnimatedVisibility(visible = enabled) {
            Column {
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
private fun ScaleSlider(title: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = String.format(Locale.US, "%.2fx", value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = { onValueChange((it * 20f).roundToInt() / 20f) },
            valueRange = 0f..5f,
            steps = 99
        )
    }
}

@Composable
private fun ProfileSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IncludeSwitchRow(
    title: String,
    description: String,
    included: Boolean,
    value: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = included, onCheckedChange = onIncludedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (included) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = value, onCheckedChange = onValueChange, enabled = included)
    }
}

@Composable
private fun IncludeRow(
    title: String,
    description: String,
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = included, onCheckedChange = onIncludedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (included) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedVisibility(visible = included) {
            Box(modifier = Modifier.padding(start = 48.dp, end = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun PresetDropdown(
    title: String,
    selectedId: String?,
    options: List<Pair<String, String>>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.profile_no_change)
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: noneLabel

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(noneLabel) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
                options.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIconPickerDialog(
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalised = query.trim().lowercase(Locale.US)
    val keys = remember(normalised) {
        if (normalised.isEmpty()) {
            TerminalTileIcons.keys
        } else {
            TerminalTileIcons.keys.filter { it.contains(normalised) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_icon)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.profile_icon_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.heightIn(max = 320.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keys, key = { it }) { key ->
                        ProfileIconCell(
                            iconKey = key,
                            selected = key == selectedKey,
                            onClick = { onSelected(key) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ProfileIconCell(iconKey: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(TerminalTileIcons.resFor(iconKey)),
            contentDescription = iconKey,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
    }
}
