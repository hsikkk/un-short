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
}
