package com.arslan.customanimator.notify.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.R
import com.arslan.customanimator.hasAllNotifyPermissions
import com.arslan.customanimator.notify.data.IgnoreManager
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.service.isPrimeNotifyServiceEnabled
import com.arslan.customanimator.notify.service.setPrimeNotifyServiceEnabled
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.DividerSoft
import com.arslan.customanimator.ui.components.SettingRow
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import kotlinx.coroutines.launch

@Composable
fun NotifyHomeSection(
    modifier: Modifier = Modifier,
    onNavigateToRules: () -> Unit = {},
    onNavigateToFilters: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
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
    val ruleCount = remember(refreshState) { RulesManager(context).getRules().size }
    val filters = remember(refreshState) { IgnoreManager(context).getRules() }

    var isServiceActive by remember { mutableStateOf(isPrimeNotifyServiceEnabled(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val notificationScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppCard(contentPadding = 16.dp, highlighted = isServiceActive) {
            ToggleRow(
                title = stringResource(R.string.pn_service_title),
                subtitle = stringResource(
                    if (isServiceActive) R.string.pn_service_active_desc else R.string.pn_service_disabled_desc
                ),
                icon = Icons.Filled.Notifications,
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

        AppCard(contentPadding = 16.dp) {
            NotifyNavigationRow(
                icon = Icons.Filled.Bolt,
                title = stringResource(R.string.pn_rules),
                subtitle = stringResource(R.string.pn_rules_desc, ruleCount),
                onClick = onNavigateToRules
            )
            DividerSoft()
            NotifyNavigationRow(
                icon = Icons.Filled.FilterAlt,
                title = stringResource(R.string.pn_filters),
                subtitle = stringResource(
                    R.string.pn_filters_desc,
                    filters.size,
                    filters.count { it.blocksNotification }
                ),
                onClick = onNavigateToFilters
            )
            DividerSoft()
            NotifyNavigationRow(
                icon = Icons.Filled.History,
                title = stringResource(R.string.pn_nav_logging),
                subtitle = stringResource(R.string.pn_history_desc),
                onClick = onNavigateToHistory
            )
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
private fun NotifyNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
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
