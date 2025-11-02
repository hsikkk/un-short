package com.muuu.shortblock.service.blocking

import android.util.Log
import com.muuu.unshort.OverlayType

/**
 * 세션 상태 관리자 (Event-Driven State Machine)
 *
 * 앱별로 독립적인 상태 관리
 */
class SessionStateManager {

    private val TAG = "SessionStateManager"

    // 앱별 상태 저장
    private val stateByPackage = mutableMapOf<String, ShortsSessionState>()
    private val previousStateByPackage = mutableMapOf<String, ShortsSessionState>()

    // 앱별 스크롤 감지 데이터
    private data class ScrollData(var hash: Int = 0, var stableCount: Int = 0)
    private val scrollDataByPackage = mutableMapOf<String, ScrollData>()
    private val STABLE_THRESHOLD = 2

    /**
     * 이벤트 처리
     */
    fun handleEvent(event: SessionEvent, packageName: String) {
        val current = stateByPackage[packageName] ?: ShortsSessionState.IDLE
        previousStateByPackage[packageName] = current

        val newState = when (event) {
            is SessionEvent.EnterShorts -> transitionOnEnterShorts(current)
            is SessionEvent.ExitShorts -> transitionOnExitShorts(packageName)
            is SessionEvent.EnterBackground -> transitionOnEnterBackground(current)
            is SessionEvent.ReturnToShorts -> transitionOnReturnToShorts(current)
            is SessionEvent.TimerCompleted -> transitionOnTimerCompleted(current)
            is SessionEvent.WatchConfirmed -> transitionOnWatchConfirmed(current)
            is SessionEvent.SkipConfirmed -> transitionOnSkipConfirmed(packageName)
            is SessionEvent.ContentHashChanged -> {
                handleContentHashChanged(event.hash, packageName)
                return
            }
            is SessionEvent.Reset -> transitionOnReset(packageName)
        }

        stateByPackage[packageName] = newState

        if (current != newState) {
            Log.d(TAG, "[$packageName] ${event::class.simpleName} | $current → $newState")
        }
    }

    // ========== Transition Methods ==========

    private fun transitionOnEnterShorts(current: ShortsSessionState) =
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER

    private fun transitionOnExitShorts(packageName: String): ShortsSessionState {
        scrollDataByPackage[packageName] = ScrollData()
        return ShortsSessionState.IDLE
    }

    private fun transitionOnEnterBackground(current: ShortsSessionState) = when (current) {
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL ->
            ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL
        else -> current
    }

    private fun transitionOnReturnToShorts(current: ShortsSessionState) = when (current) {
        ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER ->
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
        ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION ->
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
        ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL ->
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
        ShortsSessionState.IDLE ->
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
        else -> current
    }

    private fun transitionOnTimerCompleted(current: ShortsSessionState) = when (current) {
        ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        ShortsSessionState.IDLE ->
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        else -> {
            Log.w(TAG, "Timer completed in unexpected state: $current")
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
        }
    }

    private fun transitionOnWatchConfirmed(current: ShortsSessionState) = when (current) {
        ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
        else -> {
            Log.w(TAG, "Watch confirmed in unexpected state: $current")
            current
        }
    }

    private fun transitionOnSkipConfirmed(packageName: String): ShortsSessionState {
        scrollDataByPackage[packageName] = ScrollData()
        return ShortsSessionState.IDLE
    }

    private fun transitionOnReset(packageName: String): ShortsSessionState {
        scrollDataByPackage[packageName] = ScrollData()
        return ShortsSessionState.IDLE
    }

    /**
     * 콘텐츠 해시 변경 처리
     */
    private fun handleContentHashChanged(newHash: Int, packageName: String) {
        val scrollData = scrollDataByPackage.getOrPut(packageName) { ScrollData() }
        val current = stateByPackage[packageName] ?: ShortsSessionState.IDLE

        if (newHash == scrollData.hash) {
            scrollData.stableCount++
            return
        }

        val scrollDetected = scrollData.stableCount >= STABLE_THRESHOLD

        if (scrollDetected && current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
            Log.d(TAG, "[$packageName] Scroll detected: ${scrollData.hash} → $newHash")
            previousStateByPackage[packageName] = current
            val newState = ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
            stateByPackage[packageName] = newState
            Log.d(TAG, "[$packageName] Scroll event | $current → $newState")
        }

        scrollData.hash = newHash
        scrollData.stableCount = 0
    }

    // ========== Query Methods ==========

    fun getOverlayType(packageName: String): OverlayType? {
        return when (stateByPackage[packageName] ?: ShortsSessionState.IDLE) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER -> OverlayType.INITIAL
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION -> OverlayType.CONFIRMATION
            else -> null
        }
    }

    fun getCurrentState(packageName: String): ShortsSessionState {
        return stateByPackage[packageName] ?: ShortsSessionState.IDLE
    }

    fun getPreviousState(packageName: String): ShortsSessionState {
        return previousStateByPackage[packageName] ?: ShortsSessionState.IDLE
    }

    fun needsOverlay(packageName: String): Boolean {
        val state = stateByPackage[packageName] ?: ShortsSessionState.IDLE
        return state == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ||
               state == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION
    }

    fun isAllowed(packageName: String): Boolean {
        return stateByPackage[packageName] == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
    }
}
