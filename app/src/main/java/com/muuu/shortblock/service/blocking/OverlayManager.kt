package com.muuu.shortblock.service.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.muuu.unshort.AppConstants
import com.muuu.unshort.OverlayType
import com.muuu.unshort.ShortsBlockOverlayActivity

/**
 * 차단 화면 Activity 관리자
 *
 * 앱별로 독립적인 차단 화면 Activity 관리
 */
class OverlayManager(
    private val context: Context,
    private val onTimerCompleted: () -> Unit,
    private val onSkip: () -> Unit,
    private val onWatch: () -> Unit
) {
    private val TAG = "OverlayManager"

    // 앱별 Activity 상태 저장
    private val activityVisibleByPackage = mutableMapOf<String, Boolean>()
    private val sessionIdByPackage = mutableMapOf<String, String>()
    private val lastShownSessionIdByPackage = mutableMapOf<String, String>()

    // 브로드캐스트 수신 (타이머 완료, Activity 닫기, Watch 확인)
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AppConstants.ACTION_TIMER_COMPLETED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    Log.d(TAG, "Timer completed broadcast: $sessionId")

                    // 모든 앱의 세션 ID 체크
                    sessionIdByPackage.forEach { (pkg, currentId) ->
                        val lastShownId = lastShownSessionIdByPackage[pkg]
                        if (sessionId == currentId || sessionId == lastShownId) {
                            Log.d(TAG, "[$pkg] Session matches - calling callback")
                            onTimerCompleted()
                        }
                    }
                }
                AppConstants.ACTION_CLOSE_OVERLAY -> {
                    Log.d(TAG, "Close overlay broadcast")
                    val sourcePackage = intent.getStringExtra("source_package")

                    // 해당 패키지의 Activity 상태 클리어
                    if (sourcePackage != null) {
                        hideOverlay(sourcePackage)
                    } else {
                        // 모든 Activity 닫기
                        activityVisibleByPackage.keys.toList().forEach { pkg ->
                            hideOverlay(pkg)
                        }
                    }

                    // Skip 콜백 호출
                    onSkip()
                }
                AppConstants.ACTION_WATCH_CONFIRMED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    val sourcePackage = intent.getStringExtra("source_package")
                    Log.d(TAG, "Watch confirmed broadcast: session=$sessionId, source=$sourcePackage")

                    // 해당 패키지의 Activity 상태 클리어
                    if (sourcePackage != null) {
                        hideOverlay(sourcePackage)
                    }

                    // Watch 콜백 호출
                    onWatch()
                }
            }
        }
    }

    private var isReceiverRegistered = false

    fun initialize() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_TIMER_COMPLETED)
                addAction(AppConstants.ACTION_CLOSE_OVERLAY)
                addAction(AppConstants.ACTION_WATCH_CONFIRMED)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(actionReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Action receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver", e)
            }
        }
    }

    /**
     * 차단 화면 Activity 표시
     */
    fun showOverlay(
        packageName: String,
        sessionId: String,
        overlayType: OverlayType = OverlayType.INITIAL
    ): String {
        // 기존 Activity가 있으면 상태만 업데이트
        if (activityVisibleByPackage[packageName] == true) {
            Log.d(TAG, "[$packageName] Activity already visible - updating overlay type to: $overlayType")
            // TODO: Activity에 브로드캐스트를 보내서 UI 업데이트
        }

        sessionIdByPackage[packageName] = sessionId
        lastShownSessionIdByPackage[packageName] = sessionId

        try {
            // Activity 시작
            val intent = Intent(context, ShortsBlockOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(ShortsBlockOverlayActivity.EXTRA_SESSION_ID, sessionId)
                putExtra(ShortsBlockOverlayActivity.EXTRA_SOURCE_PACKAGE, packageName)
                putExtra(ShortsBlockOverlayActivity.EXTRA_OVERLAY_TYPE, overlayType.name)
            }

            context.startActivity(intent)
            activityVisibleByPackage[packageName] = true

            Log.d(TAG, "[$packageName] Activity started: session=$sessionId, type=$overlayType")
        } catch (e: Exception) {
            Log.e(TAG, "[$packageName] Failed to start activity", e)
        }

        return sessionId
    }

    /**
     * 차단 화면 Activity 숨김
     */
    fun hideOverlay(packageName: String) {
        activityVisibleByPackage[packageName] = false
        Log.d(TAG, "[$packageName] Activity hidden (state cleared)")
    }

    /**
     * 차단 화면 Activity 표시 여부
     */
    fun isOverlayVisible(packageName: String): Boolean {
        return activityVisibleByPackage[packageName] == true
    }

    /**
     * 정리
     */
    fun cleanup() {
        // 모든 Activity 상태 제거
        activityVisibleByPackage.clear()

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(actionReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Action receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }

        sessionIdByPackage.clear()
        lastShownSessionIdByPackage.clear()
        Log.d(TAG, "OverlayManager cleaned up")
    }
}
