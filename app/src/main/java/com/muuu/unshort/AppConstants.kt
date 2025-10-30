package com.muuu.unshort

object AppConstants {
    const val FONT_SCALE = 0.8f

    // Timer-related constants
    const val ACTION_TIMER_STARTED = "com.muuu.unshort.TIMER_STARTED"
    const val ACTION_TIMER_COMPLETED = "com.muuu.unshort.TIMER_COMPLETED"
    const val ACTION_TIMER_CANCELLED = "com.muuu.unshort.TIMER_CANCELLED"

    // SharedPreferences keys for timer
    const val PREF_CURRENT_SESSION_ID = "current_session_id"
    const val PREF_COMPLETED_SESSION_ID = "completed_session_id"
    const val PREF_TIMER_REMAINING_SECONDS = "timer_remaining_seconds"
}
