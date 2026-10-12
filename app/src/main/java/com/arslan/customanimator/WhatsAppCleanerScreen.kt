package com.arslan.customanimator

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.DividerSoft
import com.arslan.customanimator.ui.components.HeroCard
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.BoostStats
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.WhatsAppCleanerManager
import com.arslan.customanimator.utils.WhatsAppMediaCategory
import com.arslan.customanimator.utils.WhatsAppMediaTarget
import com.arslan.customanimator.utils.WhatsAppMediaUsage
import com.arslan.customanimator.utils.WhatsAppScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CATEGORY_ICONS = mapOf(
    WhatsAppMediaCategory.IMAGES to Icons.Filled.Image,
    WhatsAppMediaCategory.VIDEOS to Icons.Filled.Videocam,
    WhatsAppMediaCategory.DOCUMENTS to Icons.Filled.Description,
    WhatsAppMediaCategory.AUDIO to Icons.Filled.MusicNote,
    WhatsAppMediaCategory.GIFS to Icons.Filled.Gif,
    WhatsAppMediaCategory.VOICE_NOTES to Icons.Filled.Mic,
    WhatsAppMediaCategory.VIDEO_NOTES to Icons.Filled.Movie,
    WhatsAppMediaCategory.STATUSES to Icons.Filled.History,
    WhatsAppMediaCategory.STICKERS to Icons.Filled.EmojiEmotions,
    WhatsAppMediaCategory.WALLPAPERS to Icons.Filled.Wallpaper,
    WhatsAppMediaCategory.PROFILE_PHOTOS to Icons.Filled.AccountCircle,
    WhatsAppMediaCategory.LINK_PREVIEWS to Icons.Filled.Link,
    WhatsAppMediaCategory.SHARED to Icons.Filled.Share,
    WhatsAppMediaCategory.TRASH to Icons.Filled.DeleteSweep,
    WhatsAppMediaCategory.THUMBNAILS to Icons.Filled.PhotoLibrary,
    WhatsAppMediaCategory.OLD_BACKUPS to Icons.Filled.Backup
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppCleanerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val scope = rememberCoroutineScope()

    var scans by remember { mutableStateOf<Map<Int, WhatsAppScan>?>(null) }
    var minAgeDays by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(emptySet<WhatsAppMediaTarget>()) }
    var isCleaning by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val scan = scans?.get(minAgeDays)
    val categories = remember(scan) { scan?.let(::categoriesWithFiles).orEmpty() }

    LaunchedEffect(hasShizukuPermission) {
        if (!hasShizukuPermission) return@LaunchedEffect
        scans = withContext(Dispatchers.IO) { WhatsAppCleanerManager.scan() }
    }

    fun clean(before: WhatsAppScan) {
        val targets = selected
        isCleaning = true
        scope.launch {
            val deleted = withContext(Dispatchers.IO) { WhatsAppCleanerManager.delete(targets, minAgeDays) }
            val after = withContext(Dispatchers.IO) { WhatsAppCleanerManager.scan() }
            val remaining = after.getValue(minAgeDays).usageOf(targets).bytes
            val freed = (before.usageOf(targets).bytes - remaining).coerceAtLeast(0L)
            scans = after
            selected = emptySet()
            isCleaning = false
            if (deleted) {
                val message = resources.getString(R.string.whatsapp_cleaner_freed, BoostStats.formatSize(context, freed))
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                maybeShowInterstitial(context)
            } else {
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val selectedUsage = scan?.usageOf(selected) ?: WhatsAppMediaUsage.NONE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.whatsapp_cleaner),
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
        bottomBar = {
            if (selectedUsage.fileCount > 0) {
                DeleteButton(usage = selectedUsage, isCleaning = isCleaning, onClick = { showConfirm = true })
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
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }
            item {
                InfoCard(
                    dismissKey = "whatsapp_cleaner_info",
                    texts = listOf(stringResource(R.string.whatsapp_cleaner_info)),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (hasShizukuPermission) {
                scanResult(
                    scans = scans,
                    minAgeDays = minAgeDays,
                    categories = categories,
                    selected = selected,
                    enabled = !isCleaning,
                    onAgeChange = { days ->
                        minAgeDays = days
                        selected = selected.intersect(scans?.get(days)?.targetsWithFiles().orEmpty().toSet())
                    },
                    onSelectionChange = { selected = it }
                )
            }
        }
    }

    if (showConfirm && scan != null) {
        DeleteConfirmDialog(
            usage = selectedUsage,
            onConfirm = {
                showConfirm = false
                clean(scan)
            },
            onDismiss = { showConfirm = false }
        )
    }
}

private fun categoriesWithFiles(scan: WhatsAppScan): List<Pair<WhatsAppMediaCategory, List<WhatsAppMediaTarget>>> =
    scan.targetsWithFiles().groupBy { it.category }.toList()

private fun LazyListScope.scanResult(
    scans: Map<Int, WhatsAppScan>?,
    minAgeDays: Int,
    categories: List<Pair<WhatsAppMediaCategory, List<WhatsAppMediaTarget>>>,
    selected: Set<WhatsAppMediaTarget>,
    enabled: Boolean,
    onAgeChange: (Int) -> Unit,
    onSelectionChange: (Set<WhatsAppMediaTarget>) -> Unit
) {
    val scan = scans?.get(minAgeDays)
    if (scans == null || scan == null) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }
    if (!scan.isMediaFolderFound) {
        item { MessageCard(stringResource(R.string.whatsapp_cleaner_not_found)) }
        return
    }
    item(key = "summary") {
        SummaryCard(scans = scans, minAgeDays = minAgeDays, enabled = enabled, onAgeChange = onAgeChange)
    }
    if (categories.isEmpty()) {
        item { MessageCard(stringResource(R.string.whatsapp_cleaner_empty)) }
        return
    }
    val (media, other) = categories.partition { (category, _) -> category.isMedia }
    listOf(R.string.whatsapp_cleaner_media_types to media, R.string.whatsapp_cleaner_other_data to other)
        .filter { (_, section) -> section.isNotEmpty() }
        .forEach { (titleRes, section) ->
            item(key = titleRes) { SectionHeader(title = stringResource(titleRes)) }
            items(section, key = { (category, _) -> category.name }) { (category, targets) ->
                CategoryCard(
                    category = category,
                    targets = targets,
                    scan = scan,
                    selected = selected,
                    enabled = enabled,
                    onSelectionChange = onSelectionChange
                )
            }
        }
}

@Composable
private fun targetTitle(target: WhatsAppMediaTarget): String = listOfNotNull(
    target.part.takeIf { target.category.isSplit }?.let { stringResource(it.titleRes) },
    target.kind?.let { stringResource(it.titleRes) }
).joinToString(" · ")

@Composable
private fun usageText(usage: WhatsAppMediaUsage): String = stringResource(
    R.string.whatsapp_cleaner_usage,
    usage.fileCount,
    BoostStats.formatSize(LocalContext.current, usage.bytes)
)

@Composable
private fun MessageCard(message: String) {
    AppCard {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryCard(
    scans: Map<Int, WhatsAppScan>,
    minAgeDays: Int,
    enabled: Boolean,
    onAgeChange: (Int) -> Unit
) {
    HeroCard(title = usageText(scans.getValue(WhatsAppCleanerManager.AGE_OPTIONS_DAYS.first()).total)) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WhatsAppCleanerManager.AGE_OPTIONS_DAYS.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { days ->
                        AgeTile(
                            days = days,
                            usage = scans.getValue(days).total,
                            selected = days == minAgeDays,
                            enabled = enabled,
                            onClick = { onAgeChange(days) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgeTile(
    days: Int,
    usage: WhatsAppMediaUsage,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.surface.copy(alpha = 0.6f),
        label = "ageTile"
    )
    val contentColor = if (selected) scheme.onPrimary else scheme.onSurface

    Column(
        modifier = modifier
            .clip(AppShapes.iconBadge)
            .background(containerColor)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (days == 0) {
                stringResource(R.string.whatsapp_cleaner_age_any)
            } else {
                stringResource(R.string.whatsapp_cleaner_age_days, days)
            },
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.8f)
        )
        Text(
            text = BoostStats.formatSize(LocalContext.current, usage.bytes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun CategoryCard(
    category: WhatsAppMediaCategory,
    targets: List<WhatsAppMediaTarget>,
    scan: WhatsAppScan,
    selected: Set<WhatsAppMediaTarget>,
    enabled: Boolean,
    onSelectionChange: (Set<WhatsAppMediaTarget>) -> Unit
) {
    val selectedCount = targets.count { it in selected }
    val state = when (selectedCount) {
        0 -> ToggleableState.Off
        targets.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }

    AppCard(contentPadding = 16.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = CATEGORY_ICONS.getValue(category))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = usageText(scan.usageOf(targets)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TriStateCheckbox(
                state = state,
                enabled = enabled,
                onClick = {
                    onSelectionChange(if (state == ToggleableState.On) selected - targets.toSet() else selected + targets)
                }
            )
        }
        if (category.hasBreakdown) {
            DividerSoft(modifier = Modifier.padding(top = 8.dp))
            targets.forEach { target ->
                PartRow(
                    title = targetTitle(target),
                    usage = scan.usageOf(listOf(target)),
                    checked = target in selected,
                    enabled = enabled,
                    onCheckedChange = { checked ->
                        onSelectionChange(if (checked) selected + target else selected - target)
                    }
                )
            }
        }
    }
}

@Composable
private fun PartRow(
    title: String,
    usage: WhatsAppMediaUsage,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = usageText(usage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeleteButton(usage: WhatsAppMediaUsage, isCleaning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isCleaning,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 52.dp)
    ) {
        if (isCleaning) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                R.string.whatsapp_cleaner_delete,
                BoostStats.formatSize(LocalContext.current, usage.bytes)
            ),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun DeleteConfirmDialog(usage: WhatsAppMediaUsage, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whatsapp_cleaner_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.whatsapp_cleaner_confirm_message,
                    usage.fileCount,
                    BoostStats.formatSize(LocalContext.current, usage.bytes)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete))
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
