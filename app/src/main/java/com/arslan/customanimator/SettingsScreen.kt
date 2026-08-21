package com.arslan.customanimator

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.ui.theme.LocalExtendedColors
import com.arslan.customanimator.ui.theme.LocalThemeController
import com.arslan.customanimator.ui.theme.Motion
import com.arslan.customanimator.ui.theme.ThemeMode
import com.arslan.customanimator.ui.theme.pressScale
import com.arslan.customanimator.utils.BackupManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.SystemResetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEVELOPER_EMAIL = "ahmetcanarslandev@gmail.com"
private const val PRIVACY_POLICY_URL =
    "https://github.com/ahmetcanarslan/customanimator/blob/main/PRIVACY_POLICY.md"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isSimpleMode: Boolean,
    onSimpleModeChange: (Boolean) -> Unit,
    inputMode: String,
    onInputModeChange: (String) -> Unit,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    hasWriteSecureSettings: Boolean,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()
    val canRevert = hasShizukuPermission || hasWriteSecureSettings
    var showRevertConfirm by remember { mutableStateOf(false) }
    var isReverting by remember { mutableStateOf(false) }

    val toast: (Int) -> Unit = { res ->
        Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
    }

    val isAdFree by rememberIsAdFree()
    val removeAdsPrice by rememberRemoveAdsPrice()
    var isRestoring by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { BackupManager.writeBackup(context, uri) }
            toast(if (success) R.string.backup_saved else R.string.action_failed)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { BackupManager.restoreBackup(context, uri) }
            toast(if (success) R.string.restore_done else R.string.restore_failed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                ThemeModeSelector()
            }

            SettingsSection(title = stringResource(R.string.settings_general)) {
                SelectableSettingRow(
                    title = stringResource(R.string.simple_mode),
                    description = stringResource(R.string.settings_simple_mode_desc),
                    selected = isSimpleMode,
                    onClick = { onSimpleModeChange(true) }
                )
                SettingDivider()
                SelectableSettingRow(
                    title = stringResource(R.string.advanced_mode),
                    description = stringResource(R.string.settings_advanced_mode_desc),
                    selected = !isSimpleMode,
                    onClick = { onSimpleModeChange(false) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_input_mode)) {
                SelectableSettingRow(
                    title = stringResource(R.string.use_sliders),
                    description = stringResource(R.string.settings_slider_desc),
                    selected = inputMode == "slider",
                    onClick = { onInputModeChange("slider") }
                )
                SettingDivider()
                SelectableSettingRow(
                    title = stringResource(R.string.use_manual_input),
                    description = stringResource(R.string.settings_manual_desc),
                    selected = inputMode == "manual",
                    onClick = { onInputModeChange("manual") }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_backup)) {
                ActionSettingRow(
                    icon = Icons.Filled.Save,
                    title = stringResource(R.string.backup_app_data),
                    description = stringResource(R.string.backup_app_data_desc),
                    onClick = {
                        backupLauncher.launch(BackupManager.suggestedFileName())
                    }
                )
                SettingDivider()
                ActionSettingRow(
                    icon = Icons.Filled.Restore,
                    title = stringResource(R.string.restore_app_data),
                    description = stringResource(R.string.restore_app_data_desc),
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                )
                SettingDivider()
                ActionSettingRow(
                    icon = Icons.Filled.SettingsBackupRestore,
                    title = stringResource(R.string.revert_everything),
                    description = if (canRevert) {
                        stringResource(R.string.revert_everything_desc)
                    } else {
                        stringResource(R.string.developer_needs_shizuku)
                    },
                    descriptionColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        if (canRevert) {
                            showRevertConfirm = true
                        } else {
                            openSetup()
                        }
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_premium)) {
                if (isAdFree) {
                    ActionSettingRow(
                        icon = Icons.Filled.CheckCircle,
                        title = stringResource(R.string.remove_ads),
                        description = stringResource(R.string.remove_ads_owned),
                        descriptionColor = LocalExtendedColors.current.success,
                        onClick = {}
                    )
                } else {
                    ActionSettingRow(
                        icon = Icons.Filled.Block,
                        title = stringResource(R.string.remove_ads),
                        description = removeAdsPrice?.let {
                            stringResource(R.string.remove_ads_desc_price, it)
                        } ?: stringResource(R.string.remove_ads_desc),
                        onClick = { startRemoveAdsPurchase(context) }
                    )
                    SettingDivider()
                    ActionSettingRow(
                        icon = Icons.Filled.ShoppingCart,
                        title = stringResource(R.string.remove_ads_restore),
                        description = stringResource(R.string.remove_ads_restore_desc),
                        onClick = {
                            if (isRestoring) return@ActionSettingRow
                            isRestoring = true
                            restorePurchases { owned ->
                                isRestoring = false
                                toast(
                                    if (owned) R.string.remove_ads_restored
                                    else R.string.remove_ads_nothing_to_restore
                                )
                            }
                        }
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_about)) {
                val contactSubject = stringResource(R.string.contact_developer_subject)
                ActionSettingRow(
                    icon = Icons.Filled.Email,
                    title = stringResource(R.string.contact_developer),
                    description = stringResource(R.string.contact_developer_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${DEVELOPER_EMAIL}")
                            putExtra(Intent.EXTRA_SUBJECT, contactSubject)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            toast(R.string.contact_developer_no_app)
                        }
                    }
                )
                SettingDivider()
                ActionSettingRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.permissions_title),
                    description = if (hasWriteSecureSettings)
                        stringResource(R.string.settings_permission_status_granted)
                    else
                        stringResource(R.string.settings_permission_status_not_granted),
                    descriptionColor = if (hasWriteSecureSettings)
                        LocalExtendedColors.current.success
                    else
                        MaterialTheme.colorScheme.error,
                    trailingIcon = Icons.Filled.ChevronRight,
                    onClick = onNavigateToPermissions
                )
                SettingDivider()
                ActionSettingRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = stringResource(R.string.setup_open),
                    description = stringResource(R.string.setup_open_desc),
                    trailingIcon = Icons.Filled.ChevronRight,
                    onClick = openSetup
                )
                SettingDivider()
                if (isPrivacyOptionsRequired()) {
                    ActionSettingRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = stringResource(R.string.settings_privacy_options),
                        description = stringResource(R.string.settings_privacy_options_desc),
                        onClick = {
                            (context as? android.app.Activity)?.let { showPrivacyOptions(it) }
                        }
                    )
                    SettingDivider()
                }
                ActionSettingRow(
                    icon = Icons.Filled.PrivacyTip,
                    title = stringResource(R.string.settings_privacy_policy),
                    description = stringResource(R.string.settings_privacy_policy_desc),
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(PRIVACY_POLICY_URL)
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isReverting) showRevertConfirm = false },
            title = { Text(stringResource(R.string.revert_everything)) },
            text = { Text(stringResource(R.string.revert_everything_confirm)) },
            confirmButton = {
                Button(
                    enabled = !isReverting,
                    onClick = {
                        isReverting = true
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                SystemResetManager.revertEverything(context, context.contentResolver)
                            }
                            isReverting = false
                            showRevertConfirm = false
                            toast(
                                if (result.allSucceeded) R.string.revert_everything_done
                                else R.string.revert_everything_partial
                            )
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (isReverting) R.string.working else R.string.revert_everything
                        )
                    )
                }
            },
            dismissButton = {
                Button(
                    enabled = !isReverting,
                    onClick = { showRevertConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.card)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SelectableSettingRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.durationMedium, easing = Motion.emphasizedEasing),
        label = "rowSelection"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = Motion.bouncy(),
        label = "rowIndicator"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.RadioButton
            )
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(indicatorScale)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource, 0.985f)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = descriptionColor
            )
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeModeSelector() {
    val controller = LocalThemeController.current
    val options = listOf(
        Triple(ThemeMode.SYSTEM, Icons.Filled.BrightnessAuto, R.string.theme_system),
        Triple(ThemeMode.LIGHT, Icons.Filled.LightMode, R.string.theme_light),
        Triple(ThemeMode.DARK, Icons.Filled.DarkMode, R.string.theme_dark)
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        val labels = options.map { stringResource(it.third) }
        val labelStyle = MaterialTheme.typography.labelMedium
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackedSegments = remember(maxWidth, labels, labelStyle, density) {
            val widestLabelPx = labels.maxOf { textMeasurer.measure(it, labelStyle).size.width }
            val segmentPx = with(density) {
                ((maxWidth - 8.dp - 8.dp) / options.size).toPx()
            }
            val iconAreaPx = with(density) { (18.dp + 6.dp + 8.dp).toPx() }
            widestLabelPx + iconAreaPx > segmentPx
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.chip)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (mode, icon, labelRes) ->
                val selected = controller.mode == mode
                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(Motion.durationMedium, easing = Motion.emphasizedEasing),
                    label = "themeSegment"
                )
                val content by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(Motion.durationMedium),
                    label = "themeSegmentContent"
                )
                val interactionSource = remember { MutableInteractionSource() }
                val segmentModifier = Modifier
                    .weight(1f)
                    .pressScale(interactionSource, 0.94f)
                    .clip(AppShapes.chip)
                    .background(container)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) { controller.updateMode(mode) }
                    .padding(vertical = 10.dp, horizontal = 4.dp)

                if (stackedSegments) {
                    Column(
                        modifier = segmentModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = content,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Row(
                        modifier = segmentModifier,
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = content,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        }
    }
}
