package com.muuu.unshort.analytics

import android.content.Context
import android.util.Log
import com.amplitude.core.events.BaseEvent
import com.amplitude.core.events.Identify
import com.muuu.unshort.BuildConfig
import com.muuu.unshort.UnshortApplication
import com.muuu.unshort.ad.DailyUnblockQuotaManager
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.util.PermissionUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Amplitude 이벤트 트래킹 및 user property 관리
 *
 * - trackEvent: 이벤트 발화 (공통 컨텍스트 자동 머지)
 * - updateXxxUserProperties: 상태 변화 시 user property Identify
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager"

    private val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private val INSTAGRAM_PACKAGE = "com.instagram.android"

    /**
     * 이벤트 트래킹
     *
     * @param includeCommon false 시 공통 컨텍스트 머지 생략 (외부 상태 의존성이 위험할 때)
     */
    fun trackEvent(
        context: Context,
        eventName: String,
        properties: Map<String, Any?>? = null,
        includeCommon: Boolean = true
    ) {
        val merged: Map<String, Any?> = try {
            if (includeCommon) {
                commonProperties(context) + (properties ?: emptyMap())
            } else {
                properties ?: emptyMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build common properties for $eventName", e)
            properties ?: emptyMap()
        }

        Log.d(TAG, "trackEvent: $eventName props=$merged")

        try {
            val event = BaseEvent().apply {
                this.eventType = eventName
                if (merged.isNotEmpty()) {
                    this.eventProperties = merged.toMutableMap()
                }
            }
            UnshortApplication.amplitude.track(event)
            Log.d(TAG, "Event tracked successfully: $eventName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to track event: $eventName", e)
        }
    }

    /**
     * 모든 이벤트에 자동으로 머지되는 컨텍스트.
     * 분석 시 사용자 상태와 시점을 빠르게 필터링하기 위한 필드.
     */
    private fun commonProperties(context: Context): Map<String, Any?> {
        val prefs = PreferencesManager(context)
        val now = LocalDateTime.now()
        return mapOf(
            "is_premium" to safeOrNull { PremiumManager.isPremium() },
            "is_sleep_mode" to safeOrNull { prefs.isSleepTime() },
            "is_blocking_enabled" to safeOrNull { prefs.isBlockingEnabled },
            "hour_of_day" to now.hour,
            "day_of_week" to now.dayOfWeek.name,
            "daily_unblock_remaining" to safeOrNull { DailyUnblockQuotaManager.getRemainingQuota(context) },
            "daily_unblock_total_today" to safeOrNull { DailyUnblockQuotaManager.getTotalUnblockToday(context) },
            "daily_ad_watched_today" to safeOrNull { DailyUnblockQuotaManager.getTotalAdsWatchedToday(context) }
        )
    }

    private inline fun <T> safeOrNull(block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    // ============= User Properties =============

    /**
     * 앱 시작 시 1회 호출.
     * 정적/장기 user property를 일괄 초기화.
     */
    fun initializeUserProperties(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            val identify = Identify()
                .set("app_version", BuildConfig.VERSION_NAME)
                .set("app_version_code", BuildConfig.VERSION_CODE)
                .set("locale", Locale.getDefault().language)
                .set("days_since_install", daysSinceInstall(prefs))
                .set("is_youtube_blocking_enabled", prefs.isAppBlockingEnabled(YOUTUBE_PACKAGE))
                .set("is_instagram_blocking_enabled", prefs.isAppBlockingEnabled(INSTAGRAM_PACKAGE))
                .set("daily_watch_limit_minutes", prefs.dailyLimitMinutes)
                .set("is_daily_limit_enabled", prefs.isDailyLimitEnabled)
                .set("is_sleep_mode_enabled", prefs.isSleepModeEnabled)
                .set("total_unblocks_lifetime", prefs.totalUnblocksLifetime)
                .set("has_completed_onboarding", prefs.hasCompletedOnboarding)

            // Remote Config 값은 nullable이라 별도로 처리
            remoteConfigInt(AppConstants.RC_UNBLOCK_QUOTA_DAILY_LIMIT)?.let {
                identify.set("daily_unblock_limit", it)
            }
            remoteConfigInt(AppConstants.RC_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT)?.let {
                identify.set("daily_unblock_ad_recharge_amount", it)
            }

            UnshortApplication.amplitude.identify(identify)
            Log.d(TAG, "User properties initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize user properties", e)
        }
    }

    /**
     * 권한 상태를 User Property로 설정
     */
    fun updatePermissionUserProperties(context: Context) {
        try {
            val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
            val hasOverlay = PermissionUtils.canDrawOverlays(context)
            val hasAllPermissions = PermissionUtils.hasAllPermissions(context)
            val manufacturer = PermissionUtils.getManufacturer()

            val identify = Identify()
                .set("has_accessibility_permission", hasAccessibility)
                .set("has_overlay_permission", hasOverlay)
                .set("has_all_permissions", hasAllPermissions)
                .set("device_manufacturer", manufacturer)

            UnshortApplication.amplitude.identify(identify)

            Log.d(TAG, "User properties updated - Accessibility: $hasAccessibility, Overlay: $hasOverlay")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update permission user properties", e)
        }
    }

    /**
     * 프리미엄 상태를 User Property로 설정
     */
    fun updatePremiumUserProperties(context: Context) {
        try {
            val isPremium = PremiumManager.isPremium()
            val premiumType = when {
                !isPremium -> "none"
                PremiumManager.isLifetimePremium() -> "lifetime"
                else -> "google_play"
            }

            val identify = Identify()
                .set("is_premium", isPremium)
                .set("premium_type", premiumType)

            UnshortApplication.amplitude.identify(identify)

            Log.d(TAG, "Premium user properties updated - isPremium: $isPremium, type: $premiumType")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update premium user properties", e)
        }
    }

    /**
     * 앱별 차단 상태 변경 시 호출
     */
    fun updateAppBlockingUserProperties(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            val identify = Identify()
                .set("is_youtube_blocking_enabled", prefs.isAppBlockingEnabled(YOUTUBE_PACKAGE))
                .set("is_instagram_blocking_enabled", prefs.isAppBlockingEnabled(INSTAGRAM_PACKAGE))
            UnshortApplication.amplitude.identify(identify)
            Log.d(TAG, "App blocking user properties updated")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update app blocking user properties", e)
        }
    }

    /**
     * Daily Limit 설정 변경 시 호출
     */
    fun updateDailyLimitUserProperties(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            val identify = Identify()
                .set("daily_watch_limit_minutes", prefs.dailyLimitMinutes)
                .set("is_daily_limit_enabled", prefs.isDailyLimitEnabled)
            UnshortApplication.amplitude.identify(identify)
            Log.d(TAG, "Daily limit user properties updated")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update daily limit user properties", e)
        }
    }

    /**
     * Sleep Mode 설정 변경 시 호출
     */
    fun updateSleepModeUserProperties(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            val identify = Identify()
                .set("is_sleep_mode_enabled", prefs.isSleepModeEnabled)
            UnshortApplication.amplitude.identify(identify)
            Log.d(TAG, "Sleep mode user properties updated")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update sleep mode user properties", e)
        }
    }

    /**
     * 즉시해제 누적 카운터 +1 후 user property 갱신
     */
    fun incrementUnblockLifetimeAndUpdate(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            prefs.incrementTotalUnblocksLifetime()
            val identify = Identify().set("total_unblocks_lifetime", prefs.totalUnblocksLifetime)
            UnshortApplication.amplitude.identify(identify)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment unblock lifetime", e)
        }
    }

    /**
     * 온보딩 완료 시 호출
     */
    fun markOnboardingCompleted(context: Context) {
        try {
            val prefs = PreferencesManager(context)
            if (!prefs.hasCompletedOnboarding) {
                prefs.hasCompletedOnboarding = true
                val identify = Identify().set("has_completed_onboarding", true)
                UnshortApplication.amplitude.identify(identify)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark onboarding completed", e)
        }
    }

    // ============= Helpers =============

    /**
     * 패키지명을 정규화된 source_app 값으로 변환
     */
    fun sourceAppOf(packageName: String?): String = when (packageName) {
        YOUTUBE_PACKAGE -> "youtube"
        INSTAGRAM_PACKAGE -> "instagram"
        null, "" -> "unknown"
        else -> packageName
    }

    private fun daysSinceInstall(prefs: PreferencesManager): Long {
        val installedAt = prefs.appInstalledAt
        if (installedAt <= 0L) return 0L
        val installedDate = java.time.Instant.ofEpochMilli(installedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return ChronoUnit.DAYS.between(installedDate, LocalDate.now()).coerceAtLeast(0L)
    }

    private fun remoteConfigInt(key: String): Int? = try {
        UnshortApplication.remoteConfig.getLong(key).toInt()
    } catch (e: Exception) {
        null
    }
}
