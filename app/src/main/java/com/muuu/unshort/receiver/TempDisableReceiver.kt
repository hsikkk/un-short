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
import com.muuu.unshort.receiver.TempDisableReceiver

/**
 * BroadcastReceiver for handling temporary disable expiration
 * Re-enables blocking when the temporary disable period ends
 */
class TempDisableReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TempDisableReceiver"
        private const val REQUEST_CODE = 2001

        /**
         * Schedule an alarm to re-enable blocking at the specified time
         */
        fun scheduleReEnable(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TempDisableReceiver::class.java).apply {
                action = AppConstants.ACTION_TEMP_DISABLE_EXPIRED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setExactAndAllowWhileIdle for more reliable delivery
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarm permission not granted
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

            Log.d(TAG, "Scheduled re-enable alarm for: $triggerAtMillis")
        }

        /**
         * Cancel any pending re-enable alarm
         */
        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TempDisableReceiver::class.java).apply {
                action = AppConstants.ACTION_TEMP_DISABLE_EXPIRED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled re-enable alarm")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        if (intent.action == AppConstants.ACTION_TEMP_DISABLE_EXPIRED) {
            handleTempDisableExpired(context)
        }
    }

    private fun handleTempDisableExpired(context: Context) {
        val prefsManager = PreferencesManager(context)

        // Check if still in temporary disable state
        if (prefsManager.isTemporarilyDisabled()) {
            Log.d(TAG, "Temp disable still active, waiting...")
            return
        }

        // Clear temp disable state
        prefsManager.clearTempDisable()

        if (prefsManager.isSleepTime()) {
            prefsManager.blockingBeforeSleep = true
            Log.d(TAG, "Temporary disable expired during sleep mode, updated blockingBeforeSleep to true")
        } else {
            prefsManager.isBlockingEnabled = true
            Log.d(TAG, "Temporary disable expired, blocking re-enabled")
        }
    }
}
