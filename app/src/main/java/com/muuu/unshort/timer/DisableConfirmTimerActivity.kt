package com.muuu.unshort.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.muuu.unshort.R
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager

/**
 * 차단 해제 확인용 타이머 Activity
 *
 * 책임:
 * - 차단 OFF 전환 시 60초 대기 + 폰 뒤집기 강제
 * - 타이머 완료 후 호출자(Activity)로 결과 반환
 *
 * 사용 방법:
 * - Activity Result API로 호출
 * - 타이머 완료 시 RESULT_OK 반환
 * - 스킵 시 RESULT_CANCELED 반환
 * - `EXTRA_PROTECTION_CONTEXT`로 어떤 보호 흐름인지 분석 컨텍스트 전달
 */
class DisableConfirmTimerActivity : BaseTimerActivity() {

    companion object {
        const val EXTRA_PROTECTION_CONTEXT = "protection_context"

        // protection_context 값
        const val CONTEXT_MAIN_TOGGLE = "main_toggle"
        const val CONTEXT_APP_DISABLE = "app_disable"
        const val CONTEXT_PREVENT_DISABLE_OFF = "prevent_disable_off"
        const val CONTEXT_DEVICE_ADMIN_OFF = "device_admin_off"
        const val CONTEXT_SLEEP_MODE_OFF = "sleep_mode_off"
        const val CONTEXT_DAILY_LIMIT_OFF = "daily_limit_off"
        const val CONTEXT_UNKNOWN = "unknown"

        fun newIntent(context: Context, protectionContext: String): Intent {
            return Intent(context, DisableConfirmTimerActivity::class.java).apply {
                putExtra(EXTRA_PROTECTION_CONTEXT, protectionContext)
            }
        }
    }

    private var protectionContext: String = CONTEXT_UNKNOWN

    override fun getLayoutResourceId(): Int = R.layout.activity_timer

    override fun provideTimerDuration(): Int = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        protectionContext = intent.getStringExtra(EXTRA_PROTECTION_CONTEXT) ?: CONTEXT_UNKNOWN

        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.DISABLE_CONFIRM_TIMER_OPENED,
            contextProperties()
        )

        Log.d(TAG, "DisableConfirmTimer onCreate - duration=${provideTimerDuration()}s, context=$protectionContext")
    }

    override fun onTimerCompleted() {
        Log.d(TAG, "Timer completed - returning RESULT_OK")

        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.DISABLE_CONFIRM_TIMER_COMPLETED,
            contextProperties()
        )

        setResult(Activity.RESULT_OK)
    }

    override fun onSkipClicked() {
        Log.d(TAG, "Skip clicked - returning RESULT_CANCELED")

        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.DISABLE_CONFIRM_TIMER_SKIPPED,
            contextProperties()
        )

        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onContinueClicked() {
        Log.d(TAG, "Continue clicked - finishing activity")
        finish()
    }

    override fun shouldShowAd(): Boolean = false

    private fun contextProperties(): Map<String, Any?> = mapOf(
        "protection_context" to protectionContext,
        "timer_duration_seconds" to provideTimerDuration()
    )
}
