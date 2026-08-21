package com.arslan.customanimator.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arslan.customanimator.AlarmRevealerScreen
import com.arslan.customanimator.AppThreadingScreen
import com.arslan.customanimator.AutoActionsScreenContent
import com.arslan.customanimator.AutoForceStopScreen
import com.arslan.customanimator.AutoPermissionDisablerScreen
import com.arslan.customanimator.BatteryScreenContent
import com.arslan.customanimator.BoostScreen
import com.arslan.customanimator.CarrierNameScreen
import com.arslan.customanimator.CleanerScreenContent
import com.arslan.customanimator.CloseAppsExclusionsScreen
import com.arslan.customanimator.CompileBoosterScreenContent
import com.arslan.customanimator.DeveloperScreenContent
import com.arslan.customanimator.DozeWhitelistScreen
import com.arslan.customanimator.GameModeScreen
import com.arslan.customanimator.GraphicsApiOverrideScreen
import com.arslan.customanimator.HotspotManagerScreen
import com.arslan.customanimator.HwuiTweaksScreen
import com.arslan.customanimator.OnboardingScreen
import com.arslan.customanimator.PerAppWidthScreen
import com.arslan.customanimator.PermissionsScreen
import com.arslan.customanimator.ProfileEditorScreen
import com.arslan.customanimator.ProfilesScreen
import com.arslan.customanimator.R
import com.arslan.customanimator.ScreenshotActionsScreen
import com.arslan.customanimator.SettingsScreen
import com.arslan.customanimator.SetupGuideScreen
import com.arslan.customanimator.SoundTileScreen
import com.arslan.customanimator.SystemMeterScreenContent
import com.arslan.customanimator.TerminalScreenContent
import com.arslan.customanimator.ToolsScreenContent
import com.arslan.customanimator.WifiPasswordsScreen
import com.arslan.customanimator.ui.LayoutHarness.assertFitsEveryScreenSize
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
    fun gameModeScreenFitsSmallPhones() = screenFits(R.string.game_mode) {
        GameModeScreen(onBack = {}, hasShizukuPermission = false)
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
    fun cleanerScreenFitsSmallPhones() = screenFits {
        CleanerScreenContent(
            hasShizukuPermission = false,
            onNavigateToCloseAppsExclusions = {}
        )
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
    fun closeAppsExclusionsScreenFitsSmallPhones() = screenFits(R.string.close_apps_exclusions) {
        CloseAppsExclusionsScreen(onBack = {})
    }

    @Test
    fun batteryScreenFitsSmallPhones() = screenFits {
        BatteryScreenContent(hasShizukuPermission = false, onNavigateToDozeWhitelist = {})
    }

    @Test
    fun developerScreenFitsSmallPhones() = screenFits {
        DeveloperScreenContent(hasShizukuPermission = false, hasWriteSecureSettings = false)
    }

    @Test
    fun compileBoosterScreenFitsSmallPhones() = screenFits {
        CompileBoosterScreenContent(hasShizukuPermission = false)
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
            onNavigateToScreenshotActions = {},
            onNavigateToSoundTile = {},
            onNavigateToWifiPasswords = {},
            onNavigateToHotspotManager = {},
            onNavigateToAlarmRevealer = {},
            onNavigateToCarrierName = {}
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
    fun alarmRevealerScreenFitsSmallPhones() = screenFits(R.string.alarm_revealer) {
        AlarmRevealerScreen(onBack = {}, hasShizukuPermission = false)
    }

    @Test
    fun carrierNameScreenFitsSmallPhones() = screenFits(R.string.carrier_name) {
        CarrierNameScreen(onBack = {}, hasShizukuPermission = false)
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
            isSimpleMode = false,
            onSimpleModeChange = {},
            inputMode = "slider",
            onInputModeChange = {},
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
    fun boostScreenFitsSmallPhones() = screenFits(R.string.boost_screen_title) {
        BoostScreen(onBack = {})
    }

    @Test
    fun deviceUnderTestIsUsableForTheseChecks() {
        assert(context.resources.displayMetrics.widthPixels > 0)
    }
}
