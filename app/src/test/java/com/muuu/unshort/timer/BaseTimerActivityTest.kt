package com.muuu.unshort.timer

import android.app.Application
import android.os.Looper
import android.os.CountDownTimer
import com.muuu.unshort.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BaseTimerActivityTest {
    class TimerActivity : BaseTimerActivity() {
        var completions = 0
        override fun getLayoutResourceId() = R.layout.activity_timer
        override fun provideTimerDuration() = 3
        override fun shouldShowAd() = false
        override fun onTimerCompleted() { completions++ }
        override fun onSkipClicked() { finish() }
        fun flip() { isFlipped = true; onPhoneFlipped() }
        fun resetSession() = resetTimerForNewSession()
        fun timer(): CountDownTimer = BaseTimerActivity::class.java
            .getDeclaredField("countDownTimer").apply { isAccessible = true }
            .get(this) as CountDownTimer
    }

    @Test fun pausesInBackgroundAndResumesWithRemainingTime() {
        val controller = Robolectric.buildActivity(TimerActivity::class.java).create().start().resume()
        val activity = controller.get()
        activity.flip()
        val original = activity.timer()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        controller.pause().stop()
        assertFalse(shadowOf(original).hasStarted())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertEquals(0, activity.completions)
        controller.start().resume()
        activity.flip()
        val resumed = activity.timer()
        assertNotSame(original, resumed)
        assertTrue(shadowOf(resumed).hasStarted())
        assertEquals(2000L, shadowOf(resumed).millisInFuture)
        // Robolectric's CountDownTimer shadow requires explicit tick/finish dispatch.
        shadowOf(resumed).invokeFinish()
        assertEquals(1, activity.completions)
        controller.pause().stop().start().resume()
        activity.flip()
        assertSame(resumed, activity.timer()) // Completed timers must not restart.
        assertEquals(1, activity.completions)
        controller.pause().stop().destroy()
    }

    @Test fun newSessionDoesNotReuseCompletedTimer() {
        val controller = Robolectric.buildActivity(TimerActivity::class.java).create().start().resume()
        val activity = controller.get()
        activity.flip()
        val original = activity.timer()
        shadowOf(original).invokeFinish()
        controller.pause()
        activity.resetSession()
        controller.resume()
        activity.flip()
        val next = activity.timer()
        assertNotSame(original, next)
        assertEquals(3000L, shadowOf(next).millisInFuture)
        assertTrue(shadowOf(next).hasStarted())
        assertEquals(1, activity.completions)
        shadowOf(next).invokeFinish()
        assertEquals(2, activity.completions)
        controller.pause().stop().destroy()
    }
}
