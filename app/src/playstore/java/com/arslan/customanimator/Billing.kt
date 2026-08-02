package com.arslan.customanimator

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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

object Billing {
    const val REMOVE_ADS_PRODUCT_ID = "remove_ads"

    private const val TAG = "Billing"
    private const val PREFS = "custom_animator_billing"
    private const val KEY_AD_FREE = "ad_free"

    private const val MAX_RECONNECT_DELAY_MS = 60_000L

    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var client: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var reconnectDelayMs = 1_000L
    private var isConnecting = false

    private var purchaseListener: ((PurchaseResult) -> Unit)? = null

    enum class PurchaseResult { PURCHASED, PENDING, CANCELLED, ERROR }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val handled = purchases.orEmpty().map { handlePurchase(it) }
                val outcome = when {
                    handled.any { it == PurchaseResult.PURCHASED } -> PurchaseResult.PURCHASED
                    handled.any { it == PurchaseResult.PENDING } -> PurchaseResult.PENDING
                    else -> PurchaseResult.ERROR
                }
                notify(outcome)
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> notify(PurchaseResult.CANCELLED)

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryPurchases()
                notify(PurchaseResult.PURCHASED)
            }

            else -> {
                Log.w(TAG, "Purchase update failed: ${result.responseCode} ${result.debugMessage}")
                notify(PurchaseResult.ERROR)
            }
        }
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        appContextRef = appContext
        _isAdFree.value = appContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AD_FREE, false)

        if (client != null) {
            queryPurchases()
            return
        }

        client = BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        connect(appContext)
    }

    private fun connect(context: Context) {
        val billing = client ?: return
        if (isConnecting || billing.isReady) return
        isConnecting = true

        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectDelayMs = 1_000L
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.w(TAG, "Setup failed: ${result.responseCode} ${result.debugMessage}")
                    scheduleReconnect(context)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                scheduleReconnect(context)
            }
        })
    }

    private fun scheduleReconnect(context: Context) {
        val delay = reconnectDelayMs
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        mainHandler.postDelayed({ connect(context) }, delay)
    }

    private fun queryProductDetails() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billing.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Product query failed: ${result.responseCode} ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            val product = details.productDetailsList
                .firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID }
            productDetails = product
            _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun queryPurchases(onDone: ((Boolean) -> Unit)? = null) {
        val billing = client
        if (billing == null || !billing.isReady) {
            onDone?.let { mainHandler.post { it(_isAdFree.value) } }
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Purchase query failed: ${result.responseCode} ${result.debugMessage}")
                onDone?.invoke(_isAdFree.value)
                return@queryPurchasesAsync
            }
            val owned = purchases.any { purchase ->
                purchase.products.contains(REMOVE_ADS_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            purchases.forEach { handlePurchase(it) }
            if (!owned) setAdFree(false)
            onDone?.invoke(_isAdFree.value)
        }
    }

    private fun handlePurchase(purchase: Purchase): PurchaseResult {
        if (!purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) return PurchaseResult.ERROR

        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                setAdFree(true)
                if (!purchase.isAcknowledged) acknowledge(purchase)
                PurchaseResult.PURCHASED
            }

            Purchase.PurchaseState.PENDING -> PurchaseResult.PENDING

            else -> PurchaseResult.ERROR
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val billing = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Acknowledge failed: ${result.responseCode} ${result.debugMessage}")
            }
        }
    }

    private fun setAdFree(value: Boolean) {
        if (_isAdFree.value == value) return
        _isAdFree.value = value
        appContextRef?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_AD_FREE, value)
            ?.apply()
    }

    private var appContextRef: Context? = null

    fun launchPurchase(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        val billing = client
        val product = productDetails

        if (billing == null || !billing.isReady) {
            init(activity)
            onResult(PurchaseResult.ERROR)
            return
        }
        if (product == null) {
            queryProductDetails()
            onResult(PurchaseResult.ERROR)
            return
        }

        purchaseListener = onResult

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()

        val result = billing.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Launch failed: ${result.responseCode} ${result.debugMessage}")
            notify(PurchaseResult.ERROR)
        }
    }

    private fun notify(result: PurchaseResult) {
        val listener = purchaseListener ?: return
        purchaseListener = null
        mainHandler.post { listener(result) }
    }
}

fun initBilling(context: Context) = Billing.init(context)

fun isAdFreeNow(): Boolean = Billing.isAdFree.value

@Composable
fun rememberIsAdFree(): State<Boolean> = Billing.isAdFree.collectAsState()

@Composable
fun rememberRemoveAdsPrice(): State<String?> = Billing.price.collectAsState()

fun launchRemoveAdsPurchase(activity: Activity, onResult: (Billing.PurchaseResult) -> Unit) =
    Billing.launchPurchase(activity, onResult)

fun restorePurchases(onDone: (Boolean) -> Unit) = Billing.queryPurchases(onDone)
