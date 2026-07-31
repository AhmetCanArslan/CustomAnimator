package com.arslan.customanimator

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

object Billing {
    enum class PurchaseResult { PURCHASED, PENDING, CANCELLED, ERROR }
}

fun initBilling(context: Context) = Unit

fun isAdFreeNow(): Boolean = true

@Composable
fun rememberIsAdFree(): State<Boolean> = remember { mutableStateOf(true) }

@Composable
fun rememberRemoveAdsPrice(): State<String?> = remember { mutableStateOf<String?>(null) }

fun launchRemoveAdsPurchase(activity: Activity, onResult: (Billing.PurchaseResult) -> Unit) =
    onResult(Billing.PurchaseResult.ERROR)

fun restorePurchases(onDone: (Boolean) -> Unit) = onDone(true)
