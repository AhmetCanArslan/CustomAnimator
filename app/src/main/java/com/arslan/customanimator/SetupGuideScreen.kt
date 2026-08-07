package com.arslan.customanimator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arslan.customanimator.ui.components.AppCard
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.components.SectionHeader
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import com.arslan.customanimator.ui.theme.MonoBody
import com.arslan.customanimator.utils.SetupHelper
import com.arslan.customanimator.utils.SetupStage
import com.arslan.customanimator.utils.ShizukuHelper
import kotlinx.coroutines.delay

private enum class SetupRoute { PHONE_ONLY, COMPUTER }

val LocalOpenSetupGuide = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun rememberSetupStage(): State<SetupStage> {
    val context = LocalContext.current
    val stage = remember { mutableStateOf(SetupHelper.stage(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var active by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    active = true
                    stage.value = SetupHelper.stage(context)
                }

                Lifecycle.Event.ON_PAUSE -> active = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(active) {
        while (active) {
            delay(1000)
            val current = SetupHelper.stage(context)
            if (current != stage.value) stage.value = current
        }
    }

    return stage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val stage by rememberSetupStage()
    var routeName by rememberSaveable {
        mutableStateOf(
            if (SetupHelper.supportsWirelessDebugging()) {
                SetupRoute.PHONE_ONLY.name
            } else {
                SetupRoute.COMPUTER.name
            }
        )
    }
    val route = SetupRoute.valueOf(routeName)
    var explainerOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.setup_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { SetupStatusCard(stage = stage) }

            item {
                ExplainerCard(
                    expanded = explainerOpen,
                    onToggle = { explainerOpen = !explainerOpen }
                )
            }

            if (stage != SetupStage.READY) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.setup_choose_route),
                        subtitle = stringResource(R.string.setup_choose_route_sub)
                    )
                }
                item {
                    RouteSelector(
                        route = route,
                        onRouteChange = { routeName = it.name }
                    )
                }
                if (route == SetupRoute.PHONE_ONLY) {
                    item { PhoneOnlySteps(stage = stage) }
                } else {
                    item { ComputerSteps() }
                }
                item { HelpFooter() }
            } else {
                item { ReadyNextSteps(onBack = onBack) }
            }
        }
    }
}

@Composable
private fun SetupStatusCard(stage: SetupStage) {
    val extended = LocalExtendedColors.current
    val ready = stage == SetupStage.READY
    val container = if (ready) extended.successContainer else MaterialTheme.colorScheme.primaryContainer
    val onContainer = if (ready) extended.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer

    AppCard(containerColor = container) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = if (ready) Icons.Filled.Verified else Icons.Filled.Security,
                size = 48.dp,
                containerColor = onContainer.copy(alpha = 0.14f),
                contentColor = onContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (ready) R.string.setup_status_ready else R.string.setup_status_not_ready
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
                Text(
                    text = stringResource(
                        when (stage) {
                            SetupStage.READY -> R.string.setup_status_ready_desc
                            SetupStage.AUTHORIZED -> R.string.setup_status_authorized_desc
                            SetupStage.NOT_AUTHORIZED -> R.string.setup_status_not_authorized_desc
                            SetupStage.NOT_RUNNING -> R.string.setup_status_not_running_desc
                            SetupStage.NOT_INSTALLED -> R.string.setup_status_not_installed_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ExplainerCard(expanded: Boolean, onToggle: () -> Unit) {
    AppCard(
        onClick = onToggle,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = Icons.AutoMirrored.Filled.HelpOutline, size = 40.dp)
            Text(
                text = stringResource(R.string.setup_explainer_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExplainerParagraph(
                    title = stringResource(R.string.setup_explainer_q1),
                    body = stringResource(R.string.setup_explainer_a1)
                )
                ExplainerParagraph(
                    title = stringResource(R.string.setup_explainer_q2),
                    body = stringResource(R.string.setup_explainer_a2)
                )
                ExplainerParagraph(
                    title = stringResource(R.string.setup_explainer_q3),
                    body = stringResource(R.string.setup_explainer_a3)
                )
                ExplainerParagraph(
                    title = stringResource(R.string.setup_explainer_q4),
                    body = stringResource(R.string.setup_explainer_a4)
                )
            }
        }
    }
}

@Composable
private fun ExplainerParagraph(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteSelector(route: SetupRoute, onRouteChange: (SetupRoute) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RouteOption(
            icon = Icons.Filled.PhoneAndroid,
            title = stringResource(R.string.setup_route_phone),
            subtitle = stringResource(
                if (SetupHelper.supportsWirelessDebugging()) {
                    R.string.setup_route_phone_sub
                } else {
                    R.string.setup_route_phone_sub_unsupported
                }
            ),
            recommended = SetupHelper.supportsWirelessDebugging(),
            selected = route == SetupRoute.PHONE_ONLY,
            onClick = { onRouteChange(SetupRoute.PHONE_ONLY) }
        )
        RouteOption(
            icon = Icons.Filled.Computer,
            title = stringResource(R.string.setup_route_computer),
            subtitle = stringResource(R.string.setup_route_computer_sub),
            recommended = !SetupHelper.supportsWirelessDebugging(),
            selected = route == SetupRoute.COMPUTER,
            onClick = { onRouteChange(SetupRoute.COMPUTER) }
        )
    }
}

@Composable
private fun RouteOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    recommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        highlighted = selected,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentPadding = 16.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = icon, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    if (recommended) {
                        StatusPill(
                            text = stringResource(R.string.setup_recommended),
                            tone = StatusTone.ACTIVE
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun PhoneOnlySteps(stage: SetupStage) {
    val context = LocalContext.current
    val installed = stage != SetupStage.NOT_INSTALLED
    val running = stage == SetupStage.NOT_AUTHORIZED || stage == SetupStage.AUTHORIZED
    val authorized = stage == SetupStage.AUTHORIZED

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupStep(
            number = 1,
            title = stringResource(R.string.setup_step_install_title),
            body = stringResource(R.string.setup_step_install_body),
            done = installed,
            active = !installed,
            primaryLabel = stringResource(R.string.setup_step_install_action),
            primaryIcon = Icons.Filled.Download,
            onPrimary = { SetupHelper.openShizukuStore(context) },
            secondaryLabel = stringResource(R.string.setup_step_install_alt),
            onSecondary = { SetupHelper.openShizukuGithub(context) }
        )
        SetupStep(
            number = 2,
            title = stringResource(R.string.setup_step_devopts_title),
            body = stringResource(R.string.setup_step_devopts_body),
            done = running,
            active = installed && !running,
            primaryLabel = stringResource(R.string.setup_step_devopts_action),
            primaryIcon = Icons.Filled.OpenInNew,
            onPrimary = { SetupHelper.openAboutPhone(context) }
        )
        SetupStep(
            number = 3,
            title = stringResource(R.string.setup_step_wireless_title),
            body = stringResource(R.string.setup_step_wireless_body),
            done = running,
            active = installed && !running,
            primaryLabel = stringResource(R.string.setup_step_wireless_action),
            primaryIcon = Icons.Filled.OpenInNew,
            onPrimary = { SetupHelper.openWirelessDebugging(context) }
        )
        SetupStep(
            number = 4,
            title = stringResource(R.string.setup_step_start_title),
            body = stringResource(R.string.setup_step_start_body),
            done = running,
            active = installed && !running,
            primaryLabel = stringResource(R.string.setup_step_start_action),
            primaryIcon = Icons.Filled.PlayArrow,
            onPrimary = {
                if (!SetupHelper.openShizukuApp(context)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.setup_shizuku_missing),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        SetupStep(
            number = 5,
            title = stringResource(R.string.setup_step_allow_title),
            body = stringResource(R.string.setup_step_allow_body),
            done = authorized || stage == SetupStage.READY,
            active = stage == SetupStage.NOT_AUTHORIZED,
            primaryLabel = stringResource(R.string.setup_step_allow_action),
            primaryIcon = Icons.Filled.Security,
            onPrimary = {
                if (running) {
                    ShizukuHelper.requestShizukuPermission(context)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.setup_not_running_yet),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        SetupStep(
            number = 6,
            title = stringResource(R.string.setup_step_finish_title),
            body = stringResource(R.string.setup_step_finish_body),
            done = stage == SetupStage.READY,
            active = authorized,
            primaryLabel = stringResource(R.string.setup_step_finish_action),
            primaryIcon = Icons.Filled.CheckCircle,
            onPrimary = {
                val granted = authorized && ShizukuHelper.grantWriteSecureSettingsPermission(context)
                Toast.makeText(
                    context,
                    context.getString(
                        if (granted) R.string.setup_finish_success else R.string.setup_finish_failed
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        RebootNote()
    }
}

@Composable
private fun ComputerSteps() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupStep(
            number = 1,
            title = stringResource(R.string.setup_pc_step1_title),
            body = stringResource(R.string.setup_pc_step1_body),
            done = false,
            active = true,
            primaryLabel = stringResource(R.string.setup_step_devopts_action),
            primaryIcon = Icons.Filled.OpenInNew,
            onPrimary = { SetupHelper.openAboutPhone(context) }
        )
        SetupStep(
            number = 2,
            title = stringResource(R.string.setup_pc_step2_title),
            body = stringResource(R.string.setup_pc_step2_body),
            done = false,
            active = true,
            primaryLabel = stringResource(R.string.setup_pc_step2_action),
            primaryIcon = Icons.Filled.OpenInNew,
            onPrimary = { SetupHelper.openDeveloperOptions(context) }
        )
        SetupStep(
            number = 3,
            title = stringResource(R.string.setup_pc_step3_title),
            body = stringResource(R.string.setup_pc_step3_body),
            done = false,
            active = true
        ) {
            CommandBox(command = SetupHelper.adbCommand(context))
        }
        RebootNote()
    }
}

@Composable
private fun RebootNote() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = AppShapes.card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.setup_reboot_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    active: Boolean,
    primaryLabel: String? = null,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    val extended = LocalExtendedColors.current
    val alpha = if (done || active) 1f else 0.55f

    AppCard(
        highlighted = active,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = 16.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) extended.successContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = extended.onSuccessContainer,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
                extra?.invoke()
                if (!done && primaryLabel != null && onPrimary != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPrimary) {
                            if (primaryIcon != null) {
                                Icon(
                                    imageVector = primaryIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(primaryLabel)
                        }
                        if (secondaryLabel != null && onSecondary != null) {
                            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandBox(command: String) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = AppShapes.field,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SelectionContainer {
                Text(
                    text = command,
                    style = MonoBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
                    Toast.makeText(
                        context,
                        context.getString(R.string.pn_command_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.pn_copy))
            }
        }
    }
}

@Composable
private fun HelpFooter() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = { SetupHelper.openShizukuGuide(context) }) {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.setup_open_official_guide))
        }
    }
}

@Composable
private fun ReadyNextSteps(onBack: () -> Unit) {
    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text(
            text = stringResource(R.string.setup_ready_next_title),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.setup_ready_next_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_ready_next_action))
        }
    }
}

@Composable
fun SetupNudgeCard(message: String, onOpenSetup: () -> Unit) {
    val extended = LocalExtendedColors.current
    AppCard(containerColor = extended.warningContainer, contentPadding = 16.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Filled.Security,
                size = 40.dp,
                containerColor = extended.onWarningContainer.copy(alpha = 0.14f),
                contentColor = extended.onWarningContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.setup_nudge_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = extended.onWarningContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onWarningContainer.copy(alpha = 0.9f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_nudge_action))
        }
    }
}
