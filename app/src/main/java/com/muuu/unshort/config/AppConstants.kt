package com.muuu.unshort.config
import com.muuu.unshort.config.AppConstants

object AppConstants {
    const val FONT_SCALE = 0.8f

    // Timer-related constants
    const val ACTION_TIMER_COMPLETED = "com.muuu.unshort.TIMER_COMPLETED"
    const val ACTION_TIMER_CANCELLED = "com.muuu.unshort.TIMER_CANCELLED"
    const val ACTION_CLOSE_OVERLAY = "com.muuu.unshort.CLOSE_OVERLAY"
    const val ACTION_TIMER_FORCE_CLOSE = "com.muuu.unshort.TIMER_FORCE_CLOSE"
    const val ACTION_WATCH_CONFIRMED = "com.muuu.unshort.WATCH_CONFIRMED"

    // SharedPreferences
    const val PREF_NAME = "app_prefs"

    // In-App Review
    const val PREF_REVIEW_PROMPTED = "review_prompted"
    const val PREF_APP_INSTALLED_AT = "app_installed_at"

    // Blocking
    const val PREF_BLOCKING_ENABLED = "blocking_enabled"
    const val PREF_ALLOWED_UNTIL_SCROLL = "allowed_until_scroll"

    // Temporary Disable
    const val PREF_TEMP_DISABLE_UNTIL = "temp_disable_until"
    const val ACTION_TEMP_DISABLE_EXPIRED = "com.muuu.unshort.TEMP_DISABLE_EXPIRED"

    // Daily Report Notification
    const val ACTION_DAILY_REPORT_ALARM = "com.muuu.unshort.DAILY_REPORT_ALARM"
    const val PREF_LAST_NOTIFICATION_DATE = "last_notification_date"
    const val PREF_DAILY_NOTIFICATIONS_ENABLED = "daily_notifications_enabled"
    const val PREF_DAILY_NOTIFICATION_HOUR = "daily_notification_hour"
    const val PREF_DAILY_NOTIFICATION_MINUTE = "daily_notification_minute"

    // Notification Permission
    const val PREF_HAS_ASKED_NOTIFICATION_PERMISSION = "has_asked_notification_permission"

    // Settings
    const val PREF_WAIT_TIME = "wait_time"
    const val PREF_HAPTIC_ENABLED = "haptic_enabled"
    const val PREF_BLOCK_SCROLLED_ONLY = "block_scrolled_only"
    const val PREF_PREVENT_IMPULSIVE_DISABLE = "prevent_impulsive_disable"

    // Session (timer)
    const val PREF_CURRENT_SESSION_ID = "current_session_id"
    const val PREF_COMPLETED_SESSION_ID = "completed_session_id"

    // Nudge
    const val PREF_HAS_VISITED_SETTINGS = "has_visited_settings"
    const val PREF_HAS_VISITED_STATISTICS = "has_visited_statistics"

    // Lifetime Premium
    const val PREF_IS_LIFETIME_PREMIUM = "is_lifetime_premium"
    const val PREF_REDEEMED_PROMO_CODE = "redeemed_promo_code"
    const val PREF_REDEEMED_AT = "redeemed_at"

    // Blocking Reminder Notification
    const val ACTION_BLOCKING_REMINDER = "com.muuu.unshort.BLOCKING_REMINDER"
    const val PREF_REMINDER_NOTIFICATIONS_ENABLED = "reminder_notifications_enabled"
    const val PREF_LAST_REMINDER_TIMESTAMP = "last_reminder_timestamp"
    const val PREF_REMINDER_COOLDOWN_MINUTES = "reminder_cooldown_minutes"
    const val PREF_REMINDER_THRESHOLD_MINUTES = "reminder_threshold_minutes"  // 기준 시간 (10분)
    const val PREF_REMINDER_WATCH_THRESHOLD_MINUTES = "reminder_watch_threshold_minutes"  // 시청 임계값 (7분)

    // Sleep Mode
    const val ACTION_SLEEP_MODE_START = "com.muuu.unshort.SLEEP_MODE_START"
    const val ACTION_SLEEP_MODE_END = "com.muuu.unshort.SLEEP_MODE_END"
    const val PREF_BLOCKING_BEFORE_SLEEP = "blocking_before_sleep"
    const val PREF_SLEEP_MODE_ENABLED = "sleep_mode_enabled"
    const val PREF_SLEEP_START_HOUR = "sleep_start_hour"
    const val PREF_SLEEP_START_MINUTE = "sleep_start_minute"
    const val PREF_SLEEP_END_HOUR = "sleep_end_hour"
    const val PREF_SLEEP_END_MINUTE = "sleep_end_minute"

    // Changelog
    const val PREF_LAST_SEEN_VERSION_CODE = "last_seen_version_code"

    // Remote Config Keys
    const val RC_SHOW_AFFILIATE_BANNER = "show_affiliate_banner"
}
