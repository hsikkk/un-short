package com.muuu.unshort.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.muuu.unshort.R
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.ui.activity.MainActivity
import java.time.LocalDate

/**
 * 일일 제한 시간 관련 알림 발송
 *
 * 두 가지 알림 채널을 관리한다:
 * - daily_limit (DEFAULT): 80% 경고 및 제한 초과 알림
 * - watch_time_monitor (LOW): 상시 모니터링 알림
 */
class DailyLimitNotifier(private val context: Context) {

    companion object {
        private const val TAG = "DailyLimitNotifier"

        private const val CHANNEL_DAILY_LIMIT = "daily_limit"
        private const val CHANNEL_WATCH_MONITOR = "watch_time_monitor"

        private const val NOTIFICATION_ID_LIMIT = com.muuu.unshort.config.AppConstants.NOTIFICATION_ID_DAILY_LIMIT
        private const val NOTIFICATION_ID_MONITOR = com.muuu.unshort.config.AppConstants.NOTIFICATION_ID_DAILY_LIMIT_MONITOR
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun sendWarningNotification() {
        val pendingIntent = createMainActivityPendingIntent(AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_WARNING)

        val title = context.getString(R.string.daily_limit_notif_warning_title)
        val message = context.getString(R.string.daily_limit_notif_warning_message)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_LIMIT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_LIMIT, notification)
        AnalyticsManager.trackEvent(
            context,
            AnalyticsEvent.NOTIFICATION_SHOWN,
            mapOf("type" to AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_WARNING)
        )
        Log.d(TAG, "80% warning notification sent")
    }

    fun sendLimitExceededNotification() {
        val pendingIntent = createMainActivityPendingIntent(AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_EXCEEDED)

        val title = context.getString(R.string.daily_limit_notif_exceeded_title)
        val message = context.getString(R.string.daily_limit_notif_exceeded_message)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_LIMIT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_LIMIT, notification)
        AnalyticsManager.trackEvent(
            context,
            AnalyticsEvent.NOTIFICATION_SHOWN,
            mapOf("type" to AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_EXCEEDED)
        )
        Log.d(TAG, "Limit exceeded notification sent")
    }

    fun updateMonitoringNotification(currentMs: Long, limitMs: Long) {
        val currentMinutes = (currentMs / 60000).toInt()
        val limitMinutes = (limitMs / 60000).toInt()
        val percentage = if (limitMs > 0) ((currentMs * 100) / limitMs).toInt() else 0
        val exceeded = currentMs >= limitMs

        val pendingIntent = createMainActivityPendingIntent(AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_MONITOR)

        val title = context.getString(R.string.daily_limit_notif_monitor_title, currentMinutes)

        val progressText = if (exceeded) {
            context.getString(R.string.daily_limit_notif_monitor_exceeded, currentMinutes, limitMinutes)
        } else {
            context.getString(R.string.daily_limit_notif_monitor_text, currentMinutes, limitMinutes)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_WATCH_MONITOR)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(progressText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percentage.coerceAtMost(100), false)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MONITOR, notification)

        // 모니터링 알림은 빈번하게 갱신되므로 일자별 1회만 trackEvent
        val prefs = PreferencesManager(context)
        val today = LocalDate.now().toString()
        if (prefs.dailyLimitMonitorNotifiedDate != today) {
            prefs.dailyLimitMonitorNotifiedDate = today
            AnalyticsManager.trackEvent(
                context,
                AnalyticsEvent.NOTIFICATION_SHOWN,
                mapOf("type" to AppConstants.NOTIFICATION_TYPE_DAILY_LIMIT_MONITOR)
            )
        }

        Log.d(TAG, "Monitoring notification updated: ${currentMinutes}min / ${limitMinutes}min ($percentage%)")
    }

    fun cancelMonitoringNotification() {
        notificationManager.cancel(NOTIFICATION_ID_MONITOR)
        Log.d(TAG, "Monitoring notification cancelled")
    }

    private fun createMainActivityPendingIntent(notificationType: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppConstants.EXTRA_LAUNCH_SOURCE, AppConstants.LAUNCH_SOURCE_NOTIFICATION)
            putExtra(AppConstants.EXTRA_NOTIFICATION_TYPE, notificationType)
        }
        // requestCode를 type별로 분리해야 PendingIntent extras가 갱신됨
        val requestCode = notificationType.hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val limitChannel = NotificationChannel(
                CHANNEL_DAILY_LIMIT,
                context.getString(R.string.daily_limit_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.daily_limit_channel_desc)
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_WATCH_MONITOR,
                context.getString(R.string.daily_limit_monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.daily_limit_monitor_channel_desc)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(limitChannel)
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }
}
