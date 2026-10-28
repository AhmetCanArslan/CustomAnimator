package com.arslan.customanimator

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.automirrored.filled.FormatTextdirectionRToL
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.components.IconBadge
import com.arslan.customanimator.ui.theme.AppShapes
import com.arslan.customanimator.utils.SystemMeterMetric
import java.text.Normalizer
import androidx.compose.material3.IconButtonDefaults
import com.arslan.customanimator.ui.theme.Motion

sealed interface SearchTarget {
    val key: String

    data class Tab(val tab: HomeTab) : SearchTarget {
        override val key: String get() = "tab:${tab.name}"
    }

    data class Screen(val screen: HomeScreen) : SearchTarget {
        override val key: String get() = "screen:${screen.name}"
    }
}

class SearchEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int?,
    val icon: ImageVector,
    val target: SearchTarget,
    @StringRes val sectionRes: Int
)

private fun HomeTab.row(@StringRes titleRes: Int, @StringRes descriptionRes: Int?, icon: ImageVector = icon()) =
    SearchEntry(titleRes, descriptionRes, icon, SearchTarget.Tab(this), labelRes())

private fun HomeTab.screen(screen: HomeScreen, @StringRes titleRes: Int, @StringRes descriptionRes: Int?, icon: ImageVector) =
    SearchEntry(titleRes, descriptionRes, icon, SearchTarget.Screen(screen), labelRes())

private fun section(@StringRes sectionRes: Int, screen: HomeScreen, @StringRes titleRes: Int, @StringRes descriptionRes: Int?, icon: ImageVector) =
    SearchEntry(titleRes, descriptionRes, icon, SearchTarget.Screen(screen), sectionRes)

private fun searchEntries(): List<SearchEntry> = listOf(
    HomeTab.ANIMATION.row(R.string.animation_header, null),
    HomeTab.ANIMATION.row(R.string.window_animation_scale, null),
    HomeTab.ANIMATION.row(R.string.transition_animation_scale, null),
    HomeTab.ANIMATION.row(R.string.animator_duration_scale, null),
    HomeTab.ANIMATION.row(R.string.saved_presets, null, Icons.Filled.Bookmarks),
    HomeTab.WIDTH.row(R.string.smallest_width, null),
    HomeTab.WIDTH.row(R.string.width_presets, null, Icons.Filled.Bookmarks),
    HomeTab.WIDTH.screen(HomeScreen.PER_APP_WIDTH, R.string.per_app_width, R.string.per_app_width_desc, Icons.Filled.AppShortcut),
    HomeTab.OPTIMIZER.row(R.string.optimizer_title, null),
    HomeTab.OPTIMIZER.row(R.string.optimizer_cleaner, R.string.optimizer_cleaner_desc, Icons.Filled.CleaningServices),
    HomeTab.OPTIMIZER.row(R.string.optimizer_performance_booster, R.string.optimizer_performance_booster_desc, Icons.Filled.Bolt),
    HomeTab.OPTIMIZER.row(R.string.clear_all_app_caches, R.string.clear_all_app_caches_desc, Icons.Filled.CleaningServices),
    HomeTab.OPTIMIZER.row(R.string.close_background_apps, R.string.close_background_apps_desc, Icons.Filled.StopCircle),
    HomeTab.OPTIMIZER.screen(HomeScreen.CLOSE_APPS_EXCLUSIONS, R.string.close_apps_exclusions, R.string.close_apps_exclusions_desc, Icons.Filled.Block),
    HomeTab.OPTIMIZER.screen(HomeScreen.WHATSAPP_CLEANER, R.string.whatsapp_cleaner, R.string.whatsapp_cleaner_desc, Icons.AutoMirrored.Filled.Chat),
    HomeTab.GAME_MODE.row(R.string.game_mode, null),
    HomeTab.SYSTEM_METER.row(R.string.system_meter_title, null),
    HomeTab.SYSTEM_METER.row(R.string.system_meter_enable, R.string.system_meter_enable_desc, Icons.Filled.Visibility)
) + SystemMeterMetric.entries.map { metric ->
    HomeTab.SYSTEM_METER.row(metricTitle(metric), metricDesc(metric), metricIcon(metric))
} + listOf(
    HomeTab.BATTERY.row(R.string.nav_battery, null),
    HomeTab.BATTERY.screen(HomeScreen.BATTERY_HEALTH, R.string.bh_title, R.string.bh_nav_desc, Icons.Filled.BatteryChargingFull),
    HomeTab.BATTERY.screen(HomeScreen.DOZE_WHITELIST, R.string.doze_whitelist, R.string.doze_whitelist_desc, Icons.Filled.Check),
    HomeTab.BATTERY.row(R.string.bt_low_power, R.string.bt_low_power_desc),
    HomeTab.BATTERY.row(R.string.bt_trigger_level, R.string.bt_trigger_level_desc),
    HomeTab.BATTERY.row(R.string.bt_auto_mode, R.string.bt_auto_mode_desc),
    HomeTab.BATTERY.row(R.string.bt_sticky, R.string.bt_sticky_desc),
    HomeTab.BATTERY.row(R.string.bt_app_standby, R.string.bt_app_standby_desc),
    HomeTab.BATTERY.row(R.string.bt_adaptive_battery, R.string.bt_adaptive_battery_desc),
    HomeTab.BATTERY.row(R.string.bt_app_auto_restriction, R.string.bt_app_auto_restriction_desc),
    HomeTab.BATTERY.row(R.string.bt_cached_freezer, R.string.bt_cached_freezer_desc, Icons.Filled.Memory),
    HomeTab.BATTERY.row(R.string.bt_ble_scan, R.string.bt_ble_scan_desc, Icons.Filled.Bluetooth),
    HomeTab.BATTERY.row(R.string.bt_adaptive_charging, R.string.bt_adaptive_charging_desc, Icons.Filled.BatteryChargingFull),
    HomeTab.BATTERY.row(R.string.bt_charging_sounds, R.string.bt_charging_sounds_desc, Icons.AutoMirrored.Filled.VolumeUp),
    HomeTab.BATTERY.row(R.string.bt_alert_low, R.string.bt_alert_low_desc, Icons.Filled.BatteryAlert),
    HomeTab.BATTERY.row(R.string.bt_alert_high, R.string.bt_alert_high_desc, Icons.Filled.BatteryAlert),
    HomeTab.TOOLS.row(R.string.tools_title, null),
    HomeTab.TOOLS.screen(HomeScreen.SHORTCUTS, R.string.shortcuts, R.string.shortcuts_desc, Icons.AutoMirrored.Filled.AddToHomeScreen),
    HomeTab.TOOLS.screen(HomeScreen.ICON_CHANGER, R.string.icon_changer, R.string.icon_changer_desc, Icons.Filled.AppShortcut),
    HomeTab.TOOLS.screen(HomeScreen.APK_EXTRACTOR, R.string.apk_extractor, R.string.apk_extractor_desc, Icons.Filled.Unarchive),
    HomeTab.TOOLS.screen(HomeScreen.APK_INSTALLER, R.string.apk_installer, R.string.apk_installer_desc, Icons.Filled.InstallMobile),
    HomeTab.TOOLS.screen(HomeScreen.ACTIVITY_MANAGER, R.string.activity_manager, R.string.activity_manager_desc, Icons.Filled.Layers),
    HomeTab.TOOLS.screen(HomeScreen.DEBLOATER, R.string.debloater, R.string.debloater_desc, Icons.Filled.DeleteSweep),
    HomeTab.TOOLS.screen(HomeScreen.COMPILE_BOOSTER, R.string.compile_booster, R.string.more_compile_booster_desc, Icons.Filled.Speed),
    HomeTab.TOOLS.screen(HomeScreen.AUTO_ACTIONS, R.string.auto_actions, R.string.more_auto_actions_desc, Icons.Filled.PlaylistRemove),
    HomeTab.TOOLS.screen(HomeScreen.AUTO_FORCE_STOP, R.string.auto_force_stop, R.string.auto_force_stop_desc, Icons.Filled.PlaylistRemove),
    HomeTab.TOOLS.screen(HomeScreen.AUTO_PERMISSION_DISABLER, R.string.auto_permission_disabler, R.string.auto_permission_disabler_desc, Icons.Filled.Shield),
    HomeTab.TOOLS.screen(HomeScreen.GRAPHICS_API_OVERRIDE, R.string.graphics_api_override, R.string.graphics_api_override_desc, Icons.Filled.VideogameAsset),
    HomeTab.TOOLS.screen(HomeScreen.HWUI_TWEAKS, R.string.hwui_tweaks, R.string.hwui_tweaks_desc, Icons.Filled.Brush),
    HomeTab.TOOLS.screen(HomeScreen.APP_THREADING, R.string.app_threading, R.string.app_threading_desc, Icons.Filled.Memory),
    HomeTab.TOOLS.screen(HomeScreen.REFRESH_RATE, R.string.refresh_rate, R.string.refresh_rate_desc, Icons.Filled.MonitorHeart),
    HomeTab.TOOLS.screen(HomeScreen.QS_TILES, R.string.qs_tiles, R.string.qs_tiles_desc, Icons.Filled.Widgets),
    HomeTab.TOOLS.screen(HomeScreen.SOUND_TILE, R.string.sound_tile, R.string.sound_tile_desc, Icons.AutoMirrored.Filled.VolumeUp),
    HomeTab.TOOLS.screen(HomeScreen.STATUS_BAR_ICONS, R.string.status_bar_icons, R.string.status_bar_icons_desc, Icons.Filled.WebAsset),
    HomeTab.TOOLS.screen(HomeScreen.SYSTEM_COLORS, R.string.system_colors, R.string.system_colors_desc, Icons.Filled.Palette),
    HomeTab.TOOLS.screen(HomeScreen.CARRIER_NAME, R.string.carrier_name, R.string.carrier_name_desc, Icons.Filled.SignalCellularAlt),
    HomeTab.TOOLS.screen(HomeScreen.BUTTON_REMAPPER, R.string.button_remapper, R.string.button_remapper_desc, Icons.Filled.PowerSettingsNew),
    HomeTab.TOOLS.row(R.string.restart_system_ui, R.string.restart_system_ui_desc, Icons.Filled.RestartAlt),
    HomeTab.TOOLS.screen(HomeScreen.WIFI_PASSWORDS, R.string.wifi_password_manager, R.string.wifi_password_manager_desc, Icons.Filled.Wifi),
    HomeTab.TOOLS.screen(HomeScreen.HOTSPOT_MANAGER, R.string.hotspot_manager, R.string.hotspot_manager_desc, Icons.Filled.WifiTethering),
    HomeTab.TOOLS.row(R.string.save_to_device, R.string.save_to_device_desc, Icons.Filled.SaveAlt),
    HomeTab.TOOLS.screen(HomeScreen.SCREENSHOT_ACTIONS, R.string.screenshot_actions, R.string.screenshot_actions_desc, Icons.Filled.Screenshot),
    HomeTab.TOOLS.screen(HomeScreen.OTP_COPIER, R.string.otp_copier, R.string.otp_copier_desc, Icons.Filled.Password),
    HomeTab.TOOLS.screen(HomeScreen.ALARM_REVEALER, R.string.alarm_revealer, R.string.alarm_revealer_desc, Icons.Filled.Alarm),
    HomeTab.TOOLS.row(R.string.disable_keyboard_animation, R.string.disable_keyboard_animation_desc, Icons.Filled.Keyboard),
    HomeTab.TOOLS.row(R.string.disable_rotation_suggestions, R.string.disable_rotation_suggestions_desc, Icons.Filled.ScreenRotation),
    HomeTab.TOOLS.row(R.string.show_clock_seconds, R.string.show_clock_seconds_desc, Icons.Filled.Schedule),
    HomeTab.TOOLS.row(R.string.remove_high_volume_warning, R.string.remove_high_volume_warning_desc, Icons.AutoMirrored.Filled.VolumeUp),
    HomeTab.TOOLS.row(R.string.lock_screen_rotation, R.string.lock_screen_rotation_desc, Icons.Filled.ScreenLockRotation),
    HomeTab.TOOLS.row(R.string.reset_rotation, R.string.reset_rotation_desc, Icons.Filled.ScreenRotation),
    HomeTab.DEVELOPER.row(R.string.developer_title, null),
    HomeTab.DEVELOPER.row(R.string.usb_debugging, R.string.usb_debugging_desc, Icons.Filled.Usb),
    HomeTab.DEVELOPER.row(R.string.wireless_debugging, R.string.wireless_debugging_desc, Icons.Filled.WifiTethering),
    HomeTab.DEVELOPER.row(R.string.dont_keep_activities, R.string.dont_keep_activities_desc, Icons.Filled.LayersClear),
    HomeTab.DEVELOPER.row(R.string.limit_background_processes, R.string.limit_background_processes_desc, Icons.Filled.Memory),
    HomeTab.DEVELOPER.row(R.string.sensors_off, R.string.sensors_off_desc, Icons.Filled.SensorsOff),
    HomeTab.DEVELOPER.row(R.string.show_taps, R.string.show_taps_desc, Icons.Filled.TouchApp),
    HomeTab.DEVELOPER.row(R.string.pointer_location, R.string.pointer_location_desc, Icons.Filled.MyLocation),
    HomeTab.DEVELOPER.row(R.string.show_layout_bounds, R.string.show_layout_bounds_desc, Icons.Filled.BorderAll),
    HomeTab.DEVELOPER.row(R.string.gpu_profiling, R.string.gpu_profiling_desc, Icons.Filled.BarChart),
    HomeTab.DEVELOPER.row(R.string.force_rtl, R.string.force_rtl_desc, Icons.AutoMirrored.Filled.FormatTextdirectionRToL),
    HomeTab.TERMINAL.row(R.string.nav_terminal, null),
    section(R.string.pn_title, HomeScreen.NOTIFY_HOME, R.string.pn_title, null, Icons.Filled.Notifications),
    section(R.string.pn_title, HomeScreen.NOTIFY_RULES, R.string.pn_rules, null, Icons.AutoMirrored.Filled.Rule),
    section(R.string.pn_title, HomeScreen.NOTIFY_LOGGING, R.string.pn_nav_logging, R.string.pn_history_desc, Icons.Filled.History),
    section(R.string.pn_title, HomeScreen.NOTIFY_IGNORED, R.string.pn_filters, null, Icons.Filled.FilterAlt),
    section(R.string.profiles_title, HomeScreen.PROFILES, R.string.profiles_title, R.string.profiles_subtitle, Icons.Filled.AccountCircle),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.settings, null, Icons.Filled.Settings),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.settings_theme, null, Icons.Filled.Palette),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.backup_app_data, R.string.backup_app_data_desc, Icons.Filled.Save),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.restore_app_data, R.string.restore_app_data_desc, Icons.Filled.Restore),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.revert_everything, R.string.revert_everything_desc, Icons.Filled.SettingsBackupRestore),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.remove_ads, R.string.remove_ads_desc, Icons.Filled.Block),
    section(R.string.settings, HomeScreen.SETTINGS, R.string.contact_developer, R.string.contact_developer_desc, Icons.Filled.Email),
    section(R.string.settings, HomeScreen.PERMISSIONS, R.string.permissions_title, null, Icons.Filled.Security),
    section(R.string.settings, HomeScreen.SETUP_GUIDE, R.string.setup_open, R.string.setup_open_desc, Icons.AutoMirrored.Filled.HelpOutline)
)

fun screenShortcutEntries(): List<SearchEntry> =
    HomeTab.entries.map { tab -> tab.row(tab.labelRes(), null) } +
        searchEntries().filter { it.target is SearchTarget.Screen }.distinctBy { it.target }

fun searchTargetOf(key: String): SearchTarget? =
    screenShortcutEntries().firstOrNull { it.target.key == key }?.target

class SearchHit(val entry: SearchEntry, val title: String, val description: String?)

class SearchGroup(@StringRes val sectionRes: Int, val hits: List<SearchHit>)

private class IndexedEntry(val hit: SearchHit, section: String) {
    val foldedTitle = foldForSearch(hit.title)
    val foldedDescription = foldForSearch(hit.description.orEmpty())
    val foldedSection = foldForSearch(section)
}

private fun indexEntries(resources: Resources): List<IndexedEntry> = searchEntries().map { entry ->
    IndexedEntry(
        SearchHit(entry, resources.getString(entry.titleRes), entry.descriptionRes?.let(resources::getString)),
        resources.getString(entry.sectionRes)
    )
}

private fun foldForSearch(text: String): String = buildString(text.length) {
    text.forEach { char -> append(foldChar(char)) }
}

private fun foldChar(char: Char): Char {
    val base = Normalizer.normalize(char.toString(), Normalizer.Form.NFD).first()
    return if (base == 'ı') 'i' else base.lowercaseChar()
}

private fun searchTerms(query: String): List<String> =
    foldForSearch(query).split(' ', '\t', '\n').filter { it.isNotEmpty() }

private fun startsWord(text: String, term: String): Boolean =
    Regex("(^|[^\\p{L}\\p{N}])" + Regex.escape(term)).containsMatchIn(text)

private fun IndexedEntry.termScore(term: String): Int? = when {
    foldedTitle.startsWith(term) -> 0
    startsWord(foldedTitle, term) -> 1
    foldedTitle.contains(term) -> 2
    foldedSection.contains(term) -> 3
    foldedDescription.contains(term) -> 4
    else -> null
}

private fun IndexedEntry.score(terms: List<String>): Int? {
    val scores = terms.map { termScore(it) ?: return null }
    return scores.sum()
}

private fun searchGroups(index: List<IndexedEntry>, terms: List<String>): List<SearchGroup> =
    index.mapNotNull { entry -> entry.score(terms)?.let { entry to it } }
        .sortedBy { it.second }
        .groupBy({ it.first.hit.entry.sectionRes }, { it.first.hit })
        .map { (sectionRes, hits) -> SearchGroup(sectionRes, hits) }

private fun highlight(text: String, terms: List<String>, style: SpanStyle): AnnotatedString {
    val folded = foldForSearch(text)
    return buildAnnotatedString {
        append(text)
        terms.forEach { term ->
            var start = folded.indexOf(term)
            while (start >= 0) {
                addStyle(style, start, start + term.length)
                start = folded.indexOf(term, start + term.length)
            }
        }
    }
}

@Composable
fun AppSearchScreen(onClose: () -> Unit, onOpen: (SearchTarget) -> Unit) {
    val resources = LocalResources.current
    val index = remember(resources) { indexEntries(resources) }
    var query by rememberSaveable { mutableStateOf("") }
    val terms = remember(query) { searchTerms(query) }
    val groups = remember(index, terms) { searchGroups(index, terms) }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val open: (SearchHit) -> Unit = { hit ->
        keyboard?.hide()
        onOpen(hit.entry.target)
    }

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(terms) { listState.scrollToItem(0) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) keyboard?.hide()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focusRequester,
                onClose = onClose,
                onSearch = { groups.firstOrNull()?.hits?.firstOrNull()?.let(open) }
            )
            if (groups.isEmpty()) {
                SearchEmptyState(query.trim())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp)
                ) {
                    groups.forEach { group ->
                        val rows = group.hits.map<SearchHit, @Composable () -> Unit> { hit ->
                            { SearchResultRow(hit = hit, terms = terms, onClick = { open(hit) }) }
                        }
                        cardSection(group.sectionRes, *rows.toTypedArray())
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onClose: () -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        shape = AppShapes.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_app_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = Motion.popIn(),
                exit = Motion.popOut()
            ) {
                IconButton(onClick = {
                    onQueryChange("")
                    focusRequester.requestFocus()
                }, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, terms: List<String>, onClick: () -> Unit) {
    val style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    val title = remember(hit, terms, style) { highlight(hit.title, terms, style) }
    val description = remember(hit, terms, style) { hit.description?.let { highlight(it, terms, style) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = hit.entry.icon, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(
            icon = Icons.Filled.SearchOff,
            size = 72.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.search_app_empty, query),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_app_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
