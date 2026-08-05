package com.arslan.customanimator.notify.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.IgnoreRule
import com.arslan.customanimator.notify.data.IgnoreType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredNotificationsSection(
    onNavigateBack: () -> Unit,
    viewModel: IgnoredNotificationsViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val iconCache by viewModel.iconCache.collectAsState()
    var confirmRemoveRule by remember { mutableStateOf<IgnoreRule?>(null) }
    var editingRule by remember { mutableStateOf<IgnoreRule?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pn_ignored_notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pn_cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.pn_ignored_notifications_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.pn_ignore_scope_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.pn_ignore_scope_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                items(rules, key = { it.id }) { rule ->
                    IgnoredRuleItem(
                        rule = rule,
                        icon = iconCache[rule.packageName],
                        onEditClick = { editingRule = rule },
                        onRestoreClick = { confirmRemoveRule = rule }
                    )
                }
            }
        }
    }

    editingRule?.let { rule ->
        EditIgnoreRuleDialog(
            rule = rule,
            onDismiss = { editingRule = null },
            onSave = { updatedRule ->
                viewModel.updateRule(updatedRule)
                editingRule = null
            }
        )
    }

    confirmRemoveRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmRemoveRule = null },
            title = {
                Text(
                    text = stringResource(R.string.pn_ignored_restore_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val desc = when {
                    rule.isRegex -> stringResource(R.string.pn_ignored_restore_desc_pattern, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.APP -> stringResource(R.string.pn_ignored_restore_desc_app, rule.appName?.ifBlank { null } ?: rule.packageName)
                    rule.type == IgnoreType.TITLE -> stringResource(R.string.pn_ignored_restore_desc_title, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.BODY -> stringResource(R.string.pn_ignored_restore_desc_body, rule.matchValue.orEmpty())
                    rule.type == IgnoreType.TITLE_AND_BODY -> stringResource(
                        R.string.pn_ignored_restore_desc_title_and_body,
                        rule.matchValue.orEmpty(),
                        rule.matchValue2.orEmpty()
                    )
                    else -> ""
                }
                Text(desc)
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeRule(rule.id)
                    confirmRemoveRule = null
                }) {
                    Text(stringResource(R.string.pn_ignored_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveRule = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun IgnoredRuleItem(
    rule: IgnoreRule,
    icon: ImageBitmap?,
    onEditClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val appDisplayName = rule.appName?.ifBlank { null } ?: rule.packageName

    val (typeLabel, badgeColor) = when {
        rule.isRegex || rule.isRegex2 -> Pair(
            stringResource(R.string.pn_ignore_dialog_regex),
            MaterialTheme.colorScheme.error
        )
        rule.type == IgnoreType.APP -> Pair(
            stringResource(R.string.pn_ignored_type_app),
            MaterialTheme.colorScheme.primary
        )
        rule.type == IgnoreType.TITLE -> Pair(
            stringResource(R.string.pn_ignored_type_title),
            MaterialTheme.colorScheme.secondary
        )
        rule.type == IgnoreType.BODY -> Pair(
            stringResource(R.string.pn_ignored_type_body),
            MaterialTheme.colorScheme.tertiary
        )
        rule.type == IgnoreType.TITLE_AND_BODY -> Pair(
            stringResource(R.string.pn_ignored_type_title_and_body),
            MaterialTheme.colorScheme.secondary
        )
        else -> Pair("", MaterialTheme.colorScheme.primary)
    }

    val titleMatch = when (rule.type) {
        IgnoreType.TITLE, IgnoreType.TITLE_AND_BODY -> rule.matchValue?.takeIf { it.isNotBlank() }
        else -> null
    }
    val bodyMatch = when (rule.type) {
        IgnoreType.BODY -> rule.matchValue?.takeIf { it.isNotBlank() }
        IgnoreType.TITLE_AND_BODY -> rule.matchValue2?.takeIf { it.isNotBlank() }
        else -> null
    }
    val hasContent = titleMatch != null || bodyMatch != null

    Card(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appDisplayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (rule.type) {
                            IgnoreType.APP -> stringResource(R.string.pn_ignored_subtitle_all)
                            IgnoreType.TITLE -> stringResource(R.string.pn_ignored_subtitle_title)
                            IgnoreType.BODY -> stringResource(R.string.pn_ignored_subtitle_body)
                            IgnoreType.TITLE_AND_BODY -> stringResource(R.string.pn_ignored_subtitle_title_and_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    contentColor = badgeColor
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.pn_ignored_edit_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRestoreClick,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.pn_ignored_restore_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (hasContent) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (titleMatch != null) {
                        MatchRow(
                            label = stringResource(R.string.pn_ignored_label_title),
                            value = titleMatch,
                            isRegex = rule.isRegex
                        )
                    }
                    if (bodyMatch != null) {
                        MatchRow(
                            label = stringResource(R.string.pn_ignored_label_body),
                            value = bodyMatch,
                            isRegex = rule.isRegex2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditIgnoreRuleDialog(
    rule: IgnoreRule,
    onDismiss: () -> Unit,
    onSave: (IgnoreRule) -> Unit
) {
    val appDisplayName = rule.appName?.ifBlank { null } ?: rule.packageName

    var matchValueText by remember { mutableStateOf(rule.matchValue.orEmpty()) }
    var matchValueIsRegex by remember { mutableStateOf(rule.isRegex) }
    var matchValue2Text by remember { mutableStateOf(rule.matchValue2.orEmpty()) }
    var matchValue2IsRegex by remember { mutableStateOf(rule.isRegex2) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pn_ignored_edit_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.pn_ignored_edit_app_scope, appDisplayName),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.pn_ignored_edit_app_scope_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                if (rule.type == IgnoreType.APP) {
                    Text(
                        text = stringResource(R.string.pn_ignored_edit_app_only_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                run {
                    if (rule.type == IgnoreType.TITLE || rule.type == IgnoreType.TITLE_AND_BODY || rule.type == IgnoreType.APP) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = matchValueText,
                                onValueChange = {
                                    matchValueText = it
                                    errorMessage = null
                                },
                                label = { Text(stringResource(R.string.pn_ignored_edit_match_title)) },
                                placeholder = {
                                    Text(stringResource(
                                        if (matchValueIsRegex) R.string.pn_ignore_dialog_hint_regex
                                        else R.string.pn_ignore_dialog_hint_exact
                                    ))
                                },
                                trailingIcon = {
                                    if (matchValueText.isNotEmpty()) {
                                        IconButton(onClick = { matchValueText = ""; errorMessage = null }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
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
                                        matchValueIsRegex = !matchValueIsRegex
                                        errorMessage = null
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.pn_ignore_dialog_regex),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = matchValueIsRegex,
                                    onCheckedChange = {
                                        matchValueIsRegex = it
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    if (rule.type == IgnoreType.BODY || rule.type == IgnoreType.TITLE_AND_BODY || rule.type == IgnoreType.APP) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = if (rule.type == IgnoreType.BODY) matchValueText else matchValue2Text,
                                onValueChange = { v ->
                                    if (rule.type == IgnoreType.BODY) matchValueText = v
                                    else matchValue2Text = v
                                    errorMessage = null
                                },
                                label = { Text(stringResource(R.string.pn_ignored_edit_match_body)) },
                                placeholder = {
                                    val isRegex = if (rule.type == IgnoreType.BODY) matchValueIsRegex else matchValue2IsRegex
                                    Text(stringResource(
                                        if (isRegex) R.string.pn_ignore_dialog_hint_regex
                                        else R.string.pn_ignore_dialog_hint_exact
                                    ))
                                },
                                trailingIcon = {
                                    val bodyVal = if (rule.type == IgnoreType.BODY) matchValueText else matchValue2Text
                                    if (bodyVal.isNotEmpty()) {
                                        IconButton(onClick = {
                                            if (rule.type == IgnoreType.BODY) matchValueText = ""
                                            else matchValue2Text = ""
                                            errorMessage = null
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
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
                                        if (rule.type == IgnoreType.BODY) matchValueIsRegex = !matchValueIsRegex
                                        else matchValue2IsRegex = !matchValue2IsRegex
                                        errorMessage = null
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.pn_ignore_dialog_regex),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = if (rule.type == IgnoreType.BODY) matchValueIsRegex else matchValue2IsRegex,
                                    onCheckedChange = { v ->
                                        if (rule.type == IgnoreType.BODY) matchValueIsRegex = v
                                        else matchValue2IsRegex = v
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
            }
        },
        confirmButton = {
            Button(onClick = {
                    val titleValue = matchValueText.trim()
                    val bodyValue = if (rule.type == IgnoreType.TITLE_AND_BODY || rule.type == IgnoreType.APP) matchValue2Text.trim() else ""
                    if ((rule.type == IgnoreType.TITLE || rule.type == IgnoreType.TITLE_AND_BODY) && titleValue.isBlank()) {
                        errorMessage = "Pattern cannot be empty."
                        return@Button
                    }
                    if ((rule.type == IgnoreType.BODY) && titleValue.isBlank()) {
                        errorMessage = "Pattern cannot be empty."
                        return@Button
                    }
                    if ((rule.type == IgnoreType.TITLE_AND_BODY) && bodyValue.isBlank()) {
                        errorMessage = "Pattern cannot be empty."
                        return@Button
                    }
                    if (matchValueIsRegex && titleValue.isNotBlank()) {
                        try { Regex(titleValue) } catch (e: Exception) {
                            errorMessage = "Invalid regex: ${e.message}"
                            return@Button
                        }
                    }
                    if (matchValue2IsRegex && bodyValue.isNotBlank()) {
                        try { Regex(bodyValue) } catch (e: Exception) {
                            errorMessage = "Invalid regex: ${e.message}"
                            return@Button
                        }
                    }
                    val updatedRule = when (rule.type) {
                        IgnoreType.APP -> when {
                            titleValue.isNotBlank() && bodyValue.isNotBlank() -> rule.copy(
                                type = IgnoreType.TITLE_AND_BODY,
                                matchValue = titleValue,
                                isRegex = matchValueIsRegex,
                                matchValue2 = bodyValue,
                                isRegex2 = matchValue2IsRegex
                            )
                            titleValue.isNotBlank() -> rule.copy(
                                type = IgnoreType.TITLE,
                                matchValue = titleValue,
                                isRegex = matchValueIsRegex,
                                matchValue2 = null,
                                isRegex2 = false
                            )
                            bodyValue.isNotBlank() -> rule.copy(
                                type = IgnoreType.BODY,
                                matchValue = bodyValue,
                                isRegex = matchValue2IsRegex,
                                matchValue2 = null,
                                isRegex2 = false
                            )
                            else -> rule
                        }
                        IgnoreType.TITLE -> rule.copy(
                            matchValue = titleValue,
                            isRegex = matchValueIsRegex
                        )
                        IgnoreType.BODY -> rule.copy(
                            matchValue = titleValue,
                            isRegex = matchValueIsRegex
                        )
                        IgnoreType.TITLE_AND_BODY -> rule.copy(
                            matchValue = titleValue,
                            isRegex = matchValueIsRegex,
                            matchValue2 = bodyValue,
                            isRegex2 = matchValue2IsRegex
                        )
                    }
                    onSave(updatedRule)
                }) {
                    Text(stringResource(R.string.save))
                }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun MatchRow(label: String, value: String, isRegex: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = if (isRegex) value else "\"$value\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
