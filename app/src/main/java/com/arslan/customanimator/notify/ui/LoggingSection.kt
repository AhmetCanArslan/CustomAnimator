package com.arslan.customanimator.notify.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arslan.customanimator.BannerAdView
import com.arslan.customanimator.maybeShowInterstitial
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.IgnoreType
import com.arslan.customanimator.notify.data.LogEntry
import com.arslan.customanimator.notify.data.MatchedRuleInfo
import com.arslan.customanimator.notify.data.RuleType
import com.arslan.customanimator.notify.data.RulePrefillData
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggingSection(
    onNavigateBack: () -> Unit,
    onNavigateToIgnored: () -> Unit = {},
    onNavigateToAddEditRule: () -> Unit = {},
    viewModel: LoggingViewModel = viewModel()
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val iconCache by viewModel.iconCache.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyRuleMatched by viewModel.onlyRuleMatched.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()
    val focusManager = LocalFocusManager.current

    var showClearDialog by remember { mutableStateOf(false) }
    var ignoreTarget by remember { mutableStateOf<LogEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<LogEntry?>(null) }
    var createRuleTarget by remember { mutableStateOf<LogEntry?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BannerAdView() },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pn_logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pn_cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.pn_logs_search_cd)
                        )
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.pn_cd_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pn_logs_settings)) },
                                onClick = {
                                    showOverflowMenu = false
                                    showSettingsDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pn_ignored_notifications_title)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToIgnored()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.pn_logs_search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.pn_cd_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text(stringResource(R.string.pn_logs_filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.FLASH,
                        onClick = { viewModel.setFilter(RuleType.FLASH) },
                        label = { Text(stringResource(R.string.pn_logs_filter_flash)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.WAKE_UP,
                        onClick = { viewModel.setFilter(RuleType.WAKE_UP) },
                        label = { Text(stringResource(R.string.pn_logs_filter_wake_up)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.AOD,
                        onClick = { viewModel.setFilter(RuleType.AOD) },
                        label = { Text(stringResource(R.string.pn_logs_filter_aod)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == RuleType.FLASH_SCREEN,
                        onClick = { viewModel.setFilter(RuleType.FLASH_SCREEN) },
                        label = { Text(stringResource(R.string.pn_flash_screen_title)) }
                    )
                }
            }

            if (logs.isEmpty()) {
                EmptyLogsState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(
                        items = logs,
                        key = { it.id },
                        contentType = { "log_entry" }
                    ) { entry ->
                        LogItem(
                            entry = entry,
                            icon = iconCache[entry.packageName],
                            onIgnoreClick = { ignoreTarget = entry },
                            onDeleteClick = { deleteTarget = entry },
                            onCreateRuleClick = { createRuleTarget = entry },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.pn_logs_clear_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.pn_logs_clear_confirm_body, logs.size))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLogs()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.pn_logs_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.pn_logs_delete_confirm_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLog(entry.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ignoreTarget?.let { entry ->
        IgnoreDialog(
            entry = entry,
            onDismiss = { ignoreTarget = null },
            onSave = { titlePattern, titleIsRegex, bodyPattern, bodyIsRegex ->
                viewModel.ignoreFromDialog(
                    entry = entry,
                    titlePattern = titlePattern,
                    titleIsRegex = titleIsRegex,
                    bodyPattern = bodyPattern,
                    bodyIsRegex = bodyIsRegex
                )
                maybeShowInterstitial(context)
                ignoreTarget = null
            }
        )
    }

    createRuleTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { createRuleTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.pn_logs_create_rule_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.pn_logs_create_rule_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pn_logs_create_rule_app, entry.appName),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (entry.title.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.pn_logs_create_rule_header, entry.title),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (entry.body.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.pn_logs_create_rule_description, entry.body),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    RulePrefillData.set(
                        packageName = entry.packageName,
                        appName = entry.appName,
                        titleKeyword = entry.title.takeIf { it.isNotBlank() },
                        bodyKeyword = entry.body.takeIf { it.isNotBlank() }
                    )
                    createRuleTarget = null
                    onNavigateToAddEditRule()
                }) {
                    Text(stringResource(R.string.pn_create_rule_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { createRuleTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettingsDialog) {
        LogSettingsDialog(
            autoDeleteDays = autoDeleteDays,
            onlyRuleMatched = onlyRuleMatched,
            showSystemApps = showSystemApps,
            onAutoDeleteDaysChanged = { viewModel.setAutoDeleteDays(it) },
            onOnlyRuleMatchedChanged = { viewModel.setOnlyRuleMatched(it) },
            onShowSystemAppsChanged = { viewModel.setShowSystemApps(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun LogSettingsDialog(
    autoDeleteDays: Int,
    onlyRuleMatched: Boolean,
    showSystemApps: Boolean,
    onAutoDeleteDaysChanged: (Int) -> Unit,
    onOnlyRuleMatchedChanged: (Boolean) -> Unit,
    onShowSystemAppsChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val deleteOptions = listOf(
        0 to stringResource(R.string.pn_logs_auto_delete_never),
        1 to stringResource(R.string.pn_logs_auto_delete_1d),
        3 to stringResource(R.string.pn_logs_auto_delete_3d),
        7 to stringResource(R.string.pn_logs_auto_delete_7d),
        30 to stringResource(R.string.pn_logs_auto_delete_30d)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pn_logs_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.pn_logs_auto_delete_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    deleteOptions.forEach { (days, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = autoDeleteDays == days,
                                onClick = { onAutoDeleteDaysChanged(days) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pn_logs_only_rule_matched),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.pn_logs_only_rule_matched_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = onlyRuleMatched,
                        onCheckedChange = onOnlyRuleMatchedChanged
                    )
                }

                AnimatedVisibility(
                    visible = !onlyRuleMatched,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pn_logs_show_system_apps),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.pn_logs_show_system_apps_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = showSystemApps,
                            onCheckedChange = onShowSystemAppsChanged
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pn_logs_settings_done))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogItem(
    entry: LogEntry,
    icon: ImageBitmap?,
    onIgnoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCreateRuleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(context, entry.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCreateRuleClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = stringResource(R.string.pn_logs_create_rule_cd),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(
                    onClick = onIgnoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = stringResource(R.string.pn_ignore_dialog_title),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.body.isNotBlank()) {
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (entry.matchedRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.matchedRules.forEach { rule ->
                        RuleBadge(rule = rule)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleBadge(rule: MatchedRuleInfo) {
    val (typeLabel, typeColor) = when (rule.ruleType) {
        RuleType.FLASH -> Pair(stringResource(R.string.pn_logs_filter_flash), MaterialTheme.colorScheme.tertiary)
        RuleType.WAKE_UP -> Pair(stringResource(R.string.pn_logs_filter_wake_up), MaterialTheme.colorScheme.secondary)
        RuleType.AOD -> Pair(stringResource(R.string.pn_logs_filter_aod), MaterialTheme.colorScheme.primary)
        RuleType.FLASH_SCREEN -> Pair(stringResource(R.string.pn_flash_screen_title), MaterialTheme.colorScheme.error)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = typeColor.copy(alpha = 0.12f),
        contentColor = typeColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = rule.ruleName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = if (rule.wasExecuted) Icons.Default.CheckCircle else Icons.Default.Clear,
                contentDescription = if (rule.wasExecuted) {
                    stringResource(R.string.pn_log_rule_executed)
                } else {
                    stringResource(R.string.pn_log_rule_blocked)
                },
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun EmptyLogsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.pn_logs_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private fun formatTimestamp(context: Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMin = diffMs / 60_000
    val diffHours = diffMs / 3_600_000

    return when {
        diffMin < 1 -> "Just now"
        diffMin < 60 -> "$diffMin min ago"
        else -> {
            val timeFmt = cachedTimeFormat(context)
            val then = Calendar.getInstance().apply { timeInMillis = timestamp }
            val nowCal = Calendar.getInstance()
            when {
                diffHours < 24 && nowCal.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) ->
                    timeFmt.format(Date(timestamp))
                diffHours < 48 -> "Yesterday " + timeFmt.format(Date(timestamp))
                else -> cachedDateFormat().format(Date(timestamp)) +
                    " " + timeFmt.format(Date(timestamp))
            }
        }
    }
}

private var _cachedTimeFormat: java.text.DateFormat? = null
private var _cachedDateFormat: SimpleDateFormat? = null

private fun cachedTimeFormat(context: Context): java.text.DateFormat =
    _cachedTimeFormat ?: android.text.format.DateFormat.getTimeFormat(context).also { _cachedTimeFormat = it }

private fun cachedDateFormat(): SimpleDateFormat =
    _cachedDateFormat ?: SimpleDateFormat("MMM d", Locale.getDefault()).also { _cachedDateFormat = it }

private fun suggestRegex(text: String): String =
    text.replace(Regex("\\d+"), "\\\\d+")

@Composable
private fun IgnoreDialog(
    entry: LogEntry,
    onDismiss: () -> Unit,
    onSave: (
        titlePattern: String?,
        titleIsRegex: Boolean,
        bodyPattern: String?,
        bodyIsRegex: Boolean
    ) -> Unit
) {
    val hasTitle = entry.title.isNotBlank()
    val hasBody = entry.body.isNotBlank()

    val initialTitle = if (hasTitle) entry.title else ""
    val initialBody = if (hasTitle) "" else entry.body

    var titleText by remember { mutableStateOf(initialTitle) }
    var titleIsRegex by remember { mutableStateOf(false) }
    var bodyText by remember { mutableStateOf(initialBody) }
    var bodyIsRegex by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pn_ignore_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.appName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hasTitle) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasBody) {
                            Text(
                                text = entry.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.pn_ignore_dialog_scope_label, entry.appName),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.pn_ignore_dialog_scope_hint, entry.appName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.pn_ignore_scope_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.pn_ignore_dialog_content_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.pn_ignore_dialog_content_hint, entry.appName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasTitle) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = {
                                titleText = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(R.string.pn_ignore_dialog_match_title)) },
                            placeholder = {
                                Text(stringResource(
                                    if (titleIsRegex) R.string.pn_ignore_dialog_hint_regex
                                    else R.string.pn_ignore_dialog_hint_exact
                                ))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            singleLine = false,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    titleIsRegex = !titleIsRegex
                                    titleText = if (titleIsRegex) suggestRegex(entry.title) else initialTitle
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pn_ignore_dialog_regex),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = titleIsRegex,
                                onCheckedChange = { checked ->
                                    titleIsRegex = checked
                                    titleText = if (checked) suggestRegex(entry.title) else initialTitle
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                if (hasBody) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = bodyText,
                            onValueChange = {
                                bodyText = it
                                errorMessage = null
                            },
                            label = { Text(stringResource(R.string.pn_ignore_dialog_match_body)) },
                            placeholder = {
                                Text(stringResource(
                                    if (bodyIsRegex) R.string.pn_ignore_dialog_hint_regex
                                    else R.string.pn_ignore_dialog_hint_exact
                                ))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            singleLine = false,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bodyIsRegex = !bodyIsRegex
                                    bodyText = if (bodyIsRegex) suggestRegex(entry.body) else initialBody
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pn_ignore_dialog_regex),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = bodyIsRegex,
                                onCheckedChange = { checked ->
                                    bodyIsRegex = checked
                                    bodyText = if (checked) suggestRegex(entry.body) else initialBody
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val useTitleFilter = hasTitle && titleText.isNotBlank()
                val useBodyFilter = hasBody && bodyText.isNotBlank()
                if (useTitleFilter && titleIsRegex) {
                    try { Regex(titleText.trim()) } catch (e: Exception) {
                        errorMessage = "Invalid title regex: ${e.message}"
                        return@Button
                    }
                }
                if (useBodyFilter && bodyIsRegex) {
                    try { Regex(bodyText.trim()) } catch (e: Exception) {
                        errorMessage = "Invalid body regex: ${e.message}"
                        return@Button
                    }
                }
                onSave(
                    if (useTitleFilter) titleText.trim() else null,
                    titleIsRegex,
                    if (useBodyFilter) bodyText.trim() else null,
                    bodyIsRegex
                )
            }) {
                Text(stringResource(R.string.pn_ignore_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
