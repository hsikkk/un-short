package com.muuu.unshort

object AppConstants {
    const val FONT_SCALE = 0.8f

    // Timer-related constants
    const val ACTION_TIMER_COMPLETED = "com.muuu.unshort.TIMER_COMPLETED"
    const val ACTION_TIMER_CANCELLED = "com.muuu.unshort.TIMER_CANCELLED"
    const val ACTION_CLOSE_OVERLAY = "com.muuu.unshort.CLOSE_OVERLAY"
    const val ACTION_TIMER_FORCE_CLOSE = "com.muuu.unshort.TIMER_FORCE_CLOSE"

    // SharedPreferences
    const val PREF_NAME = "app_prefs"

    // Onboarding
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"

    // Blocking
    const val PREF_BLOCKING_ENABLED = "blocking_enabled"
    const val PREF_ALLOWED_UNTIL_SCROLL = "allowed_until_scroll"

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

    // Lifetime Premium
    const val PREF_IS_LIFETIME_PREMIUM = "is_lifetime_premium"
    const val PREF_REDEEMED_PROMO_CODE = "redeemed_promo_code"
    const val PREF_REDEEMED_AT = "redeemed_at"

    // Remote Config Keys
    const val RC_SHOW_AFFILIATE_BANNER = "show_affiliate_banner"
}
