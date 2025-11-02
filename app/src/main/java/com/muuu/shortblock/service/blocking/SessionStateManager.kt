package com.muuu.shortblock.service.blocking

import android.util.Log
import com.muuu.unshort.OverlayType

/**
 * 세션 상태 관리자
 *
 * 책임:
 * - 쇼츠 세션 상태 관리 (State Machine)
 * - 상태 전이 로직
 * - 오버레이 타입 결정
 * - 스크롤 감지 (콘텐츠 해시 기반)
 */
class SessionStateManager {

    private val TAG = "SessionStateManager"

    // 현재 상태
    private var currentState: ShortsSessionState = ShortsSessionState.IDLE

    // 스크롤 감지를 위한 상태
    private var lastContentHash: Int = 0
    private var stableHashCount: Int = 0
    private val STABLE_THRESHOLD = 2

    /**
     * 쇼츠 화면 진입
     */
    fun onEnterShorts() {
        val previousState = currentState
        currentState = ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
        Log.d(TAG, "State: $previousState → $currentState (entered shorts)")
    }

    /**
     * 쇼츠 화면 이탈
     */
    fun onExitShorts() {
        val previousState = currentState
        currentState = ShortsSessionState.IN_APP_NOT_SHORTS
        Log.d(TAG, "State: $previousState → $currentState (exited shorts)")

        // 해시 초기화
        lastContentHash = 0
        stableHashCount = 0
    }

    /**
     * 대상 앱 진입
     */
    fun onEnterTargetApp() {
        val previousState = currentState
        currentState = ShortsSessionState.IN_APP_NOT_SHORTS
        Log.d(TAG, "State: $previousState → $currentState (entered target app)")
    }

    /**
     * 대상 앱 이탈
     */
    fun onExitTargetApp() {
        val previousState = currentState
        currentState = ShortsSessionState.IDLE
        Log.d(TAG, "State: $previousState → $currentState (exited target app)")

        // 모든 상태 초기화
        lastContentHash = 0
        stableHashCount = 0
    }

    /**
     * 타이머 완료 (30초 또는 폰 뒤집기)
     * 타이머는 항상 TimerActivity(Background)에서 완료됨
     * 타이머 완료 = 확인 필요 (자동 허용 아님!)
     */
    fun onTimerCompleted() {
        val previousState = currentState

        currentState = when (currentState) {
            // Background에서 타이머 완료 (정상 케이스)
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION

            // 서비스 재시작 등 예외 상황 (persisted state 복원)
            ShortsSessionState.IDLE ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION

            else -> {
                Log.w(TAG, "Timer completed in unexpected state: $currentState - forcing BACKGROUND_BLOCKED_NEED_CONFIRMATION")
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
            }
        }

        Log.d(TAG, "State: $previousState → $currentState (timer completed)")
    }

    /**
     * "볼래요" 버튼 클릭 (Foreground에서만 가능)
     */
    fun onWatchConfirmed() {
        val previousState = currentState

        currentState = when (currentState) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
                ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
            else -> {
                Log.w(TAG, "Watch confirmed in unexpected state: $currentState")
                currentState
            }
        }

        Log.d(TAG, "State: $previousState → $currentState (watch confirmed)")
    }

    /**
     * 백그라운드 진입 (다른 앱 전환 또는 TimerActivity)
     */
    fun onEnterBackground() {
        val previousState = currentState

        currentState = when (currentState) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL ->
                ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL
            else -> currentState
        }

        if (previousState != currentState) {
            Log.d(TAG, "State: $previousState → $currentState (entered background)")
        }
    }

    /**
     * 쇼츠 화면으로 복귀
     */
    fun onReturnToShorts() {
        val previousState = currentState

        currentState = when (currentState) {
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER ->
                ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION ->
                ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
            ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL ->
                ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
            ShortsSessionState.IDLE, ShortsSessionState.IN_APP_NOT_SHORTS ->
                ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
            else -> currentState
        }

        if (previousState != currentState) {
            Log.d(TAG, "State: $previousState → $currentState (returned to shorts)")
        }
    }

    /**
     * 콘텐츠 해시 업데이트 및 스크롤 감지
     *
     * @param newHash 새로운 콘텐츠 해시
     * @return 스크롤이 감지되었으면 true
     */
    fun onContentHashChanged(newHash: Int): Boolean {
        if (newHash == lastContentHash) {
            stableHashCount++
            return false
        } else {
            val scrollDetected = stableHashCount >= STABLE_THRESHOLD

            if (scrollDetected) {
                Log.d(TAG, "Scroll detected: hash $lastContentHash → $newHash")
                onScrollDetected()
            }

            lastContentHash = newHash
            stableHashCount = 0
            return scrollDetected
        }
    }

    /**
     * 스크롤 감지 시 호출
     */
    private fun onScrollDetected() {
        val previousState = currentState

        when (currentState) {
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL -> {
                // 허용 상태에서 스크롤 → 확인 필요
                currentState = ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
                Log.d(TAG, "State: $previousState → $currentState (scrolled after allowed)")
            }
            else -> {
                Log.d(TAG, "Scroll in state $currentState - no action")
            }
        }
    }

    /**
     * 오버레이 타입 결정
     *
     * @return 표시할 오버레이 타입, null이면 오버레이 불필요
     */
    fun getOverlayType(): OverlayType? {
        return when (currentState) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER -> OverlayType.INITIAL
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION -> OverlayType.CONFIRMATION
            else -> null
        }
    }

    /**
     * 현재 상태 반환
     */
    fun getCurrentState(): ShortsSessionState {
        return currentState
    }

    /**
     * 오버레이 표시가 필요한 상태인지 확인
     */
    fun needsOverlay(): Boolean {
        return currentState == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ||
               currentState == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
    }

    /**
     * 시청 허용 상태인지 확인
     */
    fun isAllowed(): Boolean {
        return currentState == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
    }

    /**
     * 세션 초기화
     */
    fun reset() {
        val previousState = currentState
        currentState = ShortsSessionState.IDLE
        lastContentHash = 0
        stableHashCount = 0
        Log.d(TAG, "State: $previousState → $currentState (reset)")
    }
}
