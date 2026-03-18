package com.muuu.unshort.service.dailylimit

import android.content.Context
import android.util.Log
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 일일 제한 시간 관리
 *
 * 세션 종료 후 오늘의 누적 시청 시간을 체크하여
 * 80% 경고 및 100% 초과 시 자동 차단을 트리거한다.
 * Race condition 방지를 위해 방금 기록한 세션의 watchDurationMs를
 * 파라미터로 받아 DB 쿼리 결과에 더한다.
 */
class DailyLimitManager(
    private val context: Context,
    private val prefsManager: PreferencesManager,
    private val statisticsRepository: StatisticsRepository,
    private val onWarningThreshold: () -> Unit,
    private val onLimitExceeded: () -> Unit,
    private val onMonitoringUpdate: (currentMs: Long, limitMs: Long) -> Unit
) {

    companion object {
        private const val TAG = "DailyLimitManager"
        private const val WARNING_THRESHOLD_RATIO = 0.8
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val checkMutex = Mutex()

    /**
     * 세션 종료 후 일일 제한 체크
     *
     * @param justRecordedDurationMs 방금 기록한 세션의 시청 시간 (DB 반영 전 race condition 방지)
     */
    fun checkAfterSessionEnd(justRecordedDurationMs: Long?) {
        if (!prefsManager.isDailyLimitEnabled) return
        if (!PremiumManager.isPremium()) return

        scope.launch {
            checkMutex.withLock {
                try {
                    checkDailyLimit(justRecordedDurationMs ?: 0L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking daily limit", e)
                }
            }
        }
    }

    private suspend fun checkDailyLimit(justRecordedDurationMs: Long) {
        val todayStats = statisticsRepository.getTodayStats()
        val totalWatchTimeMs = todayStats.watchTimeMs + justRecordedDurationMs

        val limitMs = prefsManager.dailyLimitMinutes * 60 * 1000L
        val warningMs = (limitMs * WARNING_THRESHOLD_RATIO).toLong()

        Log.d(TAG, "Daily limit check: ${totalWatchTimeMs}ms / ${limitMs}ms (${totalWatchTimeMs * 100 / limitMs}%)")

        // 모니터링 알림 업데이트
        if (prefsManager.isDailyLimitMonitorEnabled) {
            onMonitoringUpdate(totalWatchTimeMs, limitMs)
        }

        // 100% 초과 체크
        if (totalWatchTimeMs >= limitMs && !prefsManager.isDailyLimitExceededToday) {
            prefsManager.isDailyLimitExceededToday = true
            Log.d(TAG, "Daily limit exceeded! Auto-enabling blocking.")
            onLimitExceeded()
            return
        }

        // 80% 경고 체크
        if (totalWatchTimeMs >= warningMs
            && prefsManager.isDailyLimitWarningEnabled
            && !prefsManager.isDailyLimitWarningSentToday
            && !prefsManager.isDailyLimitExceededToday
        ) {
            prefsManager.isDailyLimitWarningSentToday = true
            Log.d(TAG, "Daily limit 80% warning triggered.")
            onWarningThreshold()
        }
    }
}
