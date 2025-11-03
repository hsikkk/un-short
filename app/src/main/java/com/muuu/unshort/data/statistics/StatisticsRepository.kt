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
     */
    fun recordSession(
        packageName: String,
        didWatch: Boolean,
        timerCompleted: Boolean
    ) {
        scope.launch {
            try {
                val session = ShortsSession(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    didWatch = didWatch,
                    timerCompleted = timerCompleted
                )
                dao.insert(session)
                Log.d(TAG, "Session recorded: pkg=$packageName, didWatch=$didWatch, timerCompleted=$timerCompleted")
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
}
