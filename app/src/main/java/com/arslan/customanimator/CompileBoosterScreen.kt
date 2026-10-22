package com.arslan.customanimator

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.HeroCard
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.CompileFilterManager
import com.arslan.customanimator.utils.LocalShizukuPermission
import java.text.NumberFormat
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar

private enum class CompilePhase { IDLE, PREPARING, RUNNING, FINISHED }

private val CompileBoosterProgressTracker.Progress.phase: CompilePhase
    get() = when {
        isFinished -> CompilePhase.FINISHED
        isRunning && total == 0 -> CompilePhase.PREPARING
        isRunning -> CompilePhase.RUNNING
        else -> CompilePhase.IDLE
    }

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
private fun rememberCompileBoosterStarter(): () -> Unit {
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    return {
        requestNotificationPermission()
        CompileBoosterService.start(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompileBoosterScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val progress by CompileBoosterProgressTracker.progress.collectAsState()
    var compileFilter by remember { mutableStateOf(CompileFilterManager.getFilter(context)) }
    val isAdFree by rememberIsAdFree()
    val startCompileBooster = rememberCompileBoosterStarter()
    val phase = progress.phase

    LaunchedEffect(Unit) { RewardedAds.prepare(context) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.compile_booster),
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            when (phase) {
                CompilePhase.IDLE -> CompileActionButton(
                    icon = Icons.Filled.Speed,
                    label = stringResource(
                        if (isAdFree) R.string.compile_all_apps else R.string.compile_all_apps_with_ad
                    ),
                    enabled = hasShizukuPermission,
                    onClick = {
                        CompileFilterManager.setFilter(context, compileFilter)
                        if (isAdFree) startCompileBooster() else requestReward(context) { startCompileBooster() }
                    }
                )
                CompilePhase.PREPARING, CompilePhase.RUNNING -> CompileActionButton(
                    icon = Icons.Filled.Stop,
                    label = stringResource(R.string.compile_booster_stop),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = { CompileBoosterService.stop(context) }
                )
                CompilePhase.FINISHED -> CompileActionButton(
                    icon = Icons.Filled.Done,
                    label = stringResource(R.string.compile_booster_done),
                    onClick = CompileBoosterProgressTracker::reset
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasShizukuPermission && phase == CompilePhase.IDLE) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }
            item(key = "status") {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "compileStatus"
                ) { shownPhase ->
                    when (shownPhase) {
                        CompilePhase.IDLE -> CompileIdleCard(filter = compileFilter)
                        CompilePhase.PREPARING -> CompilePreparingCard()
                        CompilePhase.RUNNING -> CompileRunningCard(progress = progress)
                        CompilePhase.FINISHED -> CompileFinishedCard(progress = progress)
                    }
                }
            }
            if (phase == CompilePhase.IDLE) {
                compileFilterSection(selected = compileFilter, onSelect = { compileFilter = it })
            } else {
                compileResults(progress.results)
            }
        }
    }
}

private fun LazyListScope.compileFilterSection(
    selected: CompileFilterManager.CompileFilter,
    onSelect: (CompileFilterManager.CompileFilter) -> Unit
) {
    item(key = "filter_header") { SectionHeader(title = stringResource(R.string.compile_filter_title)) }
    item(key = "filters") {
        AppCard(contentPadding = 12.dp) {
            CompileFilterManager.CompileFilter.entries.forEach { filter ->
                CompileFilterOption(
                    selected = filter == selected,
                    label = stringResource(filter.labelRes),
                    description = stringResource(filter.descriptionRes),
                    onClick = { onSelect(filter) }
                )
            }
        }
    }
}

private fun LazyListScope.compileResults(results: List<CompileBoosterProgressTracker.CompileResult>) {
    if (results.isEmpty()) return
    item(key = "results_header") { SectionHeader(title = stringResource(R.string.compile_booster_log_title)) }
    items(results.asReversed(), key = { it.packageName }) { result ->
        CompileResultRow(result = result, modifier = Modifier.animateItem())
    }
}

@Composable
private fun CompileIdleCard(filter: CompileFilterManager.CompileFilter) {
    HeroCard(
        title = stringResource(R.string.compile_booster_ready_title),
        subtitle = stringResource(R.string.compile_all_apps_confirm_message)
    ) {
        StatusPill(text = stringResource(filter.labelRes), tone = StatusTone.INFO)
    }
}

@Composable
private fun CompilePreparingCard() {
    HeroCard(
        title = stringResource(R.string.compile_booster_running_title),
        subtitle = stringResource(R.string.compile_booster_notif_text)
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompileRunningCard(progress: CompileBoosterProgressTracker.Progress) {
    val scheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(targetValue = progress.percent / 100f, label = "compileProgress")
    val remainingMillis = progress.remainingMillis

    HeroCard(
        title = stringResource(R.string.compile_booster_running_title),
        subtitle = stringResource(R.string.compile_booster_background_hint)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    trackColor = scheme.surface.copy(alpha = 0.6f)
                )
                Text(
                    text = NumberFormat.getPercentInstance().format(progress.percent / 100.0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.compile_booster_count, progress.current, progress.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimaryContainer
                )
                if (remainingMillis != null) {
                    Text(
                        text = stringResource(R.string.compile_booster_time_left, formatDuration(remainingMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        if (progress.currentPackage.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            CurrentAppRow(packageName = progress.currentPackage, label = progress.currentLabel)
        }
    }
}

@Composable
private fun CurrentAppRow(packageName: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.iconBadge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = packageName, modifier = Modifier.size(36.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompileFinishedCard(progress: CompileBoosterProgressTracker.Progress) {
    HeroCard(
        title = stringResource(
            if (progress.isStopped) R.string.compile_booster_stopped_title else R.string.compile_booster_done_title
        )
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompileStatTile(
                label = stringResource(R.string.compile_booster_stat_compiled),
                value = (progress.current - progress.failedCount).toString(),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            CompileStatTile(
                label = stringResource(R.string.compile_booster_stat_failed),
                value = progress.failedCount.toString(),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            CompileStatTile(
                label = stringResource(R.string.compile_booster_stat_time),
                value = formatDuration(progress.elapsedMillis),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun CompileStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(AppShapes.iconBadge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompileResultRow(
    result: CompileBoosterProgressTracker.CompileResult,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = result.packageName, modifier = Modifier.size(36.dp))
        Text(
            text = result.label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = stringResource(
                if (result.success) R.string.compile_booster_stat_compiled else R.string.compile_booster_stat_failed
            ),
            tint = if (result.success) LocalExtendedColors.current.success else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun CompileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 52.dp),
        shapes = ButtonDefaults.shapes()
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.titleSmall)
    }
}

private fun formatDuration(millis: Long): String = DateUtils.formatElapsedTime(millis / 1000)
