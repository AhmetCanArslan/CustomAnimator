package com.arslan.customanimator

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.BoostStats
import com.arslan.customanimator.utils.CloseAppsExclusionManager
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.MemoryBooster
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SPINNER_FRAMES = listOf("|", "/", "-", "\\")

private const val TERMINAL_LINE_DELAY_MS = 220L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val scope = rememberCoroutineScope()

    var hasShizukuPermission by remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
    val isAdFree by rememberIsAdFree()

    val lines = remember { mutableStateListOf<String>() }
    var isRunning by remember { mutableStateOf(false) }
    var isPreparingAd by remember { mutableStateOf(false) }
    var statusLabel by remember { mutableStateOf<String?>(null) }

    var spinnerTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(120)
            spinnerTick++
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(lines.size, isRunning) {
        val last = (lines.size + (if (isRunning) 1 else 0) - 1).coerceAtLeast(0)
        listState.animateScrollToItem(last)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasShizukuPermission = ShizukuHelper.hasShizukuPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!isAdFree) RewardedAds.prepare(context)
    }

    fun startRun() {
        if (isRunning) return
        isRunning = true
        statusLabel = context.getString(R.string.boost_running_label)
        lines.clear()
        scope.launch {
            executeOptimization(context, { text -> lines.add(text) }, { status -> statusLabel = status })
            isRunning = false
            statusLabel = null
        }
    }

    val startBoost: () -> Unit = {
        when {
            !hasShizukuPermission -> Toast.makeText(
                context,
                context.getString(R.string.boost_widget_needs_shizuku),
                Toast.LENGTH_SHORT
            ).show()

            isAdFree -> startRun()

            else -> {
                isPreparingAd = true
                RewardedAds.show(context) { result ->
                    isPreparingAd = false
                    when (result) {
                        RewardedAds.Result.REWARDED -> startRun()
                        RewardedAds.Result.CANCELLED -> Toast.makeText(
                            context,
                            context.getString(R.string.boost_ad_reward_denied),
                            Toast.LENGTH_SHORT
                        ).show()

                        RewardedAds.Result.NOT_READY, RewardedAds.Result.ERROR ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.boost_ad_unavailable),
                                Toast.LENGTH_LONG
                            ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.boost_screen_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasShizukuPermission) {
                SetupNudgeCard(
                    message = stringResource(R.string.developer_needs_shizuku),
                    onOpenSetup = openSetup
                )
            }

            UnlockCard(isAdFree = isAdFree)

            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 380.dp)
                        .padding(12.dp)
                ) {
                    if (lines.isEmpty() && !isRunning) {
                        item {
                            Text(
                                text = stringResource(R.string.boost_terminal_empty),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(lines) { line ->
                            TerminalLine(line)
                        }
                        if (isRunning) {
                            item {
                                Text(
                                    text = "${statusLabel ?: "> working"}${
                                        SPINNER_FRAMES[spinnerTick % SPINNER_FRAMES.size]
                                    }",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = startBoost,
                enabled = hasShizukuPermission && !isRunning && !isPreparingAd,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                if (isRunning || isPreparingAd) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (isRunning) R.string.boost_running_label
                            else R.string.boost_ad_loading
                        )
                    )
                } else {
                    Icon(
                        imageVector = if (isAdFree) Icons.Filled.Bolt else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (isAdFree) R.string.boost_start else R.string.boost_watch_ad
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
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

@Composable
private fun UnlockCard(isAdFree: Boolean) {
    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isAdFree) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isAdFree) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAdFree) Icons.Filled.VerifiedUser else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isAdFree) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isAdFree) R.string.boost_premium_title else R.string.boost_ad_title
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        if (isAdFree) R.string.boost_premium_desc else R.string.boost_ad_desc
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun executeOptimization(
    context: Context,
    append: (String) -> Unit,
    setStatus: (String?) -> Unit
) {
    append("")
    append("  ┌────────────────────────────────────┐")
    append("  │    SYSTEM  OPTIMIZER  ENGINE       │")
    append("  │    Custom Animator · Boost         │")
    append("  └────────────────────────────────────┘")
    append("")
    setStatus("> Reading device state")
    append("> Reading device state")

    val before = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    val usedRam = before.totalRamBytes - before.availableRamBytes
    val usedStorage = before.totalStorageBytes - before.availableStorageBytes

    append("   · RAM      ${BoostStats.formatSize(context, usedRam)} used / ${BoostStats.formatSize(context, before.totalRamBytes)}")
    append("   · storage  ${BoostStats.formatSize(context, usedStorage)} used / ${BoostStats.formatSize(context, before.totalStorageBytes)}")
    append("   · RAM in use ${percentOf(usedRam, before.totalRamBytes)}%  ${progressBar(percentOf(usedRam, before.totalRamBytes))}")
    delay(TERMINAL_LINE_DELAY_MS)

    setStatus("> Enumerating apps")
    append("> Enumerating installed apps")
    val protectedPackages = withContext(Dispatchers.IO) {
        CloseAppsExclusionManager(context).getSelectedPackages() +
            InstalledAppsProvider.getUnsafeToKillPackages(context)
    }
    val allApps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
    val targets = allApps.filterNot { protectedPackages.contains(it.packageName) }

    append("   · launchable apps: ${allApps.size}")
    append("   · protected (skipped): ${allApps.size - targets.size}")
    append("   · will be stopped: ${targets.size}")
    delay(TERMINAL_LINE_DELAY_MS)

    append("")
    append("> [1/3] Trimming app caches")
    setStatus("> Trimming caches")
    val cacheOk = withContext(Dispatchers.IO) { DeveloperOptionsManager.clearAllAppCaches() }
    val afterCache = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    val cacheFreed = (afterCache.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L)
    if (cacheOk) {
        append("   · pm trim-caches OK")
        append("   · storage reclaimed: ${BoostStats.formatSize(context, cacheFreed)}")
    } else {
        append("   · pm trim-caches FAILED")
    }
    delay(TERMINAL_LINE_DELAY_MS)

    append("")
    append("> [2/3] Stopping background apps")
    setStatus("> Stopping background apps")
    var stopped = 0
    var failed = 0
    targets.forEach { app ->
        val ok = withContext(Dispatchers.IO) { DeveloperOptionsManager.forceStopApp(app.packageName) }
        if (ok) stopped++ else failed++
        setStatus("> Stopping ${stopped + failed}/${targets.size}")
        append("   ${if (ok) "·" else "!"} ${app.label.take(28)}  ${if (ok) "stopped" else "skipped"}")
    }
    append("   · stopped $stopped, skipped $failed")
    delay(TERMINAL_LINE_DELAY_MS)

    append("")
    append("> [3/3] Compacting memory")
    setStatus("> Compacting memory")
    val memoryOk = withContext(Dispatchers.IO) { MemoryBooster.boost() }
    append(if (memoryOk) "   · am kill-all + am compact all full OK" else "   · memory compaction FAILED")
    delay(TERMINAL_LINE_DELAY_MS)

    val after = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
    val storageFreed = (after.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L)
    val ramFreed = (after.availableRamBytes - before.availableRamBytes).coerceAtLeast(0L)
    val usedRamAfter = after.totalRamBytes - after.availableRamBytes

    append("")
    append("  ── Optimization complete ──")
    append("   storage freed:  ${BoostStats.formatSize(context, storageFreed)}")
    append("   RAM freed:      ${BoostStats.formatSize(context, ramFreed)}")
    append("   available RAM:  ${BoostStats.formatSize(context, after.availableRamBytes)}")
    append("   RAM in use ${percentOf(usedRamAfter, after.totalRamBytes)}%  ${progressBar(percentOf(usedRamAfter, after.totalRamBytes))}")
    setStatus(null)
}

private fun percentOf(part: Long, total: Long): Int =
    if (total <= 0L) 0 else ((part.toDouble() / total) * 100).toInt().coerceIn(0, 100)

private fun progressBar(percent: Int): String {
    val filled = (percent / 10).coerceIn(0, 10)
    return "█".repeat(filled) + "░".repeat(10 - filled)
}
