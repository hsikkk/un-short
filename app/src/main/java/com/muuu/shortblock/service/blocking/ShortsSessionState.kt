package com.muuu.shortblock.service.blocking

/**
 * 쇼츠 세션 상태
 *
 * @param isBackground 백그라운드 상태 여부 (미디어 일시정지 판단에 사용)
 */
enum class ShortsSessionState(
    val isBackground: Boolean
) {
    /**
     * 쇼츠 화면 아님 (앱 외부 또는 앱 내 non-shorts)
     */
    IDLE(false),

    /**
     * 쇼츠 화면 - 타이머 진행 필요
     * 오버레이: INITIAL ("다시 한번 생각해보기")
     */
    IN_SHORTS_BLOCKED_NEED_TIMER(false),

    /**
     * 쇼츠 화면 - 타이머 완료, 최종 선택 대기
     * 오버레이: CONFIRMATION ("볼래요" / "안볼래요")
     */
    IN_SHORTS_BLOCKED_NEED_CONFIRMATION(false),

    /**
     * 쇼츠 화면 - 다음 스크롤까지 시청 허용
     * 오버레이: 없음
     */
    IN_SHORTS_ALLOWED_UNTIL_SCROLL(false),

    /**
     * 백그라운드 - 타이머 진행 전/중
     * (TimerActivity 실행 중이거나 다른 앱으로 이동)
     */
    BACKGROUND_BLOCKED_NEED_TIMER(true),

    /**
     * 백그라운드 - 타이머 완료, 확인 대기
     * 복귀 시 CONFIRMATION 오버레이 표시
     */
    BACKGROUND_BLOCKED_NEED_CONFIRMATION(true),

    /**
     * 백그라운드 - "볼래요" 클릭 후 다음 스크롤까지 허용
     * 복귀 시 계속 시청 허용
     */
    BACKGROUND_ALLOWED_UNTIL_SCROLL(true)
}
