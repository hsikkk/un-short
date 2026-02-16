package com.muuu.unshort.data.statistics.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.muuu.unshort.data.statistics.entity.ShortsSession

/**
 * 쇼츠 세션 DAO
 *
 * 세션 데이터 저장 및 조회
 */
@Dao
interface ShortsSessionDao {

    /**
     * 세션 추가
     */
    @Insert
    suspend fun insert(session: ShortsSession)

    /**
     * 특정 시각 이후의 세션 조회
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 세션 리스트 (최신순)
     */
    @Query("SELECT * FROM shorts_sessions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getSessionsAfter(startTime: Long): List<ShortsSession>

    /**
     * 특정 시각 이전의 세션 삭제 (30일 데이터 정리용)
     *
     * @param cutoffTime 기준 시각 (밀리초)
     * @return 삭제된 행 수
     */
    @Query("DELETE FROM shorts_sessions WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long): Int

    /**
     * 특정 시각 이후의 세션 총 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime")
    suspend fun getSessionCount(startTime: Long): Int

    /**
     * 특정 시각 이후 시청하지 않은 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @return Skip한 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND didWatch = 0")
    suspend fun getSkipCount(startTime: Long): Int

    /**
     * 특정 시각 이후 첫 쇼츠만 시청한 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 첫 진입 세션에서 시청한 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND didWatch = 1 AND isScrollSession = 0")
    suspend fun getFirstWatchOnlyCount(startTime: Long): Int

    /**
     * 특정 시각 이후 스크롤 시청한 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 스크롤 후 재진입 세션에서 시청한 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND didWatch = 1 AND isScrollSession = 1")
    suspend fun getScrollWatchCount(startTime: Long): Int

    /**
     * 특정 날짜 범위의 세션 총 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getSessionCountBetween(startTime: Long, endTime: Long): Int

    /**
     * 특정 날짜 범위에서 시청한 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 시청한 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND didWatch = 1")
    suspend fun getWatchCountBetween(startTime: Long, endTime: Long): Int

    /**
     * 특정 날짜 범위에서 첫 쇼츠만 시청한 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 첫 진입 세션에서 시청한 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND didWatch = 1 AND isScrollSession = 0")
    suspend fun getFirstWatchOnlyCountBetween(startTime: Long, endTime: Long): Int

    /**
     * 특정 날짜 범위에서 스크롤 시청한 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 스크롤 후 재진입 세션에서 시청한 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND didWatch = 1 AND isScrollSession = 1")
    suspend fun getScrollWatchCountBetween(startTime: Long, endTime: Long): Int

    /**
     * 특정 날짜 범위에서 특정 앱의 시청 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @param packageName 앱 패키지명
     * @return 해당 앱에서 시청한 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND packageName = :packageName AND didWatch = 1")
    suspend fun getWatchCountByAppBetween(startTime: Long, endTime: Long, packageName: String): Int

    // ========== 시청 시간 관련 쿼리 ==========

    /**
     * 특정 시각 이후의 총 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 총 시청 시간 (밀리초), null이면 0 반환
     */
    @Query("SELECT COALESCE(SUM(watchDurationMs), 0) FROM shorts_sessions WHERE timestamp >= :startTime AND watchDurationMs IS NOT NULL")
    suspend fun getTotalWatchTime(startTime: Long): Long

    /**
     * 특정 날짜 범위의 총 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 총 시청 시간 (밀리초), null이면 0 반환
     */
    @Query("SELECT COALESCE(SUM(watchDurationMs), 0) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND watchDurationMs IS NOT NULL")
    suspend fun getTotalWatchTimeBetween(startTime: Long, endTime: Long): Long

    /**
     * 특정 시각 이후의 평균 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 평균 시청 시간 (밀리초), 데이터 없으면 0 반환
     */
    @Query("SELECT COALESCE(AVG(watchDurationMs), 0) FROM shorts_sessions WHERE timestamp >= :startTime AND watchDurationMs IS NOT NULL")
    suspend fun getAverageWatchTime(startTime: Long): Long

    /**
     * 특정 날짜 범위의 평균 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 평균 시청 시간 (밀리초), 데이터 없으면 0 반환
     */
    @Query("SELECT COALESCE(AVG(watchDurationMs), 0) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND watchDurationMs IS NOT NULL")
    suspend fun getAverageWatchTimeBetween(startTime: Long, endTime: Long): Long

    /**
     * 특정 날짜 범위의 앱별 총 시청 시간 (밀리초)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @param packageName 앱 패키지명
     * @return 해당 앱의 총 시청 시간 (밀리초), null이면 0 반환
     */
    @Query("SELECT COALESCE(SUM(watchDurationMs), 0) FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime AND packageName = :packageName AND watchDurationMs IS NOT NULL")
    suspend fun getTotalWatchTimeByAppBetween(startTime: Long, endTime: Long, packageName: String): Long

    /**
     * 특정 시각 이후 시청 시간이 기록된 세션 개수
     *
     * @param startTime 시작 시각 (밀리초)
     * @return 시청 시간이 있는 세션 개수
     */
    @Query("SELECT COUNT(*) FROM shorts_sessions WHERE timestamp >= :startTime AND watchDurationMs IS NOT NULL AND watchDurationMs > 0")
    suspend fun getWatchSessionCount(startTime: Long): Int

    // ========== 일별 통계 조회 ==========

    /**
     * 특정 날짜 범위의 모든 세션 조회 (일별 리포트용)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 세션 리스트 (시간순)
     */
    @Query("SELECT * FROM shorts_sessions WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ShortsSession>

    /**
     * 특정 날짜 범위의 통계를 한번에 조회 (최적화)
     *
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return [attemptCount, watchedCount, totalWatchTimeMs] 배열
     */
    @Query("""
        SELECT
            COUNT(*) as attemptCount,
            SUM(CASE WHEN didWatch = 1 THEN 1 ELSE 0 END) as watchedCount,
            COALESCE(SUM(watchDurationMs), 0) as totalWatchTimeMs
        FROM shorts_sessions
        WHERE timestamp >= :startTime AND timestamp < :endTime
    """)
    suspend fun getStatsForDateOptimized(startTime: Long, endTime: Long): DailyStatsRaw?

    /**
     * 일별 통계 집계 결과 (Raw 데이터)
     */
    data class DailyStatsRaw(
        val attemptCount: Int,
        val watchedCount: Int,
        val totalWatchTimeMs: Long
    )

    /**
     * 특정 앱의 특정 기간 내 세션 개수 조회
     *
     * @param packageName 앱 패키지명
     * @param startTime 시작 시각 (밀리초)
     * @param endTime 종료 시각 (밀리초)
     * @return 세션 개수
     */
    @Query("""
        SELECT COUNT(*)
        FROM shorts_sessions
        WHERE packageName = :packageName
          AND timestamp >= :startTime
          AND timestamp < :endTime
    """)
    suspend fun getSessionCountByPackageBetween(
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Int
}
