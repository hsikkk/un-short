package com.muuu.shortblock.service.blocking

import android.content.Context
import android.util.Log
import com.muuu.unshort.BlockOverlay

/**
 * 오버레이 생명주기 관리자
 *
 * 책임:
 * - BlockOverlay 인스턴스 생성 및 관리
 * - 오버레이 표시/숨김 처리
 * - 타이머 완료 콜백 처리
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

    /**
     * 초기화 (현재는 불필요하지만 인터페이스 유지)
     */
    fun initialize() {
        Log.d(TAG, "OverlayManager initialized")
    }

    /**
     * 정리
     */
    fun cleanup() {
        hideOverlay()
        Log.d(TAG, "OverlayManager cleaned up")
    }

    /**
     * 오버레이 표시
     *
     * @param packageName 차단 대상 앱 패키지명
     * @return 생성된 세션 ID
     */
    fun showOverlay(packageName: String, sessionId: String): String {
        if (blockOverlay?.isShowing() == true) {
            Log.w(TAG, "Overlay already showing - not showing again")
            return currentSessionId
        }

        currentSessionId = sessionId

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
                sourcePackage = packageName
            )

            blockOverlay = overlay
            Log.d(TAG, "Overlay shown for session: $sessionId, package: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }

        return sessionId
    }

    /**
     * 오버레이 숨김
     */
    fun hideOverlay() {
        blockOverlay?.let { overlay ->
            try {
                if (overlay.isShowing()) {
                    overlay.dismiss()
                    Log.d(TAG, "Overlay dismissed for session: $currentSessionId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing overlay", e)
            }
        }

        blockOverlay = null
        currentSessionId = ""
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
