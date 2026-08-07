package com.arslan.customanimator.notify.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.BannerAdView
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.NotificationRule
import com.arslan.customanimator.notify.data.RuleType
import com.arslan.customanimator.notify.data.RulesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesSection(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditRule: (String?) -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    var rules by remember { mutableStateOf(rulesManager.getRules()) }

    var ruleToDelete by remember { mutableStateOf<NotificationRule?>(null) }

    Scaffold(
        bottomBar = { BannerAdView() },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.pn_rules),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pn_cd_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.pn_cd_add)) },
                text = { Text(stringResource(R.string.pn_add_new_rule)) },
                onClick = { onNavigateToAddEditRule(null) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            if (rules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pn_no_rules_configured),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    NotificationRuleCard(
                        rule = rule,
                        modifier = Modifier.animateItem(),
                        onToggle = { isEnabled ->
                            rulesManager.toggleRule(rule.id, isEnabled)
                            rules = rulesManager.getRules()
                        },
                        onEdit = { onNavigateToAddEditRule(rule.id) },
                        onDelete = { ruleToDelete = rule }
                    )
                }
            }

        }
    }

    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = {
                Text(
                    stringResource(R.string.pn_delete_rule),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.pn_confirm_delete_rule)) },
            confirmButton = {
                Button(
                    onClick = {
                        rulesManager.removeRule(rule.id)
                        rules = rulesManager.getRules()
                        ruleToDelete = null
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}

@Composable
private fun NotificationRuleCard(
    rule: NotificationRule,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = rule.appNames.joinToString(", ")
                            .ifBlank { stringResource(R.string.pn_no_apps_chosen) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (rule.keywords.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.pn_keywords_format,
                                rule.keywords.joinToString(", ")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (rule.titleKeywords.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.pn_title_keywords_format,
                                rule.titleKeywords.joinToString(", ")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (rule.bodyKeywords.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.pn_body_keywords_format,
                                rule.bodyKeywords.joinToString(", ")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rule.actions.forEach { action ->
                            val label = when (action.type) {
                                RuleType.FLASH -> stringResource(R.string.pn_logs_filter_flash)
                                RuleType.WAKE_UP -> stringResource(R.string.pn_logs_filter_wake_up)
                                RuleType.AOD -> stringResource(R.string.pn_logs_filter_aod)
                                RuleType.FLASH_SCREEN -> stringResource(R.string.pn_flash_screen_title)
                                RuleType.WIDGET -> stringResource(R.string.pn_widget_action_title)
                            }
                            val color = when (action.type) {
                                RuleType.FLASH -> MaterialTheme.colorScheme.tertiary
                                RuleType.WAKE_UP -> MaterialTheme.colorScheme.secondary
                                RuleType.AOD -> MaterialTheme.colorScheme.primary
                                RuleType.FLASH_SCREEN -> MaterialTheme.colorScheme.error
                                RuleType.WIDGET -> MaterialTheme.colorScheme.primaryContainer
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = color.copy(alpha = 0.15f),
                                contentColor = color
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = onToggle
                    )
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.pn_cd_edit),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
