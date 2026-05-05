package com.muuu.unshort.feedblock.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * 피드 차단 (베타) 전용 설정 관리
 */
class FeedBlockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isBetaEnabled: Boolean
        get() = prefs.getBoolean(KEY_BETA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BETA_ENABLED, value).apply()

    var isInstagramEnabled: Boolean
        get() = prefs.getBoolean(KEY_INSTAGRAM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTAGRAM_ENABLED, value).apply()

    var isYoutubeEnabled: Boolean
        get() = prefs.getBoolean(KEY_YOUTUBE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_YOUTUBE_ENABLED, value).apply()

    var isThreadsEnabled: Boolean
        get() = prefs.getBoolean(KEY_THREADS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_THREADS_ENABLED, value).apply()

    var isFacebookEnabled: Boolean
        get() = prefs.getBoolean(KEY_FACEBOOK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FACEBOOK_ENABLED, value).apply()

    fun isAppEnabled(packageName: String): Boolean = when (packageName) {
        "com.instagram.android" -> isInstagramEnabled
        "com.google.android.youtube" -> isYoutubeEnabled
        "com.instagram.barcelona" -> isThreadsEnabled
        "com.facebook.katana" -> isFacebookEnabled
        else -> false
    }

    companion object {
        const val PREF_NAME = "feedblock_prefs"
        private const val KEY_BETA_ENABLED = "beta_enabled"
        private const val KEY_INSTAGRAM_ENABLED = "instagram_enabled"
        private const val KEY_YOUTUBE_ENABLED = "youtube_enabled"
        private const val KEY_THREADS_ENABLED = "threads_enabled"
        private const val KEY_FACEBOOK_ENABLED = "facebook_enabled"
    }
}
