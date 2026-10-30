package com.arslan.customanimator

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.DividerSoft
import com.arslan.customanimator.ui.components.ExpandableCard
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.SettingRow
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.components.ToggleRow
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import com.arslan.customanimator.ui.theme.Motion
import com.arslan.customanimator.utils.LocalShizukuPermission
import com.arslan.customanimator.utils.OtpCopierManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslan.customanimator.ui.components.ScreenTopBar
import com.arslan.customanimator.utils.ShizukuHelper

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
    var isToastEnabled by remember { mutableStateOf(OtpCopierManager.isToastEnabled(context)) }
    var enableAfterAccess by remember { mutableStateOf(false) }
    var hasSensitiveAccess by remember { mutableStateOf<Boolean?>(null) }
    var keywords by remember { mutableStateOf(OtpCopierManager.getKeywords(context)) }
    var patterns by remember { mutableStateOf(OtpCopierManager.getPatterns(context)) }
    var excludes by remember { mutableStateOf(OtpCopierManager.getExcludes(context)) }

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
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.otp_copier),
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OtpScenarioCard() }

            item {
                OtpSwitchCard(
                    isEnabled = isEnabled && hasNotificationAccess,
                    hasNotificationAccess = hasNotificationAccess,
                    onEnabledChange = { newValue ->
                        if (newValue && !hasNotificationAccess) requestNotificationAccess() else setEnabled(newValue)
                    },
                    onRequestAccess = requestNotificationAccess,
                    isToastEnabled = isToastEnabled,
                    onToastEnabledChange = { newValue ->
                        isToastEnabled = newValue
                        OtpCopierManager.setToastEnabled(context, newValue)
                    }
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
                            }, shapes = ButtonDefaults.shapes()) {
                                Text(stringResource(R.string.reset), maxLines = 1)
                            }
                        }
                    }
                )
            }

            item {
                OtpWordsCard(
                    words = keywords,
                    emptyText = stringResource(R.string.otp_copier_keywords_empty),
                    inputLabel = stringResource(R.string.otp_copier_keyword_add),
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

            item {
                SectionHeader(
                    title = stringResource(R.string.otp_copier_excludes),
                    subtitle = stringResource(R.string.otp_copier_excludes_desc)
                )
            }

            item {
                OtpWordsCard(
                    words = excludes,
                    emptyText = stringResource(R.string.otp_copier_excludes_empty),
                    inputLabel = stringResource(R.string.otp_copier_exclude_add),
                    onAdd = {
                        OtpCopierManager.addExclude(context, it)
                        excludes = OtpCopierManager.getExcludes(context)
                    },
                    onRemove = {
                        OtpCopierManager.removeExclude(context, it)
                        excludes = OtpCopierManager.getExcludes(context)
                    }
                )
            }

            item { SectionHeader(title = stringResource(R.string.otp_copier_try)) }

            item { OtpTestCard(keywords = keywords, patterns = patterns, excludes = excludes) }

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

private const val SCENARIO_DURATION_MS = 4200

private fun scenarioPhase(progress: Float, start: Float, end: Float): Float =
    Motion.emphasizedEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))

private fun GraphicsLayerScope.scenarioVisibility(progress: Float, start: Float, end: Float) {
    val shown = scenarioPhase(progress, start, end)
    val hidden = scenarioPhase(progress, 0.88f, 1f)
    alpha = shown * (1f - hidden)
}

@Composable
private fun OtpScenarioCard() {
    val transition = rememberInfiniteTransition(label = "otpScenario")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SCENARIO_DURATION_MS, easing = LinearEasing)),
        label = "otpScenarioProgress"
    )
    val code = stringResource(R.string.otp_copier_demo_code)

    AppCard(contentPadding = 16.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OtpScenarioNotification(code = code, progress = { progress })
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    scenarioVisibility(progress, 0.42f, 0.55f)
                    translationY = (1f - scenarioPhase(progress, 0.42f, 0.55f)) * -8.dp.toPx()
                }
            )
            OtpScenarioClipboard(code = code, progress = { progress })
        }
    }
}

@Composable
private fun OtpScenarioNotification(code: String, progress: () -> Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scenarioVisibility(progress(), 0f, 0.14f)
                translationY = (1f - scenarioPhase(progress(), 0f, 0.14f)) * -16.dp.toPx()
            }
            .clip(AppShapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBadge(icon = Icons.Filled.Sms, size = 36.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OtpScenarioBar(fraction = 0.45f)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OtpScenarioBar(fraction = 0.3f)
                OtpScenarioCodeChip(code = code, progress = progress)
            }
        }
    }
}

@Composable
private fun OtpScenarioBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(8.dp)
            .clip(AppShapes.chip)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
    )
}

@Composable
private fun OtpScenarioCodeChip(code: String, progress: () -> Float) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val highlight = scenarioPhase(progress(), 0.22f, 0.36f)
                    alpha = highlight
                    scaleX = 0.8f + 0.2f * highlight
                    scaleY = 0.8f + 0.2f * highlight
                }
                .clip(AppShapes.chip)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Text(
            text = code,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun OtpScenarioClipboard(code: String, progress: () -> Float) {
    Row(
        modifier = Modifier
            .graphicsLayer {
                scenarioVisibility(progress(), 0.55f, 0.68f)
                val pop = 0.85f + 0.15f * scenarioPhase(progress(), 0.55f, 0.68f)
                scaleX = pop
                scaleY = pop
            }
            .clip(AppShapes.chip)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ContentPaste,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.otp_copier_copied, code),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    onRequestAccess: () -> Unit,
    isToastEnabled: Boolean,
    onToastEnabledChange: (Boolean) -> Unit
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
            Button(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.grant), maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        DividerSoft()
        ToggleRow(
            title = stringResource(R.string.otp_copier_toast),
            subtitle = stringResource(R.string.otp_copier_toast_desc),
            icon = Icons.Filled.Notifications,
            checked = isToastEnabled,
            onCheckedChange = onToastEnabledChange
        )
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
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
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
private fun OtpWordsCard(
    words: List<String>,
    emptyText: String,
    inputLabel: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    AppCard(contentPadding = 16.dp) {
        if (words.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            words.forEach { word ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(word) },
                    label = { Text(word) },
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
            label = inputLabel,
            errorText = null,
            isValid = { word -> words.none { it.equals(word.trim(), ignoreCase = true) } },
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
        IconButton(onClick = onRemove, shapes = IconButtonDefaults.shapes()) {
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
            IconButton(onClick = submit, enabled = canAdd, shapes = IconButtonDefaults.shapes()) {
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
private fun OtpTestCard(keywords: List<String>, patterns: List<String>, excludes: List<String>) {
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
            OtpTestResult(code = OtpCopierManager.extractCode(keywords, patterns, excludes, sample))
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
