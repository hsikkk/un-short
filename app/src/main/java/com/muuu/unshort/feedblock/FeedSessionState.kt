package com.muuu.unshort.feedblock

/**
 * 피드 차단 세션 상태
 *
 * 컨셉: 진입 즉시 차단 화면 + grace 기반 단일 세션 정책.
 *
 * 전이:
 * - Idle → 피드 진입 → Blocked (overlay launch)
 * - Blocked → 사용자 OK → Passed
 * - Blocked → 사용자 Close → Idle (HOME 이탈)
 * - Passed → 피드/앱 이탈 → Paused (이탈 시각 기록)
 * - Paused → grace 이내 복귀 → Passed
 * - Paused → grace 경과 후 진입 → Idle 후 즉시 Blocked 재트리거
 */
sealed interface FeedSessionState {
    data object Idle : FeedSessionState

    data class Blocked(val packageName: String) : FeedSessionState

    data class Passed(val packageName: String) : FeedSessionState

    data class Paused(
        val packageName: String,
        val pausedAt: Long
    ) : FeedSessionState
}
