package com.muuu.unshort.prefs

import android.content.Context
import android.content.SharedPreferences
import com.muuu.unshort.AppConstants

/**
 * Centralized manager for SharedPreferences access
 *
 * Provides type-safe access to all app preferences with default values.
 * Follows Single Responsibility Principle by encapsulating all preference logic.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)

    // ========== Onboarding ==========

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_ONBOARDING_COMPLETED, value).apply()

    // ========== Blocking ==========

    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_BLOCKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_BLOCKING_ENABLED, value).apply()

    var isAllowedUntilScroll: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_ALLOWED_UNTIL_SCROLL, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_ALLOWED_UNTIL_SCROLL, value).apply()

    fun clearAllowedUntilScroll() {
        prefs.edit().remove(AppConstants.PREF_ALLOWED_UNTIL_SCROLL).apply()
    }

    // ========== Temporary Disable ==========

    /**
     * 일시 해제 만료 시간 (epoch milliseconds)
     * 0이면 일시 해제 상태 아님
     */
    var tempDisableUntil: Long
        get() = prefs.getLong(AppConstants.PREF_TEMP_DISABLE_UNTIL, 0L)
        set(value) = prefs.edit().putLong(AppConstants.PREF_TEMP_DISABLE_UNTIL, value).apply()

    /**
     * 일시 해제 상태인지 확인
     */
    fun isTemporarilyDisabled(): Boolean {
        val until = tempDisableUntil
        return until > 0 && System.currentTimeMillis() < until
    }

    /**
     * 일시 해제 남은 시간 (milliseconds)
     */
    fun getTempDisableRemainingTime(): Long {
        val until = tempDisableUntil
        if (until <= 0) return 0
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * 일시 해제 상태 초기화
     */
    fun clearTempDisable() {
        prefs.edit().remove(AppConstants.PREF_TEMP_DISABLE_UNTIL).apply()
    }

    // ========== Settings ==========

    var waitTime: Int
        get() = prefs.getInt(AppConstants.PREF_WAIT_TIME, 30)
        set(value) = prefs.edit().putInt(AppConstants.PREF_WAIT_TIME, value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_HAPTIC_ENABLED, value).apply()

    var isBlockScrolledOnly: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_BLOCK_SCROLLED_ONLY, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_BLOCK_SCROLLED_ONLY, value).apply()

    var isPreventImpulsiveDisable: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_PREVENT_IMPULSIVE_DISABLE, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_PREVENT_IMPULSIVE_DISABLE, value).apply()

    // ========== Session ==========

    var currentSessionId: String?
        get() = prefs.getString(AppConstants.PREF_CURRENT_SESSION_ID, null)
        set(value) = prefs.edit().putString(AppConstants.PREF_CURRENT_SESSION_ID, value).apply()

    var completedSessionId: String?
        get() = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, null)
        set(value) = prefs.edit().putString(AppConstants.PREF_COMPLETED_SESSION_ID, value).apply()

    fun clearCompletedSessionId() {
        prefs.edit().remove(AppConstants.PREF_COMPLETED_SESSION_ID).apply()
    }

    fun clearCurrentSessionId() {
        prefs.edit().remove(AppConstants.PREF_CURRENT_SESSION_ID).apply()
    }

    // ========== Nudge ==========

    var hasVisitedSettings: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_HAS_VISITED_SETTINGS, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_HAS_VISITED_SETTINGS, value).apply()

    var hasVisitedStatistics: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_HAS_VISITED_STATISTICS, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_HAS_VISITED_STATISTICS, value).apply()

    // ========== Lifetime Premium ==========

    var isLifetimePremium: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_IS_LIFETIME_PREMIUM, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_IS_LIFETIME_PREMIUM, value).apply()

    var redeemedPromoCode: String?
        get() = prefs.getString(AppConstants.PREF_REDEEMED_PROMO_CODE, null)
        set(value) = prefs.edit().putString(AppConstants.PREF_REDEEMED_PROMO_CODE, value).apply()

    var redeemedAt: Long
        get() = prefs.getLong(AppConstants.PREF_REDEEMED_AT, 0L)
        set(value) = prefs.edit().putLong(AppConstants.PREF_REDEEMED_AT, value).apply()

    // ========== Daily Report Notification ==========

    var lastNotificationDate: String
        get() = prefs.getString(AppConstants.PREF_LAST_NOTIFICATION_DATE, "") ?: ""
        set(value) = prefs.edit().putString(AppConstants.PREF_LAST_NOTIFICATION_DATE, value).apply()

    var isDailyNotificationsEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_DAILY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_DAILY_NOTIFICATIONS_ENABLED, value).apply()

    var dailyNotificationHour: Int
        get() = prefs.getInt(AppConstants.PREF_DAILY_NOTIFICATION_HOUR, 20)
        set(value) = prefs.edit().putInt(AppConstants.PREF_DAILY_NOTIFICATION_HOUR, value).apply()

    var dailyNotificationMinute: Int
        get() = prefs.getInt(AppConstants.PREF_DAILY_NOTIFICATION_MINUTE, 0)
        set(value) = prefs.edit().putInt(AppConstants.PREF_DAILY_NOTIFICATION_MINUTE, value).apply()

    // ========== Notification Permission ==========

    var hasAskedNotificationPermission: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_HAS_ASKED_NOTIFICATION_PERMISSION, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_HAS_ASKED_NOTIFICATION_PERMISSION, value).apply()

    // ========== Batch Operations ==========

    /**
     * Clear all session-related state
     */
    fun clearSessionState() {
        prefs.edit().apply {
            remove(AppConstants.PREF_CURRENT_SESSION_ID)
            remove(AppConstants.PREF_COMPLETED_SESSION_ID)
            remove(AppConstants.PREF_ALLOWED_UNTIL_SCROLL)
            apply()
        }
    }
}
