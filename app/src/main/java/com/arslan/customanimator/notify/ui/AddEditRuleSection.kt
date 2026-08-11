package com.arslan.customanimator.notify.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Warning
import com.arslan.customanimator.BannerAdView
import com.arslan.customanimator.maybeShowInterstitial
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRuleSection(
    ruleId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePattern: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val rulesManager = remember { RulesManager(context) }
    val ignoreManager = remember { IgnoreManager(context) }
    val ignoreRules = remember { ignoreManager.getRules() }
    val existingRules = remember { rulesManager.getRules() }
    val hasProximitySensor = remember { rulesManager.hasProximitySensor() }

    val draftKey = ruleId?.takeIf { it != "new" } ?: "new"
    val draft = remember { AddEditRuleDraft.load(context, draftKey) }

    val prefill = remember { if (ruleId == null || ruleId == "new") RulePrefillData.consume() else null }

    val initialRule = remember(ruleId) {
        if (ruleId != null && ruleId != "new") {
            rulesManager.getRules().find { it.id == ruleId }
        } else null
    }

    val initialFlashAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.FLASH }
    }
    val initialWakeUpAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.WAKE_UP }
    }
    val initialAodAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.AOD }
    }
    val initialScreenFlashAction = remember(initialRule) {
        initialRule?.actions?.firstOrNull { it.type == RuleType.FLASH_SCREEN }
    }

    LaunchedEffect(Unit) {
        AppListManager.refresh(context)
    }

    val installedApps by AppListManager.installedApps.collectAsState()
    var selectedApps by remember(installedApps) {
        mutableStateOf(
            when {
                draft != null -> installedApps.filter { it.packageName in draft.selectedPackageNames }
                initialRule != null -> installedApps.filter { initialRule.packageNames.contains(it.packageName) }
                prefill != null -> installedApps.filter { it.packageName == prefill.packageName }
                else -> emptyList()
            }
        )
    }

    var titleKeywords by remember {
        mutableStateOf(
            draft?.titleKeywords
                ?: initialRule?.titleKeywords?.ifEmpty { initialRule.keywords }
                ?: prefill?.titleKeyword?.let { listOf(it) }
                ?: emptyList()
        )
    }
    var currentTitleKeyword by remember { mutableStateOf(draft?.currentTitleKeyword ?: "") }
    var bodyKeywords by remember {
        mutableStateOf(
            draft?.bodyKeywords
                ?: initialRule?.bodyKeywords
                ?: prefill?.bodyKeyword?.let { listOf(it) }
                ?: emptyList()
        )
    }
    var currentBodyKeyword by remember { mutableStateOf(draft?.currentBodyKeyword ?: "") }

    var customPatterns by remember { mutableStateOf(rulesManager.getCustomPatterns()) }
    var patternToDelete by remember { mutableStateOf<com.arslan.customanimator.notify.data.CustomPattern?>(null) }
    var flashEnabled by remember { mutableStateOf(draft?.flashEnabled ?: (initialFlashAction != null)) }
    var flashPattern by remember {
        mutableStateOf(
            draft?.flashPattern?.let { runCatching { FlashPattern.valueOf(it) }.getOrNull() }
                ?: initialFlashAction?.flashPattern
                ?: FlashPattern.HEARTBEAT
        )
    }
    var flashCustomPatternId by remember {
        mutableStateOf(draft?.flashCustomPatternId ?: initialFlashAction?.customPatternId)
    }
    var expandedFlashPatterns by remember { mutableStateOf(false) }

    var wakeUpEnabled by remember { mutableStateOf(draft?.wakeUpEnabled ?: (initialWakeUpAction != null)) }
    var screenDurationSeconds by remember {
        mutableIntStateOf(draft?.screenDurationSeconds ?: initialWakeUpAction?.screenDurationSeconds ?: 10)
    }
    var pocketModeEnabled by remember {
        mutableStateOf(draft?.pocketModeEnabled ?: initialWakeUpAction?.pocketModeEnabled ?: true)
    }
    var expandedWakeUpDuration by remember { mutableStateOf(false) }

    var aodEnabled by remember { mutableStateOf(draft?.aodEnabled ?: (initialAodAction != null)) }
    var aodDurationSeconds by remember {
        mutableIntStateOf(draft?.aodDurationSeconds ?: initialAodAction?.aodDurationSeconds ?: 10)
    }
    var expandedAodDuration by remember { mutableStateOf(false) }

    var screenFlashEnabled by remember {
        mutableStateOf(draft?.screenFlashEnabled ?: (initialScreenFlashAction != null))
    }
    var screenFlashColor by remember {
        mutableStateOf(
            draft?.screenFlashColor?.let { runCatching { ScreenFlashColor.valueOf(it) }.getOrNull() }
                ?: initialScreenFlashAction?.screenFlashColor?.let { runCatching { ScreenFlashColor.valueOf(it) }.getOrNull() }
                ?: ScreenFlashColor.RED
        )
    }
    var screenFlashDurationSeconds by remember {
        mutableIntStateOf(draft?.screenFlashDurationSeconds ?: initialScreenFlashAction?.screenFlashDurationSeconds ?: 5)
    }
    var expandedScreenFlashDuration by remember { mutableStateOf(false) }

    var applyOnVibration by remember {
        mutableStateOf(draft?.applyOnVibration ?: initialRule?.applyOnVibration ?: true)
    }
    var applyOnSilent by remember {
        mutableStateOf(draft?.applyOnSilent ?: initialRule?.applyOnSilent ?: true)
    }
    var applyOnDND by remember {
        mutableStateOf(draft?.applyOnDND ?: initialRule?.applyOnDND ?: true)
    }
    var preventMultipleNotifications by remember {
        mutableStateOf(draft?.preventMultipleNotifications ?: initialRule?.preventMultipleNotifications ?: false)
    }

    val wakeUpDurationOptions = listOf(0, 5, 10, 15, 30, 60)
    val aodDurationOptions = listOf(-1, -2, 5, 10, 15, 30, 60, 120, 300)
    val screenFlashDurationOptions = listOf(5, 10, 30, 60)

    val atLeastOneAction = flashEnabled || wakeUpEnabled || aodEnabled || screenFlashEnabled

    val ignoreConflicts by remember(selectedApps, titleKeywords, bodyKeywords) {
        derivedStateOf {
            val conflicts = mutableListOf<String>()
            for (app in selectedApps) {
                val pkg = app.packageName
                val appName = app.name
                val appRules = ignoreRules.filter { it.packageName == pkg }
                if (appRules.isEmpty()) continue
                if (appRules.any { it.type == IgnoreType.APP }) {
                    conflicts.add("🔕 All \"$appName\" notifications are muted")
                    continue
                }
                for (kw in titleKeywords) {
                    val hit = appRules.any { rule ->
                        (rule.type == IgnoreType.TITLE || rule.type == IgnoreType.TITLE_AND_BODY) &&
                            !rule.matchValue.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue, rule.isRegex)
                    }
                    if (hit) conflicts.add("🔕 Title keyword \"$kw\" in \"$appName\" is muted")
                }
                for (kw in bodyKeywords) {
                    val hit = appRules.any { rule ->
                        (rule.type == IgnoreType.BODY &&
                            !rule.matchValue.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue, rule.isRegex)) ||
                        (rule.type == IgnoreType.TITLE_AND_BODY &&
                            !rule.matchValue2.isNullOrBlank() &&
                            ignorePhraseMatches(kw, rule.matchValue2, rule.isRegex2))
                    }
                    if (hit) conflicts.add("🔕 Body keyword \"$kw\" in \"$appName\" is muted")
                }
            }
            conflicts
        }
    }

    val ruleConflicts by remember(selectedApps, titleKeywords, bodyKeywords) {
        derivedStateOf {
            val conflicts = mutableListOf<String>()
            val selectedPkgs = selectedApps.map { it.packageName }.toSet()

            for (existingRule in existingRules) {
                if (existingRule.id == initialRule?.id) continue
                if (!existingRule.isEnabled) continue

                val sharedPkgs = existingRule.packageNames.intersect(selectedPkgs)
                if (sharedPkgs.isEmpty()) continue

                val appLabel = selectedApps
                    .filter { it.packageName in sharedPkgs }
                    .joinToString(", ") { it.name }

                val existingTitleKws = (existingRule.keywords + existingRule.titleKeywords)
                val existingBodyKws = existingRule.bodyKeywords
                val existingHasNoKeywords = existingTitleKws.isEmpty() && existingBodyKws.isEmpty()
                val newHasNoKeywords = titleKeywords.isEmpty() && bodyKeywords.isEmpty()

                when {
                    existingHasNoKeywords && newHasNoKeywords -> {
                        conflicts.add("DUPLICATE_RULE|$appLabel")
                    }
                    existingHasNoKeywords -> {
                        conflicts.add("EXISTING_ALL|$appLabel")
                    }
                    newHasNoKeywords -> {
                        conflicts.add("NEW_ALL|$appLabel")
                    }
                    else -> {
                        for (kw in titleKeywords) {
                            if (existingTitleKws.any { ekw ->
                                    kw.lowercase().contains(ekw.lowercase()) ||
                                        ekw.lowercase().contains(kw.lowercase())
                                }) {
                                conflicts.add("TITLE_OVERLAP|$kw|$appLabel")
                            }
                        }
                        for (kw in bodyKeywords) {
                            if (existingBodyKws.any { ekw ->
                                    kw.lowercase().contains(ekw.lowercase()) ||
                                        ekw.lowercase().contains(kw.lowercase())
                                }) {
                                conflicts.add("BODY_OVERLAP|$kw|$appLabel")
                            }
                        }
                    }
                }
            }
            conflicts.distinct()
        }
    }

    var latestDraft by remember { mutableStateOf<AddEditRuleDraft.Draft?>(null) }
    SideEffect {
        latestDraft = AddEditRuleDraft.Draft(
            ruleId = draftKey,
            selectedPackageNames = selectedApps.map { it.packageName },
            titleKeywords = titleKeywords,
            currentTitleKeyword = currentTitleKeyword,
            bodyKeywords = bodyKeywords,
            currentBodyKeyword = currentBodyKeyword,
            flashEnabled = flashEnabled,
            flashPattern = flashPattern.name,
            flashCustomPatternId = flashCustomPatternId,
            wakeUpEnabled = wakeUpEnabled,
            screenDurationSeconds = screenDurationSeconds,
            pocketModeEnabled = pocketModeEnabled,
            aodEnabled = aodEnabled,
            aodDurationSeconds = aodDurationSeconds,
            screenFlashEnabled = screenFlashEnabled,
            screenFlashColor = screenFlashColor.name,
            screenFlashDurationSeconds = screenFlashDurationSeconds,
            applyOnVibration = applyOnVibration,
            applyOnSilent = applyOnSilent,
            applyOnDND = applyOnDND,
            preventMultipleNotifications = preventMultipleNotifications,
        )
    }
    val shouldDiscardDraft = remember { booleanArrayOf(false) }

    BackHandler {
        shouldDiscardDraft[0] = true
        AddEditRuleDraft.clear(context, draftKey)
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!shouldDiscardDraft[0]) {
                latestDraft?.let { AddEditRuleDraft.save(context, it) }
            }
        }
    }

    val consumeAllScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom) {
        scrollState.animateScrollTo(scrollState.value + imeBottom)
    }

    Scaffold(
        bottomBar = { BannerAdView() },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialRule != null) stringResource(R.string.pn_edit_rule_title)
                        else stringResource(R.string.pn_create_rule_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        shouldDiscardDraft[0] = true
                        AddEditRuleDraft.clear(context, draftKey)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.pn_cd_back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (selectedApps.isNotEmpty() && atLeastOneAction) {
                                val pendingTitle = currentTitleKeyword.trim()
                                val finalTitleKeywords = if (pendingTitle.isNotBlank() && !titleKeywords.contains(pendingTitle))
                                    titleKeywords + pendingTitle else titleKeywords
                                val pendingBody = currentBodyKeyword.trim()
                                val finalBodyKeywords = if (pendingBody.isNotBlank() && !bodyKeywords.contains(pendingBody))
                                    bodyKeywords + pendingBody else bodyKeywords

                                val actions = buildList {
                                    if (flashEnabled)
                                        add(RuleAction.flash(flashPattern, flashCustomPatternId))
                                    if (wakeUpEnabled)
                                        add(RuleAction.wakeUp(
                                            screenDurationSeconds,
                                            if (hasProximitySensor) pocketModeEnabled else false
                                        ))
                                    if (aodEnabled)
                                        add(RuleAction.aod(aodDurationSeconds))
                                    if (screenFlashEnabled)
                                        add(RuleAction.flashScreen(screenFlashColor, screenFlashDurationSeconds))
                                }
                                val newRule = initialRule?.copy(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = emptyList(),
                                    titleKeywords = finalTitleKeywords,
                                    bodyKeywords = finalBodyKeywords,
                                    actions = actions,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND,
                                    preventMultipleNotifications = preventMultipleNotifications
                                ) ?: NotificationRule(
                                    packageNames = selectedApps.map { it.packageName },
                                    appNames = selectedApps.map { it.name },
                                    keywords = emptyList(),
                                    titleKeywords = finalTitleKeywords,
                                    bodyKeywords = finalBodyKeywords,
                                    actions = actions,
                                    applyOnVibration = applyOnVibration,
                                    applyOnSilent = applyOnSilent,
                                    applyOnDND = applyOnDND,
                                    preventMultipleNotifications = preventMultipleNotifications
                                )
                                if (initialRule != null) {
                                    rulesManager.updateRule(newRule)
                                } else {
                                    rulesManager.addRule(newRule)
                                }
                                shouldDiscardDraft[0] = true
                                AddEditRuleDraft.clear(context, draftKey)
                                maybeShowInterstitial(context)
                                onNavigateBack()
                            }
                        },
                        enabled = selectedApps.isNotEmpty() && atLeastOneAction,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.pn_trigger_keywords),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            stringResource(R.string.pn_trigger_title_keywords),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AnimatedVisibility(
                            visible = titleKeywords.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(titleKeywords, key = { it }) { kw ->
                                    AssistChip(
                                        onClick = { titleKeywords = titleKeywords - kw },
                                        label = { Text(kw) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.pn_cd_remove)
                                            )
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = currentTitleKeyword,
                            onValueChange = { currentTitleKeyword = it },
                            label = { Text(stringResource(R.string.pn_add_title_keyword_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val kw = currentTitleKeyword.trim()
                                    if (kw.isNotBlank() && !titleKeywords.contains(kw)) {
                                        titleKeywords = titleKeywords + kw
                                        currentTitleKeyword = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val kw = currentTitleKeyword.trim()
                                        if (kw.isNotBlank() && !titleKeywords.contains(kw)) {
                                            titleKeywords = titleKeywords + kw
                                            currentTitleKeyword = ""
                                        }
                                    },
                                    enabled = currentTitleKeyword.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.pn_cd_add)
                                    )
                                }
                            },
                            singleLine = true
                        )

                        HorizontalDivider()

                        Text(
                            stringResource(R.string.pn_trigger_body_keywords),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AnimatedVisibility(
                            visible = bodyKeywords.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(bodyKeywords, key = { it }) { kw ->
                                    AssistChip(
                                        onClick = { bodyKeywords = bodyKeywords - kw },
                                        label = { Text(kw) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.pn_cd_remove)
                                            )
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = currentBodyKeyword,
                            onValueChange = { currentBodyKeyword = it },
                            label = { Text(stringResource(R.string.pn_add_body_keyword_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val kw = currentBodyKeyword.trim()
                                    if (kw.isNotBlank() && !bodyKeywords.contains(kw)) {
                                        bodyKeywords = bodyKeywords + kw
                                        currentBodyKeyword = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val kw = currentBodyKeyword.trim()
                                        if (kw.isNotBlank() && !bodyKeywords.contains(kw)) {
                                            bodyKeywords = bodyKeywords + kw
                                            currentBodyKeyword = ""
                                        }
                                    },
                                    enabled = currentBodyKeyword.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.pn_cd_add)
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                }

                if (ignoreConflicts.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.pn_rule_conflict_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.pn_rule_conflict_warning_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            ignoreConflicts.forEach { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (ruleConflicts.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.pn_rule_trigger_conflict_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.pn_rule_trigger_conflict_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            ruleConflicts.forEach { msg ->
                                Text(
                                    text = formatConflictMessage(msg),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.pn_actions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { flashEnabled = !flashEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.pn_flash_pattern),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = flashEnabled,
                                    onCheckedChange = { flashEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = flashEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedFlashPatterns,
                                        onExpandedChange = { expandedFlashPatterns = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val selectedLabel = if (flashCustomPatternId != null) {
                                            customPatterns.find { it.id == flashCustomPatternId }?.name
                                                ?: stringResource(R.string.pn_unknown_custom)
                                        } else {
                                            flashPattern.displayName
                                        }
                                        OutlinedTextField(
                                            value = selectedLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.pn_flash_pattern)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedFlashPatterns
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedFlashPatterns,
                                            onDismissRequest = { expandedFlashPatterns = false }
                                        ) {
                                            FlashPattern.entries.forEach { pattern ->
                                                DropdownMenuItem(
                                                    text = { Text(pattern.displayName) },
                                                    onClick = {
                                                        flashPattern = pattern
                                                        flashCustomPatternId = null
                                                        expandedFlashPatterns = false
                                                    }
                                                )
                                            }
                                            if (customPatterns.isNotEmpty()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                customPatterns.forEach { cPattern ->
                                                    DropdownMenuItem(
                                                        text = { Text(cPattern.name) },
                                                        onClick = {
                                                            flashCustomPatternId = cPattern.id
                                                            expandedFlashPatterns = false
                                                        },
                                                        trailingIcon = {
                                                            IconButton(
                                                                onClick = {
                                                                    expandedFlashPatterns = false
                                                                    patternToDelete = cPattern
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = stringResource(R.string.delete),
                                                                    tint = MaterialTheme.colorScheme.error,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = onNavigateToCreatePattern,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.pn_create_custom_pattern))
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { wakeUpEnabled = !wakeUpEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.pn_wake_up_screen_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = wakeUpEnabled,
                                    onCheckedChange = { wakeUpEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = wakeUpEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedWakeUpDuration,
                                        onExpandedChange = { expandedWakeUpDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val durLabel = if (screenDurationSeconds == 0)
                                            stringResource(R.string.pn_default_duration)
                                        else
                                            stringResource(R.string.pn_duration_format, screenDurationSeconds)
                                        OutlinedTextField(
                                            value = durLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.pn_screen_duration)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedWakeUpDuration
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedWakeUpDuration,
                                            onDismissRequest = { expandedWakeUpDuration = false }
                                        ) {
                                            wakeUpDurationOptions.forEach { sec ->
                                                val label = if (sec == 0)
                                                    stringResource(R.string.pn_default_duration)
                                                else
                                                    stringResource(R.string.pn_duration_format, sec)
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        screenDurationSeconds = sec
                                                        expandedWakeUpDuration = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = if (hasProximitySensor)
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { pocketModeEnabled = !pocketModeEnabled }
                                        else Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = pocketModeEnabled && hasProximitySensor,
                                            onCheckedChange = { if (hasProximitySensor) pocketModeEnabled = it },
                                            enabled = hasProximitySensor
                                        )
                                        Column {
                                            Text(
                                                text = stringResource(R.string.pn_pocket_mode),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (hasProximitySensor)
                                                    MaterialTheme.colorScheme.onSurface
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                            Text(
                                                text = if (hasProximitySensor)
                                                    stringResource(R.string.pn_pocket_mode_desc)
                                                else
                                                    stringResource(R.string.pn_proximity_sensor_unavailable),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (hasProximitySensor)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { aodEnabled = !aodEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.pn_turn_on_aod_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = aodEnabled,
                                    onCheckedChange = { aodEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = aodEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = expandedAodDuration,
                                        onExpandedChange = { expandedAodDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val aodLabel = when (aodDurationSeconds) {
                                            -1 -> stringResource(R.string.pn_until_dismiss_notification)
                                            -2 -> stringResource(R.string.pn_until_unlocking_phone)
                                            else -> stringResource(
                                                R.string.pn_duration_seconds,
                                                aodDurationSeconds
                                            )
                                        }
                                        OutlinedTextField(
                                            value = aodLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.pn_aod_duration)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expandedAodDuration
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedAodDuration,
                                            onDismissRequest = { expandedAodDuration = false }
                                        ) {
                                            aodDurationOptions.forEach { sec ->
                                                val label = when (sec) {
                                                    -1 -> stringResource(R.string.pn_until_dismiss_notification)
                                                    -2 -> stringResource(R.string.pn_until_unlocking_phone)
                                                    else -> stringResource(
                                                        R.string.pn_duration_seconds,
                                                        sec
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        aodDurationSeconds = sec
                                                        expandedAodDuration = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { screenFlashEnabled = !screenFlashEnabled },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.pn_flash_screen_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = screenFlashEnabled,
                                    onCheckedChange = { screenFlashEnabled = it }
                                )
                            }
                            AnimatedVisibility(
                                visible = screenFlashEnabled,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.pn_flash_screen_color_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ScreenFlashColor.entries.chunked(4).forEach { row ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.forEach { color ->
                                                    val isSelected = screenFlashColor == color
                                                    Surface(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clickable { screenFlashColor = color },
                                                        shape = MaterialTheme.shapes.small,
                                                        color = androidx.compose.ui.graphics.Color(color.colorArgb),
                                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                                            3.dp,
                                                            MaterialTheme.colorScheme.onSurface
                                                        ) else null,
                                                        shadowElevation = if (isSelected) 4.dp else 0.dp
                                                    ) {}
                                                }
                                            }
                                        }
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = expandedScreenFlashDuration,
                                        onExpandedChange = { expandedScreenFlashDuration = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val durationLabel = stringResource(
                                            R.string.pn_duration_seconds,
                                            screenFlashDurationSeconds
                                        )
                                        OutlinedTextField(
                                            value = durationLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.pn_flash_screen_duration_label)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScreenFlashDuration)
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedScreenFlashDuration,
                                            onDismissRequest = { expandedScreenFlashDuration = false }
                                        ) {
                                            screenFlashDurationOptions.forEach { sec ->
                                                val label = stringResource(R.string.pn_duration_seconds, sec)
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        screenFlashDurationSeconds = sec
                                                        expandedScreenFlashDuration = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.pn_apply_rule_on),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable { applyOnVibration = !applyOnVibration },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnVibration,
                                    onCheckedChange = { applyOnVibration = it }
                                )
                                Text(
                                    stringResource(R.string.pn_vibration),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.clickable { applyOnSilent = !applyOnSilent },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnSilent,
                                    onCheckedChange = { applyOnSilent = it }
                                )
                                Text(
                                    stringResource(R.string.pn_silence),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.clickable { applyOnDND = !applyOnDND },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyOnDND,
                                    onCheckedChange = { applyOnDND = it }
                                )
                                Text(
                                    stringResource(R.string.pn_dnd),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preventMultipleNotifications = !preventMultipleNotifications
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = preventMultipleNotifications,
                                onCheckedChange = { preventMultipleNotifications = it }
                            )
                            Text(
                                stringResource(R.string.pn_prevent_multiple_notifications),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

            AppSelectionTable(
                installedApps = installedApps,
                selectedApps = selectedApps,
                onSelectedAppsChanged = { selectedApps = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .nestedScroll(consumeAllScrollConnection)
            )
        }
    }

    patternToDelete?.let { pattern ->
        AlertDialog(
            onDismissRequest = { patternToDelete = null },
            title = {
                Text(
                    stringResource(R.string.pn_delete_pattern),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.pn_confirm_delete_pattern, pattern.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        rulesManager.removeCustomPattern(pattern.id)
                        if (flashCustomPatternId == pattern.id) {
                            flashCustomPatternId = null
                        }
                        customPatterns = rulesManager.getCustomPatterns()
                        patternToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { patternToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun formatConflictMessage(rawMessage: String): String {
    val parts = rawMessage.split("|")
    return when (parts.getOrNull(0)) {
        "DUPLICATE_RULE" -> stringResource(R.string.pn_conflict_duplicate_rule, parts.getOrNull(1).orEmpty())
        "EXISTING_ALL" -> stringResource(R.string.pn_conflict_existing_all, parts.getOrNull(1).orEmpty())
        "NEW_ALL" -> stringResource(R.string.pn_conflict_new_all, parts.getOrNull(1).orEmpty())
        "TITLE_OVERLAP" -> stringResource(R.string.pn_conflict_title_overlap, parts.getOrNull(1).orEmpty(), parts.getOrNull(2).orEmpty())
        "BODY_OVERLAP" -> stringResource(R.string.pn_conflict_body_overlap, parts.getOrNull(1).orEmpty(), parts.getOrNull(2).orEmpty())
        else -> rawMessage
    }
}

private fun ignorePhraseMatches(keyword: String, ignorePattern: String, isRegex: Boolean): Boolean {
    return if (isRegex) {
        try {
            Regex(ignorePattern, RegexOption.IGNORE_CASE).containsMatchIn(keyword)
        } catch (_: Exception) { false }
    } else {
        keyword.equals(ignorePattern, ignoreCase = true)
    }
}
