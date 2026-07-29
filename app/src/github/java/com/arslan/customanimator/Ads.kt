package com.arslan.customanimator

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable

/** No-op stubs: the github flavor ships without ads or the Play Services Ads dependency. */
fun initAds(activity: Activity) = Unit

@Composable
fun BannerAdView() = Unit

fun maybeShowInterstitial(context: Context) = Unit

fun preloadInterstitial(context: Context) = Unit
