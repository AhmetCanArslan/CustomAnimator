package com.arslan.customanimator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.data.WifiNetwork
import com.arslan.customanimator.data.WifiSecurity
import com.arslan.customanimator.utils.QrCodeRenderer
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.WifiBackupCodec
import com.arslan.customanimator.utils.WifiConfigReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val QR_SIZE_PX = 640
private const val CONNECTED_POLL_MS = 5000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiPasswordsScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var result by remember { mutableStateOf<WifiConfigReader.Result?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var revealedSsids by remember { mutableStateOf(setOf<String>()) }
    var connectedSsid by remember { mutableStateOf<String?>(null) }

    var qrNetwork by remember { mutableStateOf<WifiNetwork?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<Pair<WifiBackupCodec.Format, String>?>(null) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }

    val reload: suspend () -> Unit = {
        result = null
        result = withContext(Dispatchers.IO) { WifiConfigReader.readSavedNetworks(context) }
    }

    LaunchedEffect(hasShizukuPermission) { reload() }

    LaunchedEffect(Unit) {
        while (true) {
            connectedSsid = withContext(Dispatchers.IO) { WifiConfigReader.getConnectedSsid(context) }
            delay(CONNECTED_POLL_MS)
        }
    }

    val savedNetworks = (result as? WifiConfigReader.Result.Success)?.networks.orEmpty()

    val allNetworks = remember(savedNetworks, connectedSsid) {
        savedNetworks.sortedWith(
            compareByDescending<WifiNetwork> { it.ssid == connectedSsid }.thenBy { it.ssid.lowercase() }
        )
    }

    val filteredNetworks = remember(allNetworks, searchQuery) {
        allNetworks.filter { searchQuery.isBlank() || it.ssid.contains(searchQuery, ignoreCase = true) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val (format, password) = pendingExport ?: return@rememberLauncherForActivityResult
        pendingExport = null
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = WifiBackupCodec.encode(allNetworks, format, password)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("no stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                context.getString(if (success) R.string.wifi_export_done else R.string.wifi_export_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.wifi_password_manager),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { coroutineScope.launch { reload() } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.wifi_refresh))
                    }
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.wifi_more))
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        WifiBackupCodec.Format.entries.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(stringResource(exportLabelRes(format))) },
                                leadingIcon = { Icon(Icons.Filled.Upload, contentDescription = null) },
                                onClick = {
                                    showExportMenu = false
                                    if (format == WifiBackupCodec.Format.ENCRYPTED) {
                                        showExportPasswordDialog = true
                                    } else {
                                        pendingExport = format to ""
                                        exportLauncher.launch(WifiBackupCodec.fileName(format))
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = { BannerAdView() }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onShowSelectedOnlyChange = {},
                    showFilterCheckbox = false,
                    placeholder = stringResource(R.string.wifi_search)
                )
            }

            connectedSsid?.let { ssid ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.wifi_connected_to, ssid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            when (val state = result) {
                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WifiConfigReader.Result.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (state.needsShizukuPermission) {
                            WarningCard(
                                message = stringResource(state.messageRes),
                                actionLabel = stringResource(R.string.grant_shizuku_permission),
                                onAction = { ShizukuHelper.requestShizukuPermission(context) }
                            )
                        } else {
                            WarningCard(
                                message = stringResource(state.messageRes),
                                actionLabel = stringResource(R.string.wifi_refresh),
                                onAction = { coroutineScope.launch { reload() } }
                            )
                        }
                    }
                }
                is WifiConfigReader.Result.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredNetworks.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.wifi_no_networks),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                        items(filteredNetworks, key = { it.ssid }) { network ->
                            WifiNetworkCard(
                                network = network,
                                isConnected = network.ssid == connectedSsid,
                                isRevealed = revealedSsids.contains(network.ssid),
                                onToggleReveal = {
                                    revealedSsids = if (revealedSsids.contains(network.ssid)) {
                                        revealedSsids - network.ssid
                                    } else {
                                        revealedSsids + network.ssid
                                    }
                                },
                                onCopy = {
                                    copyToClipboard(context, network.ssid, network.password)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.wifi_password_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onShowQr = { qrNetwork = network }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    qrNetwork?.let { network ->
        WifiQrDialog(network = network, onDismiss = { qrNetwork = null })
    }


    if (showExportPasswordDialog) {
        WifiPasswordDialog(
            title = stringResource(R.string.wifi_export_encrypted),
            message = stringResource(R.string.wifi_export_password_message),
            confirmLabel = stringResource(R.string.wifi_export_action),
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { password ->
                showExportPasswordDialog = false
                pendingExport = WifiBackupCodec.Format.ENCRYPTED to password
                exportLauncher.launch(WifiBackupCodec.fileName(WifiBackupCodec.Format.ENCRYPTED))
            }
        )
    }

}


@StringRes
private fun exportLabelRes(format: WifiBackupCodec.Format): Int = when (format) {
    WifiBackupCodec.Format.PLAIN -> R.string.wifi_export_plain
    WifiBackupCodec.Format.COMPRESSED -> R.string.wifi_export_compressed
    WifiBackupCodec.Format.ENCRYPTED -> R.string.wifi_export_encrypted
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}


@Composable
private fun WifiNetworkCard(
    network: WifiNetwork,
    isConnected: Boolean,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onShowQr: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                SecurityChip(network.security, isConnected)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        network.security == WifiSecurity.OPEN -> stringResource(R.string.wifi_open_network)
                        isRevealed -> network.password
                        else -> "•".repeat(network.password.length.coerceIn(6, 16))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (network.security != WifiSecurity.OPEN) {
                    IconButton(onClick = onToggleReveal) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.wifi_toggle_password)
                        )
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.wifi_copy_password))
                    }
                }
                IconButton(onClick = onShowQr) {
                    Icon(Icons.Filled.QrCode2, contentDescription = stringResource(R.string.wifi_show_qr))
                }
            }
        }
    }
}

@Composable
private fun SecurityChip(security: WifiSecurity, isConnected: Boolean) {
    val label = if (isConnected) {
        stringResource(R.string.wifi_connected)
    } else {
        when (security) {
            WifiSecurity.WPA -> "WPA"
            WifiSecurity.WEP -> "WEP"
            WifiSecurity.OPEN -> stringResource(R.string.wifi_open)
        }
    }
    val container = if (isConnected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun WifiQrDialog(network: WifiNetwork, onDismiss: () -> Unit) {
    var bitmap by remember(network.ssid, network.password) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(network.ssid, network.password) {
        bitmap = withContext(Dispatchers.Default) {
            QrCodeRenderer.render(QrCodeRenderer.wifiPayload(network), QR_SIZE_PX)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(network.ssid) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val image = bitmap
                if (image != null) {
                    androidx.compose.foundation.Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(R.string.wifi_show_qr),
                        modifier = Modifier.size(240.dp)
                    )
                } else {
                    Text(stringResource(R.string.wifi_qr_failed), style = MaterialTheme.typography.bodyMedium,)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.wifi_qr_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}


@Composable
private fun WifiPasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium,)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.wifi_backup_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }, enabled = password.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
