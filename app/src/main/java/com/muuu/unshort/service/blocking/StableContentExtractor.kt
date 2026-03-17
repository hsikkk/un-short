package com.muuu.unshort.service.blocking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 콘텐츠 핑거프린트 (제목/채널/설명을 개별 해시로 분리)
 *
 * UI 변경에도 동일 콘텐츠를 인식하기 위한 구조
 */
data class ContentFingerprint(
    val titleHash: Int,
    val channelHash: Int,
    val descriptionHash: Int
) {
    /**
     * 핑거프린트가 유효한지 확인
     *
     * 재생 완료/일시정지 시 UI 요소가 사라져 빈 문자열(해시=0)이 추출되는 경우를 방지
     * 최소 2개 이상의 필드가 유효해야 함 (제목+채널 또는 제목+설명)
     */
    fun isValid(): Boolean {
        var validCount = 0
        if (titleHash != 0) validCount++
        if (channelHash != 0) validCount++
        if (descriptionHash != 0) validCount++
        return validCount >= 2
    }

    /**
     * 다른 핑거프린트와의 유사도 계산
     *
     * @return 0.0 ~ 1.0 (0: 완전히 다름, 1.0: 완전히 같음)
     */
    fun similarityWith(other: ContentFingerprint): Float {
        var matches = 0
        if (titleHash == other.titleHash) matches++
        if (channelHash == other.channelHash) matches++
        if (descriptionHash == other.descriptionHash) matches++
        return matches / 3f
    }

    /**
     * 2/3 이상 일치하는지 확인 (동일 콘텐츠 판별 기준)
     */
    fun isSimilarTo(other: ContentFingerprint): Boolean {
        return similarityWith(other) >= 0.67f  // 2/3 이상
    }
}

/**
 * 안정적인 콘텐츠 추출기 (YouTube Shorts 전용)
 *
 * UI 요소를 완전히 배제하고, 제목/채널명/설명 등 핵심 콘텐츠만 추출
 */
class StableContentExtractor {

    private val TAG = "StableContentExtractor"

    /**
     * YouTube Shorts의 콘텐츠 핑거프린트 추출 (유사도 비교용)
     *
     * @param rootNode 루트 노드
     * @return 제목/채널/설명을 개별 해시로 가진 핑거프린트
     */
    fun extractYouTubeFingerprint(rootNode: AccessibilityNodeInfo): ContentFingerprint {
        val container = rootNode.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/reel_player_page_container"
        )?.firstOrNull() ?: rootNode

        // 1차: View ID 기반 추출 (기존 방식)
        var title = extractFromViewIds(container, listOf(
            "com.google.android.youtube:id/reel_player_title",
            "com.google.android.youtube:id/video_title"
        ))
        var channel = extractFromViewIds(container, listOf(
            "com.google.android.youtube:id/reel_player_channel_name",
            "com.google.android.youtube:id/channel_name"
        ))
        var description = extractFromViewIds(container, listOf(
            "com.google.android.youtube:id/reel_player_description"
        ))

        // 2차: View ID로 못 찾으면 contentDescription 패턴 기반 추출
        if (title.isEmpty() && channel.isEmpty()) {
            Log.d(TAG, "View ID extraction failed, falling back to contentDescription")
            val allDescs = mutableListOf<String>()
            collectContentDescriptions(container, allDescs, depth = 0, maxDepth = 8)
            Log.d(TAG, "Collected ${allDescs.size} contentDescriptions: $allDescs")

            // 채널: "구독합니다" 패턴에서 @채널명 추출
            for (desc in allDescs) {
                if (desc.contains("구독합니다") && desc.contains("@")) {
                    channel = extractChannelFromDesc(desc)
                    if (channel.isNotEmpty()) {
                        Log.d(TAG, "  [DESC] Channel: '$channel'")
                        break
                    }
                }
            }

            // 제목: UI 요소를 제외한 contentDescription 중 제목 후보 탐색
            for (desc in allDescs) {
                if (isContentTitle(desc)) {
                    title = desc
                    Log.d(TAG, "  [DESC] Title: '$title'")
                    break
                }
            }
        }

        val titleHash = title.trim().hashCode()
        val channelHash = channel.trim().hashCode()
        val descriptionHash = description.trim().hashCode()

        Log.d(TAG, "Fingerprint - title: '$title' (hash: $titleHash)")
        Log.d(TAG, "Fingerprint - channel: '$channel' (hash: $channelHash)")
        Log.d(TAG, "Fingerprint - desc: '$description' (hash: $descriptionHash)")

        return ContentFingerprint(titleHash, channelHash, descriptionHash)
    }

    /**
     * 노드 트리에서 모든 contentDescription을 수집
     */
    private fun collectContentDescriptions(
        node: AccessibilityNodeInfo,
        result: MutableList<String>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return

        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotEmpty()) {
                result.add(desc)
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectContentDescriptions(child, result, depth + 1, maxDepth)
                child.recycle()
            }
        }
    }

    /**
     * contentDescription이 제목인지 판별
     *
     * UI 요소(좋아요, 댓글, 공유 등)를 제외하고
     * 콘텐츠 제목으로 판별 가능한 패턴만 허용
     */
    private fun isContentTitle(desc: String): Boolean {
        if (desc.length <= 3) return false

        val uiPatterns = listOf(
            "좋아요", "싫어요", "댓글", "공유", "리믹스",
            "구독", "사운드", "동영상", "더보기", "라이브",
            "검색", "알림", "설정", "메뉴", "닫기", "뒤로",
            "탐색", "홈", "보관함", "Shorts", "shorts"
        )

        return uiPatterns.none { desc.contains(it, ignoreCase = true) }
    }

    /**
     * "구독합니다" contentDescription에서 "@채널명" 추출
     */
    private fun extractChannelFromDesc(desc: String): String {
        // "@XXX을(를) 구독합니다." 패턴
        val atIndex = desc.indexOf("@")
        if (atIndex >= 0) {
            val endPatterns = listOf("을(를)", "을 ", "를 ", " 구독")
            for (pattern in endPatterns) {
                val endIndex = desc.indexOf(pattern, atIndex)
                if (endIndex > atIndex) {
                    return desc.substring(atIndex, endIndex)
                }
            }
            // 패턴이 없으면 @ 이후 전체
            return desc.substring(atIndex).trim()
        }
        return ""
    }


    /**
     * 여러 View ID에서 텍스트 추출 (첫 번째로 찾은 것 반환)
     */
    private fun extractFromViewIds(container: AccessibilityNodeInfo, viewIds: List<String>): String {
        for (viewId in viewIds) {
            val nodes = container.findAccessibilityNodeInfosByViewId(viewId)
            nodes?.forEach { node ->
                val text = node.text?.toString() ?: ""
                node.recycle()
                if (text.isNotEmpty() && text.length > 2) {
                    return text
                }
            }
        }
        return ""
    }

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
