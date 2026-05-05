package com.muuu.unshort.feedblock.detection

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.muuu.unshort.feedblock.FeedTarget

/**
 * 피드 화면 감지 엔진
 *
 * 판단 순서:
 * 1. 제외 컨테이너(릴스 뷰어 등) 가시 시 false
 * 2. 시그니처 (ViewID 또는 content-desc) 가시 + 홈탭 selected 시 true
 * 3. 폴백: 시그니처 + 피드 컨테이너 가시 시 true
 */
class FeedDetectionEngine {

    fun detectFeed(rootNode: AccessibilityNodeInfo, target: FeedTarget): Boolean {
        if (anyVisible(rootNode, target.excludeViewIds)) {
            Log.d(TAG, "[${target.displayName}] excluded container visible -> not feed")
            return false
        }

        val signatureRequired = target.feedItemSignatureViewIds.isNotEmpty() ||
            target.feedItemSignatureContentDescriptions.isNotEmpty()
        val feedSignatureVisible = if (signatureRequired) {
            anyVisible(rootNode, target.feedItemSignatureViewIds) ||
                anyContentDescVisible(rootNode, target.feedItemSignatureContentDescriptions)
        } else {
            true  // 시그니처 미정의 시 검사 스킵 (YouTube 같은 obfuscated)
        }

        val homeTabSelected = isHomeTabSelectedByViewId(rootNode, target.homeTabViewId) ||
            isHomeTabSelectedByContentDesc(rootNode, target.homeTabContentDescription)

        val feedContainerVisible = anyVisible(rootNode, target.feedContainerViewIds)

        Log.d(
            TAG,
            "[${target.displayName}] sig=$feedSignatureVisible, " +
                "homeSel=$homeTabSelected, container=$feedContainerVisible"
        )

        if (homeTabSelected && feedSignatureVisible) return true
        if (feedContainerVisible && feedSignatureVisible) return true
        return false
    }

    private fun anyVisible(node: AccessibilityNodeInfo, viewIds: List<String>): Boolean {
        if (viewIds.isEmpty()) return false
        for (viewId in viewIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                val visible = nodes.any { it.isVisibleToUser }
                nodes.forEach { @Suppress("DEPRECATION") it.recycle() }
                if (visible) return true
            }
        }
        // findAccessibilityNodeInfosByViewId는 일부 환경에서 Compose testTag(패키지 prefix
        // 없는 ID)를 매칭하지 못한다. viewIdResourceName 직접 비교로 fallback.
        return findVisibleByViewIdMatch(node, viewIds, 0)
    }

    private fun findVisibleByViewIdMatch(
        node: AccessibilityNodeInfo,
        viewIds: List<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) return false
        val vid = node.viewIdResourceName
        if (!vid.isNullOrEmpty() && node.isVisibleToUser) {
            for (target in viewIds) {
                if (vid == target) return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findVisibleByViewIdMatch(child, viewIds, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
    }

    private fun anyContentDescVisible(
        node: AccessibilityNodeInfo,
        patterns: List<String>
    ): Boolean {
        if (patterns.isEmpty()) return false
        return findVisibleByContentDescAny(node, patterns, 0)
    }

    private fun findVisibleByContentDescAny(
        node: AccessibilityNodeInfo,
        patterns: List<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) return false

        val desc = node.contentDescription?.toString()
        if (desc != null && node.isVisibleToUser) {
            for (p in patterns) {
                if (desc.contains(p, ignoreCase = true)) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findVisibleByContentDescAny(child, patterns, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
    }

    private fun isHomeTabSelectedByViewId(node: AccessibilityNodeInfo, homeTabViewId: String): Boolean {
        if (homeTabViewId.isEmpty()) return false
        val nodes = node.findAccessibilityNodeInfosByViewId(homeTabViewId)
        if (!nodes.isNullOrEmpty()) {
            val selected = nodes.any { it.isSelected }
            nodes.forEach { @Suppress("DEPRECATION") it.recycle() }
            if (selected) return true
        }
        return findSelectedByViewIdMatch(node, homeTabViewId, 0)
    }

    private fun findSelectedByViewIdMatch(
        node: AccessibilityNodeInfo,
        targetViewId: String,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) return false
        if (node.viewIdResourceName == targetViewId && node.isSelected) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findSelectedByViewIdMatch(child, targetViewId, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
    }

    private fun isHomeTabSelectedByContentDesc(
        node: AccessibilityNodeInfo,
        pattern: String?
    ): Boolean {
        if (pattern.isNullOrEmpty()) return false
        return findSelectedByContentDesc(node, pattern, 0)
    }

    private fun findSelectedByContentDesc(
        node: AccessibilityNodeInfo,
        pattern: String,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) return false

        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains(pattern, ignoreCase = true) && node.isSelected) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findSelectedByContentDesc(child, pattern, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
    }

    private fun dumpVisibleViewIds(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 30) return
        val vid = node.viewIdResourceName
        val cls = node.className
        if (!vid.isNullOrEmpty() && node.isVisibleToUser) {
            Log.d(TAG, "  ".repeat(depth) + "vid=$vid cls=$cls")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                dumpVisibleViewIds(child, depth + 1)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    companion object {
        private const val TAG = "FeedDetectionEngine"
        private const val MAX_DEPTH = 30
    }
}
