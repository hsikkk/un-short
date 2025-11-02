package com.muuu.shortblock.service.blocking

import android.util.Log

/**
 * 스크롤 감지 및 세션 관리자
 *
 * 책임:
 * - 콘텐츠 해시 변화를 추적하여 스크롤 감지
 * - 타이머 완료 후 스크롤 전까지 허용 상태 관리
 * - 오버레이 표시 조건 판단
 * - 세션 상태 초기화
 */
class ScrollDetectionManager {

    private val TAG = "ScrollDetectionManager"

    // 스크롤 감지를 위한 상태
    private var lastShortsContentHash: Int = 0
    private var stableHashCount: Int = 0
    private val STABLE_THRESHOLD = 2  // 해시가 N번 연속 안정되면 스크롤로 판단

    // 타이머 완료 후 허용 상태
    private var allowedUntilScroll: Boolean = false
    private var justScrolled: Boolean = false

    // 오버레이 중복 표시 방지
    private var overlayWasShown: Boolean = false

    // 이전 화면 상태
    private var wasInShortsScreen: Boolean = false

    /**
     * 콘텐츠 해시 업데이트 및 스크롤 감지
     *
     * @param newHash 새로운 콘텐츠 해시
     * @return 스크롤이 감지되었으면 true
     */
    fun onContentHashChanged(newHash: Int): Boolean {
        if (newHash == lastShortsContentHash) {
            // 해시가 동일하면 안정화 카운트 증가
            stableHashCount++
            return false
        } else {
            // 해시가 변경됨 -> 새로운 콘텐츠
            val scrollDetected = stableHashCount >= STABLE_THRESHOLD

            if (scrollDetected) {
                Log.d(TAG, "Scroll detected: hash changed from $lastShortsContentHash to $newHash after $stableHashCount stable checks")
                onScrollDetected()
            }

            lastShortsContentHash = newHash
            stableHashCount = 0
            return scrollDetected
        }
    }

    /**
     * 스크롤 감지 시 호출
     */
    private fun onScrollDetected() {
        // 타이머 완료 후 허용된 상태였다면 다시 차단으로 전환
        if (allowedUntilScroll) {
            Log.d(TAG, "User scrolled after timer completion - blocking again")
            allowedUntilScroll = false
            justScrolled = true
        }
    }

    /**
     * 쇼츠 화면 진입 여부 판단
     *
     * @param isInShortsScreen 현재 쇼츠 화면인지 여부
     * @return 방금 쇼츠 화면에 진입했으면 true
     */
    fun checkShortsScreenEntry(isInShortsScreen: Boolean): Boolean {
        val justEntered = !wasInShortsScreen && isInShortsScreen
        wasInShortsScreen = isInShortsScreen

        if (justEntered) {
            Log.d(TAG, "Just entered shorts screen")
        }

        return justEntered
    }

    /**
     * 쇼츠 화면 이탈 여부 판단
     *
     * @param isInShortsScreen 현재 쇼츠 화면인지 여부
     * @return 방금 쇼츠 화면에서 이탈했으면 true
     */
    fun checkShortsScreenExit(isInShortsScreen: Boolean): Boolean {
        val justExited = wasInShortsScreen && !isInShortsScreen
        wasInShortsScreen = isInShortsScreen

        if (justExited) {
            Log.d(TAG, "Just exited shorts screen")
        }

        return justExited
    }

    /**
     * 오버레이를 표시해야 하는지 판단
     *
     * @param isInShortsScreen 현재 쇼츠 화면인지 여부
     * @return 오버레이 표시가 필요하면 true
     */
    fun shouldShowOverlay(isInShortsScreen: Boolean): Boolean {
        if (!isInShortsScreen) {
            return false
        }

        // 타이머 완료 후 스크롤 전까지 허용된 상태
        if (allowedUntilScroll) {
            Log.d(TAG, "Allowed until scroll - not showing overlay")
            return false
        }

        // 방금 스크롤한 경우 한 번은 패스 (오버레이가 바로 닫히는 것 방지)
        if (justScrolled) {
            Log.d(TAG, "Just scrolled - skipping overlay once")
            justScrolled = false
            return false
        }

        // 이미 오버레이가 표시된 상태
        if (overlayWasShown) {
            Log.d(TAG, "Overlay already shown - not showing again")
            return false
        }

        return true
    }

    /**
     * 오버레이 표시 상태 업데이트
     */
    fun markOverlayShown() {
        overlayWasShown = true
        Log.d(TAG, "Overlay marked as shown")
    }

    /**
     * 타이머 완료 처리 (30초 경과 또는 폰 뒤집기)
     * 이후 스크롤 전까지 허용
     */
    fun onTimerCompleted() {
        allowedUntilScroll = true
        overlayWasShown = false
        Log.d(TAG, "Timer completed - allowed until next scroll")
    }

    /**
     * 세션 초기화 (앱 종료, 홈 버튼 등)
     */
    fun clearSession() {
        Log.d(TAG, "Clearing session state")
        allowedUntilScroll = false
        overlayWasShown = false
        justScrolled = false
        lastShortsContentHash = 0
        stableHashCount = 0
        wasInShortsScreen = false
    }

    /**
     * 쇼츠 화면 이탈 시 부분 초기화
     */
    fun onExitShortsScreen() {
        Log.d(TAG, "Exited shorts screen - partial reset")
        overlayWasShown = false
        allowedUntilScroll = false
        lastShortsContentHash = 0
        stableHashCount = 0
    }

    // Getter 메서드들
    fun isAllowedUntilScroll(): Boolean = allowedUntilScroll
    fun isOverlayShown(): Boolean = overlayWasShown
    fun wasInShortsScreen(): Boolean = wasInShortsScreen
}
