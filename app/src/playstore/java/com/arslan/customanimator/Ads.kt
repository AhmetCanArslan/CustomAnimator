package com.arslan.customanimator

import android.app.Activity
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
import com.google.android.gms.ads.MobileAds

private val BANNER_AD_UNIT_ID: String
    get() = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"
    } else {
        BuildConfig.BANNER_AD_UNIT_ID
    }

fun initAds(activity: Activity) {
    MobileAds.initialize(activity)
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
