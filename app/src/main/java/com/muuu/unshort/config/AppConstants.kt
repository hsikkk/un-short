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
    const val PREF_INSTANT_UNBLOCK_ENABLED = "instant_unblock_enabled"
    const val PREF_PREVENT_IMPULSIVE_DISABLE = "prevent_impulsive_disable"
    const val PREF_CUSTOM_MESSAGE_BEFORE_TIMER = "custom_message_before_timer"
    const val PREF_CUSTOM_MESSAGE_AFTER_TIMER = "custom_message_after_timer"

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

    // Daily Limit
    const val NOTIFICATION_ID_DAILY_LIMIT = 1003
    const val NOTIFICATION_ID_DAILY_LIMIT_MONITOR = 1004
    const val PREF_DAILY_LIMIT_ENABLED = "daily_limit_enabled"
    const val PREF_DAILY_LIMIT_MINUTES = "daily_limit_minutes"
    const val PREF_DAILY_LIMIT_WARNING_ENABLED = "daily_limit_warning_enabled"
    const val PREF_DAILY_LIMIT_MONITOR_ENABLED = "daily_limit_monitor_enabled"
    const val PREF_DAILY_LIMIT_EXCEEDED_TODAY = "daily_limit_exceeded_today"
    const val PREF_DAILY_LIMIT_WARNING_SENT_TODAY = "daily_limit_warning_sent_today"
    const val ACTION_DAILY_LIMIT_RESET = "com.muuu.unshort.DAILY_LIMIT_RESET"

    // Changelog
    const val PREF_LAST_SEEN_VERSION_CODE = "last_seen_version_code"

    // Remote Config Keys
    const val RC_UNBLOCK_QUOTA_DAILY_LIMIT = "unblock_quota_daily_limit"
    const val RC_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT = "unblock_quota_ad_recharge_amount"

    // Daily Unblock Quota (즉시 해제 한도 시스템)
    const val PREF_DAILY_UNBLOCK_QUOTA_REMAINING = "daily_unblock_quota_remaining"
    const val PREF_DAILY_UNBLOCK_QUOTA_LAST_RESET_DATE = "daily_unblock_quota_last_reset_date"
    const val PREF_DAILY_UNBLOCK_QUOTA_INSTALL_DATE = "daily_unblock_quota_install_date"
    const val PREF_DAILY_UNBLOCK_TOTAL_TODAY = "daily_unblock_total_today"
    const val PREF_DAILY_AD_WATCHED_TODAY = "daily_ad_watched_today"

    // Daily Unblock Quota - Intent extras / Broadcast actions
    const val EXTRA_ENTRY_FROM_SCROLL = "entry_from_scroll"
    const val ACTION_INSTANT_UNBLOCK = "com.muuu.unshort.ACTION_INSTANT_UNBLOCK"
    const val ACTION_DAILY_UNBLOCK_QUOTA_RESET = "com.muuu.unshort.DAILY_UNBLOCK_QUOTA_RESET"

    // Daily Unblock Quota - 기본값
    const val DEFAULT_UNBLOCK_QUOTA_DAILY_LIMIT = 10
    const val DEFAULT_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT = 3

    // Analytics - lifetime / onboarding state
    const val PREF_TOTAL_UNBLOCKS_LIFETIME = "total_unblocks_lifetime"
    const val PREF_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    const val PREF_LAST_FIRST_LAUNCH_DATE = "last_first_launch_date"
    const val PREF_DAILY_LIMIT_MONITOR_NOTIFIED_DATE = "daily_limit_monitor_notified_date"
    const val PREF_LAST_KNOWN_ACCESSIBILITY = "last_known_accessibility"
    const val PREF_LAST_KNOWN_OVERLAY = "last_known_overlay"
    const val PREF_LAST_KNOWN_NOTIFICATION = "last_known_notification"

    // Analytics - intent extras
    const val EXTRA_LAUNCH_SOURCE = "launch_source"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_ENTRY_SOURCE = "entry_source"

    // launch_source values
    const val LAUNCH_SOURCE_ICON = "icon"
    const val LAUNCH_SOURCE_NOTIFICATION = "notification"

    // notification_type values
    const val NOTIFICATION_TYPE_DAILY_REPORT = "daily_report"
    const val NOTIFICATION_TYPE_BLOCKING_REMINDER = "blocking_reminder"
    const val NOTIFICATION_TYPE_DAILY_LIMIT_WARNING = "daily_limit_warning"
    const val NOTIFICATION_TYPE_DAILY_LIMIT_EXCEEDED = "daily_limit_exceeded"
    const val NOTIFICATION_TYPE_DAILY_LIMIT_MONITOR = "daily_limit_monitor"

    // entry_source values for screens
    const val ENTRY_SOURCE_MAIN_BUTTON = "main_button"
    const val ENTRY_SOURCE_NOTIFICATION = "notification"
    const val ENTRY_SOURCE_FEATURE_LOCK = "feature_lock"
    const val ENTRY_SOURCE_QUOTA_EXHAUSTED = "quota_exhausted_dialog"
    const val ENTRY_SOURCE_SETTINGS = "settings"
    const val ENTRY_SOURCE_REPORT = "report"
    const val ENTRY_SOURCE_DIRECT = "direct"
}
