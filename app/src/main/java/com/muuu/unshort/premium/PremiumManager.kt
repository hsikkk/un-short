package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.muuu.unshort.BuildConfig
import com.muuu.unshort.admin.DeviceAdminManager
import java.util.concurrent.TimeUnit

/**
 * 프리미엄 기능 관리자
 *
 * PremiumProvider를 래핑하여 앱 전체에서 프리미엄 상태를 관리
 * Callback 리스너 시스템으로 프리미엄 상태 변경 시 자동 갱신
 */
object PremiumManager {

    private lateinit var provider: PremiumProvider
    private lateinit var appContext: Context
    private var isPremiumCache: Boolean = false

    /**
     * 프리미엄 상태 변경 리스너 목록
     * 프리미엄 구매/복원 시 자동으로 호출됨
     */
    private val listeners = mutableListOf<() -> Unit>()

    /**
     * Application.onCreate()에서 호출
     *
     * @param context Application 컨텍스트
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Debug 빌드: DummyProvider, Release 빌드: GooglePlayBillingProvider
//        provider = GooglePlayBillingProvider(context)
        provider = if (BuildConfig.DEBUG) {
            DummyPremiumProvider(context)
        } else {
            GooglePlayBillingProvider(context)
        }

        // 초기 상태 동기화
        provider.syncPremiumStatus {
            isPremiumCache = provider.isPremium()
        }

        // WorkManager로 24시간 주기 동기화 스케줄
        schedulePremiumSync()
    }

    /**
     * WorkManager로 24시간마다 프리미엄 상태 동기화 스케줄
     *
     * 안전성:
     * - API 26+ 지원 (minSdk = 26)
     * - 백그라운드 제약 대응 (expedited 사용 안 함)
     * - 실패 시 포그라운드 동기화로 대체
     */
    private fun schedulePremiumSync() {
        try {
            val syncWork = PeriodicWorkRequestBuilder<PremiumSyncWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES  // flex interval
            )
                .build()

            WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(
                    "premium_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWork
                )
        } catch (e: Exception) {
            // WorkManager 초기화 실패 시 무시 (치명적이지 않음)
            // 포그라운드 동기화는 여전히 작동
            android.util.Log.w("PremiumManager", "Failed to schedule premium sync", e)
        }
    }

    /**
     * 프리미엄 여부 확인
     *
     * @return true if premium, false otherwise
     */
    fun isPremium(): Boolean {
        return isPremiumCache
    }

    /**
     * 프리미엄 상태 변경 리스너 등록
     *
     * 프리미엄 구매/복원 시 자동으로 호출됨
     * Activity의 Lifecycle에 연동하여 destroy 시 자동 제거 권장
     *
     * @param listener 프리미엄 상태 변경 시 호출될 콜백
     */
    fun addPremiumChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * 프리미엄 상태 변경 리스너 제거
     *
     * Activity destroy 시 호출하여 메모리 누수 방지
     *
     * @param listener 제거할 리스너
     */
    fun removePremiumChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 프리미엄 상태 동기화 (Public)
     *
     * 포그라운드 진입 시 또는 WorkManager에서 호출
     */
    fun syncPremiumStatus() {
        try {
            provider.syncPremiumStatus {
                val newStatus = provider.isPremium()
                if (isPremiumCache != newStatus) {
                    updatePremiumStatus(newStatus)
                }
            }
        } catch (e: Exception) {
            // 동기화 실패 시 현재 상태 유지
        }
    }

    /**
     * 프리미엄 상태 업데이트 (내부용)
     *
     * @param isPremium 새로운 프리미엄 상태
     */
    private fun updatePremiumStatus(isPremium: Boolean) {
        if (isPremiumCache != isPremium) {
            val wasPremium = isPremiumCache
            isPremiumCache = isPremium

            // 프리미엄 → 무료 전환 시 (구독 해지)
            if (wasPremium && !isPremium) {
                onPremiumDowngrade()
            }

            // 모든 리스너에게 알림
            listeners.forEach { listener ->
                listener.invoke()
            }
        }
    }

    /**
     * 프리미엄 해지 시 처리
     *
     * Device Admin 자동 해제 등
     */
    private fun onPremiumDowngrade() {
        try {
            // Device Admin 자동 해제
            val deviceAdminManager = DeviceAdminManager(appContext)
            if (deviceAdminManager.isDeviceAdminActive()) {
                deviceAdminManager.removeAdmin()
            }
        } catch (e: Exception) {
            // Device Admin 해제 실패 시 무시 (치명적이지 않음)
        }
    }

    /**
     * 프리미엄 구매 화면 표시
     *
     * @param activity Activity 컨텍스트
     * @param onResult 구매 결과 콜백 (성공: true, 실패/취소: false)
     */
    fun showPremiumPurchase(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        provider.startPurchaseFlow(activity) { success ->
            if (success) {
                updatePremiumStatus(true)
            }
            onResult(success)
        }
    }

    /**
     * 구매 복원
     *
     * @param activity Activity 컨텍스트
     * @param onResult 복원 결과 콜백 (성공: true, 실패: false)
     */
    fun restorePurchases(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        provider.restorePurchases(activity) { success ->
            if (success) {
                updatePremiumStatus(true)
            }
            onResult(success)
        }
    }

    /**
     * 구독 정보 조회 (UI 표시용)
     *
     * 주의: 만료일은 추정값이며 보안 검증 목적으로 사용 불가
     *
     * @return SubscriptionInfo or null (비구독자)
     */
    fun getSubscriptionInfo(): SubscriptionInfo? {
        return (provider as? GooglePlayBillingProvider)?.getSubscriptionInfo()
    }

    /**
     * 구독 관리 페이지 열기
     *
     * Provider를 통해 플랫폼별 구독 관리 페이지로 이동
     * (Google Play Store, App Store 등)
     *
     * @param context Context
     */
    fun openSubscriptionManagement(context: Context) {
        provider.openSubscriptionManagement(context)
    }
}
