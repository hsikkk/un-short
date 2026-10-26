package com.muuu.unshort.service.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserYoutubeDetectorTest {
    @Test fun recognizesWholeSiteIncludingCollapsedSamsungAddress() {
        listOf("\u200em.youtube.com", "youtube.com", "www.youtube.com/watch?v=123",
            "https://m.youtube.com/shorts/abc", "https://music.youtube.com/", "HTTPS://YOUTUBE.COM/")
            .forEach { assertTrue(it, BrowserYoutubeDetector.isYoutubeAddress(it)) }
    }

    @Test fun rejectsSearchesSpoofsAndOtherSchemes() {
        listOf("", "youtube", "youtube.com search", "youtube.com.evil.com", "notyoutube.com",
            "https://example.com/youtube.com", "https://youtube.com@example.com",
            "https://user@youtube.com", "javascript://youtube.com", "file://youtube.com",
            "https://[", "https://youtube.com\\@evil.com")
            .forEach { assertFalse(it, BrowserYoutubeDetector.isYoutubeAddress(it)) }
    }
}
