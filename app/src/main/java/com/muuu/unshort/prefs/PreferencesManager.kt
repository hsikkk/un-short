package com.muuu.unshort.prefs

import android.content.Context
import android.content.SharedPreferences
import com.muuu.unshort.config.AppConstants

/**
 * Centralized manager for SharedPreferences access
 *
 * Provides type-safe access to all app preferences with default values.
 * Follows Single Responsibility Principle by encapsulating all preference logic.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)

    // ========== In-App Review ==========

    var hasReviewPromptShown: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_REVIEW_PROMPTED, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_REVIEW_PROMPTED, value).apply()

    var appInstalledAt: Long
        get() = prefs.getLong(AppConstants.PREF_APP_INSTALLED_AT, 0L)
        set(value) = prefs.edit().putLong(AppConstants.PREF_APP_INSTALLED_AT, value).apply()

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

    // ========== Blocking Reminder Notification ==========

    /**
     * 차단 권유 알림 활성화 여부
     */
    var isReminderNotificationsEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_REMINDER_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_REMINDER_NOTIFICATIONS_ENABLED, value).apply()

    /**
     * 마지막 알림 발송 시각 (epoch milliseconds)
     */
    var lastReminderTimestamp: Long
        get() = prefs.getLong(AppConstants.PREF_LAST_REMINDER_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(AppConstants.PREF_LAST_REMINDER_TIMESTAMP, value).apply()

    /**
     * 알림 쿨다운 시간 (분 단위, 기본 60분)
     */
    var reminderCooldownMinutes: Int
        get() = prefs.getInt(AppConstants.PREF_REMINDER_COOLDOWN_MINUTES, 60)
        set(value) = prefs.edit().putInt(AppConstants.PREF_REMINDER_COOLDOWN_MINUTES, value).apply()

    /**
     * 연속 시청 감지 기준 시간 (분 단위, 기본 10분)
     * 이 시간 내의 총 시청 시간을 확인
     */
    var reminderWindowMinutes: Int
        get() = prefs.getInt(AppConstants.PREF_REMINDER_THRESHOLD_MINUTES, 10)
        set(value) = prefs.edit().putInt(AppConstants.PREF_REMINDER_THRESHOLD_MINUTES, value).apply()

    /**
     * 연속 시청 감지 임계값 (분 단위, 기본 7분)
     * reminderWindowMinutes 내에 이 시간 이상 시청하면 알림
     */
    var reminderThresholdMinutes: Int
        get() = prefs.getInt(AppConstants.PREF_REMINDER_WATCH_THRESHOLD_MINUTES, 7)
        set(value) = prefs.edit().putInt(AppConstants.PREF_REMINDER_WATCH_THRESHOLD_MINUTES, value).apply()

    /**
     * 알림을 보낼 수 있는 상태인지 확인 (쿨다운 체크)
     */
    fun canSendReminderNotification(): Boolean {
        if (!isReminderNotificationsEnabled) return false

        val lastTimestamp = lastReminderTimestamp
        if (lastTimestamp == 0L) return true

        val cooldownMs = reminderCooldownMinutes * 60 * 1000L
        return System.currentTimeMillis() - lastTimestamp >= cooldownMs
    }

    // ========== Sleep Mode ==========

    var blockingBeforeSleep: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_BLOCKING_BEFORE_SLEEP, true)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_BLOCKING_BEFORE_SLEEP, value).apply()

    var isSleepModeEnabled: Boolean
        get() = prefs.getBoolean(AppConstants.PREF_SLEEP_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(AppConstants.PREF_SLEEP_MODE_ENABLED, value).apply()

    var sleepStartHour: Int
        get() = prefs.getInt(AppConstants.PREF_SLEEP_START_HOUR, 22)
        set(value) = prefs.edit().putInt(AppConstants.PREF_SLEEP_START_HOUR, value).apply()

    var sleepStartMinute: Int
        get() = prefs.getInt(AppConstants.PREF_SLEEP_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(AppConstants.PREF_SLEEP_START_MINUTE, value).apply()

    var sleepEndHour: Int
        get() = prefs.getInt(AppConstants.PREF_SLEEP_END_HOUR, 6)
        set(value) = prefs.edit().putInt(AppConstants.PREF_SLEEP_END_HOUR, value).apply()

    var sleepEndMinute: Int
        get() = prefs.getInt(AppConstants.PREF_SLEEP_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(AppConstants.PREF_SLEEP_END_MINUTE, value).apply()

    /**
     * 현재 시간이 수면 시간대인지 판단
     *
     * 자정을 넘기는 경우(예: 22:00~06:00)와
     * 같은 날인 경우(예: 01:00~06:00) 모두 처리
     */
    fun isSleepTime(): Boolean {
        if (!isSleepModeEnabled) return false

        val now = java.time.LocalTime.now()
        val start = java.time.LocalTime.of(sleepStartHour, sleepStartMinute)
        val end = java.time.LocalTime.of(sleepEndHour, sleepEndMinute)

        return if (start > end) {
            now >= start || now < end
        } else if (start == end) {
            false
        } else {
            now in start..<end
        }
    }

    /**
     * 수면 시간대의 표시용 문자열 반환 (예: "22:00~06:00")
     */
    fun getSleepTimeDisplayString(): String {
        return String.format("%02d:%02d~%02d:%02d", sleepStartHour, sleepStartMinute, sleepEndHour, sleepEndMinute)
    }

    // ========== Changelog ==========

    var lastSeenVersionCode: Int
        get() = prefs.getInt(AppConstants.PREF_LAST_SEEN_VERSION_CODE, 0)
        set(value) = prefs.edit().putInt(AppConstants.PREF_LAST_SEEN_VERSION_CODE, value).apply()

    fun shouldShowChangelog(currentVersionCode: Int): Boolean {
        val lastSeen = lastSeenVersionCode
        return lastSeen > 0 && currentVersionCode > lastSeen
    }

    fun markChangelogSeen(currentVersionCode: Int) {
        lastSeenVersionCode = currentVersionCode
    }

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

    // ========== Per-App Blocking Settings ==========

    /**
     * 특정 앱의 차단 활성화 상태 가져오기
     *
     * @param packageName 앱 패키지명
     * @return 차단 활성화 여부 (기본값: true)
     */
    fun isAppBlockingEnabled(packageName: String): Boolean {
        val key = "blocking_enabled_$packageName"
        return prefs.getBoolean(key, true)
    }

    /**
     * 특정 앱의 차단 활성화 상태 설정
     *
     * @param packageName 앱 패키지명
     * @param enabled 차단 활성화 여부
     */
    fun setAppBlockingEnabled(packageName: String, enabled: Boolean) {
        val key = "blocking_enabled_$packageName"
        prefs.edit().putBoolean(key, enabled).apply()
    }

    /**
     * 차단이 활성화된 앱 패키지 목록 가져오기
     *
     * @return 활성화된 앱 패키지명 Set
     */
    fun getEnabledAppPackages(): Set<String> {
        return com.muuu.unshort.service.blocking.AppBlockingRegistry.ALL_CONFIGS
            .filter { isAppBlockingEnabled(it.packageName) }
            .map { it.packageName }
            .toSet()
    }
}
