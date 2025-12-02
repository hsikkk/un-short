package com.muuu.unshort.analytics

import android.content.Context
import android.util.Log
import com.amplitude.android.Identify
import com.amplitude.core.events.BaseEvent
import com.muuu.unshort.PermissionUtils
import com.muuu.unshort.UnshortApplication

object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    /**
     * 이벤트 트래킹
     */
    fun trackEvent(
        context: Context,
        eventName: String,
        properties: Map<String, Any?>? = null
    ) {
        Log.d(TAG, "trackEvent: $eventName")

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
     * 권한 상태를 User Property로 설정
     */
    fun updatePermissionUserProperties(context: Context) {
        val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        val hasOverlay = PermissionUtils.canDrawOverlays(context)
        val hasAllPermissions = PermissionUtils.hasAllPermissions(context)
        val manufacturer = PermissionUtils.getManufacturer()

        // Amplitude user properties 설정
        val identify = Identify()
            .set("has_accessibility_permission", hasAccessibility)
            .set("has_overlay_permission", hasOverlay)
            .set("has_all_permissions", hasAllPermissions)
            .set("device_manufacturer", manufacturer)

        UnshortApplication.amplitude.identify(identify)

        Log.d(TAG, "User properties updated - Accessibility: $hasAccessibility, Overlay: $hasOverlay")
    }
}
