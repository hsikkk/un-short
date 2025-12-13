package com.muuu.unshort.service.blocking

import com.muuu.unshort.config.OverlayType

/**
 * 차단 진행 단계
 *
 * 쇼츠 차단 프로세스의 진행 상태
 */
enum class BlockingStage {
    /**
     * 차단 없음 (쇼츠 화면 아님)
     */
    NONE,

    /**
     * 타이머 진행 필요
     * - 쇼츠 진입 시 초기 상태
     * - 타이머 완료 전까지 유지
     */
    NEED_TIMER,

    /**
     * 타이머 완료 후 최종 확인 필요
     * - "볼래요" / "안볼래요" 선택 대기
     */
    NEED_CONFIRMATION,

    /**
     * 시청 허용 (다음 스크롤까지)
     * - "볼래요" 선택 후 상태
     * - 스크롤 감지 시 NEED_TIMER로 전환
     */
    WATCHING
}

/**
 * Activity 실행 상태
 *
 * 현재 어떤 Activity가 실행 중인지 추적
 */
enum class ActivityState {
    /**
     * Activity 없음
     * - 쇼츠 앱만 foreground에 있음
     */
    NONE,

    /**
     * OverlayActivity 실행 중
     * - 차단 화면 표시 중
     */
    OVERLAY_RUNNING,

    /**
     * TimerActivity 실행 중
     * - 타이머 진행 중
     */
    TIMER_RUNNING
}

/**
 * 쇼츠 앱 위치
 *
 * 사용자가 쇼츠 화면에 있는지 여부
 */
enum class ShortsLocation {
    /**
     * 쇼츠 화면 아님
     * - 앱 외부 또는 앱 내 다른 화면
     */
    OUTSIDE,

    /**
     * 쇼츠 화면
     * - YouTube Shorts, Instagram Reels, TikTok 등
     */
    IN_SHORTS
}

/**
 * 쇼츠 세션 상태 (3차원 구조)
 *
 * 기존 8개 enum 상태를 3개 차원으로 분리하여 명확성 향상:
 * - BlockingStage: 차단 프로세스 진행 단계
 * - ActivityState: 우리 앱 Activity 실행 여부
 * - ShortsLocation: 쇼츠 앱 위치 (쇼츠 화면 vs 외부)
 *
 * @param blockingStage 차단 진행 단계
 * @param activityState Activity 실행 상태
 * @param shortsLocation 쇼츠 앱 위치
 * @param sessionId 현재 세션 ID (쇼츠 진입 시 생성)
 */
data class SessionState(
    val blockingStage: BlockingStage = BlockingStage.NONE,
    val activityState: ActivityState = ActivityState.NONE,
    val shortsLocation: ShortsLocation = ShortsLocation.OUTSIDE,
    val sessionId: String? = null
) {
    /**
     * 오버레이 표시 필요 여부
     *
     * 조건:
     * - 쇼츠 화면에 있고
     * - 오버레이가 이미 떠있지 않고 (중복 방지)
     * - 타이머 필요 또는 확인 필요 단계
     *
     * 참고: 타이머가 백그라운드에서 진행 중일 때도 오버레이를 띄워서 쇼츠 접근 차단
     */
    fun needsOverlay(): Boolean =
        shortsLocation == ShortsLocation.IN_SHORTS &&
        activityState != ActivityState.OVERLAY_RUNNING &&
        (blockingStage == BlockingStage.NEED_TIMER ||
         blockingStage == BlockingStage.NEED_CONFIRMATION)

    /**
     * 표시할 오버레이 타입
     *
     * @return INITIAL (타이머 필요), CONFIRMATION (확인 필요), null (오버레이 불필요)
     */
    fun getOverlayType(): OverlayType? = when {
        !needsOverlay() -> null
        blockingStage == BlockingStage.NEED_TIMER -> OverlayType.INITIAL
        blockingStage == BlockingStage.NEED_CONFIRMATION -> OverlayType.CONFIRMATION
        else -> null
    }

    /**
     * 시청 중 여부
     *
     * @return true if 쇼츠 화면에서 시청 허용 상태
     */
    fun isWatching(): Boolean =
        blockingStage == BlockingStage.WATCHING &&
        shortsLocation == ShortsLocation.IN_SHORTS

    /**
     * 미디어 일시정지 필요 여부
     *
     * Activity가 실행 중일 때 쇼츠 앱의 미디어를 일시정지해야 함
     */
    fun shouldPauseMedia(): Boolean =
        activityState != ActivityState.NONE

    companion object {
        /**
         * 기본 IDLE 상태 (쇼츠 화면 아님)
         */
        val IDLE = SessionState(
            blockingStage = BlockingStage.NONE,
            activityState = ActivityState.NONE,
            shortsLocation = ShortsLocation.OUTSIDE,
            sessionId = null
        )
    }
}

/**
 * 기존 enum (Deprecated)
 *
 * @deprecated Use SessionState data class with BlockingStage, ActivityState, ShortsLocation
 */
@Deprecated(
    message = "Use SessionState data class instead",
    replaceWith = ReplaceWith("SessionState"),
    level = DeprecationLevel.WARNING
)
enum class ShortsSessionState(
    val isBackground: Boolean
) {
    IDLE(false),
    IN_SHORTS_BLOCKED_NEED_TIMER(false),
    IN_SHORTS_BLOCKED_NEED_CONFIRMATION(false),
    IN_SHORTS_ALLOWED_UNTIL_SCROLL(false),
    BACKGROUND_BLOCKED_NEED_TIMER(true),
    BACKGROUND_BLOCKED_NEED_CONFIRMATION(true),
    BACKGROUND_ALLOWED_UNTIL_SCROLL(true)
}
