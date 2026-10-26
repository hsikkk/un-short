package com.muuu.unshort.service.blocking

import android.app.Application
import android.view.accessibility.AccessibilityNodeInfo
import com.muuu.unshort.prefs.PreferencesManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BrowserYoutubeNodeTest {
    private val browser = "com.sec.android.app.sbrowser"
    private fun address() = AccessibilityNodeInfo.obtain().apply {
        packageName = browser
        className = "android.widget.EditText"
        viewIdResourceName = BrowserYoutubeDetector.addressIds.getValue(browser)
        text = "\u200em.youtube.com"
        isVisibleToUser = true
    }

    @Test fun optionDefaultsOffAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = PreferencesManager(context)
        assertFalse(prefs.isBrowserYoutubeBlockingEnabled)
        prefs.isBrowserYoutubeBlockingEnabled = true
        assertTrue(PreferencesManager(context).isBrowserYoutubeBlockingEnabled)
    }

    @Test fun rejectsEditingHiddenAndOtherAppAddresses() {
        val node = address()
        assertTrue(BrowserYoutubeDetector.isYoutubePage(node, browser))
        node.isFocused = true
        assertFalse(BrowserYoutubeDetector.isYoutubePage(node, browser))
        node.isFocused = false
        node.isVisibleToUser = false
        assertFalse(BrowserYoutubeDetector.isYoutubePage(node, browser))
        node.isVisibleToUser = true
        node.packageName = "other.app"
        assertFalse(BrowserYoutubeDetector.isYoutubePage(node, browser))
    }

    @Test fun rejectsWebContentImpersonatingAddressAndAmbiguousWindows() {
        val web = address().apply { className = "android.webkit.WebView" }
        assertFalse(BrowserYoutubeDetector.isYoutubePage(web, browser))
        val root = AccessibilityNodeInfo.obtain().apply { packageName = browser }
        shadowOf(root).addChild(address())
        shadowOf(root).addChild(address())
        assertFalse(BrowserYoutubeDetector.isYoutubePage(root, browser))
    }

    @Test fun unknownAddressDoesNotMeanLeavingYoutube() {
        val node = address()
        node.isFocused = true
        assertEquals(BrowserYoutubeDetector.Page.UNKNOWN, BrowserYoutubeDetector.classifyPage(node, browser))
        node.isFocused = false
        node.text = "example.com"
        assertEquals(BrowserYoutubeDetector.Page.OTHER, BrowserYoutubeDetector.classifyPage(node, browser))
    }

    @Test fun browserUsesTimerAndConfirmationEvenWhenFirstShortIsAllowed() {
        val context = RuntimeEnvironment.getApplication()
        PreferencesManager(context).isBlockScrolledOnly = true
        val state = SessionStateManager(context, isBlockingEnabled = { true })
        state.handleEvent(SessionEvent.EnterShorts, browser)
        assertEquals(BlockingStage.NEED_TIMER, state.getCurrentState(browser).blockingStage)
        val id = state.getCurrentState(browser).sessionId!!
        state.handleEvent(SessionEvent.ActivityStarted(ActivityType.TIMER, id), browser)
        state.handleEvent(SessionEvent.TimerCompleted(id), browser)
        assertEquals(BlockingStage.NEED_CONFIRMATION, state.getCurrentState(browser).blockingStage)
        state.handleEvent(SessionEvent.WatchConfirmed, browser)
        state.handleEvent(SessionEvent.ActivityDestroyed(ActivityType.OVERLAY), browser)
        assertTrue(state.isAllowed(browser))
        state.handleEvent(SessionEvent.ExitShorts, browser)
        state.handleEvent(SessionEvent.EnterShorts, browser)
        assertEquals(BlockingStage.NEED_TIMER, state.getCurrentState(browser).blockingStage)
        state.handleEvent(SessionEvent.EnterShorts, "com.google.android.youtube")
        assertEquals(BlockingStage.WATCHING, state.getCurrentState("com.google.android.youtube").blockingStage)
    }
}
