package com.muuu.shortblock.service.blocking

import android.util.Log

/**
 * 시청 시간 추적기
 *
 * 책임:
 * - 시청 시작/일시정지/재개 관리
 * - 포그라운드 시청 시간 누적 (백그라운드 제외)
 * - 세션별 시청 시간 계산
 */
class WatchTimeTracker {

    private val TAG = "WatchTimeTracker"

    // 최초 시청 시작 시각
    private var watchStartTime: Long? = null

    // 현재 포그라운드 시청 구간 시작 시각
    private var currentWatchStartTime: Long? = null

    // 누적 시청 시간 (밀리초, 포그라운드만)
    private var accumulatedWatchTime: Long = 0

    /**
     * 시청 시작
     *
     * @return 시작 시각 (밀리초)
     */
    fun start(): Long {
        val now = System.currentTimeMillis()
        if (watchStartTime == null) {
            watchStartTime = now
        }
        currentWatchStartTime = now
        Log.d(TAG, "Watch started at $now")
        return now
    }

    /**
     * 시청 일시정지
     *
     * @return 현재 구간 시청 시간 (밀리초), 시청 중이 아니면 0
     */
    fun pause(): Long {
        val duration = currentWatchStartTime?.let { startTime ->
            val elapsed = System.currentTimeMillis() - startTime
            accumulatedWatchTime += elapsed
            currentWatchStartTime = null
            Log.d(TAG, "Watch paused: duration=${elapsed}ms, accumulated=${accumulatedWatchTime}ms")
            elapsed
        } ?: 0L

        return duration
    }

    /**
     * 시청 재개 (포그라운드 복귀 시)
     */
    fun resume() {
        if (currentWatchStartTime == null) {
            currentWatchStartTime = System.currentTimeMillis()
            Log.d(TAG, "Watch resumed at $currentWatchStartTime")
        }
    }

    /**
     * 초기화 (세션 종료 시)
     */
    fun reset() {
        watchStartTime = null
        currentWatchStartTime = null
        accumulatedWatchTime = 0
        Log.d(TAG, "Watch time reset")
    }

    /**
     * 최초 시청 시작 시각
     */
    fun getWatchStartTime(): Long? = watchStartTime

    /**
     * 누적 시청 시간 (밀리초)
     */
    fun getAccumulatedTime(): Long = accumulatedWatchTime

    /**
     * 시청 중인지 여부
     */
    fun isWatching(): Boolean = currentWatchStartTime != null
}
