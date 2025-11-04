package com.muuu.unshort.premium

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

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
        // 더미 다이얼로그 표시
        AlertDialog.Builder(activity)
            .setTitle("✨ 프리미엄 업그레이드")
            .setMessage(
                """
                프리미엄 기능:

                ✅ 광고 제거
                ✅ 타이머 자유 설정 (5초~5분)
                ✅ 스크롤한 쇼츠만 차단
                ✅ 충동적 해제 방지
                ✅ 앱 삭제 방지

                결제 시스템 준비 중입니다.
                """.trimIndent()
            )
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }
            .show()
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
