package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 더미 프리미엄 제공자 (개발/테스트용)
 *
 * 실제 결제 연동 전에 프리미엄 기능을 테스트하기 위한 구현
 * 나중에 GooglePlayPremiumProvider나 RevenueCatPremiumProvider로 교체
 */
class DummyPremiumProvider(
    private val context: Context
) : PremiumProvider {

    companion object {
        private const val PREF_NAME = "dummy_premium_prefs"
        private const val KEY_IS_PREMIUM = "is_premium"
    }

    private val dummyPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun isPremium(): Boolean {
        // Dummy Pref에서 읽기
        return dummyPrefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    override fun startPurchaseFlow(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        // 더미: Pref에 저장하고 즉시 성공 처리
        dummyPrefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .apply()

        Toast.makeText(activity, "더미 구매 완료!", Toast.LENGTH_SHORT).show()
        onResult(true)
    }

    override fun syncPremiumStatus(onComplete: () -> Unit) {
        // 더미: 즉시 완료
        onComplete()
    }

    override fun restorePurchases(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        // 더미: 복원할 것 없음
        Toast.makeText(activity, "복원할 구매 내역이 없습니다", Toast.LENGTH_SHORT).show()
        onResult(false)
    }
}
