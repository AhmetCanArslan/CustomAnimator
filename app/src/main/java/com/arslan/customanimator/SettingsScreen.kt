package com.arslan.customanimator

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isSimpleMode: Boolean,
    onSimpleModeChange: (Boolean) -> Unit,
    inputMode: String,
    onInputModeChange: (String) -> Unit,
    isShizukuAvailable: Boolean,
    hasWriteSecureSettings: Boolean,
    onShowPermissionDetails: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // General section: Simple / Advanced mode
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

            // Input method section
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

            // About section
            SettingsSection(title = stringResource(R.string.settings_about)) {
                if (isShizukuAvailable) {
                    ActionSettingRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.permission_details),
                        description = if (hasWriteSecureSettings)
                            stringResource(R.string.settings_permission_status_granted)
                        else
                            stringResource(R.string.settings_permission_status_not_granted),
                        descriptionColor = if (hasWriteSecureSettings)
                            Color(0xFF2E7D32)
                        else
                            MaterialTheme.colorScheme.error,
                        onClick = onShowPermissionDetails
                    )
                    SettingDivider()
                }
                if (!BuildConfig.HAS_ADS) {
                    val context = LocalContext.current
                    ActionSettingRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.source_code),
                        description = stringResource(R.string.settings_source_code_desc),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.github.com/ahmetcanarslan/customanimator")
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun SelectableSettingRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = description,
                fontSize = 12.sp,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = descriptionColor)
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
