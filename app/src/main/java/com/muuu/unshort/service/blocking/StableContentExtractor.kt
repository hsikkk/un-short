package com.muuu.unshort.service.blocking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 안정적인 콘텐츠 추출기 (YouTube Shorts 전용)
 *
 * UI 요소를 완전히 배제하고, 제목/채널명/설명 등 핵심 콘텐츠만 추출
 */
class StableContentExtractor {

    private val TAG = "StableContentExtractor"

    /**
     * YouTube Shorts의 안정적인 콘텐츠 추출
     *
     * @param rootNode 루트 노드
     * @return 핵심 콘텐츠 문자열
     */
    fun extractYouTubeContent(rootNode: AccessibilityNodeInfo): String {
        val contentBuilder = StringBuilder()

        // 1. 컨테이너 찾기
        val container = rootNode.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/reel_player_page_container"
        )?.firstOrNull() ?: rootNode

        // 2. 핵심 View ID 타겟팅
        val targetViewIds = listOf(
            "com.google.android.youtube:id/reel_player_title",        // 제목
            "com.google.android.youtube:id/reel_player_channel_name", // 채널명
            "com.google.android.youtube:id/reel_player_description",  // 설명
            "com.google.android.youtube:id/video_title",              // 대체 제목
            "com.google.android.youtube:id/channel_name"              // 대체 채널명
        )

        // 3. 각 View ID에서 텍스트 추출
        for (viewId in targetViewIds) {
            val nodes = container.findAccessibilityNodeInfosByViewId(viewId)
            nodes?.forEach { node ->
                node.text?.toString()?.let { text ->
                    if (text.isNotEmpty() && text.length > 2) {
                        contentBuilder.append(text).append("|")
                        Log.d(TAG, "Extracted from $viewId: $text")
                    }
                }
                node.recycle()
            }
        }

        // 4. View ID로 찾지 못한 경우 패턴 기반 추출 (폴백)
        if (contentBuilder.isEmpty()) {
            Log.d(TAG, "No content from View IDs, trying pattern-based extraction")
            extractByPattern(container, contentBuilder)
        }

        val result = contentBuilder.toString()
        Log.d(TAG, "Final stable content (length=${result.length}): $result")
        return result
    }

    /**
     * 패턴 기반 콘텐츠 추출 (View ID로 찾지 못한 경우 폴백)
     */
    private fun extractByPattern(node: AccessibilityNodeInfo, contentBuilder: StringBuilder, depth: Int = 0) {
        if (depth > 5) return  // 최대 깊이 제한

        val viewId = node.viewIdResourceName ?: ""

        // 제외할 View ID 패턴 (UI 요소)
        val excludedPatterns = listOf(
            "comment", "like", "share", "button", "progress",
            "time", "duration", "engagement", "seek", "control"
        )

        // 제외 대상이면 스킵
        if (excludedPatterns.any { viewId.contains(it, ignoreCase = true) }) {
            return
        }

        // Bounds 체크 - 우측 사이드바(x >= 1200) 제외
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.left >= 1200) {
            return  // 우측 영역(좋아요, 댓글 등) 제외
        }

        // 텍스트 추출
        node.text?.toString()?.let { text ->
            if (text.length > 2 && !text.matches(Regex("^[\\d:.,KMB만천억\\s]+$"))) {
                // 숫자/시간 패턴 제외, 최소 길이 완화 (10 → 2)
                contentBuilder.append(text).append("|")
                Log.d(TAG, "Extracted by pattern: $text (bounds: ${bounds.left})")
            }
        }

        // ContentDescription도 체크 (채널명, 제목 등)
        node.contentDescription?.toString()?.let { desc ->
            // @ 시작하는 채널명 추출
            if (desc.startsWith("@") && desc.length > 2) {
                contentBuilder.append(desc).append("|")
                Log.d(TAG, "Extracted channel from desc: $desc")
            }
        }

        // 자식 노드 재귀
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                extractByPattern(child, contentBuilder, depth + 1)
                child.recycle()
            }
        }
    }
}
