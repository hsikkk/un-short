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
     * 다른 핑거프린트와의 유사도 계산 (디버깅용)
     *
     * 단순 3등분 비율. 실제 동일 콘텐츠 판별은 isSimilarTo() 사용.
     *
     * @return 0.0 ~ 1.0
     */
    fun similarityWith(other: ContentFingerprint): Float {
        var matches = 0
        if (titleHash == other.titleHash) matches++
        if (channelHash == other.channelHash) matches++
        if (descriptionHash == other.descriptionHash) matches++
        return matches / 3f
    }

    /**
     * 동일 콘텐츠인지 판단 (동적 분모 기반)
     *
     * 두 핑거프린트 모두 추출에 성공한 필드(non-zero)만 분모에 포함.
     * 같은 영상이라도 title이 비결정적으로 변하는 케이스(YouTube ViewPager가
     * 인접 영상 metadata를 같이 트리에 둠)를 견디기 위해 임계값을 과반으로 완화.
     *
     * 규칙:
     * - 유효 필드 0개: 비교 불가 → false
     * - 유효 필드 1개: 그 한 필드라도 일치하면 동일 (보수적이지만 채널만이라도 같으면 같은 채널 영상 일부 허용)
     * - 유효 필드 2개: 1개 이상 일치 (50% 이상)
     * - 유효 필드 3개: 2개 이상 일치 (67% 이상)
     */
    fun isSimilarTo(other: ContentFingerprint): Boolean {
        val results = mutableListOf<Boolean>()
        if (titleHash != 0 && other.titleHash != 0) {
            results.add(titleHash == other.titleHash)
        }
        if (channelHash != 0 && other.channelHash != 0) {
            results.add(channelHash == other.channelHash)
        }
        if (descriptionHash != 0 && other.descriptionHash != 0) {
            results.add(descriptionHash == other.descriptionHash)
        }
        if (results.isEmpty()) return false

        val matches = results.count { it }
        // 과반 일치 기준: ceil(size/2)
        val required = (results.size + 1) / 2
        return matches >= required
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
            val allDescs = mutableListOf<DescWithBounds>()
            collectContentDescriptionsWithBounds(container, allDescs, depth = 0, maxDepth = 8)
            Log.d(TAG, "Collected ${allDescs.size} contentDescriptions")

            // 채널: "구독합니다" 패턴에서 @채널명 추출 (탐색 순서 무관)
            for (entry in allDescs) {
                val desc = entry.desc
                if (desc.contains("구독합니다") && desc.contains("@")) {
                    channel = extractChannelFromDesc(desc)
                    if (channel.isNotEmpty()) {
                        Log.d(TAG, "  [DESC] Channel: '$channel'")
                        break
                    }
                }
            }

            // 제목: YouTube ViewPager가 인접 영상 metadata를 같이 트리에 두는 경우가 있어
            // 트리 순회 순서가 비결정적. bounds.top 기준으로 정렬 후 첫 번째 유효 후보 채택.
            // 추가 정렬키로 left, desc 자체를 사용하여 동률 시에도 결정론 보장.
            val titleCandidates = allDescs
                .filter { isContentTitle(it.desc) }
                .sortedWith(compareBy({ it.boundsTop }, { it.boundsLeft }, { it.desc }))

            if (titleCandidates.isNotEmpty()) {
                title = titleCandidates.first().desc
                Log.d(TAG, "  [DESC] Title (top-anchored): '$title' from ${titleCandidates.size} candidates")
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
     * contentDescription + 화면 좌표를 함께 보관 (정렬용)
     */
    private data class DescWithBounds(
        val desc: String,
        val boundsTop: Int,
        val boundsLeft: Int
    )

    /**
     * 노드 트리에서 모든 contentDescription을 bounds와 함께 수집
     *
     * 정렬을 통한 결정론적 title 추출을 위해 bounds.top / left를 함께 저장.
     */
    private fun collectContentDescriptionsWithBounds(
        node: AccessibilityNodeInfo,
        result: MutableList<DescWithBounds>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return

        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotEmpty()) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                result.add(DescWithBounds(desc, bounds.top, bounds.left))
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectContentDescriptionsWithBounds(child, result, depth + 1, maxDepth)
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
     * 디버깅용 view tree dump
     *
     * 핑거프린트 오인식으로 같은 영상이 재차단되는 케이스 추적용.
     * 너무 깊으면 logcat 한도(4KB/line)에 잘리므로 청크 단위로 출력.
     *
     * @param rootNode 덤프할 루트
     * @param tag 로그 태그 prefix (예: "REBLOCK_DUMP")
     * @param maxDepth 최대 탐색 깊이 (기본 12)
     */
    fun dumpNodeTree(rootNode: AccessibilityNodeInfo, tag: String, maxDepth: Int = 12) {
        val builder = StringBuilder()
        builder.append("\n===== $tag START =====\n")
        appendNode(rootNode, builder, 0, maxDepth)
        builder.append("===== $tag END =====")

        // logcat 4KB 한도 회피용 청크 분할
        val chunkSize = 3500
        val text = builder.toString()
        var i = 0
        var chunkIdx = 0
        while (i < text.length) {
            val end = (i + chunkSize).coerceAtMost(text.length)
            Log.w(TAG, "[$tag #${chunkIdx}] ${text.substring(i, end)}")
            i = end
            chunkIdx++
        }
    }

    private fun appendNode(
        node: AccessibilityNodeInfo,
        builder: StringBuilder,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return

        val indent = "  ".repeat(depth)
        val viewId = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
        val selected = node.isSelected
        val visible = node.isVisibleToUser
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""

        // 의미 있는 노드만 로그 (빈 노드는 구조만 짧게)
        if (text.isNotEmpty() || desc.isNotEmpty() || viewId.isNotEmpty()) {
            builder.append(indent)
                .append("[$cls]")
            if (viewId.isNotEmpty()) builder.append(" id=").append(viewId.substringAfter(":id/"))
            if (text.isNotEmpty()) builder.append(" txt=\"").append(text).append("\"")
            if (desc.isNotEmpty()) builder.append(" desc=\"").append(desc).append("\"")
            builder.append(" b=").append(bounds.toShortString())
            if (selected) builder.append(" SEL")
            if (!visible) builder.append(" HIDDEN")
            builder.append("\n")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                appendNode(child, builder, depth + 1, maxDepth)
                child.recycle()
            }
        }
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
