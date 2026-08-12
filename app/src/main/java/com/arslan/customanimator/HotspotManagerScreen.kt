package com.arslan.customanimator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arslan.customanimator.data.HotspotBand
import com.arslan.customanimator.data.HotspotCapabilities
import com.arslan.customanimator.data.HotspotClient
import com.arslan.customanimator.data.HotspotConfig
import com.arslan.customanimator.data.HotspotRandomization
import com.arslan.customanimator.data.HotspotSecurity
import com.arslan.customanimator.data.HotspotSnapshot
import com.arslan.customanimator.data.HotspotState
import com.arslan.customanimator.data.HotspotTimeout
import com.arslan.customanimator.data.WifiNetwork
import com.arslan.customanimator.data.WifiSecurity
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.SettingRow
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.HotspotManager
import com.arslan.customanimator.utils.QrCodeRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val POLL_INTERVAL_MS = 3000L
private const val QR_SIZE_PX = 640

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotManagerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf<HotspotSnapshot?>(null) }
    var draft by remember { mutableStateOf(HotspotConfig()) }
    var isDirty by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }
    var passphraseVisible by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }

    val toast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    val failureText = stringResource(R.string.hotspot_action_failed)

    val reportOutcome: (HotspotManager.Outcome) -> Boolean = { outcome ->
        when (outcome) {
            is HotspotManager.Outcome.Success -> true
            is HotspotManager.Outcome.Failure -> {
                toast(outcome.message?.let { "$failureText: $it" } ?: failureText)
                false
            }
        }
    }

    val refresh: suspend () -> Unit = {
        val result = withContext(Dispatchers.IO) { HotspotManager.readState(context) }
        if (result != null) {
            snapshot = result
            if (!isDirty) draft = result.config
        }
        isLoading = false
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isVisible = true
                Lifecycle.Event.ON_PAUSE -> isVisible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasShizukuPermission, refreshKey, isVisible) {
        if (!hasShizukuPermission) {
            isLoading = false
            return@LaunchedEffect
        }
        while (isVisible) {
            refresh()
            delay(POLL_INTERVAL_MS)
        }
    }

    val current = snapshot
    val config = current?.config ?: HotspotConfig()
    val caps = current?.capabilities

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.hotspot_manager),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hotspot_open_system_settings)) },
                            leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                runCatching {
                                    context.startActivity(HotspotManager.systemHotspotSettingsIntent())
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = { BannerAdView() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.hotspot_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            if (current?.isSupported == false) {
                item {
                    AppCard {
                        Text(
                            stringResource(R.string.hotspot_unsupported),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                HotspotStatusCard(
                    snapshot = current,
                    isLoading = isLoading,
                    isWorking = isWorking,
                    passphraseVisible = passphraseVisible,
                    enabled = hasShizukuPermission,
                    onTogglePassphrase = { passphraseVisible = !passphraseVisible },
                    onCopyPassphrase = {
                        copyToClipboard(context, config.passphrase)
                        toast(context.getString(R.string.hotspot_copied))
                    },
                    onShowQr = { showQr = true },
                    onToggleHotspot = { enable ->
                        scope.launch {
                            isWorking = true
                            val outcome = withContext(Dispatchers.IO) {
                                HotspotManager.setEnabled(context, enable)
                            }
                            reportOutcome(outcome)
                            delay(1200)
                            refresh()
                            isWorking = false
                        }
                    }
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.hotspot_connected_devices),
                    subtitle = stringResource(
                        R.string.hotspot_connected_count,
                        current?.clients?.size ?: 0
                    )
                )
            }

            item {
                AppCard {
                    val clients = current?.clients.orEmpty()
                    if (clients.isEmpty()) {
                        Text(
                            stringResource(R.string.hotspot_no_clients),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        clients.forEachIndexed { index, client ->
                            if (index > 0) HorizontalDivider()
                            ClientRow(
                                client = client,
                                actionIcon = Icons.Filled.Block,
                                actionLabel = stringResource(R.string.hotspot_block),
                                enabled = hasShizukuPermission && !isWorking,
                                onAction = {
                                    scope.launch {
                                        isWorking = true
                                        val outcome = withContext(Dispatchers.IO) {
                                            HotspotManager.blockClient(context, config, client.macAddress)
                                        }
                                        if (reportOutcome(outcome)) {
                                            toast(context.getString(R.string.hotspot_blocked_toast))
                                        }
                                        refresh()
                                        isWorking = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.hotspot_blocked_devices),
                    subtitle = stringResource(R.string.hotspot_blocked_devices_desc)
                )
            }

            item {
                AppCard {
                    val blocked = config.blockedDevices
                    if (blocked.isEmpty()) {
                        Text(
                            stringResource(R.string.hotspot_no_blocked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        blocked.forEachIndexed { index, mac ->
                            if (index > 0) HorizontalDivider()
                            ClientRow(
                                client = HotspotClient(macAddress = mac),
                                actionIcon = Icons.Filled.RestartAlt,
                                actionLabel = stringResource(R.string.hotspot_unblock),
                                enabled = hasShizukuPermission && !isWorking,
                                onAction = {
                                    scope.launch {
                                        isWorking = true
                                        val outcome = withContext(Dispatchers.IO) {
                                            HotspotManager.unblockClient(context, config, mac)
                                        }
                                        reportOutcome(outcome)
                                        refresh()
                                        isWorking = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.hotspot_configuration),
                    subtitle = stringResource(R.string.hotspot_configuration_desc)
                )
            }

            item {
                HotspotConfigCard(
                    draft = draft,
                    capabilities = caps,
                    enabled = hasShizukuPermission && !isWorking,
                    onDraftChange = {
                        draft = it
                        isDirty = true
                    }
                )
            }

            item {
                val ssidError = HotspotManager.validateSsid(draft.ssid)
                val passError = HotspotManager.validatePassphrase(draft.passphrase, draft.security)
                val showErrors = current != null && isDirty
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ssidError.takeIf { showErrors }?.let {
                        Text(
                            text = stringResource(
                                when (it) {
                                    HotspotManager.SsidError.TOO_SHORT -> R.string.hotspot_ssid_too_short
                                    HotspotManager.SsidError.TOO_LONG -> R.string.hotspot_ssid_too_long
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    passError.takeIf { showErrors }?.let {
                        Text(
                            text = stringResource(
                                when (it) {
                                    HotspotManager.PassphraseError.TOO_SHORT -> R.string.hotspot_passphrase_too_short
                                    HotspotManager.PassphraseError.TOO_LONG -> R.string.hotspot_passphrase_too_long
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = hasShizukuPermission && !isWorking && isDirty &&
                                ssidError == null && passError == null,
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    val outcome = withContext(Dispatchers.IO) {
                                        HotspotManager.applyConfig(context, draft)
                                    }
                                    if (reportOutcome(outcome)) {
                                        isDirty = false
                                        toast(context.getString(R.string.hotspot_config_applied))
                                    }
                                    delay(600)
                                    refresh()
                                    isWorking = false
                                }
                            }
                        ) {
                            Text(stringResource(R.string.hotspot_apply))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = isDirty,
                            onClick = {
                                draft = config
                                isDirty = false
                            }
                        ) {
                            Text(stringResource(R.string.hotspot_discard))
                        }
                    }
                    if (current?.error != null) {
                        Text(
                            text = current.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        }
    }

    if (showQr) {
        HotspotQrDialog(config = config, onDismiss = { showQr = false })
    }

}

@Composable
private fun HotspotStatusCard(
    snapshot: HotspotSnapshot?,
    isLoading: Boolean,
    isWorking: Boolean,
    passphraseVisible: Boolean,
    enabled: Boolean,
    onTogglePassphrase: () -> Unit,
    onCopyPassphrase: () -> Unit,
    onShowQr: () -> Unit,
    onToggleHotspot: (Boolean) -> Unit
) {
    val state = snapshot?.state ?: HotspotState.DISABLED
    val config = snapshot?.config ?: HotspotConfig()
    val isOn = state == HotspotState.ENABLED
    AppCard(highlighted = isOn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = if (isOn) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                containerColor = if (isOn) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isOn) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.ssid.ifBlank { stringResource(R.string.hotspot_no_ssid) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(hotspotStateLabel(state)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = stringResource(securityLabel(config.security)),
                tone = if (config.security == HotspotSecurity.OPEN) {
                    StatusTone.WARNING
                } else {
                    StatusTone.ACTIVE
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (config.security != HotspotSecurity.OPEN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (passphraseVisible) {
                        config.passphrase.ifBlank { stringResource(R.string.hotspot_unknown_passphrase) }
                    } else {
                        "•".repeat(config.passphrase.length.coerceAtLeast(8))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onTogglePassphrase) {
                    Icon(
                        imageVector = if (passphraseVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = stringResource(
                            if (passphraseVisible) {
                                R.string.hotspot_hide_password
                            } else {
                                R.string.hotspot_show_password
                            }
                        )
                    )
                }
                IconButton(onClick = onCopyPassphrase) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.hotspot_copy_password)
                    )
                }
                IconButton(onClick = onShowQr) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = stringResource(R.string.hotspot_show_qr)
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.hotspot_open_network),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShowQr) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = stringResource(R.string.hotspot_show_qr)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isWorking && !isLoading,
            onClick = { onToggleHotspot(!isOn) }
        ) {
            if (isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                stringResource(
                    if (isOn) R.string.hotspot_stop else R.string.hotspot_start
                )
            )
        }
    }
}

@Composable
private fun HotspotConfigCard(
    draft: HotspotConfig,
    capabilities: HotspotCapabilities?,
    enabled: Boolean,
    onDraftChange: (HotspotConfig) -> Unit
) {
    var draftPassphraseVisible by remember { mutableStateOf(false) }
    AppCard {
        OutlinedTextField(
            value = draft.ssid,
            onValueChange = { onDraftChange(draft.copy(ssid = it)) },
            label = { Text(stringResource(R.string.hotspot_ssid)) },
            singleLine = true,
            enabled = enabled,
            shape = AppShapes.field,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.passphrase,
            onValueChange = { onDraftChange(draft.copy(passphrase = it)) },
            label = { Text(stringResource(R.string.hotspot_passphrase)) },
            singleLine = true,
            enabled = enabled && draft.security != HotspotSecurity.OPEN,
            shape = AppShapes.field,
            visualTransformation = if (draftPassphraseVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { draftPassphraseVisible = !draftPassphraseVisible }) {
                        Icon(
                            imageVector = if (draftPassphraseVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = stringResource(
                                if (draftPassphraseVisible) {
                                    R.string.hotspot_hide_password
                                } else {
                                    R.string.hotspot_show_password
                                }
                            )
                        )
                    }
                    IconButton(
                        enabled = enabled && draft.security != HotspotSecurity.OPEN,
                        onClick = {
                            onDraftChange(draft.copy(passphrase = HotspotManager.generatePassphrase()))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Casino,
                            contentDescription = stringResource(R.string.hotspot_random_password)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        val securityOptions = (capabilities?.supportedSecurityTypes
            ?: listOf(HotspotSecurity.OPEN, HotspotSecurity.WPA2_PSK)).sorted()
        ChoiceRow(
            title = stringResource(R.string.hotspot_security),
            icon = Icons.Filled.Lock,
            selectedLabel = stringResource(securityLabel(draft.security)),
            options = securityOptions.map { it to stringResource(securityLabel(it)) },
            enabled = enabled,
            onSelect = { value ->
                onDraftChange(
                    draft.copy(
                        security = value,
                        passphrase = if (value != HotspotSecurity.OPEN && draft.passphrase.isBlank()) {
                            HotspotManager.generatePassphrase()
                        } else {
                            draft.passphrase
                        }
                    )
                )
            }
        )

        val bandOptions = capabilities?.supportedBands ?: listOf(HotspotBand.BAND_2GHZ)
        ChoiceRow(
            title = stringResource(R.string.hotspot_band),
            icon = Icons.Filled.NetworkCheck,
            selectedLabel = stringResource(bandLabel(draft.band)),
            options = bandOptions.map { it to stringResource(bandLabel(it)) },
            enabled = enabled,
            onSelect = { onDraftChange(draft.copy(band = it)) }
        )

        ChoiceRow(
            title = stringResource(R.string.hotspot_mac_randomization),
            icon = Icons.Filled.Router,
            selectedLabel = stringResource(randomizationLabel(draft.macRandomization)),
            options = HotspotRandomization.ALL.map { it to stringResource(randomizationLabel(it)) },
            enabled = enabled && capabilities?.isMacRandomizationSupported != false,
            onSelect = { onDraftChange(draft.copy(macRandomization = it)) }
        )

        ToggleRow(
            title = stringResource(R.string.hotspot_hidden),
            subtitle = stringResource(R.string.hotspot_hidden_desc),
            icon = Icons.Filled.VisibilityOff,
            checked = draft.isHidden,
            enabled = enabled,
            onCheckedChange = { onDraftChange(draft.copy(isHidden = it)) }
        )

        ToggleRow(
            title = stringResource(R.string.hotspot_auto_shutdown),
            subtitle = stringResource(R.string.hotspot_auto_shutdown_desc),
            icon = Icons.Filled.Timer,
            checked = draft.isAutoShutdownEnabled,
            enabled = enabled,
            onCheckedChange = { onDraftChange(draft.copy(isAutoShutdownEnabled = it)) }
        )

        ChoiceRow(
            title = stringResource(R.string.hotspot_auto_shutdown_timeout),
            icon = Icons.Filled.Timer,
            selectedLabel = timeoutLabel(draft.autoShutdownTimeout),
            options = HotspotTimeout.ALL.map { it.toInt() to timeoutLabel(it) },
            enabled = enabled && draft.isAutoShutdownEnabled,
            onSelect = { minutes ->
                val timeout = HotspotTimeout.ALL.firstOrNull { it.toInt() == minutes }
                    ?: HotspotTimeout.DEFAULT
                onDraftChange(draft.copy(autoShutdownTimeout = timeout))
            }
        )

        val ceiling = (capabilities?.maxSupportedClients ?: 0).takeIf { it > 0 } ?: 16
        SettingRow(
            title = stringResource(R.string.hotspot_max_clients),
            subtitle = if (draft.maxClients == 0) {
                stringResource(R.string.hotspot_max_clients_unlimited)
            } else {
                draft.maxClients.toString()
            },
            icon = Icons.Filled.Devices,
            enabled = enabled,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.size(36.dp),
                        enabled = enabled && draft.maxClients > 0,
                        onClick = { onDraftChange(draft.copy(maxClients = draft.maxClients - 1)) }
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    FilledTonalIconButton(
                        modifier = Modifier.size(36.dp),
                        enabled = enabled && draft.maxClients < ceiling,
                        onClick = { onDraftChange(draft.copy(maxClients = draft.maxClients + 1)) }
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedLabel: String,
    options: List<Pair<Int, String>>,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingRow(
            title = title,
            subtitle = selectedLabel,
            icon = icon,
            enabled = enabled,
            onClick = { expanded = true },
            trailing = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientRow(
    client: HotspotClient,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    SettingRow(
        title = client.hostname ?: client.macAddress,
        subtitle = listOfNotNull(
            client.ipAddress,
            client.macAddress.takeIf { client.hostname != null }
        ).joinToString(" • ").ifBlank { client.macAddress },
        icon = Icons.Filled.Devices,
        enabled = true,
        trailing = {
            TextButton(onClick = onAction, enabled = enabled) {
                Icon(actionIcon, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    )
}

@Composable
private fun HotspotQrDialog(config: HotspotConfig, onDismiss: () -> Unit) {
    val network = WifiNetwork(
        ssid = config.ssid,
        password = config.passphrase,
        security = if (config.security == HotspotSecurity.OPEN) WifiSecurity.OPEN else WifiSecurity.WPA,
        isHidden = config.isHidden
    )
    val bitmap = remember(config) {
        QrCodeRenderer.render(QrCodeRenderer.wifiPayload(network), QR_SIZE_PX)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AppShapes.card, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = config.ssid.ifBlank { stringResource(R.string.hotspot_no_ssid) },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(240.dp)
                    )
                } else {
                    Text(stringResource(R.string.hotspot_qr_failed))
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        }
    }
}


private fun copyToClipboard(context: Context, value: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("hotspot", value))
}

private fun hotspotStateLabel(state: Int) = when (state) {
    HotspotState.ENABLED -> R.string.hotspot_state_enabled
    HotspotState.ENABLING -> R.string.hotspot_state_enabling
    HotspotState.DISABLING -> R.string.hotspot_state_disabling
    HotspotState.FAILED -> R.string.hotspot_state_failed
    else -> R.string.hotspot_state_disabled
}

private fun securityLabel(security: Int) = when (security) {
    HotspotSecurity.OPEN -> R.string.hotspot_security_open
    HotspotSecurity.WPA3_SAE_TRANSITION -> R.string.hotspot_security_wpa3_transition
    HotspotSecurity.WPA3_SAE -> R.string.hotspot_security_wpa3
    else -> R.string.hotspot_security_wpa2
}

private fun bandLabel(band: Int) = when (band) {
    HotspotBand.BAND_5GHZ -> R.string.hotspot_band_5
    HotspotBand.BAND_6GHZ -> R.string.hotspot_band_6
    else -> R.string.hotspot_band_2
}

private fun randomizationLabel(value: Int) = when (value) {
    HotspotRandomization.PERSISTENT -> R.string.hotspot_randomization_persistent
    HotspotRandomization.NON_PERSISTENT -> R.string.hotspot_randomization_non_persistent
    else -> R.string.hotspot_randomization_none
}

@Composable
private fun timeoutLabel(timeout: Long): String = when (timeout) {
    HotspotTimeout.DEFAULT -> stringResource(R.string.hotspot_timeout_default)
    else -> stringResource(R.string.hotspot_timeout_minutes, (timeout / 60000L).toInt())
}
