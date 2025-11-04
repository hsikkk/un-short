package com.muuu.unshort

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.muuu.ad.MuuuAdManagner
import com.muuu.unshort.premium.PremiumManager

class UnshortApplication : Application() {

    companion object {
        lateinit var amplitude: Amplitude
            private set
        private const val TAG = "UnshortApplication"
    }

    override fun onCreate() {
        super.onCreate()

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

        Log.d(TAG, "Muuu Ad SDK initialized with debug mode: ${BuildConfig.DEBUG}")
        Log.d(TAG, "Amplitude initialized - API Key: ${BuildConfig.AMPLITUDE_API_KEY.take(8)}...")
        Log.d(TAG, "Debug mode: ${BuildConfig.DEBUG}")

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
}
