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
        /**
         * 개발/테스트용 플래그
         * true로 설정하면 모든 프리미엄 기능 활성화
         *
         * TODO: 실제 배포 시 제거하거나 BuildConfig로 제어
         */
        private const val DEBUG_PREMIUM_ENABLED = false
    }

    override fun isPremium(): Boolean {
        // 개발 중에는 true로 설정해서 프리미엄 기능 테스트 가능
        return DEBUG_PREMIUM_ENABLED
    }

    override fun startPurchaseFlow(
        activity: Activity,
        onResult: (success: Boolean) -> Unit
    ) {
        // PremiumUpgradeActivity 실행
        val intent = Intent(activity, Class.forName("com.muuu.unshort.PremiumUpgradeActivity"))
        activity.startActivity(intent)

        // 더미: 항상 실패 (구매 안 함)
        onResult(false)
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
