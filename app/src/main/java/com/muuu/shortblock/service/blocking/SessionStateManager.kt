package com.muuu.shortblock.service.blocking

import android.util.Log
import com.muuu.unshort.OverlayType

/**
 * 세션 상태 관리자 (Event-Driven State Machine)
 *
 * 책임:
 * - 세션 이벤트 처리
 * - 상태 전이 로직
 * - 오버레이 타입 결정
 * - 스크롤 감지
 */
class SessionStateManager {

    private val TAG = "SessionStateManager"

    // 현재 상태 및 이전 상태
    private var currentState: ShortsSessionState = ShortsSessionState.IDLE
    private var previousState: ShortsSessionState = ShortsSessionState.IDLE

    // 스크롤 감지를 위한 상태
    private var lastContentHash: Int = 0
    private var stableHashCount: Int = 0
    private val STABLE_THRESHOLD = 2

    /**
     * 이벤트 처리 - 모든 상태 전이는 이벤트를 통해 발생
     */
    fun handleEvent(event: SessionEvent) {
        previousState = currentState

        currentState = when (event) {
            is SessionEvent.EnterShorts -> transitionOnEnterShorts()
            is SessionEvent.ExitShorts -> transitionOnExitShorts()
            is SessionEvent.EnterBackground -> transitionOnEnterBackground()
            is SessionEvent.ReturnToShorts -> transitionOnReturnToShorts()
            is SessionEvent.TimerCompleted -> transitionOnTimerCompleted()
            is SessionEvent.WatchConfirmed -> transitionOnWatchConfirmed()
            is SessionEvent.SkipConfirmed -> transitionOnSkipConfirmed()
            is SessionEvent.ContentHashChanged -> {
                handleContentHashChanged(event.hash)
                return  // 스크롤 감지는 내부에서 처리
            }
            is SessionEvent.Reset -> transitionOnReset()
        }

        if (previousState != currentState) {
            Log.d(TAG, "Event: ${event::class.simpleName} | $previousState → $currentState")
        }
    }

    // ========== State Transition Methods ==========

    private fun transitionOnEnterShorts() = ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER

    private fun transitionOnExitShorts(): ShortsSessionState {
        lastContentHash = 0
        stableHashCount = 0
        return ShortsSessionState.IN_APP_NOT_SHORTS
    }

    private fun transitionOnEnterBackground() = when (currentState) {
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL ->
            ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL
        else -> currentState
    }

    private fun transitionOnReturnToShorts() = when (currentState) {
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

    private fun transitionOnTimerCompleted() = when (currentState) {
        ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        ShortsSessionState.IDLE ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        else -> {
            Log.w(TAG, "Timer completed in unexpected state: $currentState")
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        }
    }

    private fun transitionOnWatchConfirmed() = when (currentState) {
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
        else -> {
            Log.w(TAG, "Watch confirmed in unexpected state: $currentState")
            currentState
        }
    }

    private fun transitionOnSkipConfirmed(): ShortsSessionState {
        // "안볼래요" 클릭 - 앱 이탈 예정이므로 IDLE로
        lastContentHash = 0
        stableHashCount = 0
        return ShortsSessionState.IDLE
    }

    private fun transitionOnReset(): ShortsSessionState {
        lastContentHash = 0
        stableHashCount = 0
        return ShortsSessionState.IDLE
    }

    /**
     * 콘텐츠 해시 변경 처리 (스크롤 감지)
     */
    private fun handleContentHashChanged(newHash: Int) {
        if (newHash == lastContentHash) {
            stableHashCount++
            return
        }

        val scrollDetected = stableHashCount >= STABLE_THRESHOLD

        if (scrollDetected) {
            Log.d(TAG, "Scroll detected: hash $lastContentHash → $newHash")
            previousState = currentState

            // 스크롤 시 상태 전이
            currentState = when (currentState) {
                ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL ->
                    ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
                else -> currentState
            }

            if (previousState != currentState) {
                Log.d(TAG, "Scroll event | $previousState → $currentState")
            }
        }

        lastContentHash = newHash
        stableHashCount = 0
    }

    // ========== Query Methods ==========

    /**
     * 오버레이 타입 결정
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
    fun getCurrentState(): ShortsSessionState = currentState

    /**
     * 이전 상태 반환
     */
    fun getPreviousState(): ShortsSessionState = previousState

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
}
