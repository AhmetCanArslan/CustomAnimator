package com.arslan.customanimator

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

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

fun initAds(activity: Activity) {
    MobileAds.initialize(activity) {
        InterstitialAds.preload(activity)
    }
}

@Composable
fun BannerAdView() {
    // Keyed on width so a rotation or a density/smallest-width change rebuilds the AdView with an
    // ad size that still matches the screen instead of keeping the size captured at first layout.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    key(screenWidthDp) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
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
            }
        }
    )
    }
}

object InterstitialAds {
    private const val PREFS_NAME = "custom_animator_ads"
    private const val KEY_LAST_SHOWN = "interstitial_last_shown"
    private const val KEY_ACTION_COUNT = "interstitial_action_count"

    private const val MIN_INTERVAL_MS = 15L * 60 * 1000
    private const val FREE_ACTIONS = 5

    private const val TAG = "InterstitialAd"

    private const val LAUNCH_GRACE_MS = 45L * 1000

    private val processStartElapsedMs = SystemClock.elapsedRealtime()

    private var ad: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (ad != null || isLoading) {
            Log.d(TAG, "preload skipped (ready=${ad != null}, loading=$isLoading)")
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
        val activity = context.findActivity()
        if (activity == null) {
            Log.w(TAG, "Skipped: no Activity behind the context")
            return
        }
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val actionCount = prefs.getInt(KEY_ACTION_COUNT, 0) + 1
        if (actionCount <= FREE_ACTIONS) {
            prefs.edit().putInt(KEY_ACTION_COUNT, actionCount).apply()
            Log.d(TAG, "Skipped: free action $actionCount/$FREE_ACTIONS")
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

        val loaded = ad
        if (loaded == null) {
            Log.d(TAG, "Skipped: no ad ready, warming one up for the next trigger")
            preload(activity)
            return
        }

        Log.d(TAG, "Showing (action $actionCount)")

        ad = null
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Dismissed")
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Failed to show: ${error.code} ${error.message}")
                preload(activity)
            }
        }
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply()
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
