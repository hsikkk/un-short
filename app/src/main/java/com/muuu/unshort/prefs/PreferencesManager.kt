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
