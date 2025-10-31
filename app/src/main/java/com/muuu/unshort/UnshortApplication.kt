package com.muuu.unshort

import android.app.Application
import android.util.Log
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration

class UnshortApplication : Application() {

    companion object {
        lateinit var amplitude: Amplitude
            private set
        private const val TAG = "UnshortApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Amplitude 초기화
        amplitude = Amplitude(
            Configuration(
                apiKey = BuildConfig.AMPLITUDE_API_KEY,
                context = applicationContext
            )
        )

        Log.d(TAG, "Amplitude initialized - API Key: ${BuildConfig.AMPLITUDE_API_KEY.take(8)}...")
        Log.d(TAG, "Debug mode: ${BuildConfig.DEBUG}")
    }
}
