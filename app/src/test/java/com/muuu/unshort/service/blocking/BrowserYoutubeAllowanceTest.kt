package com.muuu.unshort.service.blocking

import android.app.Application
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BrowserYoutubeAllowanceTest {
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("allowance-test", Context.MODE_PRIVATE)
    private var now = 10000L
    private var boot = 4
    private fun allowance() = BrowserYoutubeAllowance(prefs, { now }, { boot })

    @Test fun expiresAtFiveMinutesWithoutExtendingOnReads() {
        val window = allowance()
        assertEquals(0L, window.remainingMs())
        window.grant()
        assertEquals(300000L, window.remainingMs())
        now += 299999
        assertEquals(1L, window.remainingMs())
        now++
        assertEquals(0L, window.remainingMs())
        now += 5000
        assertEquals(0L, window.remainingMs())
    }

    @Test fun browsersAndServiceRestartsShareTheSameDeadline() {
        allowance().grant()
        now += 60000
        assertEquals(240000L, allowance().remainingMs())
        now += 100000 // time away or asleep still counts
        assertEquals(140000L, allowance().remainingMs())
    }

    @Test fun rebootInvalidatesEvenWhenElapsedTimeExceedsOldUptime() {
        allowance().grant()
        boot++
        now += 1000
        assertEquals(0L, allowance().remainingMs())
    }

    @Test fun disablingClearsAllowanceAndExplicitApprovalStartsNewFiveMinutes() {
        val window = allowance()
        window.grant()
        window.clear()
        assertEquals(0L, window.remainingMs())
        now += 60000
        window.grant()
        assertEquals(300000L, window.remainingMs())
    }
}
