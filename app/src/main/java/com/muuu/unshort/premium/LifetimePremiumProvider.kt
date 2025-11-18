package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.promo.PromoCodeValidator

/**
 * Lifetime Premium Provider
 *
 * 프로모션 코드를 통한 영구 프리미엄 제공
 * Google Play Billing을 사용하지 않고 로컬 검증으로 동작
 */
class LifetimePremiumProvider(context: Context) : PremiumProvider {

    private val prefsManager = PreferencesManager(context)

    /**
     * Lifetime Premium 상태 확인
     *
     * SharedPreferences에 저장된 상태를 반환
     */
    override fun isPremium(): Boolean {
        return prefsManager.isLifetimePremium
    }

    /**
     * Lifetime Premium은 구매 플로우가 없음
     *
     * 프로모션 코드 입력을 통해서만 활성화 가능
     */
    override fun startPurchaseFlow(activity: Activity, onResult: (success: Boolean) -> Unit) {
        // Lifetime Premium은 구매가 아닌 프로모션 코드로만 활성화
        onResult(false)
    }

    /**
     * 프리미엄 상태 동기화
     *
     * Lifetime Premium은 로컬 상태만 확인하므로 동기화가 간단함
     */
    override fun syncPremiumStatus(onComplete: () -> Unit) {
        // 로컬 상태 확인만 수행
        val isActive = prefsManager.isLifetimePremium

        // 이미 로컬에 저장되어 있으므로 즉시 완료
        onComplete()
    }

    /**
     * 구매 복원
     *
     * Lifetime Premium은 기기에 종속되지 않고,
     * 같은 프로모션 코드로 여러 기기에서 재사용 불가능
     * (1회용 코드 정책)
     */
    override fun restorePurchases(activity: Activity, onResult: (success: Boolean) -> Unit) {
        // Lifetime Premium은 복원이 아닌 새 기기에서 재입력 필요
        // (보안상 1회용 코드 정책)
        onResult(false)
    }

    /**
     * 구독 관리 페이지 열기
     *
     * Lifetime Premium은 별도 관리 페이지가 없음
     */
    override fun openSubscriptionManagement(context: Context) {
        // Lifetime Premium은 구독 관리가 필요 없음
        // No-op
    }

    /**
     * 프로모션 코드로 Lifetime Premium 활성화
     *
     * @param code 프로모션 코드
     * @return ValidationResult
     */
    fun activateWithPromoCode(code: String): PromoCodeValidator.ValidationResult {
        val result = PromoCodeValidator.validate(code)

        if (result is PromoCodeValidator.ValidationResult.Valid) {
            // Lifetime Premium 활성화
            prefsManager.isLifetimePremium = true
            prefsManager.redeemedPromoCode = code
            prefsManager.redeemedAt = System.currentTimeMillis()
        }

        return result
    }

    /**
     * Lifetime Premium 비활성화
     *
     * 개발/테스트용
     */
    fun deactivate() {
        prefsManager.isLifetimePremium = false
        prefsManager.redeemedPromoCode = null
        prefsManager.redeemedAt = 0L
    }

    /**
     * 활성화된 프로모션 코드 정보 조회
     */
    fun getRedeemedInfo(): RedeemedInfo? {
        if (!prefsManager.isLifetimePremium) {
            return null
        }

        return RedeemedInfo(
            code = prefsManager.redeemedPromoCode ?: "",
            redeemedAt = prefsManager.redeemedAt
        )
    }

    data class RedeemedInfo(
        val code: String,
        val redeemedAt: Long
    )
}
