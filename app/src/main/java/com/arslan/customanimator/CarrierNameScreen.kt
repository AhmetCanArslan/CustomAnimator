package com.arslan.customanimator

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.utils.CarrierNameManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.SimSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrierNameScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPhonePermission by remember { mutableStateOf(CarrierNameManager.hasPhonePermission(context)) }
    var slots by remember { mutableStateOf<List<SimSlot>>(emptyList()) }
    var selectedSubId by remember { mutableStateOf(-1) }
    var newName by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhonePermission = granted
        reloadKey++
    }

    LaunchedEffect(reloadKey, hasPhonePermission) {
        slots = CarrierNameManager.getSimSlots(context)
        if (slots.none { it.subId == selectedSubId }) {
            selectedSubId = slots.firstOrNull()?.subId ?: -1
        }
    }

    val runAction: (suspend () -> Boolean) -> Unit = { action ->
        isBusy = true
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { action() }
            isBusy = false
            Toast.makeText(
                context,
                context.getString(if (success) R.string.action_succeeded else R.string.action_failed),
                Toast.LENGTH_SHORT
            ).show()
            reloadKey++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.carrier_name),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!CarrierNameManager.isSupported()) {
                item {
                    WarningCard(stringResource(R.string.carrier_name_unsupported))
                }
                return@LazyColumn
            }

            if (!hasShizukuPermission) {
                item {
                    WarningCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        actionLabel = stringResource(R.string.grant_shizuku_permission),
                        onAction = { ShizukuHelper.requestShizukuPermission(context) }
                    )
                }
            }

            if (!hasPhonePermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.card
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.carrier_name_needs_phone_permission),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            }) {
                                Text(stringResource(R.string.grant), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            if (slots.isEmpty()) {
                item { WarningCard(stringResource(R.string.carrier_name_no_sim)) }
                return@LazyColumn
            }

            if (slots.size > 1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.card
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            slots.forEach { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSubId == slot.subId,
                                        onClick = { selectedSubId = slot.subId }
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.carrier_name_slot,
                                            slot.slotIndex + 1,
                                            slot.carrierName
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        slots.firstOrNull { it.subId == selectedSubId }?.let { slot ->
                            Text(
                                text = stringResource(R.string.carrier_name_current, slot.carrierName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(R.string.carrier_name_new)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    runAction { CarrierNameManager.setCarrierName(selectedSubId, newName) }
                                },
                                enabled = hasShizukuPermission && !isBusy && newName.isNotBlank()
                            ) {
                                Text(stringResource(R.string.apply_settings), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    newName = ""
                                    runAction { CarrierNameManager.resetCarrierName(selectedSubId) }
                                },
                                enabled = hasShizukuPermission && !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(stringResource(R.string.reset), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.carrier_name_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}
