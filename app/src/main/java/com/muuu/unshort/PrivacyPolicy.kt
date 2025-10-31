package com.muuu.unshort

/**
 * Privacy Policy version management for accessibility service consent
 *
 * When privacy policy is updated, increment CURRENT_VERSION to require re-consent
 */
object PrivacyPolicy {
    /**
     * Current privacy policy version
     * Increment this when policy changes to require user re-consent
     */
    const val CURRENT_VERSION = 1

    /**
     * SharedPreferences keys
     */
    const val PREF_CONSENT_VERSION = "privacy_consent_version"
    const val PREF_CONSENT_TIMESTAMP = "privacy_consent_timestamp"
}
