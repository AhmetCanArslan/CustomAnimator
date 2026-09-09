package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.WifiCalling
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.StatusBarIconsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StatusBarIconEntry(
    val slot: String,
    val labelRes: Int,
    val icon: ImageVector
)

private data class StatusBarIconSection(
    val titleRes: Int,
    val entries: List<StatusBarIconEntry>
)

private val statusBarIconSections = listOf(
    StatusBarIconSection(
        R.string.status_bar_icons_section_status,
        listOf(
            StatusBarIconEntry(StatusBarIconsManager.SLOT_CLOCK, R.string.status_bar_icon_clock, Icons.Filled.Schedule),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_BATTERY, R.string.status_bar_icon_battery, Icons.Filled.BatteryFull),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_ALARM, R.string.status_bar_icon_alarm, Icons.Filled.Alarm)
        )
    ),
    StatusBarIconSection(
        R.string.status_bar_icons_section_connectivity,
        listOf(
            StatusBarIconEntry(StatusBarIconsManager.SLOT_WIFI, R.string.status_bar_icon_wifi, Icons.Filled.Wifi),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_INTERNET, R.string.status_bar_icon_internet, Icons.Filled.Language),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_MOBILE, R.string.status_bar_icon_mobile, Icons.Filled.SignalCellularAlt),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_ETHERNET, R.string.status_bar_icon_ethernet, Icons.Filled.SettingsEthernet),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_AIRPLANE, R.string.status_bar_icon_airplane, Icons.Filled.AirplanemodeActive),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_HOTSPOT, R.string.status_bar_icon_hotspot, Icons.Filled.WifiTethering),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_BLUETOOTH, R.string.status_bar_icon_bluetooth, Icons.Filled.Bluetooth),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_NFC, R.string.status_bar_icon_nfc, Icons.Filled.Nfc),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_VPN, R.string.status_bar_icon_vpn, Icons.Filled.VpnKey),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_ROAMING, R.string.status_bar_icon_roaming, Icons.Filled.Public),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_WIFI_STANDARD, R.string.status_bar_icon_wifi_standard, Icons.Filled.NetworkWifi),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_DATA_SAVER, R.string.status_bar_icon_data_saver, Icons.Filled.DataUsage),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_DATA_CONNECTION, R.string.status_bar_icon_data_connection, Icons.Filled.NetworkCheck),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_CDMA_ERI, R.string.status_bar_icon_cdma_eri, Icons.Filled.SettingsInputAntenna)
        )
    ),
    StatusBarIconSection(
        R.string.status_bar_icons_section_sound,
        listOf(
            StatusBarIconEntry(StatusBarIconsManager.SLOT_ZEN, R.string.status_bar_icon_zen, Icons.Filled.DoNotDisturbOn),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_VOLUME, R.string.status_bar_icon_volume, Icons.Filled.VolumeUp),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_MUTE, R.string.status_bar_icon_mute, Icons.Filled.VolumeOff),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_HEADSET, R.string.status_bar_icon_headset, Icons.Filled.Headset),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SPEAKERPHONE, R.string.status_bar_icon_speakerphone, Icons.Filled.RecordVoiceOver),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_TTY, R.string.status_bar_icon_tty, Icons.Filled.Hearing),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_IMS, R.string.status_bar_icon_ims, Icons.Filled.PhoneInTalk),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_VOLTE, R.string.status_bar_icon_volte, Icons.Filled.PhoneInTalk),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_VONR, R.string.status_bar_icon_vonr, Icons.Filled.PhoneInTalk),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_VOWIFI, R.string.status_bar_icon_vowifi, Icons.Filled.WifiCalling),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_WIFI_CALLING, R.string.status_bar_icon_wifi_calling, Icons.Filled.WifiCalling),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_CALL_STRENGTH, R.string.status_bar_icon_call_strength, Icons.Filled.NetworkCell),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_NO_CALLING, R.string.status_bar_icon_no_calling, Icons.Filled.PhoneDisabled)
        )
    ),
    StatusBarIconSection(
        R.string.status_bar_icons_section_system,
        listOf(
            StatusBarIconEntry(StatusBarIconsManager.SLOT_LOCATION, R.string.status_bar_icon_location, Icons.Filled.LocationOn),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_ROTATE, R.string.status_bar_icon_rotate, Icons.Filled.ScreenRotation),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_CAST, R.string.status_bar_icon_cast, Icons.Filled.Cast),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_CONNECTED_DISPLAY, R.string.status_bar_icon_connected_display, Icons.Filled.ScreenShare),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SCREEN_RECORD, R.string.status_bar_icon_screen_record, Icons.Filled.Videocam),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SENSORS_OFF, R.string.status_bar_icon_sensors_off, Icons.Filled.SensorsOff),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_MANAGED_PROFILE, R.string.status_bar_icon_managed_profile, Icons.Filled.Work),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_IME, R.string.status_bar_icon_ime, Icons.Filled.Keyboard),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SYNC_ACTIVE, R.string.status_bar_icon_sync_active, Icons.Filled.Sync),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SYNC_FAILING, R.string.status_bar_icon_sync_failing, Icons.Filled.SyncProblem),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_BLUETOOTH_BATTERY, R.string.status_bar_icon_bluetooth_battery, Icons.Filled.BluetoothConnected),
            StatusBarIconEntry(StatusBarIconsManager.SLOT_SECURE, R.string.status_bar_icon_secure, Icons.Filled.Lock)
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBarIconsScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val contentResolver = context.contentResolver
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var hiddenSlots by remember { mutableStateOf(emptySet<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(hasShizukuPermission) {
        val current = withContext(Dispatchers.IO) {
            StatusBarIconsManager.getHiddenSlots(contentResolver)
        }
        hiddenSlots = current
        isLoading = false
    }

    val applySlot: (String, Boolean) -> Unit = { slot, visible ->
        val previous = hiddenSlots
        hiddenSlots = if (visible) previous - slot else previous + slot
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                StatusBarIconsManager.setSlotHidden(contentResolver, slot, !visible)
            }
            if (success) {
                maybeShowInterstitial(context)
            } else {
                hiddenSlots = previous
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val applyAll: (Boolean) -> Unit = { visible ->
        val previous = hiddenSlots
        hiddenSlots = if (visible) emptySet() else StatusBarIconsManager.allSlots.toSet()
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                if (visible) {
                    StatusBarIconsManager.showAll(contentResolver)
                } else {
                    StatusBarIconsManager.hideAll(contentResolver)
                }
            }
            if (!success) {
                hiddenSlots = previous
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.status_bar_icons),
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
                    text = stringResource(R.string.status_bar_icons_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        ActionRow(
                            icon = Icons.Filled.Visibility,
                            title = stringResource(R.string.status_bar_icons_show_all),
                            description = stringResource(R.string.status_bar_icons_show_all_desc),
                            buttonLabel = stringResource(R.string.status_bar_icons_show_all),
                            enabled = hasShizukuPermission && !isLoading,
                            onClick = { applyAll(true) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        ActionRow(
                            icon = Icons.Filled.VisibilityOff,
                            title = stringResource(R.string.status_bar_icons_hide_all),
                            description = stringResource(R.string.status_bar_icons_hide_all_desc),
                            buttonLabel = stringResource(R.string.status_bar_icons_hide_all),
                            enabled = hasShizukuPermission && !isLoading,
                            onClick = { applyAll(false) }
                        )
                    }
                }
            }

            statusBarIconSections.forEach { section ->
                item { DevSectionTitle(stringResource(section.titleRes)) }
                item {
                    StatusBarIconSectionCard(
                        entries = section.entries,
                        hiddenSlots = hiddenSlots,
                        enabled = hasShizukuPermission && !isLoading,
                        onToggle = applySlot
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusBarIconSectionCard(
    entries: List<StatusBarIconEntry>,
    hiddenSlots: Set<String>,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                ToggleRow(
                    icon = entry.icon,
                    title = stringResource(entry.labelRes),
                    description = entry.slot,
                    checked = entry.slot !in hiddenSlots,
                    enabled = enabled,
                    onCheckedChange = { visible -> onToggle(entry.slot, visible) }
                )
            }
        }
    }
}
