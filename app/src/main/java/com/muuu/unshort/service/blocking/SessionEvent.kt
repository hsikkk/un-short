package com.muuu.unshort.service.blocking
import com.muuu.unshort.ui.activity.ShortsBlockOverlayActivity

/**
 * Activity 타입
 *
 * Activity 생명주기 이벤트에서 어떤 Activity인지 구분
 */
enum class ActivityType {
    /**
     * 차단 화면 Activity (ShortsBlockOverlayActivity)
     */
    OVERLAY,

    /**
     * 타이머 Activity (ShortsBlockTimerActivity)
     */
    TIMER
}

/**
 * 세션 상태 변경 이벤트
 *
 * 모든 상태 전이는 이벤트를 통해 발생
 */
sealed interface SessionEvent {

    /**
     * 쇼츠 화면 진입
     */
    data object EnterShorts : SessionEvent

    /**
     * 쇼츠 화면 이탈 (앱 내 다른 화면으로)
     */
    data object ExitShorts : SessionEvent

    /**
     * Activity 시작
     *
     * @param type Activity 타입 (OVERLAY 또는 TIMER)
     * @param sessionId 세션 ID (Activity 시작 시 전달받은 값)
     */
    data class ActivityStarted(
        val type: ActivityType,
        val sessionId: String?
    ) : SessionEvent

    /**
     * Activity 종료
     *
     * @param type Activity 타입 (OVERLAY 또는 TIMER)
     */
    data class ActivityDestroyed(
        val type: ActivityType
    ) : SessionEvent

    /**
     * 타이머 완료 (30초 또는 폰 뒤집기)
     *
     * @param sessionId 타이머가 완료된 세션 ID
     */
    data class TimerCompleted(
        val sessionId: String
    ) : SessionEvent

    /**
     * "볼래요" 버튼 클릭
     */
    data object WatchConfirmed : SessionEvent

    /**
     * "안볼래요" 버튼 클릭
     */
    data object SkipConfirmed : SessionEvent

    /**
     * 콘텐츠 해시 변경 (스크롤 감지용)
     */
    data class ContentHashChanged(val hash: Int) : SessionEvent

    /**
     * 세션 리셋 (앱 종료, 서비스 재시작 등)
     */
    data object Reset : SessionEvent

    /**
     * 백그라운드 진입 (Deprecated)
     *
     * @deprecated Activity lifecycle events로 대체됨 (ActivityStarted, ActivityDestroyed)
     */
    @Deprecated(
        message = "Use ActivityStarted/ActivityDestroyed instead",
        level = DeprecationLevel.WARNING
    )
    data object EnterBackground : SessionEvent

    /**
     * 쇼츠 화면으로 복귀 (Deprecated)
     *
     * @deprecated Activity lifecycle events로 대체됨
     */
    @Deprecated(
        message = "Use ActivityDestroyed instead",
        level = DeprecationLevel.WARNING
    )
    data object ReturnToShorts : SessionEvent
}
