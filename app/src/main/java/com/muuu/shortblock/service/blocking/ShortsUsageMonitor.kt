package com.muuu.shortblock.service.blocking

import android.content.Context
import android.util.Log
import com.muuu.unshort.data.statistics.AppDatabase
import com.muuu.unshort.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 쇼츠 연속 시청 감지 및 알림 트리거 관리
 *
 * 차단이 OFF인 상태에서 사용자가 N분 이상 연속으로 쇼츠를 시청하면
 * 차단 기능 활성화를 권유하는 알림을 발송합니다.
 *
 * 책임:
 * - StatisticsRepository의 세션 데이터를 쿼리하여 최근 N분 내 시청 시간 집계
 * - 임계값 초과 시 알림 트리거 판단
 * - 알림 쿨다운 관리
 */
class ShortsUsageMonitor(
    context: Context,
    private val prefsManager: PreferencesManager,
    private val onThresholdExceeded: () -> Unit
) {

    companion object {
        private const val TAG = "ShortsUsageMonitor"
    }

    private val dao = AppDatabase.getDatabase(context).shortsSessionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 쇼츠 진입 시 호출하여 임계값 체크
     *
     * 최근 windowMinutes분 내 총 시청 시간이 thresholdMinutes분을 초과하면 알림 발송
     * 예: 최근 10분 내 총 시청 시간이 7분 이상이면 알림
     */
    fun checkOnEnterShorts() {
        scope.launch {
            try {
                // 차단이 활성화되어 있으면 알림 불필요
                if (prefsManager.isBlockingEnabled) {
                    Log.d(TAG, "차단 활성화 상태 - 알림 불필요")
                    return@launch
                }

                // 일시 해제 상태면 알림 불필요
                if (prefsManager.isTemporarilyDisabled()) {
                    Log.d(TAG, "일시 해제 상태 - 알림 불필요")
                    return@launch
                }

                // 알림 기능이 비활성화되어 있으면 패스
                if (!prefsManager.isReminderNotificationsEnabled) {
                    Log.d(TAG, "알림 기능 비활성화")
                    return@launch
                }

                // 쿨다운 체크
                if (!prefsManager.canSendReminderNotification()) {
                    Log.d(TAG, "쿨다운 기간 - 알림 불가")
                    return@launch
                }

                // 최근 windowMinutes분 내 총 시청 시간 조회
                val windowMinutes = prefsManager.reminderWindowMinutes  // 10분
                val thresholdMinutes = prefsManager.reminderThresholdMinutes  // 7분
                val windowMs = windowMinutes * 60 * 1000L
                val thresholdMs = thresholdMinutes * 60 * 1000L
                val startTime = System.currentTimeMillis() - windowMs

                val totalWatchTimeMs = dao.getTotalWatchTime(startTime)

                Log.d(TAG, "최근 ${windowMinutes}분 내 총 시청 시간: ${totalWatchTimeMs}ms (임계값: ${thresholdMs}ms = ${thresholdMinutes}분)")

                // 임계값 초과 시 알림 트리거
                if (totalWatchTimeMs >= thresholdMs) {
                    Log.d(TAG, "임계값 초과! 알림 발송")
                    onThresholdExceeded()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking threshold", e)
            }
        }
    }
}
