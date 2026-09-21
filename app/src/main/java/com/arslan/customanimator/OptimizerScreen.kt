package com.arslan.customanimator

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.BoostSnapshot
import com.arslan.customanimator.utils.BoostStats
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.utils.CloseAppsExclusionManager
import com.arslan.customanimator.utils.CompileFilterManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.MemoryBooster
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.LocalShizukuPermission

private val SPINNER_FRAMES = listOf("|", "/", "-", "\\")

private const val TERMINAL_LINE_DELAY_MS = 220L
private const val SCAN_STEP_DELAY_MS = 520L
private const val STAGE_DELAY_MS = 1100L
private const val PER_APP_DELAY_MS = 140L
private const val FINALIZE_DELAY_MS = 1500L

private class TerminalWriter(
    private val append: (String) -> Unit,
    private val setStatus: (String?) -> Unit
) {
    fun blank() = append("")

    suspend fun line(text: String, delayMs: Long = TERMINAL_LINE_DELAY_MS) {
        append(text)
        delay(delayMs)
    }

    suspend fun stage(title: String, status: String) {
        append("")
        setStatus(status)
        line(title, STAGE_DELAY_MS)
    }

    fun status(text: String?) = setStatus(text)
}

private class CleanOutcome(val storageFreed: Long, val ramFreed: Long)

@Composable
fun OptimizerScreenContent(onNavigateToCloseAppsExclusions: () -> Unit) {
    var showBooster by rememberSaveable { mutableStateOf(false) }

    if (showBooster) {
        BoosterTerminalScreen(onClose = { showBooster = false })
    } else {
        OptimizerHome(
            onOpenBooster = { showBooster = true },
            onNavigateToCloseAppsExclusions = onNavigateToCloseAppsExclusions
        )
    }
}

@Composable
private fun OptimizerHome(
    onOpenBooster: () -> Unit,
    onNavigateToCloseAppsExclusions: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val scope = rememberCoroutineScope()

    val hasShizukuPermission = LocalShizukuPermission.current
    val isAdFree by rememberIsAdFree()

    var isCleaning by remember { mutableStateOf(false) }
    var isPreparingAd by remember { mutableStateOf(false) }
    var cleanOutcome by remember { mutableStateOf<CleanOutcome?>(null) }

    LaunchedEffect(Unit) {
        if (!isAdFree) RewardedAds.prepare(context)
    }

    fun startClean() {
        if (isCleaning) return
        isCleaning = true
        cleanOutcome = null
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { runClean(context) }
            isCleaning = false
            cleanOutcome = outcome
            Toast.makeText(
                context,
                resources.getString(
                    R.string.boost_widget_result,
                    BoostStats.formatSize(context, outcome.storageFreed),
                    BoostStats.formatSize(context, outcome.ramFreed)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val gatedStart: (() -> Unit) -> Unit = { action ->
        if (isAdFree) {
            action()
        } else {
            isPreparingAd = true
            RewardedAds.show(context) { result ->
                isPreparingAd = false
                if (result == RewardedAds.Result.REWARDED) action() else showAdFailure(context, result)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasShizukuPermission) {
            SetupNudgeCard(
                message = stringResource(R.string.developer_needs_shizuku),
                onOpenSetup = openSetup
            )
        }

        InfoCard(
            dismissKey = "optimizer_info",
            texts = listOf(stringResource(R.string.optimizer_info))
        )

        ActionCard(
            icon = Icons.Filled.Bolt,
            title = stringResource(R.string.optimizer_performance_booster),
            description = stringResource(R.string.optimizer_performance_booster_desc),
            buttonLabel = stringResource(if (isAdFree) R.string.boost_start else R.string.boost_watch_ad),
            adSupported = !isAdFree,
            busy = isPreparingAd,
            enabled = hasShizukuPermission && !isCleaning && !isPreparingAd,
            onClick = { gatedStart(onOpenBooster) }
        )

        ActionCard(
            icon = Icons.Filled.CleaningServices,
            title = stringResource(R.string.optimizer_cleaner),
            description = stringResource(R.string.optimizer_cleaner_desc),
            buttonLabel = stringResource(
                when {
                    isCleaning -> R.string.optimizer_cleaning_label
                    isAdFree -> R.string.optimizer_clean_start
                    else -> R.string.optimizer_clean_watch_ad
                }
            ),
            adSupported = !isAdFree,
            busy = isCleaning || isPreparingAd,
            enabled = hasShizukuPermission && !isCleaning && !isPreparingAd,
            onClick = { gatedStart(::startClean) }
        )

        Card(
            shape = AppShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationRow(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.close_apps_exclusions),
                description = stringResource(R.string.close_apps_exclusions_desc),
                onClick = onNavigateToCloseAppsExclusions
            )
        }

        cleanOutcome?.let { outcome ->
            CleanResultCard(outcome = outcome)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    adSupported: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = if (adSupported) Icons.Filled.PlayCircleOutline else icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CleanResultCard(outcome: CleanOutcome) {
    val context = LocalContext.current
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.optimizer_clean_done),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.boost_widget_result,
                    BoostStats.formatSize(context, outcome.storageFreed),
                    BoostStats.formatSize(context, outcome.ramFreed)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoosterTerminalScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    val lines = remember { mutableStateListOf<String>() }
    var isRunning by remember { mutableStateOf(false) }
    var statusLabel by remember { mutableStateOf<String?>(null) }
    var runJob by remember { mutableStateOf<Job?>(null) }

    val listState = rememberLazyListState()
    val spinnerTick = rememberSpinnerTick(isRunning)

    LaunchedEffect(lines.size, isRunning) {
        val last = (lines.size + (if (isRunning) 1 else 0) - 1).coerceAtLeast(0)
        listState.animateScrollToItem(last)
    }

    LaunchedEffect(Unit) {
        isRunning = true
        statusLabel = resources.getString(R.string.boost_running_label)
        val writer = TerminalWriter({ text -> lines.add(text) }, { status -> statusLabel = status })
        runJob = scope.launch {
            try {
                executeBoost(context, writer)
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    lines.add("")
                    lines.add("  ── ${resources.getString(R.string.boost_cancelled)} ──")
                }
                throw e
            } finally {
                isRunning = false
                statusLabel = null
                runJob = null
            }
        }
    }

    BackHandler {
        runJob?.cancel()
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.optimizer_performance_booster)) },
                navigationIcon = {
                    IconButton(onClick = {
                        runJob?.cancel()
                        onClose()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TerminalCard(
                lines = lines,
                listState = listState,
                isRunning = isRunning,
                statusLabel = statusLabel,
                spinnerTick = spinnerTick,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    runJob?.cancel()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(if (isRunning) R.string.boost_cancel else R.string.close),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun rememberSpinnerTick(isRunning: Boolean): Int {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(120)
            tick++
        }
    }
    return tick
}

private fun showAdFailure(context: Context, result: RewardedAds.Result) {
    val message = if (result == RewardedAds.Result.CANCELLED) {
        R.string.boost_ad_reward_denied
    } else {
        R.string.boost_ad_unavailable
    }
    Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
}

private fun runClean(context: Context): CleanOutcome {
    val before = BoostStats.snapshot(context)
    DeveloperOptionsManager.clearAllAppCaches()
    MemoryBooster.boost()
    ShizukuHelper.executeShellCommand(arrayOf("sync"))
    val after = BoostStats.snapshot(context)
    return CleanOutcome(
        storageFreed = (after.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L),
        ramFreed = (after.availableRamBytes - before.availableRamBytes).coerceAtLeast(0L)
    )
}

@Composable
private fun TerminalCard(
    lines: List<String>,
    listState: LazyListState,
    isRunning: Boolean,
    statusLabel: String?,
    spinnerTick: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            if (lines.isEmpty() && !isRunning) {
                item {
                    Text(
                        text = stringResource(R.string.optimizer_terminal_empty),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(lines) { line -> TerminalLine(line) }
                if (isRunning) {
                    item {
                        Text(
                            text = "${statusLabel ?: "> working"}${SPINNER_FRAMES[spinnerTick % SPINNER_FRAMES.size]}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLine(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
    )
}

private suspend fun banner(writer: TerminalWriter, subtitle: String) {
    val padded = subtitle.padEnd(13)
    writer.blank()
    writer.line("  ┌────────────────────────────────────┐", 0L)
    writer.line("  │    SYSTEM  OPTIMIZER  ENGINE       │", 0L)
    writer.line("  │    Custom Animator · $padded  │", 0L)
    writer.line("  └────────────────────────────────────┘", STAGE_DELAY_MS)
}

private suspend fun reportDeviceState(context: Context, writer: TerminalWriter, before: BoostSnapshot) {
    val usedRam = before.totalRamBytes - before.availableRamBytes
    val usedStorage = before.totalStorageBytes - before.availableStorageBytes
    writer.line("   · RAM      ${BoostStats.formatSize(context, usedRam)} used / ${BoostStats.formatSize(context, before.totalRamBytes)}", SCAN_STEP_DELAY_MS)
    writer.line("   · storage  ${BoostStats.formatSize(context, usedStorage)} used / ${BoostStats.formatSize(context, before.totalStorageBytes)}", SCAN_STEP_DELAY_MS)
    writer.line("   · RAM in use ${percentOf(usedRam, before.totalRamBytes)}%  ${progressBar(percentOf(usedRam, before.totalRamBytes))}", SCAN_STEP_DELAY_MS)
}

private suspend fun trimCaches(context: Context, writer: TerminalWriter, beforeStorage: Long): Long {
    val ok = withContext(Dispatchers.IO) { DeveloperOptionsManager.clearAllAppCaches() }
    val after = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    val freed = (after.availableStorageBytes - beforeStorage).coerceAtLeast(0L)
    if (ok) {
        writer.line("   · pm trim-caches OK")
        writer.line("   · storage reclaimed: ${BoostStats.formatSize(context, freed)}", STAGE_DELAY_MS)
    } else {
        writer.line("   · pm trim-caches FAILED", STAGE_DELAY_MS)
    }
    return freed
}

private suspend fun compactMemory(writer: TerminalWriter) {
    val memoryOk = withContext(Dispatchers.IO) { MemoryBooster.compact() }
    writer.line(if (memoryOk) "   · am compact all full OK" else "   · memory compaction FAILED")
    val syncOk = withContext(Dispatchers.IO) { ShizukuHelper.executeShellCommand(arrayOf("sync")) }
    writer.line(if (syncOk) "   · filesystem buffers flushed" else "   · sync FAILED", STAGE_DELAY_MS)
}

private suspend fun closeBackgroundApps(context: Context, writer: TerminalWriter, apps: List<InstalledAppInfo>): Int {
    val skip = withContext(Dispatchers.IO) {
        CloseAppsExclusionManager(context).getSelectedPackages() +
            InstalledAppsProvider.getUnsafeToKillPackages(context)
    }
    val targets = apps.filterNot { skip.contains(it.packageName) }
    var closed = 0
    targets.forEachIndexed { index, app ->
        val ok = withContext(Dispatchers.IO) { DeveloperOptionsManager.forceStopApp(app.packageName) }
        if (ok) closed++
        writer.status("> Closing ${index + 1}/${targets.size}")
        writer.line("   ${if (ok) "·" else "!"} ${app.label.take(28)}  ${if (ok) "closed" else "skipped"}", PER_APP_DELAY_MS)
    }
    writer.line("   · closed $closed of ${targets.size}", STAGE_DELAY_MS)
    return closed
}

private suspend fun recompileApps(writer: TerminalWriter, apps: List<InstalledAppInfo>, filter: CompileFilterManager.CompileFilter): Int {
    var compiled = 0
    apps.forEachIndexed { index, app ->
        val ok = withContext(Dispatchers.IO) {
            DeveloperOptionsManager.compileApp(app.packageName, filter, force = false)
        }
        if (ok) compiled++
        writer.status("> Compiling ${index + 1}/${apps.size}")
        writer.line("   ${if (ok) "·" else "!"} ${app.label.take(28)}  ${if (ok) "optimized" else "skipped"}", PER_APP_DELAY_MS)
    }
    writer.line("   · optimized $compiled, skipped ${apps.size - compiled}", STAGE_DELAY_MS)
    return compiled
}

private suspend fun executeBoost(context: Context, writer: TerminalWriter) {
    val startedAt = System.currentTimeMillis()
    banner(writer, "Boost")

    writer.stage("> [1/6] Reading device state", "> Reading device state")
    val before = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    reportDeviceState(context, writer, before)

    writer.stage("> [2/6] Enumerating installed apps", "> Enumerating apps")
    val allApps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
    writer.line("   · launchable apps: ${allApps.size}", SCAN_STEP_DELAY_MS)

    writer.stage("> [3/6] Trimming app caches", "> Trimming caches")
    val cacheFreed = trimCaches(context, writer, before.availableStorageBytes)

    writer.stage("> [4/6] Closing background apps", "> Closing background apps")
    val closed = closeBackgroundApps(context, writer, allApps)

    val filter = CompileFilterManager.CompileFilter.SPEED_PROFILE
    writer.stage("> [5/6] Recompiling apps", "> Recompiling apps")
    val compiled = recompileApps(writer, allApps, filter)

    writer.stage("> [6/6] Compacting memory", "> Compacting memory")
    compactMemory(writer)

    writer.status("> Verifying")
    writer.line("> Verifying device state", FINALIZE_DELAY_MS)
    val after = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    summarize(context, writer, before, after, cacheFreed, System.currentTimeMillis() - startedAt)

    writer.line("   APPS", SCAN_STEP_DELAY_MS)
    writer.line("     scanned    ${allApps.size}", SCAN_STEP_DELAY_MS)
    writer.line("     optimized  $compiled  (${filter.value})", SCAN_STEP_DELAY_MS)
    writer.line("     closed     $closed", SCAN_STEP_DELAY_MS)
    writer.blank()

    writer.line("  ── Your device is optimized ──", SCAN_STEP_DELAY_MS)
    writer.line("   Apps launch from freshly compiled code and", SCAN_STEP_DELAY_MS)
    writer.line("   memory has been compacted for smoother use.", FINALIZE_DELAY_MS)
    writer.blank()
    writer.line("  > done", 0L)
    writer.status(null)
}

private suspend fun summarize(
    context: Context,
    writer: TerminalWriter,
    before: BoostSnapshot,
    after: BoostSnapshot,
    cacheFreed: Long,
    elapsed: Long
) {
    val usedRam = before.totalRamBytes - before.availableRamBytes
    val usedRamAfter = after.totalRamBytes - after.availableRamBytes
    val ramFreed = (after.availableRamBytes - before.availableRamBytes).coerceAtLeast(0L)
    val storageFreed = (after.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L)
    val ramPercentBefore = percentOf(usedRam, before.totalRamBytes)
    val ramPercentAfter = percentOf(usedRamAfter, after.totalRamBytes)

    writer.blank()
    writer.line("  ╔══════════════════════════════════════╗", SCAN_STEP_DELAY_MS)
    writer.line("  ║        OPTIMIZATION  COMPLETE        ║", SCAN_STEP_DELAY_MS)
    writer.line("  ╚══════════════════════════════════════╝", SCAN_STEP_DELAY_MS)
    writer.blank()

    writer.line("   MEMORY", SCAN_STEP_DELAY_MS)
    writer.line("     before  ${progressBar(ramPercentBefore)}  $ramPercentBefore%  ${BoostStats.formatSize(context, usedRam)} used", SCAN_STEP_DELAY_MS)
    writer.line("     after   ${progressBar(ramPercentAfter)}  $ramPercentAfter%  ${BoostStats.formatSize(context, usedRamAfter)} used", SCAN_STEP_DELAY_MS)
    writer.line("     freed   ${BoostStats.formatSize(context, ramFreed)}${deltaSuffix(ramPercentBefore - ramPercentAfter)}", SCAN_STEP_DELAY_MS)
    writer.line("     free    ${BoostStats.formatSize(context, after.availableRamBytes)} of ${BoostStats.formatSize(context, after.totalRamBytes)}", SCAN_STEP_DELAY_MS)
    writer.blank()

    writer.line("   STORAGE", SCAN_STEP_DELAY_MS)
    writer.line("     caches  ${BoostStats.formatSize(context, cacheFreed)} trimmed", SCAN_STEP_DELAY_MS)
    writer.line("     freed   ${BoostStats.formatSize(context, storageFreed)}", SCAN_STEP_DELAY_MS)
    writer.line("     free    ${BoostStats.formatSize(context, after.availableStorageBytes)} of ${BoostStats.formatSize(context, after.totalStorageBytes)}", SCAN_STEP_DELAY_MS)
    writer.blank()

    writer.line("   RUN", SCAN_STEP_DELAY_MS)
    writer.line("     duration ${formatDuration(elapsed)}", SCAN_STEP_DELAY_MS)
    writer.blank()
}

private fun deltaSuffix(percentPoints: Int): String =
    if (percentPoints > 0) "  ▼ $percentPoints% load" else ""

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun percentOf(part: Long, total: Long): Int =
    if (total <= 0L) 0 else ((part.toDouble() / total) * 100).toInt().coerceIn(0, 100)

private fun progressBar(percent: Int): String {
    val filled = (percent / 10).coerceIn(0, 10)
    return "█".repeat(filled) + "░".repeat(10 - filled)
}
