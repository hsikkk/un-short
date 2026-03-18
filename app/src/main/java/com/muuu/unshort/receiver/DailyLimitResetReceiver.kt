package com.muuu.unshort.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.prefs.PreferencesManager
import java.util.Calendar

/**
 * 자정에 일일 제한 상태를 리셋하는 BroadcastReceiver
 *
 * DailyReportReceiver 패턴을 따라 AlarmManager로
 * 매일 자정에 일일 제한 플래그를 초기화하고 모니터링 알림을 제거한다.
 */
class DailyLimitResetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyLimitResetReceiver"
        private const val REQUEST_CODE = 3003
        private const val NOTIFICATION_ID_MONITOR = com.muuu.unshort.config.AppConstants.NOTIFICATION_ID_DAILY_LIMIT_MONITOR

        fun scheduleReset(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyLimitResetReceiver::class.java).apply {
                action = AppConstants.ACTION_DAILY_LIMIT_RESET
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

            Log.d(TAG, "Scheduled daily limit reset at midnight: $triggerTime")
        }

        fun cancelReset(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyLimitResetReceiver::class.java).apply {
                action = AppConstants.ACTION_DAILY_LIMIT_RESET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled daily limit reset alarm")
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

        if (intent.action == AppConstants.ACTION_DAILY_LIMIT_RESET) {
            val prefsManager = PreferencesManager(context)

            if (!prefsManager.isDailyLimitEnabled) {
                Log.d(TAG, "Daily limit disabled, skipping reset")
                return
            }

            prefsManager.clearDailyLimitDailyState()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_MONITOR)

            Log.d(TAG, "Daily limit state reset at midnight")

            scheduleReset(context)
        }
    }
}
