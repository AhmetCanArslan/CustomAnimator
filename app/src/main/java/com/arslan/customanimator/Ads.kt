package com.arslan.customanimator

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.arslan.customanimator.utils.SettingsManager
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

private val BANNER_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        BuildConfig.BANNER_AD_UNIT_ID
    }

private val INTERSTITIAL_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"
    } else {
        BuildConfig.INTERSTITIAL_AD_UNIT_ID
    }

private val APP_OPEN_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/9257395921"
    } else {
        BuildConfig.APP_OPEN_AD_UNIT_ID
    }

private const val ADS_PREFS = "custom_animator_ads"

private val isMobileAdsInitialized = AtomicBoolean(false)

fun initAds(activity: Activity) {
    if (isAdFreeNow()) {
        Log.d("Ads", "Skipped: ads removed by purchase")
        return
    }
    AdsConsent.gather(activity) {
        initializeMobileAds(activity)
    }
}

private fun initializeMobileAds(activity: Activity) {
    if (!AdsConsent.canRequestAds(activity)) {
        Log.d("Ads", "Initialization deferred: consent not obtained")
        return
    }
    if (!isMobileAdsInitialized.compareAndSet(false, true)) return

    Thread {
        MobileAds.initialize(activity.applicationContext) {
            activity.runOnUiThread {
                InterstitialAds.preload(activity)
                AppOpenAds.register(activity.application)
            }
        }
    }.start()
}

object AdsConsent {
    private const val TAG = "AdsConsent"

    private var consentInformation: ConsentInformation? = null

    fun gather(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .build()
                    )
                }
            }
            .build()

        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    onReady()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                onReady()
            }
        )
    }

    fun canRequestAds(context: Context): Boolean {
        val info = consentInformation
            ?: UserMessagingPlatform.getConsentInformation(context).also { consentInformation = it }
        return info.canRequestAds()
    }

    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options error: ${error.errorCode} ${error.message}")
            }
        }
    }
}

private object AdBudget {
    private const val KEY_DAY = "ads_day_of_year"
    private const val KEY_FULLSCREEN_TODAY = "ads_fullscreen_today"

    private const val MAX_FULLSCREEN_PER_DAY = 12

    fun remainingToday(context: Context): Int {
        val prefs = context.getSharedPreferences(ADS_PREFS, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (prefs.getInt(KEY_DAY, -1) != today) return MAX_FULLSCREEN_PER_DAY
        return MAX_FULLSCREEN_PER_DAY - prefs.getInt(KEY_FULLSCREEN_TODAY, 0)
    }

    fun record(context: Context) {
        val prefs = context.getSharedPreferences(ADS_PREFS, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val shownToday = if (prefs.getInt(KEY_DAY, -1) == today) {
            prefs.getInt(KEY_FULLSCREEN_TODAY, 0)
        } else {
            0
        }
        prefs.edit()
            .putInt(KEY_DAY, today)
            .putInt(KEY_FULLSCREEN_TODAY, shownToday + 1)
            .apply()
    }
}

object FullScreenAdState {
    @Volatile
    var isShowing: Boolean = false
}

@Composable
fun BannerAdView(applyNavigationBarPadding: Boolean = true) {
    val isAdFree by rememberIsAdFree()
    if (isAdFree) {
        if (applyNavigationBarPadding) {
            Spacer(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
        }
        return
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val lifecycleOwner = LocalLifecycleOwner.current

    key(screenWidthDp) {
        val adViewHolder = remember { arrayOfNulls<AdView>(1) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adViewHolder[0]?.pause()
                    Lifecycle.Event.ON_RESUME -> adViewHolder[0]?.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adViewHolder[0]?.destroy()
                adViewHolder[0] = null
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (applyNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier
                ),
            factory = { context ->
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
                AdView(context).apply {
                    setAdSize(adSize)
                    adUnitId = BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("BannerAd", "Ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e("BannerAd", "Ad failed to load: ${error.code} ${error.message}")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                    adViewHolder[0] = this
                }
            }
        )
    }
}

object AppOpenAds {
    private const val TAG = "AppOpenAd"
    private const val KEY_LAST_SHOWN = "app_open_last_shown"

    private const val MIN_INTERVAL_MS = 4L * 60 * 1000
    private const val AD_EXPIRY_MS = 4L * 60 * 60 * 1000
    private const val COLD_START_GRACE_MS = 20L * 1000

    private val processStartElapsedMs = SystemClock.elapsedRealtime()

    private var ad: AppOpenAd? = null
    private var loadedAtMs = 0L
    private var isLoading = false
    private var registered = false
    private var currentActivity: Activity? = null

    fun register(application: Application) {
        if (registered) return
        registered = true

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    if (activity is MainActivity) currentActivity = activity
                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivity === activity) currentActivity = null
                }

                override fun onActivityCreated(activity: Activity, bundle: android.os.Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) {
                    if (currentActivity === activity) currentActivity = null
                }
                override fun onActivitySaveInstanceState(activity: Activity, bundle: android.os.Bundle) = Unit
            }
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    showIfEligible()
                }
            }
        )

        preload(application)
    }

    fun preload(context: Context) {
        if (isAdFreeNow()) return
        if (isLoading || isAdAvailable()) return
        if (!AdsConsent.canRequestAds(context)) return

        isLoading = true
        AppOpenAd.load(
            context.applicationContext,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    isLoading = false
                    ad = loaded
                    loadedAtMs = SystemClock.elapsedRealtime()
                    Log.d(TAG, "Loaded and ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    ad = null
                    Log.e(TAG, "Failed to load: ${error.code} ${error.message}")
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return ad != null && SystemClock.elapsedRealtime() - loadedAtMs < AD_EXPIRY_MS
    }

    private fun showIfEligible() {
        if (isAdFreeNow()) return
        val activity = currentActivity ?: return
        if (activity !is MainActivity || activity.isFinishing || activity.isDestroyed) return
        if (FullScreenAdState.isShowing) return

        if (SystemClock.elapsedRealtime() - processStartElapsedMs < COLD_START_GRACE_MS) {
            Log.d(TAG, "Skipped: cold start grace")
            preload(activity)
            return
        }

        if (!SettingsManager.hasCompletedOnboarding(activity)) {
            Log.d(TAG, "Skipped: onboarding not finished")
            return
        }

        val prefs = activity.getSharedPreferences(ADS_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        if (lastShown in 1..now && now - lastShown < MIN_INTERVAL_MS) {
            Log.d(TAG, "Skipped: ${(now - lastShown) / 1000}s since last app open ad")
            return
        }

        if (AdBudget.remainingToday(activity) <= 0) {
            Log.d(TAG, "Skipped: daily full-screen budget spent")
            return
        }

        val loaded = ad
        if (!isAdAvailable() || loaded == null) {
            preload(activity)
            return
        }

        ad = null
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullScreenAdState.isShowing = true
            }

            override fun onAdDismissedFullScreenContent() {
                FullScreenAdState.isShowing = false
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                FullScreenAdState.isShowing = false
                Log.e(TAG, "Failed to show: ${error.code} ${error.message}")
                preload(activity)
            }
        }
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply()
        AdBudget.record(activity)
        loaded.show(activity)
    }
}

object InterstitialAds {
    private const val KEY_LAST_SHOWN = "interstitial_last_shown"
    private const val KEY_ACTION_COUNT = "interstitial_action_count"

    private const val MIN_INTERVAL_MS = 3L * 60 * 1000
    private const val FREE_ACTIONS = 5
    private const val ACTIONS_PER_AD = 3

    private const val TAG = "InterstitialAd"

    private const val LAUNCH_GRACE_MS = 45L * 1000

    private val processStartElapsedMs = SystemClock.elapsedRealtime()

    private var ad: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (isAdFreeNow()) return
        if (ad != null || isLoading) {
            Log.d(TAG, "preload skipped (ready=${ad != null}, loading=$isLoading)")
            return
        }
        if (!AdsConsent.canRequestAds(context)) {
            Log.d(TAG, "preload skipped: consent not obtained")
            return
        }
        isLoading = true
        Log.d(TAG, "Loading")
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    isLoading = false
                    ad = loaded
                    Log.d(TAG, "Loaded and ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    ad = null
                    Log.e(TAG, "Failed to load: ${error.code} ${error.message}")
                }
            }
        )
    }

    fun maybeShow(context: Context) {
        if (isAdFreeNow()) return
        val activity = context.findActivity()
        if (activity == null) {
            Log.w(TAG, "Skipped: no Activity behind the context")
            return
        }
        if (FullScreenAdState.isShowing) {
            Log.d(TAG, "Skipped: another full-screen ad is showing")
            return
        }
        val prefs = activity.getSharedPreferences(ADS_PREFS, Context.MODE_PRIVATE)

        val actionCount = prefs.getInt(KEY_ACTION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_ACTION_COUNT, actionCount).apply()

        if (actionCount <= FREE_ACTIONS) {
            Log.d(TAG, "Skipped: free action $actionCount/$FREE_ACTIONS")
            return
        }

        if ((actionCount - FREE_ACTIONS) % ACTIONS_PER_AD != 0) {
            Log.d(TAG, "Skipped: action $actionCount, waiting for every ${ACTIONS_PER_AD}th")
            preload(activity)
            return
        }

        val sinceLaunchMs = SystemClock.elapsedRealtime() - processStartElapsedMs
        if (sinceLaunchMs < LAUNCH_GRACE_MS) {
            Log.d(TAG, "Skipped: only ${sinceLaunchMs}ms since launch (grace $LAUNCH_GRACE_MS ms)")
            return
        }

        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        val now = System.currentTimeMillis()

        if (lastShown in 1..now && now - lastShown < MIN_INTERVAL_MS) {
            Log.d(TAG, "Skipped: ${(now - lastShown) / 1000}s since last ad (interval ${MIN_INTERVAL_MS / 1000}s)")
            return
        }

        if (AdBudget.remainingToday(activity) <= 0) {
            Log.d(TAG, "Skipped: daily full-screen budget spent")
            return
        }

        val loaded = ad
        if (loaded == null) {
            Log.d(TAG, "Skipped: no ad ready, warming one up for the next trigger")
            preload(activity)
            return
        }

        Log.d(TAG, "Showing (action $actionCount)")

        ad = null
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                FullScreenAdState.isShowing = true
            }

            override fun onAdDismissedFullScreenContent() {
                FullScreenAdState.isShowing = false
                Log.d(TAG, "Dismissed")
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                FullScreenAdState.isShowing = false
                Log.e(TAG, "Failed to show: ${error.code} ${error.message}")
                preload(activity)
            }
        }
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply()
        AdBudget.record(activity)
        loaded.show(activity)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun maybeShowInterstitial(context: Context) = InterstitialAds.maybeShow(context)

fun preloadInterstitial(context: Context) = InterstitialAds.preload(context)

fun isPrivacyOptionsRequired(): Boolean = AdsConsent.isPrivacyOptionsRequired()

fun showPrivacyOptions(activity: Activity) = AdsConsent.showPrivacyOptions(activity)
