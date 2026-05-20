package com.muuu.unshort

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.muuu.ad.MuuuAdManagner
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.receiver.DailyUnblockQuotaResetReceiver
import com.muuu.unshort.R
import java.time.LocalDate

class UnshortApplication : Application() {

    companion object {
        lateinit var amplitude: Amplitude
            private set
        lateinit var analytics: FirebaseAnalytics
            private set
        lateinit var crashlytics: FirebaseCrashlytics
            private set
        lateinit var remoteConfig: FirebaseRemoteConfig
            private set
        private const val TAG = "UnshortApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        // Firebase Analytics 초기화
        analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)

        // Firebase Crashlytics 초기화
        crashlytics = Firebase.crashlytics
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Firebase Remote Config 초기화
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Remote Config 기본값 설정
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Remote Config fetch
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase Remote Config fetched and activated")
                } else {
                    Log.w(TAG, "Firebase Remote Config fetch failed", task.exception)
                }
            }

        Log.d(TAG, "Firebase initialized - Analytics, Crashlytics, Remote Config")

        // PremiumManager 초기화
        PremiumManager.initialize(this)

        // Muuu Ad SDK 초기화
        MuuuAdManagner.init(
            application = this,
            debugLogEnabled = BuildConfig.DEBUG
        )

        // Amplitude 초기화
        amplitude = Amplitude(
            Configuration(
                apiKey = BuildConfig.AMPLITUDE_API_KEY,
                context = applicationContext,
                flushQueueSize = if(BuildConfig.DEBUG) 1 else 20,
                flushIntervalMillis = 50000,
            )
        )

        // Android ID를 Amplitude user ID로 설정
        val androidId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        amplitude.setUserId(androidId)

        // 정적/장기 User Property 초기화
        AnalyticsManager.initializeUserProperties(applicationContext)

        // 권한 상태를 User Property로 설정
        AnalyticsManager.updatePermissionUserProperties(applicationContext)

        // 프리미엄 상태를 User Property로 설정
        AnalyticsManager.updatePremiumUserProperties(applicationContext)

        Log.d(TAG, "Muuu Ad SDK initialized with debug mode: ${BuildConfig.DEBUG}")
        Log.d(TAG, "Amplitude initialized - API Key: ${BuildConfig.AMPLITUDE_API_KEY.take(8)}...")
        Log.d(TAG, "Debug mode: ${BuildConfig.DEBUG}")

        // FeedBlockService 컴포넌트가 어떤 이유로 DISABLED 되어 있으면 항상 ENABLED로 복구.
        // 매니페스트는 enabled="true"이지만, setComponentEnabledSetting(DISABLED)이 한 번
        // 호출되면 시스템이 영구 저장하여 재설치 후에도 잔존하는 케이스가 있다. DISABLED
        // 상태에서는 시스템이 service를 "not installed"로 판정 → 시스템 접근성 UI에서 사라지고
        // bind 거부됨. prefs와 무관하게 항상 ENABLED를 보장하고, 실제 동작 여부는
        // FeedBlockService.onAccessibilityEvent에서 prefsManager.isBetaEnabled로 통제한다.
        ensureFeedBlockServiceEnabled()

        // 일일 즉시 해제 한도 시스템 초기화
        initializeDailyUnblockQuota()

        // 앱 Foreground/Background 감지
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                // 앱이 foreground로 진입할 때마다 프리미엄 상태 동기화
                PremiumManager.syncPremiumStatus()
                Log.d(TAG, "App moved to foreground - syncing premium status")
            }
        })
    }

    /**
     * 일일 즉시 해제 한도 시스템 초기화
     *
     * - install date 최초 1회 기록 (신규 사용자 그레이스 기간 판정 기준)
     * - 자정 자동 리셋 알람 등록 (Lazy 보정으로 이중 보강)
     */
    private fun initializeDailyUnblockQuota() {
        try {
            val prefsManager = PreferencesManager(this)
            if (prefsManager.dailyUnblockQuotaInstallDate.isBlank()) {
                prefsManager.dailyUnblockQuotaInstallDate = LocalDate.now().toString()
                Log.d(TAG, "Daily unblock quota install date set: ${prefsManager.dailyUnblockQuotaInstallDate}")
            }
            DailyUnblockQuotaResetReceiver.scheduleReset(this)
            Log.d(TAG, "Daily unblock quota reset alarm scheduled")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize daily unblock quota", e)
        }
    }

    private fun ensureFeedBlockServiceEnabled() {
        try {
            val component = ComponentName(
                this,
                com.muuu.unshort.feedblock.FeedBlockService::class.java
            )
            val current = packageManager.getComponentEnabledSetting(component)
            if (current == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                current == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                return
            }
            packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "FeedBlockService component re-enabled (was state=$current)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enable FeedBlockService component", e)
        }
    }
}
