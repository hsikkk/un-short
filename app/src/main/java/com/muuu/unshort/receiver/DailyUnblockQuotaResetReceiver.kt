package com.muuu.unshort.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.muuu.unshort.ad.DailyUnblockQuotaManager
import com.muuu.unshort.config.AppConstants
import java.util.Calendar

/**
 * 자정에 일일 즉시 해제 한도를 리셋하는 BroadcastReceiver
 *
 * DailyLimitResetReceiver 패턴을 100% 모방.
 * AlarmManager 누락 시 DailyUnblockQuotaManager의 Lazy 보정으로 이중 보강.
 */
class DailyUnblockQuotaResetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyQuotaResetReceiver"
        private const val REQUEST_CODE = 3004

        fun scheduleReset(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyUnblockQuotaResetReceiver::class.java).apply {
                action = AppConstants.ACTION_DAILY_UNBLOCK_QUOTA_RESET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = getNextMidnight()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Scheduled daily unblock quota reset at midnight: $triggerTime")
        }

        fun cancelReset(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyUnblockQuotaResetReceiver::class.java).apply {
                action = AppConstants.ACTION_DAILY_UNBLOCK_QUOTA_RESET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled daily unblock quota reset alarm")
        }

        private fun getNextMidnight(): Long {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        if (intent.action == AppConstants.ACTION_DAILY_UNBLOCK_QUOTA_RESET) {
            DailyUnblockQuotaManager.resetDaily(context)
            Log.d(TAG, "Daily unblock quota reset at midnight")
            scheduleReset(context)
        }
    }
}
