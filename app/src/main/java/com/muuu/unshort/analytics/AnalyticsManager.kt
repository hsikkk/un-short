package com.muuu.unshort.analytics

import android.content.Context
import android.util.Log
import com.amplitude.core.events.BaseEvent
import com.muuu.unshort.PrivacyPolicy
import com.muuu.unshort.UnshortApplication

object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    /**
     * 이벤트 트래킹
     * 개인정보 동의가 완료된 경우에만 전송
     */
    fun trackEvent(
        context: Context,
        eventName: String,
        properties: Map<String, Any?>? = null
    ) {
        // 개인정보 동의 확인
        val hasConsent = hasValidPrivacyConsent(context)
        Log.d(TAG, "trackEvent: $eventName, hasConsent: $hasConsent")

        if (!hasConsent) {
            Log.d(TAG, "Event blocked - no privacy consent: $eventName")
            return
        }

        // Amplitude에 이벤트 전송
        val event = BaseEvent().apply {
            this.eventType = eventName
            properties?.let { props ->
                this.eventProperties = props.toMutableMap()
            }
        }

        UnshortApplication.amplitude.track(event)
        Log.d(TAG, "Event tracked successfully: $eventName")
    }

    /**
     * 개인정보 동의 여부 확인
     */
    private fun hasValidPrivacyConsent(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedVersion = prefs.getInt(PrivacyPolicy.PREF_CONSENT_VERSION, 0)
        val isValid = savedVersion >= PrivacyPolicy.CURRENT_VERSION
        Log.d(TAG, "Privacy consent check - savedVersion: $savedVersion, requiredVersion: ${PrivacyPolicy.CURRENT_VERSION}, isValid: $isValid")
        return isValid
    }
}
