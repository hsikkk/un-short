package com.muuu.unshort.receiver

import android.app.AlarmManager
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
 * 수면 모드 자동 차단 ON/OFF를 위한 BroadcastReceiver
 *
 * 수면 시간 진입 시 차단을 자동으로 활성화하고,
 * 기상 시간에 수면 진입 전 상태로 복원한다.
 */
class SleepModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepModeReceiver"
        private const val REQUEST_CODE_START = 4001
        private const val REQUEST_CODE_END = 4002

        fun scheduleAlarms(context: Context) {
            val prefsManager = PreferencesManager(context)
            if (!prefsManager.isSleepModeEnabled) return

            if (prefsManager.isSleepTime()) {
                handleSleepStart(context, prefsManager)
                scheduleAlarm(
                    context,
                    prefsManager.sleepEndHour,
                    prefsManager.sleepEndMinute,
                    AppConstants.ACTION_SLEEP_MODE_END,
                    REQUEST_CODE_END
                )
                Log.d(TAG, "Currently in sleep time. Blocking ON, scheduled end alarm only.")
                return
            }

            scheduleAlarm(
                context,
                prefsManager.sleepStartHour,
                prefsManager.sleepStartMinute,
                AppConstants.ACTION_SLEEP_MODE_START,
                REQUEST_CODE_START
            )
            scheduleAlarm(
                context,
                prefsManager.sleepEndHour,
                prefsManager.sleepEndMinute,
                AppConstants.ACTION_SLEEP_MODE_END,
                REQUEST_CODE_END
            )
            Log.d(TAG, "Scheduled sleep alarms: start=${prefsManager.sleepStartHour}:${prefsManager.sleepStartMinute}, end=${prefsManager.sleepEndHour}:${prefsManager.sleepEndMinute}")
        }

        fun cancelAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            cancelPendingIntent(context, alarmManager, AppConstants.ACTION_SLEEP_MODE_START, REQUEST_CODE_START)
            cancelPendingIntent(context, alarmManager, AppConstants.ACTION_SLEEP_MODE_END, REQUEST_CODE_END)
            Log.d(TAG, "Cancelled all sleep mode alarms")
        }

        private fun cancelPendingIntent(
            context: Context,
            alarmManager: AlarmManager,
            action: String,
            requestCode: Int
        ) {
            val intent = Intent(context, SleepModeReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun scheduleAlarm(
            context: Context,
            hour: Int,
            minute: Int,
            action: String,
            requestCode: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SleepModeReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = getNextTriggerTime(hour, minute)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        private fun getNextTriggerTime(hour: Int, minute: Int): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis
        }

        private fun handleSleepStart(context: Context, prefsManager: PreferencesManager) {
            prefsManager.blockingBeforeSleep = prefsManager.isBlockingEnabled

            prefsManager.isBlockingEnabled = true
            Log.d(TAG, "Sleep mode started. Previous blocking state saved: ${prefsManager.blockingBeforeSleep}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        val prefsManager = PreferencesManager(context)

        when (intent.action) {
            AppConstants.ACTION_SLEEP_MODE_START -> {
                if (!prefsManager.isSleepModeEnabled) return

                handleSleepStart(context, prefsManager)
                scheduleAlarms(context)
            }
            AppConstants.ACTION_SLEEP_MODE_END -> {
                if (!prefsManager.isSleepModeEnabled) return

                val previousState = prefsManager.blockingBeforeSleep
                if (!previousState) {
                    prefsManager.isBlockingEnabled = false
                }
                Log.d(TAG, "Sleep mode ended. Restored blocking to: $previousState")

                scheduleAlarms(context)
            }
        }
    }
}
