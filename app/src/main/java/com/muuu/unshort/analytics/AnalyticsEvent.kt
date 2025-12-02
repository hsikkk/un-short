package com.muuu.unshort.analytics

object AnalyticsEvent {
    // App Lifecycle
    const val APP_LAUNCHED = "app_launched"

    // Blocking Toggle
    const val BLOCKING_ENABLED = "blocking_enabled"
    const val BLOCKING_DISABLED = "blocking_disabled"

    // Onboarding
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_COMPLETED = "onboarding_completed"

    // Privacy Consent
    const val PRIVACY_CONSENT_ACCEPTED = "privacy_consent_accepted"

    // Overlay
    const val OVERLAY_SHOWN_BEFORE_TIMER = "overlay_shown_before_timer"
    const val OVERLAY_SHOWN_AFTER_TIMER = "overlay_shown_after_timer"
    const val OVERLAY_BUTTON_START_TIMER = "overlay_button_start_timer"
    const val OVERLAY_BUTTON_SKIP = "overlay_button_skip"
    const val OVERLAY_BUTTON_WATCH = "overlay_button_watch"

    // Timer
    const val TIMER_ACTIVITY_OPENED = "timer_activity_opened"
    const val TIMER_COMPLETED = "timer_completed"

    // Disable Confirm Timer
    const val DISABLE_CONFIRM_TIMER_OPENED = "disable_confirm_timer_opened"
    const val DISABLE_CONFIRM_TIMER_COMPLETED = "disable_confirm_timer_completed"
    const val DISABLE_CONFIRM_TIMER_SKIPPED = "disable_confirm_timer_skipped"

    // Affiliate
    const val AFFILIATE_PRODUCT_CLICKED = "affiliate_product_clicked"

    // In-App Review
    const val REVIEW_PROMPT_SHOWN = "review_prompt_shown"

    // Premium Feature Lock Interactions
    const val PREMIUM_FEATURE_LOCK_CLICKED = "premium_feature_lock_clicked"

    // Premium Upgrade Screen
    const val PREMIUM_UPGRADE_SCREEN_VIEWED = "premium_upgrade_screen_viewed"
    const val PREMIUM_UPGRADE_BUTTON_CLICKED = "premium_upgrade_button_clicked"

    // Promo Code Flow
    const val PROMO_CODE_DIALOG_OPENED = "promo_code_dialog_opened"
    const val PROMO_CODE_SUBMITTED = "promo_code_submitted"
    const val PROMO_CODE_REDEEMED_SUCCESS = "promo_code_redeemed_success"
    const val PROMO_CODE_REDEEMED_FAILED = "promo_code_redeemed_failed"

    // Google Play Purchase Flow
    const val PREMIUM_PURCHASE_STARTED = "premium_purchase_started"
    const val PREMIUM_PURCHASE_COMPLETED = "premium_purchase_completed"
    const val PREMIUM_PURCHASE_FAILED = "premium_purchase_failed"
    const val PREMIUM_PURCHASE_CANCELLED = "premium_purchase_cancelled"

    // Subscription Management
    const val SUBSCRIPTION_MANAGEMENT_OPENED = "subscription_management_opened"

    // Premium Status
    const val PREMIUM_STATUS_ACTIVATED = "premium_status_activated"
    const val PREMIUM_STATUS_DEACTIVATED = "premium_status_deactivated"

    // Screen Views
    const val SETTINGS_SCREEN_VIEWED = "settings_screen_viewed"
    const val REPORT_SCREEN_VIEWED = "report_screen_viewed"
}
