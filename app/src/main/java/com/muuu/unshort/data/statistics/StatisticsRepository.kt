package com.muuu.unshort.data.statistics

import android.content.Context
import android.util.Log
import com.muuu.unshort.data.statistics.entity.ShortsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // ========== 리포트 기능을 위한 데이터 모델 및 메서드 ==========

    /**
     * 일별 통계 데이터 모델
     */
    data class DailyStats(
        val date: Long,              // 날짜 시작 타임스탬프
        val attemptCount: Int,       // 총 세션 개수 (진입 횟수)
        val watchedCount: Int,       // 시청한 세션 개수
        val watchTimeMs: Long        // 총 시청 시간 (밀리초)
    )

    /**
     * 앱별 통계 데이터 모델
     */
    data class AppStats(
        val packageName: String,     // 앱 패키지명
        val attemptCount: Int,       // 해당 앱 세션 개수
        val watchedCount: Int,       // 시청한 세션 개수
        val watchTimeMs: Long        // 시청 시간 (밀리초)
    )

    /**
     * 시간대별 통계 데이터 모델
     */
    data class HourlyStats(
        val hour: Int,               // 시간 (0-23)
        val attemptCount: Int,       // 진입 횟수
        val watchedCount: Int,       // 시청 횟수
        val watchTimeMs: Long        // 시청 시간 (밀리초)
    )

    /**
     * 최근 N일간의 일별 통계 조회
     *
     * @param days 조회할 일수 (기본 7일)
     * @return 일별 통계 리스트 (날짜 오름차순)
     */
    suspend fun getDailyStats(days: Int = 7): List<DailyStats> = withContext(Dispatchers.IO) {
        val dailyStats = mutableListOf<DailyStats>()

        repeat(days) { index ->
            // 각 루프마다 새 Calendar 생성
            val calendar = java.util.Calendar.getInstance()

            // 날짜 경계 계산
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1 - index))
            val dayStart = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            // 데이터 조회
            val attemptCount = dao.getSessionCountBetween(dayStart, dayEnd)
            val watchedCount = dao.getWatchCountBetween(dayStart, dayEnd)
            val watchTimeMs = dao.getTotalWatchTimeBetween(dayStart, dayEnd)

            dailyStats.add(DailyStats(dayStart, attemptCount, watchedCount, watchTimeMs))
        }

        dailyStats
    }

    /**
     * 오늘의 통계 조회 (최적화 - 단일 쿼리)
     *
     * @return 오늘의 통계
     */
    suspend fun getTodayStats(): DailyStats = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayEnd = calendar.timeInMillis

        // 최적화된 단일 쿼리 사용 (3개 쿼리 → 1개 쿼리)
        val rawStats = dao.getStatsForDateOptimized(dayStart, dayEnd)

        if (rawStats != null) {
            DailyStats(dayStart, rawStats.attemptCount, rawStats.watchedCount, rawStats.totalWatchTimeMs)
        } else {
            DailyStats(dayStart, 0, 0, 0L)
        }
    }

    /**
     * 오늘의 앱별 통계 조회
     *
     * @return 앱별 통계 리스트 (시청 시간 내림차순)
     */
    suspend fun getTodayAppStats(): List<AppStats> = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayEnd = calendar.timeInMillis

        // 오늘의 모든 세션 조회
        val sessions = dao.getSessionsBetween(dayStart, dayEnd)

        // 패키지명으로 그룹화
        sessions.groupBy { it.packageName }
            .map { (packageName, sessions) ->
                AppStats(
                    packageName = packageName,
                    attemptCount = sessions.size,
                    watchedCount = sessions.count { it.didWatch },
                    watchTimeMs = sessions.sumOf { it.watchDurationMs ?: 0L }
                )
            }
            .sortedByDescending { it.watchTimeMs }
    }

    /**
     * 특정 날짜의 통계 조회
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 해당 날짜의 통계
     */
    suspend fun getStatsForDate(startTime: Long, endTime: Long): DailyStats = withContext(Dispatchers.IO) {
        val attemptCount = dao.getSessionCountBetween(startTime, endTime)
        val watchedCount = dao.getWatchCountBetween(startTime, endTime)
        val watchTimeMs = dao.getTotalWatchTimeBetween(startTime, endTime)

        DailyStats(startTime, attemptCount, watchedCount, watchTimeMs)
    }

    /**
     * 특정 날짜를 마지막으로 하는 N일간의 일별 통계 조회
     *
     * @param endDate 종료 날짜 (밀리초)
     * @param days 조회할 일수 (기본 7일)
     * @return 일별 통계 리스트 (날짜 오름차순)
     */
    suspend fun getDailyStatsEndingOn(endDate: Long, days: Int = 7): List<DailyStats> = withContext(Dispatchers.IO) {
        val dailyStats = mutableListOf<DailyStats>()
        val normalizedEndDate = getStartOfDay(endDate)

        repeat(days) { index ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = normalizedEndDate
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1 - index))
            val dayStart = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            val attemptCount = dao.getSessionCountBetween(dayStart, dayEnd)
            val watchedCount = dao.getWatchCountBetween(dayStart, dayEnd)
            val watchTimeMs = dao.getTotalWatchTimeBetween(dayStart, dayEnd)

            dailyStats.add(DailyStats(dayStart, attemptCount, watchedCount, watchTimeMs))
        }

        dailyStats
    }

    /**
     * 특정 날짜의 앱별 통계 조회
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 앱별 통계 리스트 (시청 시간 내림차순)
     */
    suspend fun getAppStatsForDate(startTime: Long, endTime: Long): List<AppStats> = withContext(Dispatchers.IO) {
        val sessions = dao.getSessionsBetween(startTime, endTime)

        sessions.groupBy { it.packageName }
            .map { (packageName, sessions) ->
                AppStats(
                    packageName = packageName,
                    attemptCount = sessions.size,
                    watchedCount = sessions.count { it.didWatch },
                    watchTimeMs = sessions.sumOf { it.watchDurationMs ?: 0L }
                )
            }
            .sortedByDescending { it.watchTimeMs }
    }

    /**
     * 특정 날짜의 시간대별 통계 조회
     *
     * @param startTime 날짜 시작 시각 (밀리초)
     * @param endTime 날짜 종료 시각 (밀리초)
     * @return 시간대별 통계 리스트 (시간 오름차순, 0-23시)
     */
    suspend fun getHourlyStatsForDate(startTime: Long, endTime: Long): List<HourlyStats> = withContext(Dispatchers.IO) {
        // 해당 날짜의 모든 세션 조회
        val sessions = dao.getSessionsBetween(startTime, endTime)

        // 시간 단위로 그룹화 (0시, 1시, ..., 23시)
        val hourlyMap = mutableMapOf<Int, MutableList<ShortsSession>>()
        for (i in 0 until 24) {
            hourlyMap[i] = mutableListOf()
        }

        sessions.forEach { session ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = session.timestamp
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)  // 0-23
            hourlyMap[hour]?.add(session)
        }

        // HourlyStats 리스트 생성
        hourlyMap.entries
            .sortedBy { it.key }
            .map { (hour, sessions) ->
                HourlyStats(
                    hour = hour,
                    attemptCount = sessions.size,
                    watchedCount = sessions.count { it.didWatch },
                    watchTimeMs = sessions.sumOf { it.watchDurationMs ?: 0L }
                )
            }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 특정 앱의 최근 N분 이내 차단 시도 개수
     *
     * @param packageName 앱 패키지명
     * @param minutes 확인할 시간 범위 (분)
     * @return 차단 시도 개수
     */
    suspend fun getRecentSessionCountByApp(packageName: String, minutes: Int): Int {
        val now = System.currentTimeMillis()
        val startTime = now - (minutes * 60 * 1000L)
        return dao.getSessionCountByPackageBetween(packageName, startTime, now)
    }
}
