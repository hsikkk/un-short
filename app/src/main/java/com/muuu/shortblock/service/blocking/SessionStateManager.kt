package com.muuu.shortblock.service.blocking

import android.content.Context
import android.util.Log
import com.muuu.unshort.OverlayType

/**
 * 세션 상태 관리자 (Event-Driven State Machine)
 *
 * 앱별로 독립적인 상태 관리
 */
class SessionStateManager(
    private val context: Context,
    private val isBlockingEnabled: () -> Boolean,
    private val onSessionEnd: ((SessionEndInfo) -> Unit)? = null
) {

    private val TAG = "SessionStateManager"

    /**
     * 세션 종료 정보
     */
    data class SessionEndInfo(
        val packageName: String,
        val previousState: ShortsSessionState,
        val newState: ShortsSessionState,
        val event: SessionEvent,
        val didWatch: Boolean,
        val watchStartTime: Long?,
        val watchDurationMs: Long?
    )

    // 앱별 상태 저장
    private val stateByPackage = mutableMapOf<String, ShortsSessionState>()
    private val previousStateByPackage = mutableMapOf<String, ShortsSessionState>()

    // 앱별 시청 시간 추적
    private val watchTimeByPackage = mutableMapOf<String, WatchTimeTracker>()

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
            is SessionEvent.EnterShorts -> transitionOnEnterShorts(current, packageName)
            is SessionEvent.ExitShorts -> transitionOnExitShorts(current, packageName)
            is SessionEvent.EnterBackground -> transitionOnEnterBackground(current, packageName)
            is SessionEvent.ReturnToShorts -> transitionOnReturnToShorts(current, packageName)
            is SessionEvent.TimerCompleted -> transitionOnTimerCompleted(current)
            is SessionEvent.WatchConfirmed -> transitionOnWatchConfirmed(current, packageName)
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

        // 세션 종료 판단 및 콜백 호출
        if (shouldRecordSession(current, newState, event)) {
            val didWatch = current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
            val watchTracker = watchTimeByPackage[packageName]

            // 현재 시청 중이면 마지막 구간 추가
            watchTracker?.pause()

            onSessionEnd?.invoke(
                SessionEndInfo(
                    packageName = packageName,
                    previousState = current,
                    newState = newState,
                    event = event,
                    didWatch = didWatch,
                    watchStartTime = watchTracker?.getWatchStartTime(),
                    watchDurationMs = watchTracker?.getAccumulatedTime()
                )
            )
            Log.d(TAG, "[$packageName] Session recorded: didWatch=$didWatch, duration=${watchTracker?.getAccumulatedTime()}ms")

            // 시청 시간 초기화
            watchTracker?.reset()
        }
    }

    /**
     * 세션 기록이 필요한 상태 전이인지 판단
     */
    private fun shouldRecordSession(
        current: ShortsSessionState,
        newState: ShortsSessionState,
        event: SessionEvent
    ): Boolean {
        return when {
            // "안볼래요" 버튼
            event is SessionEvent.SkipConfirmed -> true

            // 시청 중 쇼츠 화면 이탈 (같은 앱 내 다른 화면)
            current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
                && newState == ShortsSessionState.IDLE
                && event is SessionEvent.ExitShorts -> true

            // 앱 완전 이탈 (Reset 이벤트)
            event is SessionEvent.Reset && current != ShortsSessionState.IDLE -> true

            else -> false
        }
    }

    // ========== Transition Methods ==========

    private fun transitionOnEnterShorts(
        current: ShortsSessionState,
        packageName: String
    ): ShortsSessionState {
        // Initialize ScrollData for scroll detection
        scrollDataByPackage[packageName] = ScrollData()

        // 차단이 OFF일 때: 즉시 ALLOWED 상태로 전환 + 시청 시간 측정 시작
        if (!isBlockingEnabled()) {
            Log.d(TAG, "[$packageName] Blocking disabled - allowing shorts and starting watch time tracking")
            val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }
            tracker.start()
            return ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
        }

        // Check if "block scrolled shorts only" feature is enabled
        val prefsManager = com.muuu.unshort.prefs.PreferencesManager(context)
        val blockScrolledOnly = prefsManager.isBlockScrolledOnly

        // If coming from IDLE and feature is enabled → Allow first shorts
        if (blockScrolledOnly && current == ShortsSessionState.IDLE) {
            Log.d(TAG, "[$packageName] First shorts allowed - ScrollData initialized")
            val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }
            tracker.start()
            return ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
        }

        // Default behavior: block and need timer
        Log.d(TAG, "[$packageName] Blocking shorts - ScrollData initialized")
        return ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
    }

    private fun transitionOnExitShorts(current: ShortsSessionState, packageName: String): ShortsSessionState {
        scrollDataByPackage[packageName] = ScrollData()
        // 시청 중이었으면 일시정지
        if (current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
            watchTimeByPackage[packageName]?.pause()
        }
        return ShortsSessionState.IDLE
    }

    private fun transitionOnEnterBackground(current: ShortsSessionState, packageName: String): ShortsSessionState {
        // 시청 중이었으면 일시정지
        if (current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
            watchTimeByPackage[packageName]?.pause()
        }

        return when (current) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION ->
                ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL ->
                ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL
            else -> current
        }
    }

    private fun transitionOnReturnToShorts(current: ShortsSessionState, packageName: String): ShortsSessionState {
        val newState = when (current) {
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

        // ALLOWED 상태로 복귀한 경우 시청 재개
        if (newState == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
            watchTimeByPackage[packageName]?.resume()
        }

        return newState
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

    private fun transitionOnWatchConfirmed(current: ShortsSessionState, packageName: String): ShortsSessionState {
        return when (current) {
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION -> {
                // 시청 시작
                val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }
                tracker.start()
                ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
            }
            else -> {
                Log.w(TAG, "Watch confirmed in unexpected state: $current")
                current
            }
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

        Log.d(TAG, "[$packageName] ContentHash: new=$newHash, old=${scrollData.hash}, stableCount=${scrollData.stableCount}")

        if (newHash == scrollData.hash) {
            scrollData.stableCount++
            Log.d(TAG, "[$packageName] Same hash, stableCount=${scrollData.stableCount}")
            return
        }

        val scrollDetected = scrollData.stableCount >= STABLE_THRESHOLD

        Log.d(TAG, "[$packageName] Hash changed! scrollDetected=$scrollDetected (stableCount=${scrollData.stableCount}, threshold=$STABLE_THRESHOLD)")

        if (scrollDetected && current == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
            Log.d(TAG, "[$packageName] Scroll detected: ${scrollData.hash} → $newHash")
            previousStateByPackage[packageName] = current

            // 세션 기록 먼저 수행 (스크롤 = 시청 완료)
            val watchTracker = watchTimeByPackage[packageName]
            watchTracker?.pause()

            // 세션 종료 정보 먼저 수집 (reset 전에)
            val sessionEndInfo = SessionEndInfo(
                packageName = packageName,
                previousState = current,
                newState = current, // 임시, 아래서 업데이트
                event = SessionEvent.ContentHashChanged(newHash),
                didWatch = true,
                watchStartTime = watchTracker?.getWatchStartTime(),
                watchDurationMs = watchTracker?.getAccumulatedTime()
            )
            Log.d(TAG, "[$packageName] Session recorded: didWatch=true (scroll), duration=${watchTracker?.getAccumulatedTime()}ms")

            // 항상 reset (차단 상태 무관)
            watchTracker?.reset()

            // 차단 상태에 따라 다음 상태 결정
            val newState = if (!isBlockingEnabled()) {
                // 차단 OFF: 즉시 새 세션 시작 (ALLOWED 상태 유지)
                Log.d(TAG, "[$packageName] Blocking disabled - staying in ALLOWED state after scroll, starting new session from 0")
                val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }
                tracker.start()
                ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL
            } else {
                // 차단 ON: 새 영상이므로 다시 차단 필요
                Log.d(TAG, "[$packageName] Scroll event | $current → IN_SHORTS_BLOCKED_NEED_TIMER (new video)")
                ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER
            }

            stateByPackage[packageName] = newState

            // 세션 종료 콜백 호출 (올바른 newState로)
            onSessionEnd?.invoke(sessionEndInfo.copy(newState = newState))
        } else {
            Log.d(TAG, "[$packageName] Scroll not triggered: scrollDetected=$scrollDetected, current=$current")
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
