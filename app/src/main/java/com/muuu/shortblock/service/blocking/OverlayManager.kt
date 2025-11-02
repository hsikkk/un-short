package com.muuu.shortblock.service.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.muuu.unshort.AppConstants
import com.muuu.unshort.BlockOverlay
import com.muuu.unshort.OverlayType

/**
 * 오버레이 생명주기 관리자
 *
 * 책임:
 * - BlockOverlay 인스턴스 생성 및 관리
 * - 오버레이 표시/숨김 처리
 * - 타이머 완료 브로드캐스트 수신 및 콜백 처리
 */
class OverlayManager(
    private val context: Context,
    private val onTimerCompleted: () -> Unit,
    private val onSkip: () -> Unit,
    private val onWatch: () -> Unit
) {
    private val TAG = "OverlayManager"

    private var blockOverlay: BlockOverlay? = null
    private var currentSessionId: String = ""
    private var lastShownSessionId: String = ""  // 마지막으로 표시한 세션 ID (타이머 브로드캐스트용)

    // 타이머 완료 브로드캐스트 수신
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AppConstants.ACTION_TIMER_COMPLETED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    Log.d(TAG, "Timer completed broadcast received for session: $sessionId")
                    Log.d(TAG, "Current session: $currentSessionId, Last shown: $lastShownSessionId")

                    // 현재 세션이거나 마지막으로 표시한 세션이면 처리
                    if (sessionId == currentSessionId || sessionId == lastShownSessionId) {
                        Log.d(TAG, "Session matches - calling onTimerCompleted callback")
                        onTimerCompleted()
                        // updateButtonVisibility 제거 - overlayType으로 UI 결정됨
                    } else {
                        Log.d(TAG, "Session mismatch - ignoring")
                    }
                }
                AppConstants.ACTION_CLOSE_OVERLAY -> {
                    Log.d(TAG, "Close overlay broadcast received")
                    hideOverlay()
                }
            }
        }
    }

    private var isReceiverRegistered = false

    /**
     * 초기화 - 브로드캐스트 리시버 등록
     */
    fun initialize() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_TIMER_COMPLETED)
                addAction(AppConstants.ACTION_CLOSE_OVERLAY)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(timerReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Timer receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver", e)
            }
        }
        Log.d(TAG, "OverlayManager initialized")
    }

    /**
     * 오버레이 뷰만 제거 (리시버와 세션 ID는 유지)
     * 타이머 완료 브로드캐스트를 받기 위해 리시버와 세션 ID는 Service 생명주기 동안 유지
     */
    fun hideOverlay() {
        blockOverlay?.let { overlay ->
            try {
                if (overlay.isShowing()) {
                    overlay.dismiss()
                    Log.d(TAG, "Overlay dismissed for session: $currentSessionId (session ID retained)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing overlay", e)
            }
        }

        blockOverlay = null
        // currentSessionId는 유지! (타이머 브로드캐스트를 받기 위해)
    }

    /**
     * 정리 - 브로드캐스트 리시버 해제 (Service onDestroy에서만 호출)
     */
    fun cleanup() {
        hideOverlay()

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(timerReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Timer receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }

        currentSessionId = ""
        lastShownSessionId = ""
        Log.d(TAG, "OverlayManager cleaned up")
    }

    /**
     * 오버레이 표시
     *
     * @param packageName 차단 대상 앱 패키지명
     * @param sessionId 세션 ID
     * @param overlayType 오버레이 타입 (INITIAL 또는 CONFIRMATION)
     * @return 생성된 세션 ID
     */
    fun showOverlay(
        packageName: String,
        sessionId: String,
        overlayType: OverlayType = OverlayType.INITIAL
    ): String {
        if (blockOverlay?.isShowing() == true) {
            Log.w(TAG, "Overlay already showing - not showing again")
            return currentSessionId
        }

        currentSessionId = sessionId
        lastShownSessionId = sessionId  // 마지막 세션 ID 저장

        try {
            val overlay = BlockOverlay(context)

            overlay.show(
                onDismiss = {
                    Log.d(TAG, "Overlay dismissed")
                    blockOverlay = null
                },
                onComplete = {
                    Log.d(TAG, "Timer completed")
                    // 타이머 완료 시 콜백 호출 (버튼만 변경, 오버레이는 유지)
                    onTimerCompleted()
                },
                onSkip = {
                    Log.d(TAG, "Skip button pressed")
                    hideOverlay()
                    onSkip()
                },
                onWatch = {
                    Log.d(TAG, "Watch button pressed")
                    hideOverlay()
                    onWatch()
                },
                sessionId = sessionId,
                sourcePackage = packageName,
                overlayType = overlayType
            )

            blockOverlay = overlay
            Log.d(TAG, "Overlay shown for session: $sessionId, package: $packageName, type: $overlayType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }

        return sessionId
    }

    /**
     * 현재 오버레이가 표시 중인지 확인
     */
    fun isOverlayVisible(): Boolean {
        return blockOverlay?.isShowing() == true
    }

    /**
     * 현재 세션 ID 반환
     */
    fun getCurrentSessionId(): String {
        return currentSessionId
    }
}
