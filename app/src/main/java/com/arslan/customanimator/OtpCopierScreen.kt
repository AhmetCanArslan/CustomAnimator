package com.arslan.customanimator

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.ExpandableCard
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.SettingRow
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.OtpCopierManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpCopierScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var hasNotificationAccess by remember { mutableStateOf(OtpCopierManager.hasNotificationAccess(context)) }
    var isEnabled by remember { mutableStateOf(OtpCopierManager.isEnabled(context)) }
    var enableAfterAccess by remember { mutableStateOf(false) }
    var hasSensitiveAccess by remember { mutableStateOf<Boolean?>(null) }
    var keywords by remember { mutableStateOf(OtpCopierManager.getKeywords(context)) }
    var patterns by remember { mutableStateOf(OtpCopierManager.getPatterns(context)) }

    val setEnabled: (Boolean) -> Unit = { newValue ->
        isEnabled = newValue
        OtpCopierManager.setEnabled(context, newValue)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = OtpCopierManager.hasNotificationAccess(context)
                if (enableAfterAccess && hasNotificationAccess) setEnabled(true)
                enableAfterAccess = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasShizukuPermission) {
        hasSensitiveAccess = withContext(Dispatchers.IO) { OtpCopierManager.hasSensitiveAccess(context) }
    }

    val requestNotificationAccess: () -> Unit = {
        enableAfterAccess = true
        openNotificationAccessSettings(context)
    }

    val grantSensitiveAccess: () -> Unit = {
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { OtpCopierManager.grantSensitiveAccess(context) }
            if (success) {
                hasSensitiveAccess = true
                maybeShowInterstitial(context)
            } else {
                Toast.makeText(context, resources.getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.otp_copier),
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OtpSwitchCard(
                    isEnabled = isEnabled && hasNotificationAccess,
                    hasNotificationAccess = hasNotificationAccess,
                    onEnabledChange = { newValue ->
                        if (newValue && !hasNotificationAccess) requestNotificationAccess() else setEnabled(newValue)
                    },
                    onRequestAccess = requestNotificationAccess
                )
            }

            if (OtpCopierManager.needsSensitiveAccess() && hasSensitiveAccess == false) {
                item {
                    OtpSensitiveAccessCard(
                        hasShizukuPermission = hasShizukuPermission,
                        onGrant = grantSensitiveAccess,
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.otp_copier_keywords),
                    subtitle = stringResource(R.string.otp_copier_keywords_desc),
                    trailing = {
                        if (keywords != OtpCopierManager.DEFAULT_KEYWORDS) {
                            TextButton(onClick = {
                                OtpCopierManager.resetKeywords(context)
                                keywords = OtpCopierManager.getKeywords(context)
                            }) {
                                Text(stringResource(R.string.reset), maxLines = 1)
                            }
                        }
                    }
                )
            }

            item {
                OtpKeywordsCard(
                    keywords = keywords,
                    onAdd = {
                        OtpCopierManager.addKeyword(context, it)
                        keywords = OtpCopierManager.getKeywords(context)
                    },
                    onRemove = {
                        OtpCopierManager.removeKeyword(context, it)
                        keywords = OtpCopierManager.getKeywords(context)
                    }
                )
            }

            item { SectionHeader(title = stringResource(R.string.otp_copier_try)) }

            item { OtpTestCard(keywords = keywords, patterns = patterns) }

            item {
                OtpPatternsCard(
                    patterns = patterns,
                    onAdd = {
                        OtpCopierManager.addPattern(context, it)
                        patterns = OtpCopierManager.getPatterns(context)
                    },
                    onRemove = {
                        OtpCopierManager.removePattern(context, it)
                        patterns = OtpCopierManager.getPatterns(context)
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.otp_copier_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

private fun openNotificationAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Composable
private fun OtpSwitchCard(
    isEnabled: Boolean,
    hasNotificationAccess: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRequestAccess: () -> Unit
) {
    AppCard(contentPadding = 16.dp, highlighted = isEnabled) {
        ToggleRow(
            title = stringResource(R.string.otp_copier_enable),
            subtitle = stringResource(R.string.otp_copier_enable_desc),
            icon = Icons.Filled.Password,
            checked = isEnabled,
            onCheckedChange = onEnabledChange
        )
        if (!hasNotificationAccess) {
            Text(
                text = stringResource(R.string.otp_copier_needs_access),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant), maxLines = 1)
            }
        }
    }
}

@Composable
private fun OtpSensitiveAccessCard(
    hasShizukuPermission: Boolean,
    onGrant: () -> Unit,
    onOpenSetup: () -> Unit
) {
    val extended = LocalExtendedColors.current
    AppCard(contentPadding = 16.dp) {
        SettingRow(
            title = stringResource(R.string.otp_copier_sensitive),
            subtitle = stringResource(R.string.otp_copier_sensitive_desc),
            icon = Icons.Filled.VisibilityOff,
            iconContainerColor = extended.warningContainer,
            iconContentColor = extended.onWarningContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (hasShizukuPermission) {
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant), maxLines = 1)
            }
        } else {
            SetupNudgeCard(
                message = stringResource(R.string.otp_copier_sensitive_needs_shizuku),
                onOpenSetup = onOpenSetup
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OtpKeywordsCard(
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    AppCard(contentPadding = 16.dp) {
        if (keywords.isEmpty()) {
            Text(
                text = stringResource(R.string.otp_copier_keywords_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keywords.forEach { keyword ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(keyword) },
                    label = { Text(keyword) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(InputChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OtpAddField(
            label = stringResource(R.string.otp_copier_keyword_add),
            errorText = null,
            isValid = { word -> keywords.none { it.equals(word.trim(), ignoreCase = true) } },
            onAdd = onAdd
        )
    }
}

@Composable
private fun OtpPatternsCard(
    patterns: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExpandableCard(
        title = stringResource(R.string.otp_copier_patterns),
        subtitle = stringResource(R.string.otp_copier_patterns_desc),
        icon = Icons.Filled.Code,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        trailingPill = patterns.size.takeIf { it > 0 }?.toString(),
        pillTone = StatusTone.ACTIVE
    ) {
        patterns.forEach { pattern ->
            OtpPatternRow(pattern = pattern, onRemove = { onRemove(pattern) })
        }
        OtpAddField(
            label = stringResource(R.string.otp_copier_pattern_add),
            errorText = stringResource(R.string.otp_copier_pattern_invalid),
            isValid = { OtpCopierManager.isValidPattern(it) && it.trim() !in patterns },
            onAdd = onAdd
        )
    }
}

@Composable
private fun OtpPatternRow(pattern: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pattern,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.delete)
            )
        }
    }
}

@Composable
private fun OtpAddField(
    label: String,
    errorText: String?,
    isValid: (String) -> Boolean,
    onAdd: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val canAdd = input.isNotBlank() && isValid(input)
    val showError = input.isNotBlank() && !canAdd
    val submit: () -> Unit = {
        if (canAdd) {
            onAdd(input)
            input = ""
        }
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text(label) },
        isError = showError,
        supportingText = if (showError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = submit, enabled = canAdd) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.otp_copier_add)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OtpTestCard(keywords: List<String>, patterns: List<String>) {
    var sample by remember { mutableStateOf("") }

    AppCard(contentPadding = 16.dp) {
        OutlinedTextField(
            value = sample,
            onValueChange = { sample = it },
            label = { Text(stringResource(R.string.otp_copier_test)) },
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        if (sample.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            OtpTestResult(code = OtpCopierManager.extractCode(keywords, patterns, sample))
        }
    }
}

@Composable
private fun OtpTestResult(code: String?) {
    if (code != null) {
        StatusPill(text = stringResource(R.string.otp_copier_test_result, code), tone = StatusTone.ACTIVE)
    } else {
        StatusPill(text = stringResource(R.string.otp_copier_test_none), tone = StatusTone.NEUTRAL)
    }
}
