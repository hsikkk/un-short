package com.muuu.unshort.service.blocking

import android.view.accessibility.AccessibilityNodeInfo
import java.net.URI

/** Reads only a supported browser's native, non-editing address bar. Never stores URLs. */
object BrowserYoutubeDetector {
    val addressIds = mapOf(
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.android.chrome" to "com.android.chrome:id/url_bar"
    )

    // Separate from native per-app toggles; controlled by the browser option.
    fun configFor(packageName: String): AppBlockingConfig? {
        if (packageName !in addressIds) return null
        return AppBlockingRegistry.YOUTUBE.copy(
            packageName = packageName,
            displayName = "YouTube (Browser)",
            shortName = "YouTube (Web)",
            viewIdDetectors = emptyList(),
            textDetectors = emptyList(),
            controlsMedia = false,
            pauseIndicator = null,
            exitAction = ExitAction.HOME
        )
    }

    fun isYoutubeAddress(address: String): Boolean {
        val value = address.trim().trimStart('\u200e', '\u200f')
        if (value.isEmpty() || value.any { it.isWhitespace() }) return false
        return try {
            val uri = URI(if ("://" in value) value else "https://$value")
            val host = uri.host?.lowercase() ?: return false
            uri.scheme.lowercase() in setOf("http", "https") && uri.rawUserInfo == null &&
                (host == "youtube.com" || host.endsWith(".youtube.com"))
        } catch (_: Exception) {
            false
        }
    }

    enum class Page { YOUTUBE, OTHER, UNKNOWN }

    fun isYoutubePage(root: AccessibilityNodeInfo, packageName: String): Boolean =
        classifyPage(root, packageName) == Page.YOUTUBE

    fun classifyPage(root: AccessibilityNodeInfo, packageName: String): Page {
        val addressId = addressIds[packageName] ?: return Page.UNKNOWN
        if (root.packageName?.toString() != packageName) return Page.UNKNOWN
        var visited = 0
        val addresses = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (++visited > 500 || depth > 40 || node.className == "android.webkit.WebView") return
            if (node.viewIdResourceName == addressId && node.packageName?.toString() == packageName &&
                node.isVisibleToUser && !node.isFocused) {
                addresses.add(node.text?.toString().orEmpty())
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try { visit(child, depth + 1) } finally { child.recycle() }
                if (visited > 500) break
            }
        }
        visit(root, 0)
        if (visited > 500 || addresses.size != 1) return Page.UNKNOWN
        val address = addresses.single()
        if (isYoutubeAddress(address)) return Page.YOUTUBE
        val value = address.trim().trimStart('\u200e', '\u200f')
        return try {
            val uri = URI(if ("://" in value) value else "https://$value")
            if (uri.host != null && uri.rawUserInfo == null &&
                uri.scheme.lowercase() in setOf("http", "https")) Page.OTHER else Page.UNKNOWN
        } catch (_: Exception) { Page.UNKNOWN }
    }
}
