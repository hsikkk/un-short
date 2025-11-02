package com.muuu.unshort.analytics

import android.content.Context
import android.util.Log
import com.amplitude.core.events.BaseEvent
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
}
