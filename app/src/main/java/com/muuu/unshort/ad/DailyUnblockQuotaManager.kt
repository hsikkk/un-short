package com.muuu.unshort.ad

import android.content.Context
import android.util.Log
import com.muuu.unshort.UnshortApplication
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import java.time.LocalDate

/**
 * 일일 즉시 해제 한도 관리자
 *
 * "바로 보기" 버튼으로 차단을 즉시 해제할 수 있는 일일 한도를 관리한다.
 * - 클릭 진입 시점의 "바로 보기" 클릭으로만 한도 차감
 * - 한도 0 시 광고 1편 시청으로 +N회 충전 (Remote Config로 동적 조절)
 * - 매일 자정 자동 리셋, 알람 누락 시 Lazy 보정
 * - 프리미엄 사용자는 한도 무제한 (차감 X)
 */
object DailyUnblockQuotaManager {

    private const val TAG = "DailyUnblockQuotaMgr"

    /**
     * 잔여 한도 반환. 자정 경과 시 자동 리셋 보정.
     * 프리미엄 사용자는 Int.MAX_VALUE 반환 (무제한 표현).
     */
    fun getRemainingQuota(context: Context): Int {
        if (PremiumManager.isPremium()) return Int.MAX_VALUE
        ensureFreshState(context)
        return prefs(context).dailyUnblockQuotaRemaining
    }

    /**
     * "바로 보기" 클릭 시 한도 1 차감.
     * 프리미엄 사용자는 차감하지 않음.
     * 통계용 누적 카운터(`dailyUnblockTotalToday`)는 항상 +1.
     */
    fun consumeOnInstantUnblock(context: Context) {
        ensureFreshState(context)
        val p = prefs(context)
        if (!PremiumManager.isPremium()) {
            val current = p.dailyUnblockQuotaRemaining
            p.dailyUnblockQuotaRemaining = (current - 1).coerceAtLeast(0)
        }
        p.dailyUnblockTotalToday = p.dailyUnblockTotalToday + 1
        Log.d(TAG, "consumeOnInstantUnblock: remaining=${p.dailyUnblockQuotaRemaining}, totalToday=${p.dailyUnblockTotalToday}")
    }

    /**
     * 광고 시청 보상으로 한도 +N 충전.
     * Remote Config `unblock_quota_ad_recharge_amount`로 N 동적 조절 (기본 3).
     *
     * @return 충전된 양
     */
    fun rechargeFromAd(context: Context): Int {
        ensureFreshState(context)
        val p = prefs(context)
        val amount = adRechargeAmount()
        p.dailyUnblockQuotaRemaining = p.dailyUnblockQuotaRemaining + amount
        p.dailyAdWatchedToday = p.dailyAdWatchedToday + 1
        Log.d(TAG, "rechargeFromAd: +$amount, remaining=${p.dailyUnblockQuotaRemaining}, adsWatchedToday=${p.dailyAdWatchedToday}")
        return amount
    }

    /**
     * 자정 리셋. DailyUnblockQuotaResetReceiver에서 호출.
     */
    fun resetDaily(context: Context) {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        val previousRemaining = p.dailyUnblockQuotaRemaining
        val previousTotal = p.dailyUnblockTotalToday
        val previousAdsWatched = p.dailyAdWatchedToday

        p.dailyUnblockQuotaRemaining = dailyLimit()
        p.dailyUnblockTotalToday = 0
        p.dailyAdWatchedToday = 0
        p.dailyUnblockQuotaLastResetDate = today

        Log.d(
            TAG,
            "resetDaily: limit=${dailyLimit()}, prev(rem=$previousRemaining, total=$previousTotal, ads=$previousAdsWatched)"
        )
    }

    fun getTotalUnblockToday(context: Context): Int {
        ensureFreshState(context)
        return prefs(context).dailyUnblockTotalToday
    }

    fun getTotalAdsWatchedToday(context: Context): Int {
        ensureFreshState(context)
        return prefs(context).dailyAdWatchedToday
    }

    /**
     * Lazy 보정 — 호출 시점에 lastResetDate 비교하여 자동 리셋.
     * AlarmManager 알람이 누락되어도 다음 호출에서 자연스럽게 보정.
     */
    private fun ensureFreshState(context: Context) {
        val p = prefs(context)
        val lastResetDate = p.dailyUnblockQuotaLastResetDate
        val today = LocalDate.now().toString()
        if (lastResetDate != today) {
            resetDaily(context)
        }
    }

    /**
     * 일일 한도 (Remote Config로 동적 조절 가능, 기본 10회)
     */
    private fun dailyLimit(): Int =
        UnshortApplication.remoteConfig
            .getLong(AppConstants.RC_UNBLOCK_QUOTA_DAILY_LIMIT)
            .toInt()
            .coerceAtLeast(1)

    private fun adRechargeAmount(): Int =
        UnshortApplication.remoteConfig
            .getLong(AppConstants.RC_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT)
            .toInt()
            .coerceAtLeast(1)

    private fun prefs(context: Context): PreferencesManager =
        PreferencesManager(context.applicationContext)
}
