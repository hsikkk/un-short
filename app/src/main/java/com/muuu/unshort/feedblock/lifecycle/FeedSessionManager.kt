package com.muuu.unshort.feedblock.lifecycle

import android.util.Log
import com.muuu.unshort.feedblock.FeedSessionState
import com.muuu.unshort.feedblock.FeedTarget

/**
 * 피드 세션 라이프사이클 관리자
 *
 * 책임:
 * - 진입 시 차단 여부 판단 (Idle → Blocked, Passed → 자유, Paused → grace 검사)
 * - 통과/거부 응답 처리
 * - 이탈/복귀 시 grace 기반 재차단 결정
 *
 * @param graceMs 다른 앱으로 이탈 후 같은 세션으로 인정되는 시간 (기본 60초)
 */
class FeedSessionManager(
    private val graceMs: Long = DEFAULT_GRACE_MS
) {

    @Volatile
    private var state: FeedSessionState = FeedSessionState.Idle

    @Synchronized
    fun getState(): FeedSessionState = state

    /**
     * 피드 진입 이벤트 처리. 차단 화면을 띄워야 하면 true 반환.
     */
    @Synchronized
    fun onEnterFeed(target: FeedTarget): Boolean {
        val now = System.currentTimeMillis()
        return when (val current = state) {
            FeedSessionState.Idle -> {
                state = FeedSessionState.Blocked(target.packageName)
                true
            }
            is FeedSessionState.Paused -> {
                val withinGrace = (now - current.pausedAt) <= graceMs &&
                    current.packageName == target.packageName
                if (withinGrace) {
                    Log.d(TAG, "Within grace - resuming Passed [${target.displayName}]")
                    state = FeedSessionState.Passed(target.packageName)
                    false
                } else {
                    Log.d(TAG, "Grace expired - re-blocking [${target.displayName}]")
                    state = FeedSessionState.Blocked(target.packageName)
                    true
                }
            }
            is FeedSessionState.Passed -> false
            is FeedSessionState.Blocked -> true
        }
    }

    /**
     * 피드/앱 이탈 처리. Passed 상태에서 호출되면 Paused로 전이.
     */
    @Synchronized
    fun onExitFeed() {
        val now = System.currentTimeMillis()
        val current = state
        if (current is FeedSessionState.Passed) {
            state = FeedSessionState.Paused(current.packageName, now)
        }
    }

    /**
     * 차단 화면 통과 ("계속 볼래요")
     */
    @Synchronized
    fun onUserContinue() {
        val current = state
        if (current is FeedSessionState.Blocked) {
            state = FeedSessionState.Passed(current.packageName)
        }
    }

    /**
     * 차단 화면 거부 ("그만 볼래요" / HOME)
     */
    @Synchronized
    fun onUserStop() {
        state = FeedSessionState.Idle
    }

    /**
     * 화면 OFF / 자정 리셋 / 일일 리셋 등 강제 초기화
     */
    @Synchronized
    fun reset() {
        state = FeedSessionState.Idle
    }

    companion object {
        private const val TAG = "FeedSessionManager"
        const val DEFAULT_GRACE_MS = 60_000L
    }
}
