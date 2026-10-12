package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.CompileFilterManager

@Composable
internal fun CompileBoosterToolRow(onClick: () -> Unit) {
    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()
    NavigationRow(
        icon = Icons.Filled.Speed,
        title = stringResource(R.string.compile_booster),
        description = if (compileProgress.isRunning) {
            stringResource(R.string.compile_booster_progress, compileProgress.percent)
        } else {
            stringResource(R.string.more_compile_booster_desc)
        },
        onClick = onClick
    )
}

@Composable
internal fun rememberCompileBoosterStarter(): () -> Unit {
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    return {
        requestNotificationPermission()
        CompileBoosterService.start(context)
    }
}

@Composable
internal fun CompileBoosterDialog(
    hasShizukuPermission: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()
    if (compileProgress.isRunning) {
        CompileProgressDialog(percent = compileProgress.percent, onDismiss = onDismiss)
    } else {
        CompileConfirmDialog(
            hasShizukuPermission = hasShizukuPermission,
            onStart = onStart,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun CompileProgressDialog(percent: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compile_booster)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.compile_booster_progress, percent))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    CompileBoosterService.stop(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun CompileConfirmDialog(
    hasShizukuPermission: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    var compileFilter by remember { mutableStateOf(CompileFilterManager.getFilter(context)) }
    val isAdFree by rememberIsAdFree()

    LaunchedEffect(Unit) { RewardedAds.prepare(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compile_all_apps_confirm_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasShizukuPermission) {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = {
                            onDismiss()
                            openSetup()
                        }
                    )
                }
                Text(stringResource(R.string.compile_all_apps_confirm_message))
                Text(
                    text = stringResource(R.string.compile_filter_title),
                    style = MaterialTheme.typography.titleSmall
                )
                CompileFilterManager.CompileFilter.entries.forEach { filter ->
                    CompileFilterOption(
                        selected = filter == compileFilter,
                        label = stringResource(filter.labelRes),
                        description = stringResource(filter.descriptionRes),
                        onClick = { compileFilter = filter }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = hasShizukuPermission,
                onClick = {
                    onDismiss()
                    CompileFilterManager.setFilter(context, compileFilter)
                    if (isAdFree) onStart() else requestReward(context) { onStart() }
                }
            ) {
                Text(
                    stringResource(
                        if (isAdFree) R.string.compile_all_apps else R.string.compile_all_apps_with_ad
                    )
                )
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
