package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.muuu.unshort.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 구독 정보 (UI 표시용)
 *
 * 주의: 만료일은 추정값이며 보안 검증 목적으로 사용 불가
 */
data class SubscriptionInfo(
    val isActive: Boolean,
    val purchaseDate: Date,
    val estimatedExpiryDate: Date?,
    val autoRenewing: Boolean
)

/**
 * 가격 정보 (UI 표시용)
 */
data class PriceInfo(
    val formattedPrice: String,      // "₩5,500"
    val hasFreeTrial: Boolean,       // 무료 체험 여부
    val trialDays: Int?,              // 7 또는 null
    val offerToken: String,           // 구매 시 사용할 토큰
    val productDetails: ProductDetails
)

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

    // 현재 활성 구독 정보 (캐싱용)
    private var currentPurchase: Purchase? = null

    // 구매 플로우 콜백
    private var purchaseFlowCallback: ((Boolean) -> Unit)? = null

    // 가격 정보 캐싱 (24시간 유효)
    private var cachedPriceInfo: PriceInfo? = null
    private var lastPriceQueryTime: Long = 0L
    private val PRICE_CACHE_DURATION = 24 * 60 * 60 * 1000L

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
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.billing_error_connection_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        onResult(false)
                    }
                    return@launch
                }

                // 제품 정보 조회
                Log.d(TAG, "Querying product: productId=$PREMIUM_PRODUCT_ID, packageName=${context.packageName}")
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                billingClient?.queryProductDetailsAsync(params) { billingResult, queryResult ->
                    val productDetailsList = queryResult.productDetailsList
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                        val productDetails = productDetailsList.first()
                        val offers = productDetails.subscriptionOfferDetails

                        // 구독 오퍼 선택 (무료 체험 오퍼 우선)
                        val selectedOffer = offers?.find { offer ->
                            offer.pricingPhases.pricingPhaseList.any { phase ->
                                phase.priceAmountMicros == 0L
                            }
                        } ?: offers?.firstOrNull()

                        val offerToken = selectedOffer?.offerToken

                        if (offerToken != null) {
                            launchBillingFlow(activity, productDetails, offerToken)
                        } else {
                            Log.e(TAG, "No subscription offers available")
                            activity.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.billing_error_sku_not_found),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onResult(false)
                        }
                    } else {
                        Log.e(TAG, "Failed to query product details: responseCode=${billingResult.responseCode}, message=${billingResult.debugMessage}, productListSize=${productDetailsList.size}")
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.billing_error_sku_not_found),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                        Log.d(TAG, "Query purchases success. Total purchases: ${purchasesList.size}")

                        // 모든 구매 상태 로깅
                        purchasesList.forEach { purchase ->
                            Log.d(TAG, "Purchase found: " +
                                "products=${purchase.products}, " +
                                "state=${purchase.purchaseState}, " +
                                "autoRenewing=${purchase.isAutoRenewing}, " +
                                "acknowledged=${purchase.isAcknowledged}")
                        }

                        // 활성 구독 찾기
                        val activePurchase = purchasesList.firstOrNull { purchase ->
                            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.isAutoRenewing  // 자동 갱신 중인 구독만 인정
                        }

                        currentPurchase = activePurchase
                        isPremiumCache = activePurchase != null

                        if (activePurchase != null) {
                            Log.d(TAG, "Premium status: ACTIVE (autoRenewing)")
                        } else {
                            val canceledPurchase = purchasesList.firstOrNull { purchase ->
                                purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                !purchase.isAutoRenewing
                            }
                            if (canceledPurchase != null) {
                                Log.w(TAG, "Premium status: CANCELED (not auto-renewing, will expire)")
                            } else {
                                Log.d(TAG, "Premium status: INACTIVE (no active subscription)")
                            }
                        }
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
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.billing_error_connection_failed),
                            Toast.LENGTH_SHORT
                        ).show()
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
                            activity.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    activity.getString(R.string.billing_error_restore_no_subscription),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onResult(false)
                        }
                    } else {
                        Log.e(TAG, "Failed to restore purchases: ${billingResult.debugMessage}")
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.billing_error_restore_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
     * 구독 정보 조회 (UI 표시용)
     *
     * 주의: 만료일은 추정값이며 보안 검증 목적으로 사용 불가
     *
     * @return SubscriptionInfo or null (비구독자)
     */
    fun getSubscriptionInfo(): SubscriptionInfo? {
        val purchase = currentPurchase ?: return null

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return null
        }

        val purchaseDate = Date(purchase.purchaseTime)
        val expiryDate = calculateExpiryDate(purchaseDate)

        return SubscriptionInfo(
            isActive = true,
            purchaseDate = purchaseDate,
            estimatedExpiryDate = expiryDate,
            autoRenewing = purchase.isAutoRenewing
        )
    }

    /**
     * 구독 만료일 계산
     *
     * unshort_premium_monthly는 1개월 구독이므로 하드코딩
     */
    private fun calculateExpiryDate(purchaseDate: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = purchaseDate
        calendar.add(Calendar.MONTH, 1)
        return calendar.time
    }

    /**
     * 구독 관리 페이지 열기
     *
     * Google Play Store의 구독 관리 페이지로 Deep Link
     */
    override fun openSubscriptionManagement(context: Context) {
        try {
            val packageName = context.packageName
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?sku=$PREMIUM_PRODUCT_ID&package=$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Play Store 앱이 없는 경우 등 에러 처리
            Log.e(TAG, "Failed to open subscription management", e)
        }
    }

    /**
     * 가격 정보 쿼리 (Public API)
     *
     * UI 표시용 가격 정보 조회
     * 24시간 캐싱으로 API 호출 최소화
     *
     * @param onResult 가격 정보 콜백 (null = 로드 실패)
     */
    fun queryPriceInfo(onResult: (PriceInfo?) -> Unit) {
        // 캐시 확인
        if (cachedPriceInfo != null &&
            System.currentTimeMillis() - lastPriceQueryTime < PRICE_CACHE_DURATION) {
            onResult(cachedPriceInfo)
            return
        }

        scope.launch {
            try {
                if (!ensureConnected()) {
                    withContext(Dispatchers.Main) {
                        onResult(null)
                    }
                    return@launch
                }

                // Product details 쿼리
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                billingClient?.queryProductDetailsAsync(params) { billingResult, queryResult ->
                    val productDetailsList = queryResult.productDetailsList
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        productDetailsList.isNotEmpty()) {

                        val productDetails = productDetailsList.first()
                        val priceInfo = extractPriceInfo(productDetails)

                        if (priceInfo != null) {
                            cachedPriceInfo = priceInfo
                            lastPriceQueryTime = System.currentTimeMillis()
                        }

                        onResult(priceInfo)
                    } else {
                        Log.e(TAG, "Failed to query price: ${billingResult.debugMessage}")
                        scope.launch(Dispatchers.Main) {
                            onResult(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying price", e)
                scope.launch(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }

    /**
     * 가격 정보 추출 헬퍼
     *
     * ProductDetails에서 무료 체험 및 가격 정보 추출
     *
     * @param productDetails Google Play ProductDetails
     * @return PriceInfo or null
     */
    private fun extractPriceInfo(productDetails: ProductDetails): PriceInfo? {
        val offers = productDetails.subscriptionOfferDetails ?: return null

        // 무료 체험 오퍼 우선 선택
        val selectedOffer = offers.find { offer ->
            offer.pricingPhases.pricingPhaseList.any { phase ->
                phase.priceAmountMicros == 0L
            }
        } ?: offers.firstOrNull() ?: return null

        val pricingPhases = selectedOffer.pricingPhases.pricingPhaseList

        // 무료 체험 정보 추출
        val trialPhase = pricingPhases.firstOrNull { it.priceAmountMicros == 0L }
        val hasFreeTrial = trialPhase != null
        val trialDays = when (trialPhase?.billingPeriod) {
            "P7D" -> 7
            "P14D" -> 14
            "P30D" -> 30
            else -> null
        }

        // 정기 결제 가격 추출
        val recurringPhase = pricingPhases.lastOrNull {
            it.priceAmountMicros > 0
        } ?: return null

        return PriceInfo(
            formattedPrice = recurringPhase.formattedPrice,
            hasFreeTrial = hasFreeTrial,
            trialDays = trialDays,
            offerToken = selectedOffer.offerToken,
            productDetails = productDetails
        )
    }

    /**
     * 리소스 정리
     */
    fun cleanup() {
        billingClient?.endConnection()
        billingClient = null
    }
}
