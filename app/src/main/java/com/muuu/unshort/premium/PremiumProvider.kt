package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context

/**
 * 프리미엄 기능 제공자 인터페이스
 *
 * 다양한 결제 시스템(Google Play Billing, RevenueCat 등)을
 * 추상화하여 쉽게 교체할 수 있도록 설계
 */
interface PremiumProvider {

    /**
     * 사용자가 프리미엄 구독 중인지 확인
     *
     * @return true if premium, false otherwise
     */
    fun isPremium(): Boolean

    /**
     * 프리미엄 구매 플로우 시작
     *
     * @param activity 결제 화면을 표시할 Activity
     * @param onResult 결제 결과 콜백 (성공: true, 실패/취소: false)
     */
    fun startPurchaseFlow(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    )

    /**
     * 프리미엄 상태 동기화 (앱 시작 시 호출)
     *
     * @param onComplete 동기화 완료 콜백
     */
    fun syncPremiumStatus(onComplete: () -> Unit)

    /**
     * 구매 복원 (기기 변경 시 등)
     *
     * @param activity Activity 컨텍스트
     * @param onResult 복원 결과 콜백 (성공: true, 실패: false)
     */
    fun restorePurchases(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    )

    /**
     * 구독 관리 페이지 열기
     *
     * Play Store 등 플랫폼별 구독 관리 화면으로 이동
     *
     * @param context Context
     */
    fun openSubscriptionManagement(context: Context)
}
