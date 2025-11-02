package com.muuu.shortblock.service.blocking

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
     * 백그라운드 진입 (TimerActivity 또는 다른 앱)
     */
    data object EnterBackground : SessionEvent

    /**
     * 쇼츠 화면으로 복귀 (백그라운드에서)
     */
    data object ReturnToShorts : SessionEvent

    /**
     * 타이머 완료 (30초 또는 폰 뒤집기)
     */
    data object TimerCompleted : SessionEvent

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
}
