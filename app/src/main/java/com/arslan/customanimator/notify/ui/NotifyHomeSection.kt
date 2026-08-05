package com.arslan.customanimator.notify.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.material3.Switch
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.arslan.customanimator.R
import com.arslan.customanimator.hasAllNotifyPermissions
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.service.isPrimeNotifyServiceEnabled
import com.arslan.customanimator.notify.service.setPrimeNotifyServiceEnabled
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyHomeSection(
    modifier: Modifier = Modifier,
    onNavigateToRules: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshState by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allPermissionsGranted = remember(refreshState) { hasAllNotifyPermissions(context) }

    var isServiceActive by remember { mutableStateOf(isPrimeNotifyServiceEnabled(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val notificationScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pn_service_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isServiceActive) stringResource(R.string.pn_service_active) else stringResource(R.string.pn_service_disabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isServiceActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isServiceActive,
                        onCheckedChange = {
                            if (it && !allPermissionsGranted) {
                                showPermissionDialog = true
                            } else {
                                isServiceActive = it
                                setPrimeNotifyServiceEnabled(context, it)
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToRules,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pn_rules),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val ruleCount = remember(refreshState) {
                            RulesManager(context).getRules().size
                        }
                        Text(
                            text = stringResource(R.string.pn_rules_count, ruleCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (remainingSeconds == 0) {
                        notificationScope.launch {
                            remainingSeconds = 4
                            while (remainingSeconds > 0) {
                                kotlinx.coroutines.delay(1000)
                                remainingSeconds--
                            }
                            sendTestNotification(context)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = remainingSeconds == 0
            ) {
                Text(stringResource(R.string.pn_send_test_notification))
            }

            if (remainingSeconds > 0) {
                Text(
                    text = stringResource(R.string.pn_sending_in_seconds, remainingSeconds),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    stringResource(R.string.pn_permission_required_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.pn_permission_required_desc))
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    onNavigateToPermissions()
                }) {
                    Text(stringResource(R.string.permissions_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun RuleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.pn_cd_settings_for, title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange
            ) {
                Icon(
                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (checked) stringResource(R.string.pn_cd_uncheck_item, title) else stringResource(R.string.pn_cd_check_item, title),
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotifyHomeSectionPreview() {
    CustomAnimatorTheme {
        NotifyHomeSection()
    }
}


private fun sendTestNotification(context: Context) {
    val channelId = "notify_test_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.getString(R.string.pn_test_notification_channel)
        val descriptionText = context.getString(R.string.pn_test_notification_channel_desc)
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(context.getString(R.string.pn_test_notification_title))
        .setContentText(context.getString(R.string.pn_test_notification_text))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        try {
            notify(4502, builder.build())
        } catch (e: SecurityException) {
        }
    }
}
