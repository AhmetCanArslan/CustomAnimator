package com.arslan.customanimator

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
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
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            val widthPx = context.resources.displayMetrics.widthPixels
            val density = context.resources.displayMetrics.density
            val adWidth = (widthPx / density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
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
