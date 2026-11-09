package com.arslan.customanimator

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private fun ProductDetails.oneTimeOffer(): ProductDetails.OneTimePurchaseOfferDetails? =
    oneTimePurchaseOfferDetailsList?.minByOrNull { it.priceAmountMicros }
        ?: oneTimePurchaseOfferDetails

object Billing {
    const val PREMIUM_PRODUCT_ID = "remove_ads"

    private const val TAG = "Billing"
    private const val PREFS = "custom_animator_billing"
    private const val KEY_PREMIUM = "ad_free"
    private const val KEY_DEBUG_PREMIUM = "debug_premium"

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var appContextRef: Context? = null
    private var client: BillingClient? = null
    private var holds = 0
    private var isConnecting = false
    private var isChecking = false
    private var isPurchaseFlowOpen = false
    private val waiting = mutableListOf<Waiter>()

    private var purchaseListener: ((PurchaseResult) -> Unit)? = null

    enum class PurchaseResult { PURCHASED, PENDING, CANCELLED, ERROR }

    private class Waiter(val onReady: (BillingClient) -> Unit, val onFailed: () -> Unit)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        mainHandler.post { onPurchasesUpdated(result, purchases.orEmpty()) }
    }

    private val connectionListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            mainHandler.post { onSetupFinished(result) }
        }

        override fun onBillingServiceDisconnected() {
            mainHandler.post { onDisconnected() }
        }
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        appContextRef = appContext
        _isPremium.value = appContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(if (BuildConfig.DEBUG) KEY_DEBUG_PREMIUM else KEY_PREMIUM, false)
        if (!isChecking) queryPurchases()
    }

    fun queryPurchases(onDone: ((Boolean) -> Unit)? = null) {
        isChecking = true
        acquire(
            onReady = { billing ->
                if (_price.value == null) loadProduct(billing) { }
                loadPurchases(billing) {
                    isChecking = false
                    onDone?.invoke(_isPremium.value)
                }
                release()
            },
            onFailed = {
                isChecking = false
                onDone?.invoke(_isPremium.value)
            }
        )
    }

    fun launchPurchase(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        appContextRef = activity.applicationContext
        purchaseListener = onResult
        acquire(
            onReady = { billing ->
                loadProduct(billing) { product -> startPurchaseFlow(activity, billing, product) }
                release()
            },
            onFailed = { notify(PurchaseResult.ERROR) }
        )
    }

    private fun acquire(onReady: (BillingClient) -> Unit, onFailed: () -> Unit) {
        holds++
        val current = client
        if (current != null && current.isReady) {
            onReady(current)
            return
        }
        waiting.add(Waiter(onReady, onFailed))
        if (isConnecting) return
        val context = appContextRef
        if (context == null) {
            failWaiting()
            return
        }
        current?.endConnection()
        isConnecting = true
        val billing = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = billing
        billing.startConnection(connectionListener)
    }

    private fun hold() {
        holds++
    }

    private fun release() {
        holds--
        if (holds > 0) return
        holds = 0
        isConnecting = false
        client?.endConnection()
        client = null
    }

    private fun onSetupFinished(result: BillingResult) {
        isConnecting = false
        val billing = client
        if (billing == null || result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Setup failed: ${result.responseCode} ${result.debugMessage}")
            failWaiting()
            return
        }
        val ready = waiting.toList()
        waiting.clear()
        ready.forEach { it.onReady(billing) }
    }

    private fun onDisconnected() {
        isConnecting = false
        failWaiting()
        if (isPurchaseFlowOpen) {
            isPurchaseFlowOpen = false
            notify(PurchaseResult.ERROR)
            release()
        }
    }

    private fun failWaiting() {
        val failed = waiting.toList()
        waiting.clear()
        failed.forEach {
            it.onFailed()
            release()
        }
    }

    private fun loadProduct(billing: BillingClient, onLoaded: (ProductDetails?) -> Unit) {
        hold()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billing.queryProductDetailsAsync(params) { result, details ->
            mainHandler.post {
                val product = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    details.productDetailsList.firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
                } else {
                    Log.w(TAG, "Product query failed: ${result.responseCode} ${result.debugMessage}")
                    null
                }
                product?.oneTimeOffer()?.formattedPrice?.let { _price.value = it }
                onLoaded(product)
                release()
            }
        }
    }

    private fun loadPurchases(billing: BillingClient, onLoaded: () -> Unit) {
        hold()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billing.queryPurchasesAsync(params) { result, purchases ->
            mainHandler.post {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    applyOwnedPurchases(billing, purchases)
                } else {
                    Log.w(TAG, "Purchase query failed: ${result.responseCode} ${result.debugMessage}")
                }
                onLoaded()
                release()
            }
        }
    }

    private fun applyOwnedPurchases(billing: BillingClient, purchases: List<Purchase>) {
        val owned = purchases.any { purchase ->
            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        purchases.forEach { handlePurchase(billing, it) }
        if (!owned) setPremium(false)
    }

    private fun startPurchaseFlow(activity: Activity, billing: BillingClient, product: ProductDetails?) {
        if (product == null) {
            notify(PurchaseResult.ERROR)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .apply {
                            product.oneTimeOffer()?.offerToken?.let { setOfferToken(it) }
                        }
                        .build()
                )
            )
            .build()

        val result = billing.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Launch failed: ${result.responseCode} ${result.debugMessage}")
            notify(PurchaseResult.ERROR)
            return
        }
        isPurchaseFlowOpen = true
        hold()
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        val billing = client
        val outcome = when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchaseOutcome(billing, purchases)
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.CANCELLED
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                billing?.let { loadPurchases(it) { } }
                PurchaseResult.PURCHASED
            }
            else -> {
                Log.w(TAG, "Purchase update failed: ${result.responseCode} ${result.debugMessage}")
                PurchaseResult.ERROR
            }
        }
        notify(outcome)
        if (isPurchaseFlowOpen) {
            isPurchaseFlowOpen = false
            release()
        }
    }

    private fun purchaseOutcome(billing: BillingClient?, purchases: List<Purchase>): PurchaseResult {
        val handled = purchases.map { handlePurchase(billing, it) }
        return when {
            handled.any { it == PurchaseResult.PURCHASED } -> PurchaseResult.PURCHASED
            handled.any { it == PurchaseResult.PENDING } -> PurchaseResult.PENDING
            else -> PurchaseResult.ERROR
        }
    }

    private fun handlePurchase(billing: BillingClient?, purchase: Purchase): PurchaseResult {
        if (!purchase.products.contains(PREMIUM_PRODUCT_ID)) return PurchaseResult.ERROR

        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                setPremium(true)
                if (!purchase.isAcknowledged && billing != null) acknowledge(billing, purchase)
                PurchaseResult.PURCHASED
            }

            Purchase.PurchaseState.PENDING -> PurchaseResult.PENDING

            else -> PurchaseResult.ERROR
        }
    }

    private fun acknowledge(billing: BillingClient, purchase: Purchase) {
        hold()
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { result ->
            mainHandler.post {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.responseCode} ${result.debugMessage}")
                }
                release()
            }
        }
    }

    fun setDebugPremium(context: Context, value: Boolean) {
        if (!BuildConfig.DEBUG) return
        _isPremium.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DEBUG_PREMIUM, value) }
    }

    private fun setPremium(value: Boolean) {
        if (BuildConfig.DEBUG || _isPremium.value == value) return
        _isPremium.value = value
        appContextRef?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_PREMIUM, value)
            ?.apply()
    }

    private fun notify(result: PurchaseResult) {
        val listener = purchaseListener ?: return
        purchaseListener = null
        mainHandler.post { listener(result) }
    }
}

fun initBilling(context: Context) = Billing.init(context)

fun isPremiumNow(): Boolean = Billing.isPremium.value

@Composable
fun rememberIsPremium(): State<Boolean> = Billing.isPremium.collectAsState()

@Composable
fun rememberPremiumPrice(): State<String?> = Billing.price.collectAsState()

fun restorePurchases(onDone: (Boolean) -> Unit) = Billing.queryPurchases(onDone)

fun startPremiumPurchase(context: Context) {
    val activity = context as? Activity
    if (activity == null) {
        Toast.makeText(context, R.string.premium_unavailable, Toast.LENGTH_SHORT).show()
        return
    }
    Billing.launchPurchase(activity) { result ->
        val message = when (result) {
            Billing.PurchaseResult.PURCHASED -> R.string.premium_thanks
            Billing.PurchaseResult.PENDING -> R.string.premium_pending
            Billing.PurchaseResult.CANCELLED -> null
            Billing.PurchaseResult.ERROR -> R.string.premium_unavailable
        }
        message?.let { Toast.makeText(activity, it, Toast.LENGTH_SHORT).show() }
    }
}
