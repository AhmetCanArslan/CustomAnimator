package com.riyadmondol2006.customanimator.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.riyadmondol2006.customanimator.AlarmRevealerScreen
import com.riyadmondol2006.customanimator.ActivityManagerScreen
import com.riyadmondol2006.customanimator.ApkExtractorScreen
import com.riyadmondol2006.customanimator.ApkInstallerScreen
import com.riyadmondol2006.customanimator.AppThreadingScreen
import com.riyadmondol2006.customanimator.AutoActionsScreenContent
import com.riyadmondol2006.customanimator.AutoForceStopScreen
import com.riyadmondol2006.customanimator.AutoPermissionDisablerScreen
import com.riyadmondol2006.customanimator.BatteryScreenContent
import com.riyadmondol2006.customanimator.CloseAppsExclusionsScreen
import com.riyadmondol2006.customanimator.CompileBoosterScreen
import com.riyadmondol2006.customanimator.WhatsAppCleanerScreen
import com.riyadmondol2006.customanimator.OptimizerScreenContent
import com.riyadmondol2006.customanimator.CarrierNameScreen
import com.riyadmondol2006.customanimator.OtpCopierScreen
import com.riyadmondol2006.customanimator.DebloaterScreen
import com.riyadmondol2006.customanimator.StatusBarIconsScreen
import com.riyadmondol2006.customanimator.SystemColorsScreen
import com.riyadmondol2006.customanimator.DeveloperScreenContent
import com.riyadmondol2006.customanimator.DozeWhitelistScreen
import com.riyadmondol2006.customanimator.GameModeScreenContent
import com.riyadmondol2006.customanimator.GraphicsApiOverrideScreen
import com.riyadmondol2006.customanimator.HotspotManagerScreen
import com.riyadmondol2006.customanimator.HwuiTweaksScreen
import com.riyadmondol2006.customanimator.IconChangerScreen
import com.riyadmondol2006.customanimator.OnboardingScreen
import com.riyadmondol2006.customanimator.PerAppWidthScreen
import com.riyadmondol2006.customanimator.PermissionsScreen
import com.riyadmondol2006.customanimator.ProfileEditorScreen
import com.riyadmondol2006.customanimator.ProfilesScreen
import com.riyadmondol2006.customanimator.QsTilesScreen
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.ScreenshotActionsScreen
import com.riyadmondol2006.customanimator.SettingsScreen
import com.riyadmondol2006.customanimator.ShortcutsScreen
import com.riyadmondol2006.customanimator.SetupGuideScreen
import com.riyadmondol2006.customanimator.SoundTileScreen
import com.riyadmondol2006.customanimator.SystemMeterScreenContent
import com.riyadmondol2006.customanimator.TerminalScreenContent
import com.riyadmondol2006.customanimator.ToolsScreenContent
import com.riyadmondol2006.customanimator.WifiPasswordsScreen
import com.riyadmondol2006.customanimator.ui.LayoutHarness.assertFitsEveryScreenSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun screenFits(vararg allowedTruncations: Int, content: @Composable () -> Unit) {
        composeTestRule.assertFitsEveryScreenSize(
            allowedTruncations = allowedTruncations.map { context.getString(it) },
            content = content
        )
    }

    @Test
    fun hwuiTweaksScreenFitsSmallPhones() = screenFits(R.string.hwui_tweaks) {
        HwuiTweaksScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun appThreadingScreenFitsSmallPhones() = screenFits(R.string.app_threading) {
        AppThreadingScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun gameModeScreenFitsSmallPhones() = screenFits {
        GameModeScreenContent(hasShizukuPermission = false)
    }

    @Test
    fun dozeWhitelistScreenFitsSmallPhones() = screenFits(R.string.doze_whitelist) {
        DozeWhitelistScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun graphicsApiOverrideScreenFitsSmallPhones() = screenFits(R.string.graphics_api_override) {
        GraphicsApiOverrideScreen(
            onBack = {},
            hasShizukuPermission = false,
            hasWriteSecureSettings = false
        )
    }

    @Test
    fun autoActionsScreenFitsSmallPhones() = screenFits {
        AutoActionsScreenContent(
            hasShizukuPermission = false,
            onNavigateToAutoForceStop = {},
            onNavigateToAutoPermissionDisabler = {}
        )
    }

    @Test
    fun debloaterScreenFitsSmallPhones() = screenFits(R.string.debloater) {
        DebloaterScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun shortcutsScreenFitsSmallPhones() = screenFits(R.string.shortcuts) {
        ShortcutsScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun apkExtractorScreenFitsSmallPhones() = screenFits(R.string.apk_extractor) {
        ApkExtractorScreen(onBack = {})
    }

    @Test
    fun apkInstallerScreenFitsSmallPhones() = screenFits(R.string.apk_installer) {
        ApkInstallerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun activityManagerScreenFitsSmallPhones() = screenFits(R.string.activity_manager) {
        ActivityManagerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun iconChangerScreenFitsSmallPhones() = screenFits(R.string.icon_changer) {
        IconChangerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun autoForceStopScreenFitsSmallPhones() = screenFits(R.string.auto_force_stop) {
        AutoForceStopScreen(onBack = {}, isShizukuAvailable = false, hasShizukuPermission = false)
    }

    @Test
    fun autoPermissionDisablerScreenFitsSmallPhones() = screenFits(R.string.auto_permission_disabler) {
        AutoPermissionDisablerScreen(onBack = {}, isShizukuAvailable = false, hasShizukuPermission = false)
    }

    @Test
    fun batteryScreenFitsSmallPhones() = screenFits {
        BatteryScreenContent(
            hasShizukuPermission = false,
            onNavigateToDozeWhitelist = {},
            onNavigateToBatteryHealth = {}
        )
    }

    @Test
    fun developerScreenFitsSmallPhones() = screenFits {
        DeveloperScreenContent(hasShizukuPermission = false, hasWriteSecureSettings = false)
    }

    @Test
    fun systemMeterScreenFitsSmallPhones() = screenFits {
        SystemMeterScreenContent()
    }

    @Test
    fun toolsScreenFitsSmallPhones() = screenFits {
        ToolsScreenContent(
            hasShizukuPermission = false,
            hasWriteSecureSettings = false,
            onNavigateToGraphicsApiOverride = {},
            onNavigateToHwuiTweaks = {},
            onNavigateToAppThreading = {},
            onNavigateToRefreshRate = {},
            onNavigateToQsTiles = {},
            onNavigateToScreenshotActions = {},
            onNavigateToDebloater = {},
            onNavigateToCompileBooster = {},
            onNavigateToShortcuts = {},
            onNavigateToIconChanger = {},
            onNavigateToApkExtractor = {},
            onNavigateToApkInstaller = {},
            onNavigateToActivityManager = {},
            onNavigateToAutoActions = {},
            onNavigateToWifiPasswords = {},
            onNavigateToHotspotManager = {},
            onNavigateToAlarmRevealer = {},
            onNavigateToCarrierName = {},
            onNavigateToOtpCopier = {},
            onNavigateToStatusBarIcons = {},
            onNavigateToSystemColors = {},
            onNavigateToButtonRemapper = {}
        )
    }

    @Test
    fun terminalScreenFitsSmallPhones() = screenFits {
        TerminalScreenContent(
            hasShizukuPermission = false,
            command = TextFieldValue(""),
            onCommandChange = {},
            history = emptyList(),
            onHistoryChange = {}
        )
    }

    @Test
    fun perAppWidthScreenFitsSmallPhones() = screenFits(R.string.per_app_width) {
        PerAppWidthScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun wifiPasswordsScreenFitsSmallPhones() = screenFits(R.string.wifi_password_manager) {
        WifiPasswordsScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun hotspotManagerScreenFitsSmallPhones() = screenFits(R.string.hotspot_manager, R.string.hotspot_no_ssid) {
        HotspotManagerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun qsTilesScreenFitsSmallPhones() = screenFits(R.string.qs_tiles) {
        QsTilesScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun alarmRevealerScreenFitsSmallPhones() = screenFits(R.string.alarm_revealer) {
        AlarmRevealerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun carrierNameScreenFitsSmallPhones() = screenFits(R.string.carrier_name) {
        CarrierNameScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun otpCopierScreenFitsSmallPhones() = screenFits(R.string.otp_copier) {
        OtpCopierScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun statusBarIconsScreenFitsSmallPhones() = screenFits(R.string.status_bar_icons) {
        StatusBarIconsScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun systemColorsScreenFitsSmallPhones() = screenFits(R.string.system_colors) {
        SystemColorsScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun soundTileScreenFitsSmallPhones() = screenFits(R.string.sound_tile) {
        SoundTileScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun screenshotActionsScreenFitsSmallPhones() = screenFits(R.string.screenshot_actions) {
        ScreenshotActionsScreen(onBack = {})
    }

    @Test
    fun profilesScreenFitsSmallPhones() = screenFits(R.string.profiles_title) {
        ProfilesScreen(onBack = {}, onCreate = {}, onEdit = {}, refreshToken = 0)
    }

    @Test
    fun profileEditorScreenFitsSmallPhones() = screenFits {
        ProfileEditorScreen(profileId = null, onBack = {}, onSaved = {})
    }

    @Test
    fun permissionsScreenFitsSmallPhones() = screenFits(R.string.permissions_title) {
        PermissionsScreen(onBack = {}, isShizukuAvailable = false)
    }

    @Test
    fun setupGuideScreenFitsSmallPhones() = screenFits(R.string.setup_title) {
        SetupGuideScreen(onBack = {})
    }

    @Test
    fun settingsScreenFitsSmallPhones() = screenFits(R.string.settings) {
        SettingsScreen(
            onBack = {},
            isShizukuAvailable = false,
            hasShizukuPermission = false,
            hasWriteSecureSettings = false,
            onNavigateToPermissions = {}
        )
    }

    @Test
    fun onboardingScreenFitsSmallPhones() = screenFits {
        OnboardingScreen(onFinished = {})
    }

    @Test
    fun optimizerScreenFitsSmallPhones() = screenFits {
        OptimizerScreenContent(onNavigateToCloseAppsExclusions = {}, onNavigateToWhatsAppCleaner = {})
    }

    @Test
    fun whatsAppCleanerScreenFitsSmallPhones() = screenFits(R.string.whatsapp_cleaner) {
        WhatsAppCleanerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun compileBoosterScreenFitsSmallPhones() = screenFits(R.string.compile_booster) {
        CompileBoosterScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun closeAppsExclusionsScreenFitsSmallPhones() = screenFits(R.string.close_apps_exclusions) {
        CloseAppsExclusionsScreen(onBack = {})
    }

    @Test
    fun deviceUnderTestIsUsableForTheseChecks() {
        assert(context.resources.displayMetrics.widthPixels > 0)
    }
}
