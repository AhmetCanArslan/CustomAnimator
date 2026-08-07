package com.arslan.customanimator.notify.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.NotificationRule
import com.arslan.customanimator.notify.data.RuleType
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.data.WidgetConfig
import com.arslan.customanimator.notify.data.WidgetConfigStore
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme

class NotificationWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(Activity.RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CustomAnimatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigureScreen(
                        initialConfig = WidgetConfigStore.load(this, appWidgetId),
                        onCancel = { finish() },
                        onConfirm = { config ->
                            WidgetConfigStore.save(this, appWidgetId, config)
                            AppWidgetManager.getInstance(this).updateAppWidget(
                                appWidgetId,
                                NotificationWidgetProvider.buildRemoteViews(this, appWidgetId)
                            )
                            AppWidgetManager.getInstance(this)
                                .notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
                            setResult(Activity.RESULT_OK, resultIntent())
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

private val backgroundPalette = listOf(
    0xFF1C1B1F, 0xFF000000, 0xFFFFFFFF, 0xFF263238,
    0xFF1A237E, 0xFF004D40, 0xFF3E2723, 0xFF4A148C,
)

private val textPalette = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFE0E0E0, 0xFFFFF176,
)

private val accentPalette = listOf(
    0xFF9C27B0, 0xFF03A9F4, 0xFF4CAF50, 0xFFFF9800,
    0xFFF44336, 0xFFE91E63, 0xFF00BCD4, 0xFF9E9E9E,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigureScreen(
    initialConfig: WidgetConfig,
    onCancel: () -> Unit,
    onConfirm: (WidgetConfig) -> Unit,
) {
    val context = LocalContext.current
    val widgetRules = remember {
        RulesManager(context).getRules().filter { rule ->
            rule.actions.any { it.type == RuleType.WIDGET }
        }
    }

    var config by remember { mutableStateOf(initialConfig) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pn_widget_configure_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.pn_widget_cancel)) }
                    Button(
                        onClick = { onConfirm(config) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.pn_widget_add)) }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WidgetPreview(config)

            ConfigCard(stringResource(R.string.pn_widget_section_content)) {
                OutlinedTextField(
                    value = config.headerText,
                    onValueChange = { config = config.copy(headerText = it) },
                    label = { Text(stringResource(R.string.pn_widget_header_text)) },
                    placeholder = { Text(stringResource(R.string.pn_widget_default_header)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    stringResource(R.string.pn_widget_max_items, config.maxItems),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = config.maxItems.toFloat(),
                    onValueChange = { config = config.copy(maxItems = it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28
                )

                SwitchRow(
                    stringResource(R.string.pn_widget_show_header),
                    config.showHeader
                ) { config = config.copy(showHeader = it) }
                SwitchRow(
                    stringResource(R.string.pn_widget_show_icon),
                    config.showAppIcon
                ) { config = config.copy(showAppIcon = it) }
                SwitchRow(
                    stringResource(R.string.pn_widget_show_app_name),
                    config.showAppName
                ) { config = config.copy(showAppName = it) }
                SwitchRow(
                    stringResource(R.string.pn_widget_show_body),
                    config.showBody
                ) { config = config.copy(showBody = it) }
                SwitchRow(
                    stringResource(R.string.pn_widget_show_time),
                    config.showTime
                ) { config = config.copy(showTime = it) }
            }

            ConfigCard(stringResource(R.string.pn_widget_section_rules)) {
                if (widgetRules.isEmpty()) {
                    Text(
                        stringResource(R.string.pn_widget_no_rules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.pn_widget_rules_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.ruleIds.isEmpty(),
                            onClick = { config = config.copy(ruleIds = emptyList()) },
                            label = { Text(stringResource(R.string.pn_widget_all_rules)) }
                        )
                        widgetRules.forEach { rule ->
                            val selected = rule.id in config.ruleIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val ids = config.ruleIds.toMutableList()
                                    if (selected) ids.remove(rule.id) else ids.add(rule.id)
                                    config = config.copy(ruleIds = ids)
                                },
                                label = {
                                    Text(
                                        ruleLabel(rule),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            ConfigCard(stringResource(R.string.pn_widget_section_appearance)) {
                ColorPicker(
                    label = stringResource(R.string.pn_widget_background_color),
                    colors = backgroundPalette,
                    selected = config.backgroundColor
                ) { config = config.copy(backgroundColor = it) }

                Text(
                    stringResource(R.string.pn_widget_opacity, config.backgroundAlphaPercent),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = config.backgroundAlphaPercent.toFloat(),
                    onValueChange = { config = config.copy(backgroundAlphaPercent = it.toInt()) },
                    valueRange = 0f..100f
                )

                ColorPicker(
                    label = stringResource(R.string.pn_widget_text_color),
                    colors = textPalette,
                    selected = config.textColor
                ) { config = config.copy(textColor = it) }

                ColorPicker(
                    label = stringResource(R.string.pn_widget_accent_color),
                    colors = accentPalette,
                    selected = config.accentColor
                ) { config = config.copy(accentColor = it) }

                Text(
                    stringResource(R.string.pn_widget_corner_radius),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationWidgetProvider.cornerRadiusOptions.forEach { radius ->
                        FilterChip(
                            selected = config.cornerRadiusDp == radius,
                            onClick = { config = config.copy(cornerRadiusDp = radius) },
                            label = { Text("$radius dp") }
                        )
                    }
                }

                Text(
                    stringResource(R.string.pn_widget_text_size, config.textSizeSp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = config.textSizeSp.toFloat(),
                    onValueChange = { config = config.copy(textSizeSp = it.toInt()) },
                    valueRange = 10f..22f,
                    steps = 11
                )
            }
        }
    }
}

@Composable
private fun ConfigCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(
    label: String,
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { color ->
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onSelect(color) },
                    shape = MaterialTheme.shapes.small,
                    color = Color(color),
                    border = if (selected == color) {
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    }
                ) {}
            }
        }
    }
}

@Composable
private fun WidgetPreview(config: WidgetConfig) {
    val background = Color(config.backgroundColorWithAlpha.toLong() and 0xFFFFFFFFL)
    val textColor = Color(config.textColor)
    val accentColor = Color(config.accentColor)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.pn_widget_preview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
                .background(background)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (config.showHeader) {
                Text(
                    config.headerText.ifBlank { stringResource(R.string.pn_widget_default_header) },
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            repeat(2) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (config.showAppIcon) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.35f))
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.pn_widget_preview_title, index + 1),
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = config.textSizeSp.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (config.showBody) {
                            Text(
                                stringResource(R.string.pn_widget_preview_body),
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = (config.textSizeSp - 1).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val meta = buildList {
                            if (config.showAppName) add(stringResource(R.string.pn_widget_preview_app))
                            if (config.showTime) add(stringResource(R.string.pn_widget_preview_time))
                        }
                        if (meta.isNotEmpty()) {
                            Text(
                                meta.joinToString(" · "),
                                color = accentColor,
                                fontSize = (config.textSizeSp - 3).sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ruleLabel(rule: NotificationRule): String {
    val apps = rule.appNames.joinToString(", ").ifBlank { "?" }
    val keywords = (rule.titleKeywords + rule.bodyKeywords + rule.keywords)
    return if (keywords.isEmpty()) apps else "$apps – ${keywords.first()}"
}
