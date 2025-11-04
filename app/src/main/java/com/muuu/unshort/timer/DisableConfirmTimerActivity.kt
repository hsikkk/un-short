package com.muuu.unshort.timer

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.muuu.unshort.R
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager

/**
 * 차단 해제 확인용 타이머 Activity
 *
 * 책임:
 * - 차단 OFF 전환 시 30초 대기 + 폰 뒤집기 강제
 * - 타이머 완료 후 MainActivity로 결과 반환
 * - MainActivity에서 DisableConfirmDialog(문구 입력) 표시
 *
 * 사용 방법:
 * - MainActivity에서 Activity Result API로 호출
 * - 타이머 완료 시 RESULT_OK 반환
 * - 스킵 시 RESULT_CANCELED 반환
 */
class DisableConfirmTimerActivity : BaseTimerActivity() {

    override fun getLayoutResourceId(): Int = R.layout.activity_timer

    override fun provideTimerDuration(): Int = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Track timer activity opened for disable confirmation
        AnalyticsManager.trackEvent(this, AnalyticsEvent.DISABLE_CONFIRM_TIMER_OPENED)

        Log.d(TAG, "DisableConfirmTimer onCreate - timer duration: ${provideTimerDuration()} seconds")
    }

    override fun onTimerCompleted() {
        // Timer completed - return success result to MainActivity
        Log.d(TAG, "Timer completed - returning RESULT_OK")

        // Track analytics
        AnalyticsManager.trackEvent(this, AnalyticsEvent.DISABLE_CONFIRM_TIMER_COMPLETED)

        setResult(Activity.RESULT_OK)
    }

    override fun onSkipClicked() {
        // User clicked skip - return cancelled result
        Log.d(TAG, "Skip clicked - returning RESULT_CANCELED")

        // Track analytics
        AnalyticsManager.trackEvent(this, AnalyticsEvent.DISABLE_CONFIRM_TIMER_SKIPPED)

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onContinueClicked() {
        // Success screen continue button - close activity and let MainActivity handle
        Log.d(TAG, "Continue clicked - finishing activity")
        finish()
    }

    override fun shouldShowAd(): Boolean {
        // 차단 해제 확인에는 광고 표시하지 않음 (프리미엄 기능이므로)
        return false
    }
}
