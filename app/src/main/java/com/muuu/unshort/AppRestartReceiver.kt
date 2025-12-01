package com.muuu.unshort

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.muuu.unshort.prefs.PreferencesManager

/**
 * BroadcastReceiver for handling app updates and device boot
 * - MY_PACKAGE_REPLACED: Triggered when app is updated
 * - BOOT_COMPLETED: Triggered when device boots
 */
class AppRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppRestartReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App was updated/replaced")
                handleAppRestart(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device booted")
                handleAppRestart(context)
            }
        }
    }

    private fun handleAppRestart(context: Context) {
        val prefsManager = PreferencesManager(context)

        // Check if onboarding is completed
        if (!prefsManager.isOnboardingCompleted) {
            Log.d(TAG, "Onboarding not completed, skipping service restart")
            return
        }

        // Check if permissions are granted
        val hasAccessibilityPermission = PermissionUtils.isAccessibilityServiceEnabled(context)
        val hasOverlayPermission = PermissionUtils.canDrawOverlays(context)

        if (hasAccessibilityPermission && hasOverlayPermission) {
            Log.d(TAG, "All permissions granted, service should auto-restart")
            // Accessibility service will restart automatically by the system
            // We just need to ensure our app state is ready

            // Clear any stale session data
            prefsManager.clearCurrentSessionId()
            // Note: timer_completed and session_created_time are legacy keys, safe to ignore
            Log.d(TAG, "Cleared stale session data")

            // Schedule daily report notification (only if enabled)
            if (prefsManager.isDailyNotificationsEnabled) {
                DailyReportReceiver.scheduleDailyReport(context)
                Log.d(TAG, "Scheduled daily report notification")
            }
        } else {
            Log.d(TAG, "Missing permissions - Accessibility: $hasAccessibilityPermission, Overlay: $hasOverlayPermission")
        }
    }
}
