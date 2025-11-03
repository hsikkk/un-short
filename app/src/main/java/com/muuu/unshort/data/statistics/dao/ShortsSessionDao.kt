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
}
