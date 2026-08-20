package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.CompileFilterManager

@Composable
fun CompileBoosterScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current

    var showCompileAllConfirm by remember { mutableStateOf(false) }
    var compileFilter by remember { mutableStateOf(CompileFilterManager.getFilter(context)) }
    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()
    val isAdFree by rememberIsAdFree()

    LaunchedEffect(Unit) { RewardedAds.prepare(context) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val startCompileAll: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        CompileBoosterService.start(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                    CompileBoosterRow(
                        isRunning = compileProgress.isRunning,
                        percent = compileProgress.percent,
                        enabled = hasShizukuPermission,
                        filterLabel = stringResource(compileFilter.labelRes),
                        onClick = { showCompileAllConfirm = true },
                        onCancel = { CompileBoosterService.stop(context) }
                    )
                    if (!isAdFree) {
                        InfoNote(text = stringResource(R.string.compile_booster_ad_desc))
                    }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCompileAllConfirm) {
        AlertDialog(
            onDismissRequest = { showCompileAllConfirm = false },
            title = { Text(stringResource(R.string.compile_all_apps_confirm_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                Button(onClick = {
                    showCompileAllConfirm = false
                    CompileFilterManager.setFilter(context, compileFilter)
                    if (isAdFree) startCompileAll() else requestReward(context) { startCompileAll() }
                }) {
                    Text(
                        stringResource(
                            if (isAdFree) R.string.compile_all_apps else R.string.compile_all_apps_with_ad
                        )
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCompileAllConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
