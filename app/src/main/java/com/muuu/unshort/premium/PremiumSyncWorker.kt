package com.muuu.unshort.premium

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * 프리미엄 상태 동기화 Worker
 *
 * 24시간마다 백그라운드에서 프리미엄 구독 상태를 동기화
 */
class PremiumSyncWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // 프리미엄 상태 동기화
            PremiumManager.syncPremiumStatus()

            Result.success()
        } catch (e: Exception) {
            // 실패 시 재시도
            Result.retry()
        }
    }
}
