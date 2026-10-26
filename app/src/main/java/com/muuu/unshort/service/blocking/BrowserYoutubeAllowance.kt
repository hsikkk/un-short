package com.muuu.unshort.service.blocking

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import com.muuu.unshort.config.AppConstants

/** One five-minute window shared by supported browsers, including time spent away. */
class BrowserYoutubeAllowance(
    private val prefs: SharedPreferences,
    private val elapsed: () -> Long,
    private val bootCount: () -> Int
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE),
        { SystemClock.elapsedRealtime() },
        { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
    )

    companion object {
        const val DURATION_MS = 5 * 60 * 1000L
        private const val DEADLINE = "browser_youtube_allowance_deadline"
        private const val BOOT = "browser_youtube_allowance_boot"
    }

    fun grant() {
        prefs.edit().putLong(DEADLINE, elapsed() + DURATION_MS)
            .putInt(BOOT, bootCount()).apply()
    }

    fun remainingMs(): Long {
        if (prefs.getInt(BOOT, Int.MIN_VALUE) != bootCount()) return 0
        val remaining = prefs.getLong(DEADLINE, 0) - elapsed()
        return if (remaining in 1..DURATION_MS) remaining else 0
    }

    fun clear() {
        prefs.edit().remove(DEADLINE).remove(BOOT).apply()
    }
}
