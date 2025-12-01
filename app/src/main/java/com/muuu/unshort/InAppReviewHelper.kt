package com.muuu.unshort

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager

/**
 * Google Play In-App Review 관리
 *
 * 온보딩 완료 후 7일 경과 시 리뷰 요청 (1회만)
 */
class InAppReviewHelper(private val activity: Activity) {

    private val prefsManager = PreferencesManager(activity)
    private val reviewManager = ReviewManagerFactory.create(activity)

    companion object {
        private const val REVIEW_DELAY_MS = 7 * 24 * 60 * 60 * 1000L // 7일
        private const val TAG = "InAppReviewHelper"
    }

    /**
     * 리뷰 요청 조건 확인 후 표시
     */
    fun checkAndShowReviewIfEligible() {
        // 이미 표시한 경우 스킵
        if (prefsManager.hasReviewPromptShown) {
            return
        }

        // 온보딩 완료 시점 확인
        val completedAt = prefsManager.onboardingCompletedAt
        if (completedAt == 0L) {
            // 아직 온보딩 완료 시점이 기록되지 않음 (구버전 사용자)
            return
        }

        // 7일 경과 확인
        val elapsedMs = System.currentTimeMillis() - completedAt
        if (elapsedMs < REVIEW_DELAY_MS) {
            return
        }

        // 조건 충족 - 리뷰 요청
        showReview()
    }

    private fun showReview() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // 성공 여부와 관계없이 플래그 설정 (1회만 표시)
                    prefsManager.hasReviewPromptShown = true

                    // 분석 이벤트 기록
                    AnalyticsManager.trackEvent(
                        activity,
                        AnalyticsEvent.REVIEW_PROMPT_SHOWN
                    )

                    Log.d(TAG, "Review flow completed")
                }
            } else {
                // API 요청 실패 - 조용히 실패
                Log.e(TAG, "Review request failed", task.exception)
                prefsManager.hasReviewPromptShown = true
            }
        }
    }
}
