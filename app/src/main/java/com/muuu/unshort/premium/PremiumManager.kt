package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context

/**
 * 프리미엄 기능 관리자
 *
 * PremiumProvider를 래핑하여 앱 전체에서 프리미엄 상태를 관리
 * Callback 리스너 시스템으로 프리미엄 상태 변경 시 자동 갱신
 */
object PremiumManager {

    private lateinit var provider: PremiumProvider
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
        // TODO: 나중에 BuildConfig나 설정으로 Provider 선택 가능하게
        provider = DummyPremiumProvider(context)

        // 초기 상태 동기화
        provider.syncPremiumStatus {
            isPremiumCache = provider.isPremium()
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
     * 프리미엄 상태 업데이트 (내부용)
     *
     * @param isPremium 새로운 프리미엄 상태
     */
    private fun updatePremiumStatus(isPremium: Boolean) {
        if (isPremiumCache != isPremium) {
            isPremiumCache = isPremium

            // 모든 리스너에게 알림
            listeners.forEach { listener ->
                listener.invoke()
            }
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
}
