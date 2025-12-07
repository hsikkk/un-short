package com.muuu.unshort.timer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.muuu.unshort.AppConstants
import com.muuu.unshort.R
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager

/**
 * 쇼츠 차단용 타이머 Activity
 *
 * 책임:
 * - 쇼츠 접근 시 30초 대기 + 폰 뒤집기 강제
 * - 타이머 완료 후 세션 ID 저장 및 브로드캐스트 전송
 * - 원본 앱(YouTube, Instagram 등)으로 복귀
 */
class ShortsBlockTimerActivity : BaseTimerActivity() {

    private var currentSessionId: String = ""
    private var sourcePackageName: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_timer

    override fun provideTimerDuration(): Int = prefsManager.waitTime

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get session ID and source package from intent
        currentSessionId = intent.getStringExtra("session_id") ?: ""
        sourcePackageName = intent.getStringExtra("source_package") ?: ""
        Log.d(TAG, "ShortsBlockTimer onCreate: session=$currentSessionId, source=$sourcePackageName")

        super.onCreate(savedInstanceState)

        // Track timer activity opened
        AnalyticsManager.trackEvent(this, AnalyticsEvent.TIMER_ACTIVITY_OPENED)
    }

    override fun onTimerCompleted() {
        // Mark timer as completed for this session
        if (currentSessionId.isNotEmpty()) {
            prefsManager.completedSessionId = currentSessionId
            Log.d(TAG, "Timer completed for session: $currentSessionId")
        }

        // Track analytics
        AnalyticsManager.trackEvent(this, AnalyticsEvent.TIMER_COMPLETED)

        // 결과 반환 (Overlay Activity로)
        setResult(RESULT_OK)

        // Broadcast도 유지 (AccessibilityService용)
        val intent = Intent(AppConstants.ACTION_TIMER_COMPLETED)
        intent.setPackage(packageName)
        intent.putExtra("session_id", currentSessionId)
        sendBroadcast(intent)
        Log.d(TAG, "Timer completed broadcast sent for session: $currentSessionId")

        // Activity 종료하여 Overlay로 복귀
        finish()
    }

    override fun onSkipClicked() {
        // 취소 결과 반환
        setResult(RESULT_CANCELED)

        // Broadcast 전송
        val intent = Intent(AppConstants.ACTION_CLOSE_OVERLAY)
        intent.setPackage(packageName)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)
        Log.d(TAG, "Skip clicked - broadcast sent to close overlay")

        returnToSourceApp()
    }

    override fun onContinueClicked() {
        returnToSourceApp()
    }

    private fun returnToSourceApp() {
        Log.d(TAG, "Returning to source app: $sourcePackageName")

        // Move this task to back and finish
        moveTaskToBack(true)

        // Finish this activity and all activities in this task
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
            Log.d(TAG, "finishAndRemoveTask() called")
        }, 100)
    }
}
