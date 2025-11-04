package com.muuu.unshort.data.statistics

import android.content.Context
import android.util.Log
import com.muuu.unshort.data.statistics.entity.ShortsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 통계 데이터 Repository
 *
 * 책임:
 * - 쇼츠 세션 데이터 저장
 * - 30일 이전 데이터 자동 정리
 * - 통계 조회 (향후 UI 구현 시 사용)
 */
class StatisticsRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).shortsSessionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "StatisticsRepository"
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }

    /**
     * 세션 기록 (비동기)
     *
     * Service에서 세션 종료 시 호출
     *
     * @param packageName 차단 대상 앱 패키지명
     * @param didWatch 시청 여부 (true: 시청함, false: 시청 안함)
     * @param timerCompleted 타이머 완료 여부
     * @param isScrollSession 스크롤 세션 여부 (true: 스크롤 후 재진입, false: 첫 진입)
     * @param watchStartTime 시청 시작 시각 (밀리초)
     * @param watchEndTime 시청 종료 시각 (밀리초)
     * @param watchDurationMs 누적 시청 시간 (밀리초, 포그라운드만)
     */
    fun recordSession(
        packageName: String,
        didWatch: Boolean,
        timerCompleted: Boolean,
        isScrollSession: Boolean = false,
        watchStartTime: Long? = null,
        watchEndTime: Long? = null,
        watchDurationMs: Long? = null
    ) {
        scope.launch {
            try {
                val session = ShortsSession(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    didWatch = didWatch,
                    timerCompleted = timerCompleted,
                    isScrollSession = isScrollSession,
                    watchStartTime = watchStartTime,
                    watchEndTime = watchEndTime,
                    watchDurationMs = watchDurationMs
                )
                dao.insert(session)
                Log.d(TAG, "Session recorded: pkg=$packageName, didWatch=$didWatch, timerCompleted=$timerCompleted, isScrollSession=$isScrollSession, watchDuration=${watchDurationMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording session", e)
            }
        }
    }

    /**
     * 30일 이전 데이터 정리 (비동기)
     *
     * Service 시작 시 호출
     */
    fun cleanOldData() {
        scope.launch {
            try {
                val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS
                val deleted = dao.deleteOldSessions(cutoff)
                if (deleted > 0) {
                    Log.d(TAG, "Cleaned $deleted old sessions")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning old data", e)
            }
        }
    }

    /**
     * 최근 30일 세션 조회 (동기)
     *
     * UI에서 통계 표시 시 사용
     */
    suspend fun getRecentSessions(): List<ShortsSession> {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getSessionsAfter(thirtyDaysAgo)
    }

    /**
     * 최근 30일 세션 총 개수
     */
    suspend fun getSessionCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getSessionCount(thirtyDaysAgo)
    }

    /**
     * 최근 30일 Skip 개수 (시청 안한 세션)
     */
    suspend fun getSkipCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getSkipCount(thirtyDaysAgo)
    }

    /**
     * 최근 30일 첫 쇼츠만 시청 개수
     */
    suspend fun getFirstWatchOnlyCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getFirstWatchOnlyCount(thirtyDaysAgo)
    }

    /**
     * 최근 30일 스크롤 시청 개수
     */
    suspend fun getScrollWatchCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getScrollWatchCount(thirtyDaysAgo)
    }

    /**
     * 특정 날짜의 세션 총 개수
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 세션 개수
     */
    suspend fun getSessionCountForDate(startTime: Long, endTime: Long): Int {
        return dao.getSessionCountBetween(startTime, endTime)
    }

    /**
     * 특정 날짜의 시청 세션 개수
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 시청한 세션 개수
     */
    suspend fun getWatchCountForDate(startTime: Long, endTime: Long): Int {
        return dao.getWatchCountBetween(startTime, endTime)
    }

    /**
     * 특정 날짜의 첫 쇼츠만 시청 개수
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 첫 진입 세션에서 시청한 개수
     */
    suspend fun getFirstWatchOnlyCountForDate(startTime: Long, endTime: Long): Int {
        return dao.getFirstWatchOnlyCountBetween(startTime, endTime)
    }

    /**
     * 특정 날짜의 스크롤 시청 개수
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 스크롤 후 재진입 세션에서 시청한 개수
     */
    suspend fun getScrollWatchCountForDate(startTime: Long, endTime: Long): Int {
        return dao.getScrollWatchCountBetween(startTime, endTime)
    }

    /**
     * 특정 날짜의 앱별 시청 개수
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @param packageName 앱 패키지명
     * @return 해당 앱에서 시청한 세션 개수
     */
    suspend fun getWatchCountByAppForDate(startTime: Long, endTime: Long, packageName: String): Int {
        return dao.getWatchCountByAppBetween(startTime, endTime, packageName)
    }

    // ========== 시청 시간 관련 메서드 ==========

    /**
     * 최근 30일 총 시청 시간 (밀리초)
     */
    suspend fun getTotalWatchTime(): Long {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getTotalWatchTime(thirtyDaysAgo)
    }

    /**
     * 특정 기간 총 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 총 시청 시간 (밀리초)
     */
    suspend fun getTotalWatchTimeForPeriod(startTime: Long, endTime: Long): Long {
        return dao.getTotalWatchTimeBetween(startTime, endTime)
    }

    /**
     * 최근 30일 평균 시청 시간 (밀리초)
     */
    suspend fun getAverageWatchTime(): Long {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getAverageWatchTime(thirtyDaysAgo)
    }

    /**
     * 특정 기간 평균 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 평균 시청 시간 (밀리초)
     */
    suspend fun getAverageWatchTimeForPeriod(startTime: Long, endTime: Long): Long {
        return dao.getAverageWatchTimeBetween(startTime, endTime)
    }

    /**
     * 특정 기간 앱별 총 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @param packageName 앱 패키지명
     * @return 해당 앱의 총 시청 시간 (밀리초)
     */
    suspend fun getTotalWatchTimeByApp(startTime: Long, endTime: Long, packageName: String): Long {
        return dao.getTotalWatchTimeByAppBetween(startTime, endTime, packageName)
    }

    /**
     * 최근 30일 시청 세션 개수 (시청 시간이 있는 세션만)
     */
    suspend fun getWatchSessionCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MS
        return dao.getWatchSessionCount(thirtyDaysAgo)
    }
}
