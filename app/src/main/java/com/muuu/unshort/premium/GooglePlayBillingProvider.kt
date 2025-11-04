package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play Billing 기반 프리미엄 제공자
 *
 * 책임:
 * - Google Play Billing Library를 통한 인앱 결제 처리
 * - 프리미엄 구독 상태 관리 및 동기화
 * - 구매 복원 처리
 *
 * 구현 원칙:
 * - Single Responsibility: Billing 로직만 담당
 * - Low Coupling: PremiumProvider 인터페이스를 통해 추상화
 */
class GooglePlayBillingProvider(
    private val context: Context
) : PremiumProvider, PurchasesUpdatedListener {

    companion object {
        private const val TAG = "GooglePlayBillingProvider"

        private const val PREMIUM_PRODUCT_ID = "unshort_premium_monthly"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var billingClient: BillingClient? = null
    private var isPremiumCache: Boolean = false

    // 구매 플로우 콜백
    private var purchaseFlowCallback: ((Boolean) -> Unit)? = null

    init {
        initializeBillingClient()
    }

    /**
     * BillingClient 초기화
     */
    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        // 연결 시작
        connectToBillingClient()
    }

    /**
     * Billing 서비스 연결
     */
    private fun connectToBillingClient() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    // 연결 성공 시 구독 상태 확인
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing client disconnected, retrying connection...")
                // 재연결 시도
                connectToBillingClient()
            }
        })
    }

    override fun isPremium(): Boolean {
        return isPremiumCache
    }

    override fun startPurchaseFlow(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        purchaseFlowCallback = onResult

        scope.launch {
            try {
                // 연결 확인
                if (!ensureConnected()) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }

                // 제품 정보 조회
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                        val productDetails = productDetailsList.first()

                        // 구독 오퍼 선택 (첫 번째 오퍼 사용)
                        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

                        if (offerToken != null) {
                            launchBillingFlow(activity, productDetails, offerToken)
                        } else {
                            Log.e(TAG, "No subscription offers available")
                            onResult(false)
                        }
                    } else {
                        Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting purchase flow", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    /**
     * Billing 플로우 시작
     */
    private fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                purchaseFlowCallback?.invoke(false)
                purchaseFlowCallback = null
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                purchaseFlowCallback?.invoke(false)
                purchaseFlowCallback = null
            }
        }
    }

    /**
     * 구매 처리
     */
    private fun handlePurchase(purchase: Purchase) {
        scope.launch {
            try {
                // 구매 확인이 필요한 경우
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }

                    // 프리미엄 상태 업데이트
                    isPremiumCache = true

                    withContext(Dispatchers.Main) {
                        purchaseFlowCallback?.invoke(true)
                        purchaseFlowCallback = null
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        purchaseFlowCallback?.invoke(false)
                        purchaseFlowCallback = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling purchase", e)
                withContext(Dispatchers.Main) {
                    purchaseFlowCallback?.invoke(false)
                    purchaseFlowCallback = null
                }
            }
        }
    }

    /**
     * 구매 확인 (Acknowledge)
     */
    private suspend fun acknowledgePurchase(purchase: Purchase) {
        withContext(Dispatchers.IO) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }

    override fun syncPremiumStatus(onComplete: () -> Unit) {
        scope.launch {
            try {
                if (!ensureConnected()) {
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                    return@launch
                }

                queryPurchases()

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing premium status", e)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    /**
     * 기존 구매 조회
     */
    private fun queryPurchases() {
        scope.launch {
            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // 활성 구독이 있는지 확인
                        val hasActiveSubscription = purchasesList.any { purchase ->
                            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }

                        isPremiumCache = hasActiveSubscription
                        Log.d(TAG, "Premium status: $isPremiumCache")
                    } else {
                        Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying purchases", e)
            }
        }
    }

    override fun restorePurchases(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        scope.launch {
            try {
                if (!ensureConnected()) {
                    withContext(Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }

                // 구매 내역 조회
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val hasActiveSubscription = purchasesList.any { purchase ->
                            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }

                        if (hasActiveSubscription) {
                            isPremiumCache = true
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    } else {
                        Log.e(TAG, "Failed to restore purchases: ${billingResult.debugMessage}")
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring purchases", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    /**
     * Billing 클라이언트 연결 확인
     */
    private suspend fun ensureConnected(): Boolean = withContext(Dispatchers.IO) {
        if (billingClient?.isReady == true) {
            return@withContext true
        }

        // 재연결 시도
        var connected = false
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        })

        // 연결 대기 (최대 3초)
        var attempts = 0
        while (!connected && attempts < 30) {
            kotlinx.coroutines.delay(100)
            attempts++
            if (billingClient?.isReady == true) {
                connected = true
                break
            }
        }

        return@withContext connected
    }

    /**
     * 리소스 정리
     */
    fun cleanup() {
        billingClient?.endConnection()
        billingClient = null
    }
}
