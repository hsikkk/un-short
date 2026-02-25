package com.muuu.unshort.service.blocking

import android.content.Context
import android.util.Log
import com.muuu.unshort.config.OverlayType

/**
 * 세션 상태 관리자 (Event-Driven State Machine)
 *
 * 앱별로 독립적인 상태 관리
 * Activity 기반 3차원 상태 시스템 사용
 */
class SessionStateManager(
    private val context: Context,
    private val isBlockingEnabled: () -> Boolean,
    private val onSessionEnd: ((SessionEndInfo) -> Unit)? = null,
    private val onStateChanged: ((SessionState) -> Unit)? = null,
    private val onScrollDetected: ((String) -> Unit)? = null  // 스크롤 감지 콜백
) {

    private val TAG = "SessionStateManager"

    /**
     * 세션 종료 정보
     */
    data class SessionEndInfo(
        val packageName: String,
        val previousState: SessionState,
        val newState: SessionState,
        val event: SessionEvent,
        val didWatch: Boolean,
        val watchStartTime: Long?,
        val watchDurationMs: Long?
    )

    companion object {
        private const val MAX_CACHED_APPS = 10  // 최근 사용된 앱만 유지하여 메모리 누수 방지
    }

    // 앱별 상태 저장 (LRU 캐시, 최대 10개 앱)
    private val stateByPackage = object : LinkedHashMap<String, SessionState>(
        MAX_CACHED_APPS + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SessionState>?): Boolean {
            return size > MAX_CACHED_APPS
        }
    }

    // 앱별 시청 시간 추적 (LRU 캐시, 최대 10개 앱)
    private val watchTimeByPackage = object : LinkedHashMap<String, WatchTimeTracker>(
        MAX_CACHED_APPS + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WatchTimeTracker>?): Boolean {
            return size > MAX_CACHED_APPS
        }
    }

    // 앱별 스크롤 감지 데이터
    private data class ScrollData(var hash: Int = 0, var stableCount: Int = 0)
    private val scrollDataByPackage = object : LinkedHashMap<String, ScrollData>(
        MAX_CACHED_APPS + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ScrollData>?): Boolean {
            return size > MAX_CACHED_APPS
        }
    }
    private val STABLE_THRESHOLD = 2

    // 앱별 콘텐츠 핑거프린트 (유사도 비교용, LRU 캐시)
    private val fingerprintByPackage = object : LinkedHashMap<String, ContentFingerprint>(
        MAX_CACHED_APPS + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ContentFingerprint>?): Boolean {
            return size > MAX_CACHED_APPS
        }
    }

    /**
     * 이벤트 처리 (단일 전이 함수)
     */
    fun handleEvent(event: SessionEvent, packageName: String) {
        val current = stateByPackage[packageName] ?: SessionState.IDLE

        // ContentHashChanged는 특수 처리 (상태 전이 없음, 스크롤 감지만)
        if (event is SessionEvent.ContentHashChanged) {
            // Activity 실행 중(백그라운드)이면 ContentHashChanged 무시
            // 백그라운드에서 돌아올 때 UI 재로드로 인한 오감지 방지
            if (current.activityState != ActivityState.NONE) {
                Log.d(TAG, "[$packageName] Activity running (${current.activityState}) - ignoring ContentHashChanged")
                return
            }
            handleContentHashChanged(event.hash, event.fingerprint, packageName)
            return
        }

        val newState = computeNewState(current, event, packageName)

        if (current != newState) {
            stateByPackage[packageName] = newState
            Log.d(TAG, "[$packageName] ${event::class.simpleName} | $current → $newState")
            onStateChanged?.invoke(newState)
        }

        // 시청 시간 추적 업데이트
        updateWatchTimeTracking(current, newState, packageName)

        // 세션 종료 판단 및 콜백 호출
        handleSessionEnd(current, newState, event, packageName)
    }

    /**
     * 통합 상태 전이 함수
     *
     * 모든 이벤트에 대한 상태 전이를 한 곳에서 관리
     */
    private fun computeNewState(
        current: SessionState,
        event: SessionEvent,
        packageName: String
    ): SessionState {
        return when (event) {
            is SessionEvent.EnterShorts -> {
                // 스크롤 데이터 초기화
                scrollDataByPackage[packageName] = ScrollData()

                // 차단 OFF: 즉시 시청 허용
                if (!isBlockingEnabled()) {
                    Log.d(TAG, "[$packageName] Blocking disabled - allowing shorts")
                    return current.copy(
                        blockingStage = BlockingStage.WATCHING,
                        shortsLocation = ShortsLocation.IN_SHORTS,
                        sessionId = generateSessionId()
                    )
                }

                // "첫 영상만 허용" 기능 확인
                val prefsManager = com.muuu.unshort.prefs.PreferencesManager(context)
                val blockScrolledOnly = prefsManager.isBlockScrolledOnly

                // IDLE에서 진입 + 기능 활성화 → 첫 영상 허용
                if (blockScrolledOnly && current.shortsLocation == ShortsLocation.OUTSIDE) {
                    Log.d(TAG, "[$packageName] First shorts allowed (blockScrolledOnly)")
                    return current.copy(
                        blockingStage = BlockingStage.WATCHING,
                        shortsLocation = ShortsLocation.IN_SHORTS,
                        sessionId = generateSessionId()
                    )
                }

                // 기본: 타이머 필요
                Log.d(TAG, "[$packageName] Blocking shorts - need timer")
                current.copy(
                    blockingStage = BlockingStage.NEED_TIMER,
                    shortsLocation = ShortsLocation.IN_SHORTS,
                    sessionId = generateSessionId()
                )
            }

            is SessionEvent.ExitShorts -> {
                // 쇼츠 화면 이탈
                scrollDataByPackage[packageName] = ScrollData()
                fingerprintByPackage.remove(packageName)
                current.copy(
                    blockingStage = BlockingStage.NONE,
                    shortsLocation = ShortsLocation.OUTSIDE,
                    sessionId = null
                )
            }

            is SessionEvent.ActivityStarted -> {
                // Activity 시작
                val newActivityState = when (event.type) {
                    ActivityType.OVERLAY -> ActivityState.OVERLAY_RUNNING
                    ActivityType.TIMER -> ActivityState.TIMER_RUNNING
                }
                current.copy(activityState = newActivityState)
            }

            is SessionEvent.ActivityDestroyed -> {
                // Activity 종료 (백그라운드에서 복귀)
                // 백그라운드 복귀 시 UI 재로드를 위해 scrollData 리셋
                scrollDataByPackage[packageName] = ScrollData()
                Log.d(TAG, "[$packageName] Activity destroyed - reset scrollData for UI reload")
                current.copy(activityState = ActivityState.NONE)
            }

            is SessionEvent.TimerCompleted -> {
                // 타이머 완료
                // sessionId 검증 (옵션)
                if (event.sessionId != current.sessionId) {
                    Log.w(TAG, "[$packageName] Timer sessionId mismatch: ${event.sessionId} vs ${current.sessionId}")
                }

                current.copy(
                    blockingStage = BlockingStage.NEED_CONFIRMATION,
                    activityState = ActivityState.OVERLAY_RUNNING
                )
            }

            is SessionEvent.WatchConfirmed -> {
                // "볼래요" 클릭
                current.copy(
                    blockingStage = BlockingStage.WATCHING
                    // activityState는 ActivityDestroyed에서만 변경
                )
            }

            is SessionEvent.SkipConfirmed -> {
                // "안볼래요" 클릭
                scrollDataByPackage[packageName] = ScrollData()
                fingerprintByPackage.remove(packageName)
                current.copy(
                    blockingStage = BlockingStage.NONE,
                    shortsLocation = ShortsLocation.OUTSIDE,
                    // activityState는 ActivityDestroyed에서만 변경
                    sessionId = null
                )
            }

            is SessionEvent.Reset -> {
                // 앱 이탈 또는 서비스 재시작
                scrollDataByPackage[packageName] = ScrollData()
                fingerprintByPackage.remove(packageName)
                SessionState.IDLE
            }

            // Deprecated events (하위 호환성)
            SessionEvent.EnterBackground -> {
                @Suppress("DEPRECATION")
                Log.w(TAG, "[$packageName] EnterBackground is deprecated - use ActivityStarted instead")
                current
            }

            SessionEvent.ReturnToShorts -> {
                @Suppress("DEPRECATION")
                Log.w(TAG, "[$packageName] ReturnToShorts is deprecated - use ActivityDestroyed instead")
                current
            }

            else -> {
                Log.w(TAG, "[$packageName] Unknown event: $event")
                current
            }
        }
    }

    /**
     * 시청 시간 추적 업데이트
     */
    private fun updateWatchTimeTracking(
        previous: SessionState,
        current: SessionState,
        packageName: String
    ) {
        val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }

        val wasWatching = previous.isWatching()
        val isWatching = current.isWatching()

        when {
            // 시청 시작
            !wasWatching && isWatching -> {
                Log.d(TAG, "[$packageName] Watch started")
                tracker.start()
            }

            // 시청 일시정지 (Activity 실행)
            wasWatching && !isWatching && current.activityState != ActivityState.NONE -> {
                Log.d(TAG, "[$packageName] Watch paused (Activity running)")
                tracker.pause()
            }

            // 시청 재개 (Activity 종료)
            !wasWatching && isWatching && previous.activityState != ActivityState.NONE -> {
                Log.d(TAG, "[$packageName] Watch resumed (Activity destroyed)")
                tracker.resume()
            }

            // 시청 종료
            wasWatching && !isWatching -> {
                Log.d(TAG, "[$packageName] Watch ended")
                tracker.pause()
            }
        }
    }

    /**
     * 세션 종료 처리
     */
    private fun handleSessionEnd(
        previous: SessionState,
        current: SessionState,
        event: SessionEvent,
        packageName: String
    ) {
        if (!shouldRecordSession(previous, current, event)) {
            return
        }

        val didWatch = previous.blockingStage == BlockingStage.WATCHING
        val watchTracker = watchTimeByPackage[packageName]

        // 현재 시청 중이면 마지막 구간 추가
        watchTracker?.pause()

        onSessionEnd?.invoke(
            SessionEndInfo(
                packageName = packageName,
                previousState = previous,
                newState = current,
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

    /**
     * 세션 기록이 필요한 상태 전이인지 판단
     */
    private fun shouldRecordSession(
        previous: SessionState,
        current: SessionState,
        event: SessionEvent
    ): Boolean {
        return when {
            // "안볼래요" 버튼
            event is SessionEvent.SkipConfirmed -> true

            // 시청 중 쇼츠 화면 이탈
            previous.blockingStage == BlockingStage.WATCHING &&
            current.shortsLocation == ShortsLocation.OUTSIDE &&
            event is SessionEvent.ExitShorts -> true

            // 앱 완전 이탈 (Reset 이벤트)
            event is SessionEvent.Reset &&
            previous.shortsLocation == ShortsLocation.IN_SHORTS -> true

            else -> false
        }
    }

    /**
     * 콘텐츠 해시 변경 처리 (스크롤 감지)
     */
    private fun handleContentHashChanged(newHash: Int, newFingerprint: ContentFingerprint?, packageName: String) {
        val scrollData = scrollDataByPackage.getOrPut(packageName) { ScrollData() }
        val current = stateByPackage[packageName] ?: SessionState.IDLE

        Log.d(TAG, "[$packageName] ContentHash: new=$newHash, old=${scrollData.hash}, stableCount=${scrollData.stableCount}")

        if (newHash == scrollData.hash) {
            scrollData.stableCount++
            Log.d(TAG, "[$packageName] Same hash, stableCount=${scrollData.stableCount}")
            return
        }

        // 핑거프린트 유사도 비교 (YouTube의 경우)
        if (newFingerprint != null) {
            val oldFingerprint = fingerprintByPackage[packageName]

            // 새 핑거프린트가 유효하지 않은 경우 (재생 완료/일시정지로 UI 사라짐)
            if (!newFingerprint.isValid()) {
                Log.d(TAG, "[$packageName] New fingerprint is invalid (title=${newFingerprint.titleHash}, channel=${newFingerprint.channelHash}, desc=${newFingerprint.descriptionHash}) - skipping fingerprint check, using hash-based detection")
                // 핑거프린트 비교 스킵, 해시 기반 스크롤 감지로 진행
            } else if (oldFingerprint != null && oldFingerprint.isValid()) {
                // 둘 다 유효한 경우에만 유사도 비교
                val similarity = newFingerprint.similarityWith(oldFingerprint)
                Log.d(TAG, "[$packageName] Fingerprint similarity: $similarity (title=${newFingerprint.titleHash == oldFingerprint.titleHash}, channel=${newFingerprint.channelHash == oldFingerprint.channelHash}, desc=${newFingerprint.descriptionHash == oldFingerprint.descriptionHash})")

                if (newFingerprint.isSimilarTo(oldFingerprint)) {
                    Log.d(TAG, "[$packageName] Content is similar (${similarity * 100}%) - UI change detected, ignoring scroll")
                    // 동일 콘텐츠로 판단 - 해시만 업데이트하고 스크롤 처리 안 함
                    scrollData.hash = newHash
                    scrollData.stableCount = 0
                    fingerprintByPackage[packageName] = newFingerprint
                    return
                }

                Log.d(TAG, "[$packageName] Content is different (similarity: ${similarity * 100}%) - new shorts detected")
                fingerprintByPackage[packageName] = newFingerprint
            } else {
                // 이전 핑거프린트가 없거나 무효 - 새 유효한 핑거프린트 저장
                Log.d(TAG, "[$packageName] Saving new valid fingerprint (old was ${if (oldFingerprint == null) "null" else "invalid"})")
                fingerprintByPackage[packageName] = newFingerprint
            }
        }

        val scrollDetected = scrollData.stableCount >= STABLE_THRESHOLD

        Log.d(TAG, "[$packageName] Hash changed! scrollDetected=$scrollDetected (stableCount=${scrollData.stableCount}, threshold=$STABLE_THRESHOLD)")

        if (scrollDetected && current.blockingStage == BlockingStage.WATCHING) {
            Log.d(TAG, "[$packageName] Scroll detected: ${scrollData.hash} → $newHash")

            // 스크롤 감지 콜백 호출
            onScrollDetected?.invoke(packageName)

            // 세션 기록 먼저 수행 (스크롤 = 시청 완료)
            val watchTracker = watchTimeByPackage[packageName]
            watchTracker?.pause()

            // 세션 종료 정보 수집
            val previousState = current
            val sessionEndInfo = SessionEndInfo(
                packageName = packageName,
                previousState = previousState,
                newState = current, // 임시, 아래서 업데이트
                event = SessionEvent.ContentHashChanged(newHash),
                didWatch = true,
                watchStartTime = watchTracker?.getWatchStartTime(),
                watchDurationMs = watchTracker?.getAccumulatedTime()
            )
            Log.d(TAG, "[$packageName] Session recorded: didWatch=true (scroll), duration=${watchTracker?.getAccumulatedTime()}ms")

            // 시청 시간 초기화
            watchTracker?.reset()

            // 차단 상태에 따라 다음 상태 결정
            val newState = if (!isBlockingEnabled()) {
                // 차단 OFF: 즉시 새 세션 시작 (ALLOWED 상태 유지)
                Log.d(TAG, "[$packageName] Blocking disabled - staying in WATCHING state after scroll, starting new session")
                val tracker = watchTimeByPackage.getOrPut(packageName) { WatchTimeTracker() }
                tracker.start()
                current.copy(sessionId = generateSessionId())
            } else {
                // 차단 ON: 새 영상이므로 다시 차단 필요
                Log.d(TAG, "[$packageName] Scroll event | $current → NEED_TIMER (new video)")
                current.copy(
                    blockingStage = BlockingStage.NEED_TIMER,
                    activityState = ActivityState.NONE,
                    sessionId = generateSessionId()
                )
            }

            stateByPackage[packageName] = newState
            onStateChanged?.invoke(newState)

            // 세션 종료 콜백 호출 (올바른 newState로)
            onSessionEnd?.invoke(sessionEndInfo.copy(newState = newState))
        } else {
            Log.d(TAG, "[$packageName] Scroll not triggered: scrollDetected=$scrollDetected, current=${current.blockingStage}")
        }

        scrollData.hash = newHash
        scrollData.stableCount = 0
    }

    /**
     * 세션 ID 생성
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }

    // ========== Query Methods ==========

    /**
     * 현재 상태 조회
     */
    fun getCurrentState(packageName: String): SessionState {
        return stateByPackage[packageName] ?: SessionState.IDLE
    }

    /**
     * 오버레이 표시 필요 여부
     */
    fun needsOverlay(packageName: String): Boolean {
        return getCurrentState(packageName).needsOverlay()
    }

    /**
     * 오버레이 타입 조회
     */
    fun getOverlayType(packageName: String): OverlayType? {
        return getCurrentState(packageName).getOverlayType()
    }

    /**
     * 시청 허용 여부
     */
    fun isAllowed(packageName: String): Boolean {
        return getCurrentState(packageName).isWatching()
    }

    /**
     * 미디어 일시정지 필요 여부
     */
    fun shouldPauseMedia(packageName: String): Boolean {
        return getCurrentState(packageName).shouldPauseMedia()
    }
}
