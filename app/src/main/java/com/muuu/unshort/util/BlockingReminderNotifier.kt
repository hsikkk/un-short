package com.muuu.unshort.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.util.BlockingReminderNotifier
import com.muuu.unshort.ui.activity.MainActivity
import com.muuu.unshort.R

/**
 * 차단 기능 권유 알림 발송
 *
 * 차단이 OFF인 상태에서 연속 시청 임계값 초과 시 알림 발송
 *
 * 책임:
 * - 알림 채널 생성
 * - 알림 발송
 * - 마지막 알림 시각 기록
 */
class BlockingReminderNotifier(private val context: Context) {

    companion object {
        private const val TAG = "BlockingReminderNotif"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "blocking_reminder"
    }

    private val prefsManager = PreferencesManager(context)

    /**
     * 차단 권유 알림 발송
     */
    fun sendReminderNotification() {
        // 쿨다운 체크 (이중 체크)
        if (!prefsManager.canSendReminderNotification()) {
            Log.d(TAG, "쿨다운 기간 - 알림 발송 취소")
            return
        }

        // 알림 채널 생성
        createNotificationChannel()

        // 메인 화면으로 이동하는 PendingIntent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // TODO: [i18n] strings.xml로 이동 필요
        val title = "쇼츠를 많이 보고 계시네요"
        val message = "차단 기능을 켜보는 건 어떨까요?"

        // 알림 빌드
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 알림 발송
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // 마지막 알림 시각 기록
        prefsManager.lastReminderTimestamp = System.currentTimeMillis()

        Log.d(TAG, "차단 권유 알림 발송 완료")
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: [i18n] strings.xml로 이동 필요
            val channelName = "차단 권유 알림"
            val channelDescription = "쇼츠 연속 시청 시 차단 기능을 권유하는 알림"

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDescription
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
