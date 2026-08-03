package com.arslan.customanimator

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.utils.AlarmRevealer
import com.arslan.customanimator.utils.AlarmSource
import com.arslan.customanimator.utils.DeveloperOptionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmRevealerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var nextAlarm by remember { mutableStateOf<AlarmSource?>(null) }
    var others by remember { mutableStateOf<List<AlarmSource>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        isLoading = true
        nextAlarm = AlarmRevealer.getNextAlarm(context)
        others = withContext(Dispatchers.IO) { AlarmRevealer.getScheduledAlarmClocks(context) }
        isLoading = false
    }

    val openAppInfo: (String) -> Unit = { packageName ->
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.alarm_revealer),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
                actions = {
                    IconButton(onClick = { reloadKey++ }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh)
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (nextAlarm != null) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    if (nextAlarm != null) R.string.alarm_icon_active else R.string.alarm_icon_inactive
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        nextAlarm?.let { alarm ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = alarm.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (alarm.packageName.isNotEmpty()) {
                                Text(
                                    text = alarm.packageName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.alarm_rings_at,
                                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(Date(alarm.triggerTime))
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (alarm.packageName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { openAppInfo(alarm.packageName) }) {
                                        Text(stringResource(R.string.app_info), fontSize = 12.sp, maxLines = 1)
                                    }
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = withContext(Dispatchers.IO) {
                                                    DeveloperOptionsManager.forceStopApp(alarm.packageName)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    context.getString(
                                                        if (success) R.string.action_succeeded else R.string.action_failed
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                reloadKey++
                                            }
                                        },
                                        enabled = hasShizukuPermission,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text(stringResource(R.string.force_stop), fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (others.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.alarm_other_scheduled).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                items(others, key = { it.packageName }) { alarm ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = alarm.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = alarm.packageName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { openAppInfo(alarm.packageName) }) {
                                Text(stringResource(R.string.app_info), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            } else if (!isLoading && !hasShizukuPermission) {
                item {
                    Text(
                        text = stringResource(R.string.alarm_needs_shizuku_for_list),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
