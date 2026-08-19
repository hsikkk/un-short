package com.muuu.unshort.analytics

object AnalyticsEvent {
    // App Lifecycle
    const val APP_LAUNCHED = "app_launched"
    const val FIRST_LAUNCH = "first_launch"

    // Blocking Toggle
    const val BLOCKING_ENABLED = "blocking_enabled"
    const val BLOCKING_DISABLED = "blocking_disabled"

    // Privacy Consent
    const val PRIVACY_CONSENT_DIALOG_SHOWN = "privacy_consent_dialog_shown"
    const val PRIVACY_CONSENT_ACCEPTED = "privacy_consent_accepted"
    const val PRIVACY_CONSENT_DECLINED = "privacy_consent_declined"

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

    // Daily Unblock Quota (즉시 해제 한도 시스템)
    const val INSTANT_UNBLOCK_CLICKED = "instant_unblock_clicked"
    const val UNBLOCK_QUOTA_CONSUMED = "unblock_quota_consumed"
    const val UNBLOCK_QUOTA_EXHAUSTED = "unblock_quota_exhausted"
    const val UNBLOCK_VIA_FRICTION_NO_CHARGE = "unblock_via_friction_no_charge"
    const val AD_RECHARGE_CLICKED = "ad_recharge_clicked"
    const val AD_RECHARGE_AD_LOADED = "ad_recharge_ad_loaded"
    const val AD_RECHARGE_AD_FAILED = "ad_recharge_ad_failed"
    const val AD_RECHARGE_AD_COMPLETED = "ad_recharge_ad_completed"
    const val AD_RECHARGE_QUOTA_ADDED = "ad_recharge_quota_added"
    const val DAILY_QUOTA_RESET = "daily_quota_reset"

    // ============= Phase 2 P1 Funnel (신규) =============

    // Permission Funnel
    const val PERMISSION_SETUP_VIEWED = "permission_setup_viewed"
    const val ACCESSIBILITY_CONSENT_DIALOG_SHOWN = "accessibility_consent_dialog_shown"
    const val ACCESSIBILITY_CONSENT_AGREED = "accessibility_consent_agreed"
    const val ACCESSIBILITY_CONSENT_DECLINED = "accessibility_consent_declined"
    const val PERMISSION_SETTINGS_OPENED = "permission_settings_opened"
    const val PERMISSION_GRANTED = "permission_granted"
    const val PERMISSION_DENIED_RETURNED = "permission_denied_returned"
    const val PERMISSION_SETUP_COMPLETED = "permission_setup_completed"

    // Timer Required Flow (10분 내 진입 시도 후 차단 OFF 시도)
    const val TIMER_REQUIRED_DIALOG_SHOWN = "timer_required_dialog_shown"
    const val TIMER_REQUIRED_CHOICE = "timer_required_choice"
    const val INTERSTITIAL_AD_LOAD_FAILED = "interstitial_ad_load_failed"
    const val INTERSTITIAL_AD_SHOWN = "interstitial_ad_shown"
    const val INTERSTITIAL_AD_DISPLAY_FAILED = "interstitial_ad_display_failed"

    // Notifications
    const val NOTIFICATION_SHOWN = "notification_shown"
    const val NOTIFICATION_CLICKED = "notification_clicked"
    const val NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    const val NOTIFICATION_PERMISSION_GRANTED = "notification_permission_granted"
    const val NOTIFICATION_PERMISSION_DENIED = "notification_permission_denied"

    // Task Lock (never attach task titles or photo data)
    const val TASK_LOCK_TASK_CREATED = "task_lock_task_created"
    const val TASK_LOCK_TASK_COMPLETED = "task_lock_task_completed"
    const val TASK_LOCK_OVERLAY_SHOWN = "task_lock_overlay_shown"
    const val TASK_LOCK_POSTPONED = "task_lock_postponed"
    const val TASK_LOCK_AI_RESULT = "task_lock_ai_result"
    const val TASK_LOCK_CANCELLED = "task_lock_cancelled"
}
