package com.lavish.admob.bubble

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing for the one-time "earnings_bubble_unlock" product.
 *
 * Flow: connect -> load product + restore owned purchases -> user buys ->
 * acknowledge (must happen within 3 days or Google refunds) -> mark unlocked in RTDB.
 * Google Play is the source of truth; RTDB is a sync/cache across devices.
 */
object BubbleBillingManager : PurchasesUpdatedListener {
    private const val TAG = "EarningsBubble"
    const val PRODUCT_ID = "earnings_bubble_unlock"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var appContext: Context
    private var client: BillingClient? = null

    private val _product = MutableStateFlow<ProductDetails?>(null)
    val product: StateFlow<ProductDetails?> = _product.asStateFlow()

    /** Localised price from Play (e.g. "$1.00", "₹89.00"); null until loaded. */
    val formattedPrice: String?
        get() = _product.value?.oneTimePurchaseOfferDetails?.formattedPrice

    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress.asStateFlow()

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext
        client = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        connect()
    }

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProduct()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        client?.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _product.value = details.productDetailsList.firstOrNull()
                if (_product.value == null) {
                    // Billing 8 tells us WHY a product wasn't returned
                    // (e.g. PRODUCT_NOT_FOUND, NO_ELIGIBLE_OFFER)
                    val reasons = details.unfetchedProductList.joinToString {
                        "${it.productId}: statusCode=${it.statusCode}"
                    }
                    Log.w(TAG, "Product $PRODUCT_ID not returned for package ${appContext.packageName}. Unfetched: [$reasons]")
                } else {
                    Log.d(TAG, "Loaded product $PRODUCT_ID: ${_product.value?.oneTimePurchaseOfferDetails?.formattedPrice}")
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    /** Re-checks Play for an owned unlock (new device, reinstall, purchase finished while app closed). */
    fun restorePurchases(onDone: ((found: Boolean) -> Unit)? = null) {
        val c = client ?: return
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            val owned = result.responseCode == BillingClient.BillingResponseCode.OK &&
                purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            purchases.forEach { handlePurchase(it, silent = true) }
            if (!owned) BubbleEntitlementManager.setOwnedOnPlay(false)
            onDone?.invoke(owned)
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = _product.value
        val c = client
        if (details == null || c == null || !c.isReady) {
            Toast.makeText(activity, "Store not ready yet. Please try again in a moment.", Toast.LENGTH_SHORT).show()
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .apply { BubbleFirebase.auth.currentUser?.uid?.let { setObfuscatedAccountId(it.take(64)) } }
            .build()
        _purchaseInProgress.value = true
        val r = c.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseInProgress.value = false
            Log.w(TAG, "launchBillingFlow failed: ${r.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _purchaseInProgress.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it, silent = false) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                BubbleFirebase.log(appContext, BubbleEvents.PURCHASE_CANCELLED)
            }
            else -> {
                BubbleFirebase.log(appContext, BubbleEvents.PURCHASE_FAILED, mapOf("code" to result.responseCode))
                toast("Purchase failed. Please try again.")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, silent: Boolean) {
        if (PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> if (!silent) {
                toast("Payment pending. The bubble unlocks as soon as it's confirmed.")
            }
            Purchase.PurchaseState.PURCHASED -> {
                // Unlock locally immediately; Play already confirmed ownership.
                BubbleEntitlementManager.setOwnedOnPlay(true)
                if (!purchase.isAcknowledged) acknowledge(purchase, silent) else syncToFirebase(purchase)
            }
            else -> Unit
        }
    }

    private fun acknowledge(purchase: Purchase, silent: Boolean) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client?.acknowledgePurchase(params) { r ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                syncToFirebase(purchase)
                if (!silent) {
                    BubbleFirebase.log(appContext, BubbleEvents.PURCHASE_SUCCESS)
                    toast("Unlocked! The earnings bubble is yours forever.")
                    if (android.provider.Settings.canDrawOverlays(appContext)) {
                        EarningsBubbleService.start(appContext)
                    }
                }
            } else {
                // Will retry on next app start via restorePurchases().
                Log.w(TAG, "Acknowledge failed: ${r.debugMessage}")
            }
        }
    }

    private fun syncToFirebase(purchase: Purchase) {
        if (BubbleEntitlementManager.access.value == BubbleAccess.Unlocked &&
            BubbleEntitlementManager.isUnlockedInDb()
        ) return
        scope.launch { BubbleEntitlementManager.markUnlocked(purchase.purchaseToken, PRODUCT_ID) }
    }

    private fun toast(msg: String) {
        scope.launch { Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show() }
    }
}
