package com.muuu.unshort

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.prefs.PreferencesManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver for sending daily report notifications
 * Sends notification at 8 PM daily with today's block count
 */
class DailyReportReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyReportReceiver"
        private const val REQUEST_CODE = 3001
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "daily_report"

        /**
         * Schedule daily report notification at 8 PM
         */
        fun scheduleDailyReport(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyReportReceiver::class.java).apply {
                action = AppConstants.ACTION_DAILY_REPORT_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = getNextReportTime()

            // Use setExactAndAllowWhileIdle for reliable delivery
            // Handle Android 12+ exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarm permission not granted
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

            Log.d(TAG, "Scheduled daily report for: $triggerTime")
        }

        /**
         * Calculate next report time (8 PM today or tomorrow)
         */
        private fun getNextReportTime(): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20) // 8 PM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If 8 PM already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        if (intent.action == AppConstants.ACTION_DAILY_REPORT_ALARM) {
            handleDailyReport(context)
        }
    }

    private fun handleDailyReport(context: Context) {
        val prefsManager = PreferencesManager(context)
        val today = getTodayDateString()

        // Prevent duplicate notifications
        if (prefsManager.lastNotificationDate == today) {
            Log.d(TAG, "Notification already sent today")
            return
        }

        // Get today's block count
        val blockCount = getBlockCountToday(context)

        // Send notification
        sendNotification(context, blockCount)

        // Record notification sent
        prefsManager.lastNotificationDate = today

        // Schedule next day's alarm
        scheduleDailyReport(context)

        Log.d(TAG, "Daily report sent with $blockCount blocks")
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getBlockCountToday(context: Context): Int {
        val repository = StatisticsRepository(context)
        return runBlocking {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = System.currentTimeMillis()

            val stats = repository.getStatsForDate(todayStart, todayEnd)
            stats.attemptCount - stats.watchedCount
        }
    }

    private fun sendNotification(context: Context, blockCount: Int) {
        // Create notification channel (Android 8.0+)
        createNotificationChannel(context)

        // Generate message
        val message = "오늘 하루 리포트를 확인해보세요 ✨"

        // Create intent to open ReportActivity
        val intent = Intent(context, ReportActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘의 쇼츠 차단")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Send notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "일일 리포트",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "매일 쇼츠 차단 통계 알림"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
