package com.arslan.customanimator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import com.arslan.customanimator.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import com.arslan.customanimator.notify.data.AppListManager
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.ui.AddEditRuleSection
import com.arslan.customanimator.notify.ui.CreatePatternSection
import com.arslan.customanimator.notify.ui.IgnoredNotificationsSection
import com.arslan.customanimator.notify.ui.LoggingSection
import com.arslan.customanimator.notify.ui.NotifyHomeSection
import com.arslan.customanimator.notify.ui.RulesSection
import com.arslan.customanimator.service.AutoForceStopService
import com.arslan.customanimator.ui.components.ExpressiveNavBar
import com.arslan.customanimator.ui.components.StatusPill
import com.arslan.customanimator.ui.components.StatusTone
import com.arslan.customanimator.ui.theme.MonoNumeralLarge
import com.arslan.customanimator.ui.components.NavBarItem
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import com.arslan.customanimator.ui.theme.horizontalPagerTransition
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.ChangelogManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.data.AnimatorPreset
import com.arslan.customanimator.data.WidthPreset
import com.arslan.customanimator.utils.AnimationTileSlots
import com.arslan.customanimator.utils.TerminalPresetManager
import com.arslan.customanimator.utils.TileNumberIcon
import com.arslan.customanimator.utils.WidthTileSlots
import com.arslan.customanimator.utils.TerminalTileSlots
import com.arslan.customanimator.utils.WidthPresetManager
import rikka.shizuku.Shizuku
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.arslan.customanimator.R

class MainActivity : ComponentActivity() {
    private val shizukuRequestListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == 0) {
            ShizukuHelper.grantWriteSecureSettingsPermission(this)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initBilling(this)
        initAds(this)

        AutoForceStopService.startIfSelectionExists(this)

        AppListManager.initialize(this)
        RulesManager(this).hasProximitySensor()

        TerminalTileSlots.sync(this, TerminalPresetManager(this))
        WidthTileSlots.sync(this, WidthPresetManager(this))
        AnimationTileSlots.sync(this, PresetManager(this))

        Shizuku.addRequestPermissionResultListener(shizukuRequestListener)
        
        if (ShizukuHelper.isShizukuAvailable() && !ShizukuHelper.hasShizukuBeenRequested(this) && !ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.requestShizukuPermission(this)
            ShizukuHelper.markShizukuRequested(this)
        }
        
        setContent {
            CustomAnimatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showOnboarding by rememberSaveable {
                        mutableStateOf(!SettingsManager.hasCompletedOnboarding(this))
                    }
                    var changelog by remember {
                        mutableStateOf(ChangelogManager.unseenReleases(this))
                    }
                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinished = {
                                SettingsManager.markOnboardingCompleted(this)
                                SettingsManager.markAdInfoDialogShown(this)
                                SettingsManager.markRateDialogLater(this)
                                ChangelogManager.markCurrentSeen(this)
                                changelog = emptyList()
                                showOnboarding = false
                            }
                        )
                    } else {
                        AnimatorSelectorScreen(this)

                        if (changelog.isNotEmpty()) {
                            ChangelogDialog(
                                releases = changelog,
                                onDismiss = {
                                    ChangelogManager.markCurrentSeen(this)
                                    changelog = emptyList()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        initBilling(this)
        preloadInterstitial(this)

        if (ShizukuHelper.hasShizukuPermission()) {
            val hasSecureSettings = ContextCompat.checkSelfPermission(
                this,
                "android.permission.WRITE_SECURE_SETTINGS"
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasSecureSettings) {
                ShizukuHelper.grantWriteSecureSettingsPermission(this)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuRequestListener)
    }
}

private const val WIDTH_REVERT_MS = 15_000L

enum class HomeTab {
    ANIMATION, WIDTH, BATTERY, DEVELOPER, TERMINAL, NOTIFY
}

enum class HomeScreen {
    MAIN, SETTINGS, PROFILES, PROFILE_EDITOR, AUTO_FORCE_STOP, AUTO_PERMISSION_DISABLER, GRAPHICS_API_OVERRIDE,
    CLOSE_APPS_EXCLUSIONS, WIFI_PASSWORDS, HOTSPOT_MANAGER, ALARM_REVEALER, CARRIER_NAME, SCREENSHOT_ACTIONS, SOUND_TILE, PER_APP_DPI, PERMISSIONS, SETUP_GUIDE,
    NOTIFY_RULES, NOTIFY_LOGGING, NOTIFY_IGNORED, NOTIFY_ADD_EDIT_RULE, NOTIFY_CREATE_PATTERN
}

private val NOTIFY_SCREENS = setOf(
    HomeScreen.NOTIFY_RULES,
    HomeScreen.NOTIFY_LOGGING,
    HomeScreen.NOTIFY_IGNORED,
    HomeScreen.NOTIFY_ADD_EDIT_RULE,
    HomeScreen.NOTIFY_CREATE_PATTERN
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatorSelectorScreen(activity: MainActivity) {
    val context = activity
    val contentResolver = context.contentResolver
    val presetManager = remember { PresetManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var isApplyingSettings by remember { mutableStateOf(false) }
    val widthPresetManager = remember { WidthPresetManager(context) }
    val focusManager = LocalFocusManager.current
    
    val isShizukuAvailable = remember { ShizukuHelper.isShizukuAvailable() }
    val hasShizukuPermission = remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
    val hasWriteSecureSettings = remember { mutableStateOf(ShizukuHelper.hasWriteSecureSettingsPermission(context)) }

    val permissionLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(permissionLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasShizukuPermission.value = ShizukuHelper.hasShizukuPermission()
                hasWriteSecureSettings.value = ShizukuHelper.hasWriteSecureSettingsPermission(context)
            }
        }
        permissionLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { permissionLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    var windowAnimScale by remember {
        mutableStateOf(SettingsManager.getWindowAnimationScale(contentResolver))
    }
    var transitionAnimScale by remember {
        mutableStateOf(SettingsManager.getTransitionAnimationScale(contentResolver))
    }
    var animatorDurScale by remember {
        mutableStateOf(SettingsManager.getAnimatorDurationScale(contentResolver))
    }
    
    var windowInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", windowAnimScale)) }
    var transitionInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", transitionAnimScale)) }
    var animatorInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", animatorDurScale)) }
    
    var presetName by remember { mutableStateOf("") }
    var allPresets by remember { mutableStateOf(presetManager.getAllPresets()) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var expandedPresetId by remember { mutableStateOf<String?>(null) }
    var currentScreen by rememberSaveable {
        mutableStateOf(
            SettingsManager.getLastScreen(context)?.let { saved ->
                HomeScreen.entries.firstOrNull { it.name == saved }
            } ?: HomeScreen.MAIN
        )
    }
    val setupGuideListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val openSetupGuide: () -> Unit = { currentScreen = HomeScreen.SETUP_GUIDE }
    val developerTabListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val autoForceStopListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val autoPermissionDisablerListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val graphicsApiOverrideListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val perAppDpiListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val closeAppsExclusionsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val wifiPasswordsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val alarmRevealerListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val hotspotManagerListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val carrierNameListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val screenshotActionsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val soundTileListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val permissionsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val profilesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var editingProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var profilesRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var editingRuleId by rememberSaveable { mutableStateOf<String?>(null) }
    var notifyRuleReturnScreen by rememberSaveable { mutableStateOf(HomeScreen.NOTIFY_RULES) }
    val terminalTabListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val batteryTabListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var terminalCommand by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var terminalHistory by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedTab by rememberSaveable {
        mutableStateOf(
            SettingsManager.getLastTab(context)?.let { saved ->
                HomeTab.entries.firstOrNull { it.name == saved }
            } ?: HomeTab.ANIMATION
        )
    }

    LaunchedEffect(currentScreen) {
        hasShizukuPermission.value = ShizukuHelper.hasShizukuPermission()
        hasWriteSecureSettings.value = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        if (currentScreen != HomeScreen.SETUP_GUIDE) {
            SettingsManager.setLastScreen(context, currentScreen.name)
        }
    }

    LaunchedEffect(selectedTab) {
        SettingsManager.setLastTab(context, selectedTab.name)
    }
    var widthPresetName by remember { mutableStateOf("") }
    var allWidthPresets by remember { mutableStateOf(widthPresetManager.getAllPresets()) }
    var showWidthPresetDialog by remember { mutableStateOf(false) }
    var widthTilePreset by remember { mutableStateOf<WidthPreset?>(null) }
    var widthRevertTarget by rememberSaveable { mutableIntStateOf(-1) }
    var widthRevertDeadline by rememberSaveable { mutableLongStateOf(0L) }
    var widthRevertNow by remember { mutableLongStateOf(0L) }
    var animationTilePreset by remember { mutableStateOf<AnimatorPreset?>(null) }
    var inputMode by remember { mutableStateOf(SettingsManager.getInputMode(context)) }
    var isSimpleMode by remember { mutableStateOf(SettingsManager.getSimpleMode(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionErrorMessage by remember { mutableStateOf("") }
    var showWriteSecureWidthConfirmDialog by remember { mutableStateOf(false) }
    var showAdInfoDialog by remember {
        mutableStateOf(!isAdFreeNow() && !SettingsManager.hasShownAdInfoDialog(context))
    }
    var showRateDialog by remember {
        mutableStateOf(
            SettingsManager.hasShownAdInfoDialog(context)
                && SettingsManager.shouldShowRateDialog(context)
        )
    }
    var showWriteSecureWidthUnsupportedDialog by remember { mutableStateOf(false) }
    
    var smallestWidth by remember { mutableStateOf(SettingsManager.getSmallestWidth(context)) }
    var smallestWidthInputValue by remember { mutableStateOf(if (smallestWidth > 0) smallestWidth.toString() else "") }
    
    var shouldShowContent by remember { mutableStateOf(true) }
    var pendingInputMode by remember { mutableStateOf<String?>(null) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (shouldShowContent) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "content fade animation"
    )

    val showPermissionError: (String) -> Unit = { message ->
        permissionErrorMessage = message
        showPermissionDialog = true
    }

    val showDefaultShizukuRecommendation = {
        showPermissionError(context.getString(R.string.shizuku_use_recommended))
    }

    val resetAnimationScalesToDefault = {
        windowAnimScale = 1.0f
        transitionAnimScale = 1.0f
        animatorDurScale = 1.0f
        windowInputValue = "1.00"
        transitionInputValue = "1.00"
        animatorInputValue = "1.00"
    }

    val showSmallestWidthSuccess: (SettingsManager.SmallestWidthResult, String) -> Unit = { result, successMessage ->
        if (result.usedWriteSecureFallback) {
            if (SettingsManager.shouldShowWriteSecureWidthConfirmDialog(context)) {
                showWriteSecureWidthConfirmDialog = true
            }
        } else {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val applyAnimationScalesAndHandleResult: (Float, Float, Float, () -> Unit) -> Unit = { windowScale, transitionScale, animatorScale, onSuccess ->
        isApplyingSettings = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Result.success(
                        SettingsManager.applyAllScales(
                            context,
                            contentResolver,
                            windowScale,
                            transitionScale,
                            animatorScale
                        )
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            isApplyingSettings = false
            result.fold(
                onSuccess = { success ->
                    if (success) {
                        onSuccess()
                        maybeShowInterstitial(context)
                    } else {
                        showDefaultShizukuRecommendation()
                    }
                },
                onFailure = { e -> showPermissionError(e.message ?: context.getString(R.string.unknown_error)) }
            )
        }
    }

    val syncSimpleModeValuesAfterApply: (Float, Float) -> Unit = { finalTransition, finalAnimator ->
        if (isSimpleMode) {
            transitionAnimScale = finalTransition
            animatorDurScale = finalAnimator
            transitionInputValue = String.format(java.util.Locale.US, "%.2f", finalTransition)
            animatorInputValue = String.format(java.util.Locale.US, "%.2f", finalAnimator)
        }
    }

    val applySelectedAnimationScales = {
        val finalTransition = if (isSimpleMode) windowAnimScale else transitionAnimScale
        val finalAnimator = if (isSimpleMode) windowAnimScale else animatorDurScale

        applyAnimationScalesAndHandleResult(
            windowAnimScale,
            finalTransition,
            finalAnimator
        ) {
            syncSimpleModeValuesAfterApply(finalTransition, finalAnimator)
            Toast.makeText(
                context,
                context.getString(R.string.animation_scales_updated),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    var isFirstModeRender by remember { mutableStateOf(true) }
    LaunchedEffect(inputMode) {
        if (isFirstModeRender) {
            isFirstModeRender = false
        } else {
            shouldShowContent = false
            delay(150)
            shouldShowContent = true
        }
    }
    
    
    val toggleSimpleMode: (Boolean) -> Unit = { newSimpleMode ->
        isSimpleMode = newSimpleMode
        SettingsManager.setSimpleMode(context, isSimpleMode)
        if (isSimpleMode) {
            transitionAnimScale = windowAnimScale
            animatorDurScale = windowAnimScale
            transitionInputValue = windowInputValue
            animatorInputValue = windowInputValue
        }
    }

    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    val notifyBack: () -> Unit = {
        when (currentScreen) {
            HomeScreen.NOTIFY_IGNORED -> currentScreen = HomeScreen.NOTIFY_LOGGING
            HomeScreen.NOTIFY_ADD_EDIT_RULE -> currentScreen = notifyRuleReturnScreen
            HomeScreen.NOTIFY_CREATE_PATTERN -> currentScreen = HomeScreen.NOTIFY_ADD_EDIT_RULE
            else -> {
                currentScreen = HomeScreen.MAIN
                selectedTab = HomeTab.NOTIFY
            }
        }
    }

    BackHandler(
        enabled = currentScreen != HomeScreen.MAIN &&
            currentScreen != HomeScreen.SETTINGS &&
            currentScreen != HomeScreen.PERMISSIONS &&
            currentScreen != HomeScreen.SETUP_GUIDE &&
            currentScreen != HomeScreen.PROFILES &&
            currentScreen != HomeScreen.PROFILE_EDITOR &&
            currentScreen != HomeScreen.PER_APP_DPI &&
            currentScreen !in NOTIFY_SCREENS
    ) {
        currentScreen = HomeScreen.MAIN
        selectedTab = HomeTab.DEVELOPER
    }
    BackHandler(enabled = currentScreen == HomeScreen.PER_APP_DPI) {
        currentScreen = HomeScreen.MAIN
        selectedTab = HomeTab.WIDTH
    }
    BackHandler(enabled = currentScreen == HomeScreen.PROFILES) {
        currentScreen = HomeScreen.MAIN
    }
    BackHandler(enabled = currentScreen == HomeScreen.PROFILE_EDITOR) {
        currentScreen = HomeScreen.PROFILES
    }
    BackHandler(enabled = currentScreen == HomeScreen.SETUP_GUIDE) {
        currentScreen = HomeScreen.MAIN
    }
    BackHandler(enabled = currentScreen == HomeScreen.SETTINGS) {
        currentScreen = HomeScreen.MAIN
    }
    BackHandler(enabled = currentScreen == HomeScreen.PERMISSIONS) {
        currentScreen = HomeScreen.SETTINGS
    }
    BackHandler(enabled = currentScreen in NOTIFY_SCREENS) { notifyBack() }
    BackHandler(enabled = currentScreen == HomeScreen.MAIN) {
        if (backPressedOnce) {
            activity.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalOpenSetupGuide provides openSetupGuide) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
            } else {
                (slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300)))
            }
        },
        label = "screen transition"
    ) { targetScreen ->
    if (targetScreen == HomeScreen.SETTINGS) {
        SettingsScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            isSimpleMode = isSimpleMode,
            onSimpleModeChange = toggleSimpleMode,
            inputMode = inputMode,
            onInputModeChange = { newMode ->
                if (newMode != inputMode) {
                    inputMode = newMode
                    SettingsManager.setInputMode(context, inputMode)
                    if (inputMode == "manual") {
                        windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                        transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                        animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                    }
                }
            },
            isShizukuAvailable = isShizukuAvailable,
            hasShizukuPermission = hasShizukuPermission.value,
            hasWriteSecureSettings = hasWriteSecureSettings.value,
            onNavigateToPermissions = { currentScreen = HomeScreen.PERMISSIONS }
        )
    } else if (targetScreen == HomeScreen.PROFILES) {
        ProfilesScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            onCreate = {
                editingProfileId = null
                currentScreen = HomeScreen.PROFILE_EDITOR
            },
            onEdit = { id ->
                editingProfileId = id
                currentScreen = HomeScreen.PROFILE_EDITOR
            },
            refreshToken = profilesRefreshToken,
            listState = profilesListState
        )
    } else if (targetScreen == HomeScreen.PROFILE_EDITOR) {
        ProfileEditorScreen(
            profileId = editingProfileId,
            onBack = { currentScreen = HomeScreen.PROFILES },
            onSaved = {
                profilesRefreshToken++
                currentScreen = HomeScreen.PROFILES
            }
        )
    } else if (targetScreen == HomeScreen.AUTO_FORCE_STOP) {
        AutoForceStopScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            isShizukuAvailable = isShizukuAvailable,
            hasShizukuPermission = hasShizukuPermission.value,
            listState = autoForceStopListState
        )
    } else if (targetScreen == HomeScreen.AUTO_PERMISSION_DISABLER) {
        AutoPermissionDisablerScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            isShizukuAvailable = isShizukuAvailable,
            hasShizukuPermission = hasShizukuPermission.value,
            listState = autoPermissionDisablerListState
        )
    } else if (targetScreen == HomeScreen.GRAPHICS_API_OVERRIDE) {
        GraphicsApiOverrideScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            hasWriteSecureSettings = hasWriteSecureSettings.value,
            listState = graphicsApiOverrideListState
        )
    } else if (targetScreen == HomeScreen.PER_APP_DPI) {
        PerAppDpiScreen(
            onBack = {
                currentScreen = HomeScreen.MAIN
                selectedTab = HomeTab.WIDTH
            },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = perAppDpiListState
        )
    } else if (targetScreen == HomeScreen.WIFI_PASSWORDS) {
        WifiPasswordsScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = wifiPasswordsListState
        )
    } else if (targetScreen == HomeScreen.HOTSPOT_MANAGER) {
        HotspotManagerScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = hotspotManagerListState
        )
    } else if (targetScreen == HomeScreen.ALARM_REVEALER) {
        AlarmRevealerScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = alarmRevealerListState
        )
    } else if (targetScreen == HomeScreen.CARRIER_NAME) {
        CarrierNameScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = carrierNameListState
        )
    } else if (targetScreen == HomeScreen.SCREENSHOT_ACTIONS) {
        ScreenshotActionsScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            listState = screenshotActionsListState
        )
    } else if (targetScreen == HomeScreen.SOUND_TILE) {
        SoundTileScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            hasShizukuPermission = hasShizukuPermission.value,
            listState = soundTileListState
        )
    } else if (targetScreen == HomeScreen.SETUP_GUIDE) {
        SetupGuideScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            listState = setupGuideListState
        )
    } else if (targetScreen == HomeScreen.PERMISSIONS) {
        PermissionsScreen(
            onBack = { currentScreen = HomeScreen.SETTINGS },
            isShizukuAvailable = isShizukuAvailable,
            listState = permissionsListState
        )
    } else if (targetScreen == HomeScreen.NOTIFY_RULES) {
        RulesSection(
            onNavigateBack = notifyBack,
            onNavigateToAddEditRule = { ruleId ->
                editingRuleId = ruleId
                notifyRuleReturnScreen = HomeScreen.NOTIFY_RULES
                currentScreen = HomeScreen.NOTIFY_ADD_EDIT_RULE
            }
        )
    } else if (targetScreen == HomeScreen.NOTIFY_LOGGING) {
        LoggingSection(
            onNavigateBack = notifyBack,
            onNavigateToIgnored = { currentScreen = HomeScreen.NOTIFY_IGNORED },
            onNavigateToAddEditRule = {
                editingRuleId = null
                notifyRuleReturnScreen = HomeScreen.NOTIFY_LOGGING
                currentScreen = HomeScreen.NOTIFY_ADD_EDIT_RULE
            }
        )
    } else if (targetScreen == HomeScreen.NOTIFY_IGNORED) {
        IgnoredNotificationsSection(onNavigateBack = notifyBack)
    } else if (targetScreen == HomeScreen.NOTIFY_ADD_EDIT_RULE) {
        AddEditRuleSection(
            ruleId = editingRuleId,
            onNavigateBack = notifyBack,
            onNavigateToCreatePattern = { currentScreen = HomeScreen.NOTIFY_CREATE_PATTERN }
        )
    } else if (targetScreen == HomeScreen.NOTIFY_CREATE_PATTERN) {
        CreatePatternSection(onNavigateBack = notifyBack)
    } else if (targetScreen == HomeScreen.CLOSE_APPS_EXCLUSIONS) {
        CloseAppsExclusionsScreen(
            onBack = { currentScreen = HomeScreen.MAIN },
            listState = closeAppsExclusionsListState
        )
    } else {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                actions = {
                    if (selectedTab == HomeTab.ANIMATION || selectedTab == HomeTab.WIDTH) {
                        IconButton(
                            onClick = {
                                if (selectedTab == HomeTab.ANIMATION) {
                                    showPresetDialog = true
                                } else {
                                    showWidthPresetDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_preset)
                            )
                        }
                    }
                    if (selectedTab == HomeTab.NOTIFY) {
                        IconButton(onClick = { currentScreen = HomeScreen.NOTIFY_LOGGING }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = stringResource(R.string.pn_nav_logging)
                            )
                        }
                    }
                    IconButton(onClick = {
                        profilesRefreshToken++
                        currentScreen = HomeScreen.PROFILES
                    }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.profiles_title)
                        )
                    }
                    IconButton(onClick = { currentScreen = HomeScreen.SETTINGS }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                RemoveAdsPrompt()
                BannerAdView(applyNavigationBarPadding = false)
                ExpressiveNavBar(
                    items = listOf(
                        NavBarItem(
                            icon = Icons.Default.PlayArrow,
                            label = stringResource(R.string.nav_animation),
                            selected = selectedTab == HomeTab.ANIMATION,
                            onClick = { selectedTab = HomeTab.ANIMATION },
                            contentDescription = stringResource(R.string.animation_scale_slider)
                        ),
                        NavBarItem(
                            icon = Icons.Default.Straighten,
                            label = stringResource(R.string.nav_width),
                            selected = selectedTab == HomeTab.WIDTH,
                            onClick = { selectedTab = HomeTab.WIDTH }
                        ),
                        NavBarItem(
                            icon = Icons.Default.BatterySaver,
                            label = stringResource(R.string.nav_battery),
                            selected = selectedTab == HomeTab.BATTERY,
                            onClick = { selectedTab = HomeTab.BATTERY }
                        ),
                        NavBarItem(
                            icon = Icons.Default.DeveloperMode,
                            label = stringResource(R.string.nav_developer),
                            selected = selectedTab == HomeTab.DEVELOPER,
                            onClick = { selectedTab = HomeTab.DEVELOPER }
                        ),
                        NavBarItem(
                            icon = Icons.Default.Terminal,
                            label = stringResource(R.string.nav_terminal),
                            selected = selectedTab == HomeTab.TERMINAL,
                            onClick = { selectedTab = HomeTab.TERMINAL }
                        ),
                        NavBarItem(
                            icon = Icons.Default.Notifications,
                            label = stringResource(R.string.nav_notify),
                            selected = selectedTab == HomeTab.NOTIFY,
                            onClick = { selectedTab = HomeTab.NOTIFY },
                            contentDescription = stringResource(R.string.pn_title)
                        )
                    )
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                horizontalPagerTransition(targetState.ordinal > initialState.ordinal)
            },
            modifier = Modifier.padding(paddingValues),
            label = "tab transition"
        ) { targetTab ->
        if (targetTab == HomeTab.NOTIFY) {
        NotifyHomeSection(
            onNavigateToRules = { currentScreen = HomeScreen.NOTIFY_RULES },
            onNavigateToPermissions = { currentScreen = HomeScreen.PERMISSIONS }
        )
        } else if (targetTab == HomeTab.DEVELOPER) {
        DeveloperScreenContent(
            hasShizukuPermission = hasShizukuPermission.value,
            hasWriteSecureSettings = hasWriteSecureSettings.value,
            onNavigateToAutoForceStop = { currentScreen = HomeScreen.AUTO_FORCE_STOP },
            onNavigateToAutoPermissionDisabler = { currentScreen = HomeScreen.AUTO_PERMISSION_DISABLER },
            onNavigateToGraphicsApiOverride = { currentScreen = HomeScreen.GRAPHICS_API_OVERRIDE },
            onNavigateToCloseAppsExclusions = { currentScreen = HomeScreen.CLOSE_APPS_EXCLUSIONS },
            onNavigateToWifiPasswords = { currentScreen = HomeScreen.WIFI_PASSWORDS },
            onNavigateToHotspotManager = { currentScreen = HomeScreen.HOTSPOT_MANAGER },
            onNavigateToAlarmRevealer = { currentScreen = HomeScreen.ALARM_REVEALER },
            onNavigateToCarrierName = { currentScreen = HomeScreen.CARRIER_NAME },
            onNavigateToScreenshotActions = { currentScreen = HomeScreen.SCREENSHOT_ACTIONS },
            onNavigateToSoundTile = { currentScreen = HomeScreen.SOUND_TILE },
            listState = developerTabListState
        )
        } else if (targetTab == HomeTab.BATTERY) {
        BatteryScreenContent(
            hasShizukuPermission = hasShizukuPermission.value,
            listState = batteryTabListState
        )
        } else if (targetTab == HomeTab.TERMINAL) {
        TerminalScreenContent(
            hasShizukuPermission = hasShizukuPermission.value,
            listState = terminalTabListState,
            command = terminalCommand,
            onCommandChange = { terminalCommand = it },
            history = terminalHistory,
            onHistoryChange = { terminalHistory = it },
            isActive = targetScreen == currentScreen && targetTab == selectedTab
        )
        } else if (targetTab == HomeTab.WIDTH) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasWriteSecureSettings.value) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.setup_nudge_home),
                        onOpenSetup = openSetupGuide
                    )
                }
            }

            item {
                Card(
                    shape = AppShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.smallest_width),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.smallest_width_current,
                                        SettingsManager.getSmallestWidth(context)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    isApplyingSettings = true
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            SettingsManager.setSmallestWidth(contentResolver, context, 0)
                                        }
                                        isApplyingSettings = false
                                        if (result.success) {
                                            smallestWidth = SettingsManager.getSmallestWidth(context)
                                            smallestWidthInputValue = ""
                                            showSmallestWidthSuccess(result, context.getString(R.string.smallest_width_reset))
                                        } else {
                                            showDefaultShizukuRecommendation()
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.smallest_width_reset)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = smallestWidthInputValue,
                                onValueChange = { newValue ->
                                    smallestWidthInputValue = newValue
                                    val intVal = newValue.toIntOrNull()
                                    if (intVal != null && intVal in 320..1024) {
                                        smallestWidth = intVal
                                    }
                                },
                                label = { Text(stringResource(R.string.dp_short)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Button(
                                onClick = {
                                    val targetSmallestWidth = smallestWidthInputValue.toIntOrNull()
                                    if (targetSmallestWidth == null || targetSmallestWidth !in 320..1024) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.smallest_width_range),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    isApplyingSettings = true
                                    val previousSmallestWidth =
                                        SettingsManager.getSmallestWidth(context)
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            SettingsManager.setSmallestWidth(contentResolver, context, targetSmallestWidth)
                                        }
                                        isApplyingSettings = false
                                        if (result.success) {
                                            if (!result.usedWriteSecureFallback &&
                                                previousSmallestWidth != targetSmallestWidth
                                            ) {
                                                widthRevertTarget = previousSmallestWidth
                                                widthRevertDeadline =
                                                    System.currentTimeMillis() + WIDTH_REVERT_MS
                                            }
                                            smallestWidth = targetSmallestWidth
                                            smallestWidthInputValue = targetSmallestWidth.toString()
                                            showSmallestWidthSuccess(
                                                result,
                                                context.getString(R.string.smallest_width_applied, targetSmallestWidth)
                                            )
                                        } else {
                                            showDefaultShizukuRecommendation()
                                        }
                                    }
                                },
                                enabled = !isApplyingSettings,
                                modifier = Modifier.heightIn(min = 56.dp)
                            ) {
                                Text(
                                    stringResource(R.string.apply_settings),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = AppShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    onClick = { currentScreen = HomeScreen.PER_APP_DPI },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.per_app_dpi),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.per_app_dpi_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(onClick = { currentScreen = HomeScreen.PER_APP_DPI }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.per_app_dpi_button)
                            )
                        }
                    }
                }
            }

            if (allWidthPresets.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.width_presets),
                        modifier = Modifier.graphicsLayer(alpha = contentAlpha),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (allWidthPresets.isNotEmpty()) {
                items(allWidthPresets) { widthPreset ->
                    Card(
                        shape = AppShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = contentAlpha)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        widthPreset.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    widthPreset.tile?.let {
                                        Spacer(Modifier.width(6.dp))
                                        PresetTileBadge(TileNumberIcon.widthText(widthPreset.widthDp))
                                    }
                                }
                                Text(
                                    stringResource(R.string.preset_width_value, widthPreset.widthDp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { widthTilePreset = widthPreset }) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = stringResource(R.string.terminal_tile_menu)
                                )
                            }
                            IconButton(
                                onClick = {
                                    widthPresetManager.deletePreset(widthPreset.id)
                                    allWidthPresets = widthPresetManager.getAllPresets()
                                    Toast.makeText(context, context.getString(R.string.width_preset_deleted), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = {
                                    isApplyingSettings = true
                                    val previousSmallestWidth =
                                        SettingsManager.getSmallestWidth(context)
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            SettingsManager.setSmallestWidth(contentResolver, context, widthPreset.widthDp)
                                        }
                                        isApplyingSettings = false
                                        if (result.success) {
                                            if (!result.usedWriteSecureFallback &&
                                                previousSmallestWidth != widthPreset.widthDp
                                            ) {
                                                widthRevertTarget = previousSmallestWidth
                                                widthRevertDeadline =
                                                    System.currentTimeMillis() + WIDTH_REVERT_MS
                                            }
                                            smallestWidth = widthPreset.widthDp
                                            smallestWidthInputValue = widthPreset.widthDp.toString()
                                            if (result.usedWriteSecureFallback) {
                                                if (SettingsManager.shouldShowWriteSecureWidthConfirmDialog(context)) {
                                                    showWriteSecureWidthConfirmDialog = true
                                                }
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.width_preset_loaded_applied), Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            showDefaultShizukuRecommendation()
                                        }
                                    }
                                },
                                enabled = !isApplyingSettings,
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Text(stringResource(R.string.load), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.no_width_presets_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(16.dp)
                            .graphicsLayer(alpha = contentAlpha)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasWriteSecureSettings.value) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.setup_nudge_home),
                        onOpenSetup = openSetupGuide
                    )
                }
            }

            item {
                SyncedAnimationPreview(
                    currentScale = windowAnimScale,
                    modifier = Modifier.graphicsLayer(alpha = contentAlpha)
                )
            }
            
            if (inputMode == "slider") {
                item {
                    Card(
                        shape = AppShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.animation_scale_slider),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        resetAnimationScalesToDefault()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.restore_to_default)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        val sliderLabel = if (isSimpleMode) stringResource(R.string.animation_scale_applies_to_all) else stringResource(R.string.window_animation_scale)
                        Text(
                            stringResource(
                                R.string.labeled_value,
                                sliderLabel,
                                String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale - 0.01f).coerceAtLeast(0f)
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = windowAnimScale
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.minus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                            Slider(
                                value = windowAnimScale,
                                onValueChange = { 
                                    windowAnimScale = it
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                    if (isSimpleMode) {
                                        transitionAnimScale = it
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = it
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale + 0.01f).coerceAtMost(5f)
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = windowAnimScale
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.plus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                        }
                        if (!isSimpleMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            stringResource(
                                R.string.labeled_value,
                                stringResource(R.string.transition_animation_scale),
                                String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale - 0.01f).coerceAtLeast(0f)
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.minus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                            Slider(
                                value = transitionAnimScale,
                                onValueChange = { 
                                    transitionAnimScale = it
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale + 0.01f).coerceAtMost(5f)
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.plus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            stringResource(
                                R.string.labeled_value,
                                stringResource(R.string.animator_duration_scale),
                                String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale - 0.01f).coerceAtLeast(0f)
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.minus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                            Slider(
                                value = animatorDurScale,
                                onValueChange = { 
                                    animatorDurScale = it
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale + 0.01f).coerceAtMost(5f)
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.plus_symbol), style = MaterialTheme.typography.titleLarge,)
                            }
                        }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    applySelectedAnimationScales()
                                },
                                enabled = !isApplyingSettings,
                                modifier = Modifier.heightIn(min = 50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    stringResource(R.string.apply_settings),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                }
            }
            
            if (inputMode == "manual") {
                item {
                    Card(
                        shape = AppShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    focusManager.clearFocus()
                                }
                            )
                        }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.animation_header),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        resetAnimationScalesToDefault()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.restore_to_default)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        val inputLabel = if (isSimpleMode) stringResource(R.string.animation_scale_applies_to_all) else stringResource(R.string.window_animation)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = windowInputValue,
                                onValueChange = {
                                    windowInputValue = it
                                    val floatVal = it.replace(',', '.').toFloatOrNull()
                                    if (floatVal != null && floatVal in 0f..5.0f) {
                                        windowAnimScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                        if (isSimpleMode) {
                                            transitionAnimScale = windowAnimScale
                                            animatorDurScale = windowAnimScale
                                        }
                                    }
                                    if (isSimpleMode) {
                                        transitionInputValue = it
                                        animatorInputValue = it
                                    }
                                },
                                label = { Text(inputLabel) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Button(
                                onClick = {
                                    applySelectedAnimationScales()
                                },
                                enabled = !isApplyingSettings,
                                modifier = Modifier.heightIn(min = 56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    stringResource(R.string.apply_settings),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!isSimpleMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = transitionInputValue,
                            onValueChange = { 
                                transitionInputValue = it
                                val floatVal = it.replace(',', '.').toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    transitionAnimScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                }
                            },
                            label = { Text(stringResource(R.string.transition_animation)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = animatorInputValue,
                            onValueChange = { 
                                animatorInputValue = it
                                val floatVal = it.replace(',', '.').toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    animatorDurScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                }
                            },
                            label = { Text(stringResource(R.string.animator_duration)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        }
                    }
                }
                }
            }
            
            if (allPresets.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.saved_presets),
                        modifier = Modifier.graphicsLayer(alpha = contentAlpha),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (allPresets.isNotEmpty()) {
                items(allPresets) { preset ->
                    Card(
                        shape = AppShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = contentAlpha)
                            .background(
                                if (expandedPresetId == preset.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            preset.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        preset.tile?.let {
                                            Spacer(Modifier.width(6.dp))
                                            PresetTileBadge(
                                                TileNumberIcon.animationText(
                                                    preset.windowAnimationScale,
                                                    preset.transitionAnimationScale,
                                                    preset.animatorDurationScale
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        stringResource(R.string.preset_window_animation_value, preset.windowAnimationScale),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(R.string.preset_transition_animation_value, preset.transitionAnimationScale),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(R.string.preset_animator_duration_value, preset.animatorDurationScale),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { animationTilePreset = preset }) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = stringResource(R.string.terminal_tile_menu)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        presetManager.deletePreset(preset.id)
                                        allPresets = presetManager.getAllPresets()
                                        Toast.makeText(context, context.getString(R.string.preset_deleted), Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Column(
                                    modifier = Modifier.widthIn(min = 88.dp, max = 132.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(
                                        onClick = {
                                            windowAnimScale = preset.windowAnimationScale
                                            transitionAnimScale = preset.transitionAnimationScale
                                            animatorDurScale = preset.animatorDurationScale
                                            windowInputValue = String.format(java.util.Locale.US, "%.2f", preset.windowAnimationScale)
                                            transitionInputValue = String.format(java.util.Locale.US, "%.2f", preset.transitionAnimationScale)
                                            animatorInputValue = String.format(java.util.Locale.US, "%.2f", preset.animatorDurationScale)
                                            isApplyingSettings = true
                                            coroutineScope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    try {
                                                        Result.success(
                                                            SettingsManager.applyAllScales(
                                                                context,
                                                                contentResolver,
                                                                preset.windowAnimationScale,
                                                                preset.transitionAnimationScale,
                                                                preset.animatorDurationScale
                                                            )
                                                        )
                                                    } catch (e: Exception) {
                                                        Result.failure(e)
                                                    }
                                                }
                                                isApplyingSettings = false
                                                result.fold(
                                                    onSuccess = { success ->
                                                        if (success) {
                                                            Toast.makeText(context, context.getString(R.string.preset_loaded_applied), Toast.LENGTH_SHORT).show()
                                                            maybeShowInterstitial(context)
                                                        } else {
                                                            showDefaultShizukuRecommendation()
                                                        }
                                                    },
                                                    onFailure = { e ->
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.error_applying_preset, e.message),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !isApplyingSettings,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                    ) {
                                        Text(stringResource(R.string.load), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.no_presets_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(16.dp)
                            .graphicsLayer(alpha = contentAlpha)
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        }
    }
    }
    }
    }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(R.string.write_secure_settings_permission),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        stringResource(R.string.permission_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        stringResource(R.string.one_time_only),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        stringResource(R.string.one_time_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (isShizukuAvailable && hasShizukuPermission.value) {
                        Text(
                            stringResource(R.string.shizuku_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            stringResource(R.string.shizuku_ready_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else if (isShizukuAvailable && !hasShizukuPermission.value) {
                        Text(
                            stringResource(R.string.shizuku_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            stringResource(R.string.shizuku_available_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    if (permissionErrorMessage.isNotEmpty()) {
                        Text(
                            stringResource(R.string.error, permissionErrorMessage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    if (!hasWriteSecureSettings.value) {
                        Text(
                            stringResource(R.string.use_adb_command),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            stringResource(R.string.adb_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SelectionContainer {
                            Text(
                                stringResource(R.string.adb_command),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                        
                        Text(
                            stringResource(R.string.see_permission_details),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText(
                            context.getString(R.string.adb_command_title),
                            context.getString(R.string.adb_command)
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.copy_command))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPermissionDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showWriteSecureWidthConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWriteSecureWidthConfirmDialog = false },
            title = { Text(stringResource(R.string.write_secure_applied_title)) },
            text = { Text(stringResource(R.string.write_secure_applied_question)) },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.setSkipWriteSecureWidthConfirmDialog(context, true)
                        showWriteSecureWidthConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showWriteSecureWidthConfirmDialog = false
                        showWriteSecureWidthUnsupportedDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (showAdInfoDialog) {
        AlertDialog(
            onDismissRequest = {
                SettingsManager.markAdInfoDialogShown(context)
                showAdInfoDialog = false
            },
            title = { Text(stringResource(R.string.ad_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ad_info_message))
                    Text(
                        text = stringResource(R.string.ad_info_remove_ads_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.markAdInfoDialogShown(context)
                        showAdInfoDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ad_info_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        SettingsManager.markAdInfoDialogShown(context)
                        showAdInfoDialog = false
                        startRemoveAdsPurchase(context)
                    }
                ) {
                    Text(stringResource(R.string.remove_ads))
                }
            }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = {
                SettingsManager.markRateDialogLater(context)
                showRateDialog = false
            },
            title = { Text(stringResource(R.string.rate_dialog_title)) },
            text = { Text(stringResource(R.string.rate_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.markRateDialogRated(context)
                        showRateDialog = false
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.rate_dialog_rate))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        SettingsManager.markRateDialogLater(context)
                        showRateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.rate_dialog_later))
                }
            }
        )
    }

    if (showWriteSecureWidthUnsupportedDialog) {
        AlertDialog(
            onDismissRequest = { showWriteSecureWidthUnsupportedDialog = false },
            title = { Text(stringResource(R.string.information)) },
            text = { Text(stringResource(R.string.write_secure_not_supported_message)) },
            confirmButton = {
                Button(
                    onClick = { showWriteSecureWidthUnsupportedDialog = false }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.create_new_preset_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text(stringResource(R.string.preset_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.current_values_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.preset_values, windowAnimScale, transitionAnimScale, animatorDurScale),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            presetManager.savePreset(
                                presetName,
                                windowAnimScale,
                                transitionAnimScale,
                                animatorDurScale
                            )
                            allPresets = presetManager.getAllPresets()
                            presetName = ""
                            showPresetDialog = false
                            Toast.makeText(context, context.getString(R.string.preset_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.enter_preset_name), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPresetDialog = false
                        presetName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(widthRevertDeadline) {
        if (widthRevertDeadline > 0L) {
            while (System.currentTimeMillis() < widthRevertDeadline) {
                widthRevertNow = System.currentTimeMillis()
                kotlinx.coroutines.delay(250)
            }
            val target = widthRevertTarget
            widthRevertDeadline = 0L
            widthRevertTarget = -1
            if (target >= 0) {
                withContext(Dispatchers.IO) {
                    SettingsManager.setSmallestWidth(contentResolver, context, target)
                }
                smallestWidth = SettingsManager.getSmallestWidth(context)
                smallestWidthInputValue = smallestWidth.toString()
            }
        }
    }

    if (widthRevertDeadline > 0L) {
        val secondsLeft = ((widthRevertDeadline - widthRevertNow).coerceAtLeast(0L) / 1000L).toInt() + 1
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.width_revert_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.width_revert_body,
                        widthRevertTarget,
                        secondsLeft
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    widthRevertDeadline = 0L
                    widthRevertTarget = -1
                }) { Text(stringResource(R.string.width_revert_keep)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val target = widthRevertTarget
                    widthRevertDeadline = 0L
                    widthRevertTarget = -1
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            SettingsManager.setSmallestWidth(contentResolver, context, target)
                        }
                        smallestWidth = SettingsManager.getSmallestWidth(context)
                        smallestWidthInputValue = smallestWidth.toString()
                    }
                }) { Text(stringResource(R.string.width_revert_now)) }
            }
        )
    }

    widthTilePreset?.let { preset ->
        PresetTileDialog(
            presetName = preset.name,
            numberText = TileNumberIcon.widthText(preset.widthDp),
            existing = preset.tile,
            freeSlot = widthPresetManager.firstFreeSlot(excludingPresetId = preset.id),
            canRequestAdd = WidthTileSlots.canRequestAdd(),
            onDismiss = { widthTilePreset = null },
            onSave = { config ->
                widthPresetManager.setTileConfig(preset.id, config)
                allWidthPresets = widthPresetManager.getAllPresets()
                widthTilePreset = null
            },
            onSaveAndAdd = { config ->
                widthPresetManager.setTileConfig(preset.id, config)
                allWidthPresets = widthPresetManager.getAllPresets()
                WidthTileSlots.requestAddTile(
                    context,
                    config.slot,
                    config.label.ifBlank { preset.name },
                    TileNumberIcon.create(TileNumberIcon.widthText(preset.widthDp))
                )
                widthTilePreset = null
            }
        )
    }

    animationTilePreset?.let { preset ->
        val numberText = TileNumberIcon.animationText(
            preset.windowAnimationScale,
            preset.transitionAnimationScale,
            preset.animatorDurationScale
        )
        PresetTileDialog(
            presetName = preset.name,
            numberText = numberText,
            existing = preset.tile,
            freeSlot = presetManager.firstFreeSlot(excludingPresetId = preset.id),
            canRequestAdd = AnimationTileSlots.canRequestAdd(),
            onDismiss = { animationTilePreset = null },
            onSave = { config ->
                presetManager.setTileConfig(preset.id, config)
                allPresets = presetManager.getAllPresets()
                animationTilePreset = null
            },
            onSaveAndAdd = { config ->
                presetManager.setTileConfig(preset.id, config)
                allPresets = presetManager.getAllPresets()
                AnimationTileSlots.requestAddTile(
                    context,
                    config.slot,
                    config.label.ifBlank { preset.name },
                    TileNumberIcon.create(numberText)
                )
                animationTilePreset = null
            }
        )
    }

    if (showWidthPresetDialog) {
        AlertDialog(
            onDismissRequest = { showWidthPresetDialog = false },
            title = { Text(stringResource(R.string.create_new_width_preset_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = widthPresetName,
                        onValueChange = { widthPresetName = it },
                        label = { Text(stringResource(R.string.preset_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.current_width_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.preset_width_value, smallestWidth),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (widthPresetName.isNotBlank()) {
                            widthPresetManager.savePreset(widthPresetName, smallestWidth)
                            allWidthPresets = widthPresetManager.getAllPresets()
                            widthPresetName = ""
                            showWidthPresetDialog = false
                            Toast.makeText(context, context.getString(R.string.width_preset_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.enter_preset_name), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showWidthPresetDialog = false
                        widthPresetName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SyncedAnimationPreview(
    currentScale: Float,
    modifier: Modifier = Modifier
) {
    val baseSlideMs = 300f
    val pauseMs = 600f
    val restMs = 400f

    val slideIn1x = baseSlideMs
    val slideInCurrent = if (currentScale <= 0f) 0f else baseSlideMs * (currentScale * currentScale)

    val slideOut1x = baseSlideMs
    val slideOutCurrent = if (currentScale <= 0f) 0f else baseSlideMs * (currentScale * currentScale)

    val maxSlideIn = maxOf(slideIn1x, slideInCurrent)
    val slideOutStart = maxSlideIn + pauseMs
    val maxSlideOut = maxOf(slideOut1x, slideOutCurrent)
    val totalCycleMs = slideOutStart + maxSlideOut + restMs

    var elapsedMs by remember { mutableFloatStateOf(0f) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAppForeground = true
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isAppForeground = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentScale, isAppForeground) {
        if (!isAppForeground) return@LaunchedEffect
        
        elapsedMs = 0f
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000f
                last = now
                val step = if (dt > 500f) 0f else dt
                elapsedMs += step
                if (elapsedMs >= totalCycleMs) {
                    elapsedMs %= totalCycleMs
                }
            }
        }
    }

    Card(
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.anim_hero_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2fx", currentScale),
                        style = MonoNumeralLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val tone = when {
                    currentScale <= 0f -> StatusTone.WARNING
                    currentScale < 0.95f -> StatusTone.ACTIVE
                    currentScale <= 1.05f -> StatusTone.NEUTRAL
                    else -> StatusTone.WARNING
                }
                val toneLabel = when {
                    currentScale <= 0f -> R.string.anim_hero_off
                    currentScale < 0.95f -> R.string.anim_hero_fast
                    currentScale <= 1.05f -> R.string.anim_hero_normal
                    else -> R.string.anim_hero_slow
                }
                StatusPill(text = stringResource(toneLabel), tone = tone)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOpenCloseCard(
                    label = stringResource(R.string.preview_default_scale_label),
                    slideInMs = slideIn1x,
                    slideOutStartMs = slideOutStart,
                    slideOutMs = slideOut1x,
                    totalCycleMs = totalCycleMs,
                    elapsedMs = elapsedMs,
                    animOff = false,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
                AppOpenCloseCard(
                    label = stringResource(
                        R.string.preview_current_scale_label,
                        String.format(java.util.Locale.US, "%.2f", currentScale)
                    ),
                    slideInMs = slideInCurrent,
                    slideOutStartMs = slideOutStart,
                    slideOutMs = slideOutCurrent,
                    totalCycleMs = totalCycleMs,
                    elapsedMs = elapsedMs,
                    animOff = currentScale <= 0f,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppOpenCloseCard(
    label: String,
    slideInMs: Float,
    slideOutStartMs: Float,
    slideOutMs: Float,
    totalCycleMs: Float,
    elapsedMs: Float,
    animOff: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val progress: Float

    when {
        animOff -> {
            progress = 1f
        }
        elapsedMs < slideInMs -> {
            val frac = decelerateInterpolation(elapsedMs / slideInMs)
            progress = frac
        }
        elapsedMs < slideOutStartMs -> {
            progress = 1f
        }
        elapsedMs < slideOutStartMs + slideOutMs -> {
            val frac = accelerateInterpolation((elapsedMs - slideOutStartMs) / slideOutMs)
            progress = 1f - frac
        }
        else -> {
            progress = 0f
        }
    }

    val accentColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(80.dp)
                    .graphicsLayer(
                        scaleX = 0.4f + 0.6f * progress,
                        scaleY = 0.4f + 0.6f * progress,
                        translationY = (1f - progress) * 100f,
                        alpha = progress
                    )
                    .background(accentColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (progress > 0.3f) stringResource(R.string.preview_app_text) else "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun decelerateInterpolation(input: Float): Float {
    return 1f - (1f - input) * (1f - input)
}

private fun accelerateInterpolation(input: Float): Float {
    return input * input
}
