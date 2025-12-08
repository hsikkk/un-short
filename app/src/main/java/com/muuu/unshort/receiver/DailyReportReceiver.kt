package com.muuu.unshort.receiver

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
import com.muuu.unshort.R
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.ui.activity.ReportActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.muuu.unshort.receiver.DailyReportReceiver

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

            val triggerTime = getNextReportTime(context)

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
         * Cancel scheduled daily report notification
         */
        fun cancelDailyReport(context: Context) {
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

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled daily report alarm")
        }

        /**
         * Calculate next report time (configured hour today or tomorrow)
         */
        private fun getNextReportTime(context: Context): Long {
            val prefsManager = PreferencesManager(context)
            val notificationHour = prefsManager.dailyNotificationHour
            val notificationMinute = prefsManager.dailyNotificationMinute

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, notificationHour)
                set(Calendar.MINUTE, notificationMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If configured time already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            return calendar.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        if (intent.action == AppConstants.ACTION_DAILY_REPORT_ALARM) {
            val prefsManager = PreferencesManager(context)

            // 알림이 꺼져있으면 발송하지 않음
            if (!prefsManager.isDailyNotificationsEnabled) {
                Log.d(TAG, "Daily notifications disabled, skipping")
                return
            }

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

        // Send notification
        sendNotification(context)

        // Record notification sent
        prefsManager.lastNotificationDate = today

        // Schedule next day's alarm
        scheduleDailyReport(context)

        Log.d(TAG, "Daily report sent")
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun sendNotification(context: Context) {
        // Create notification channel (Android 8.0+)
        createNotificationChannel(context)

        // Generate message
        val message = context.getString(R.string.daily_report_notification_message)

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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.daily_report_notification_title))
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
                context.getString(R.string.daily_report_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.daily_report_channel_desc)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
