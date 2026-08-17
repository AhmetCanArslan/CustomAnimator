package com.arslan.customanimator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.service.CompileBoosterService
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.BoostStats
import com.arslan.customanimator.utils.CompileBoosterProgressTracker
import com.arslan.customanimator.utils.CompileFilterManager
import com.arslan.customanimator.utils.DeveloperOptionsManager
import com.arslan.customanimator.utils.MemoryBooster
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BOOST_SETTLE_DELAY_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var hasShizukuPermission by remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
    var isBoosting by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }

    var availableRam by remember {
        mutableStateOf(BoostStats.snapshot(context).availableRamBytes)
    }
    var availableStorage by remember {
        mutableStateOf(BoostStats.snapshot(context).availableStorageBytes)
    }

    var compileFilter by remember { mutableStateOf(CompileFilterManager.getFilter(context)) }
    val compileProgress by CompileBoosterProgressTracker.progress.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasShizukuPermission = ShizukuHelper.hasShizukuPermission()
                val snapshot = BoostStats.snapshot(context)
                availableRam = snapshot.availableRamBytes
                availableStorage = snapshot.availableStorageBytes
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val requestNotificationsIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val startCompileAll: () -> Unit = {
        requestNotificationsIfNeeded()
        CompileBoosterService.start(context)
    }

    val runBoost: () -> Unit = {
        if (!hasShizukuPermission) {
            Toast.makeText(
                context,
                context.getString(R.string.boost_widget_needs_shizuku),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            isBoosting = true
            resultText = null
            coroutineScope.launch {
                val before = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
                withContext(Dispatchers.IO) {
                    DeveloperOptionsManager.clearAllAppCaches()
                    DeveloperOptionsManager.forceStopBackgroundApps(context)
                    MemoryBooster.boost()
                }
                delay(BOOST_SETTLE_DELAY_MS)
                val after = withContext(Dispatchers.IO) { BoostStats.snapshot(context) }
                val storageFreed =
                    (after.availableStorageBytes - before.availableStorageBytes).coerceAtLeast(0L)
                val ramFreed =
                    (after.availableRamBytes - before.availableRamBytes).coerceAtLeast(0L)
                availableRam = after.availableRamBytes
                availableStorage = after.availableStorageBytes
                isBoosting = false
                resultText = context.getString(
                    R.string.boost_widget_result,
                    BoostStats.formatSize(context, storageFreed),
                    BoostStats.formatSize(context, ramFreed)
                )
                maybeShowInterstitial(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.boost_widget_title),
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

            BoostSectionTitle(stringResource(R.string.boost_status_title))

            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BoostStatRow(
                        icon = Icons.Filled.Memory,
                        label = stringResource(
                            R.string.boost_available_ram,
                            BoostStats.formatSize(context, availableRam)
                        )
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    BoostStatRow(
                        icon = Icons.Filled.Storage,
                        label = stringResource(
                            R.string.boost_available_storage,
                            BoostStats.formatSize(context, availableStorage)
                        )
                    )
                }
            }

            BoostSectionTitle(stringResource(R.string.boost_section_quick))

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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.boost_widget_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = runBoost,
                        enabled = !isBoosting && hasShizukuPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isBoosting) R.string.boost_widget_running
                                else R.string.boost_widget_button
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    resultText?.let { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            BoostSectionTitle(stringResource(R.string.boost_section_compile))

            Card(
                shape = AppShapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.compile_booster),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.compile_booster_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.compile_filter_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                    CompileFilterManager.CompileFilter.entries.forEach { filter ->
                        CompileFilterOptionRow(
                            filter = filter,
                            selected = filter == compileFilter,
                            enabled = !compileProgress.isRunning,
                            onClick = {
                                compileFilter = filter
                                CompileFilterManager.setFilter(context, filter)
                            }
                        )
                    }

                    if (compileProgress.isRunning) {
                        LinearProgressIndicator(
                            progress = { compileProgress.percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.compile_booster_progress,
                                compileProgress.percent
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { CompileBoosterService.stop(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    } else {
                        Button(
                            onClick = startCompileAll,
                            enabled = hasShizukuPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.compile_all_apps))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BoostSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun BoostStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
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
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompileFilterOptionRow(
    filter: CompileFilterManager.CompileFilter,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(filter.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = stringResource(filter.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
