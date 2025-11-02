package com.muuu.shortblock.service.blocking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 콘텐츠 해시 생성기
 *
 * 책임:
 * - AccessibilityNodeInfo 트리에서 콘텐츠를 추출하여 해시 생성
 * - 스크롤 감지를 위해 안정적인 콘텐츠만 해시에 포함
 * - 동적 요소(좋아요 수, 시간 등)는 필터링하여 제외
 */
class ContentHashGenerator {

    private val TAG = "ContentHashGenerator"

    /**
     * 주어진 노드에서 콘텐츠 해시 생성
     *
     * @param rootNode 분석할 루트 AccessibilityNodeInfo
     * @param config 해시 생성 설정
     * @return 콘텐츠 해시값
     */
    fun generateContentHash(
        rootNode: AccessibilityNodeInfo,
        config: HashConfig
    ): Int {
        // 컨테이너 노드 찾기
        val targetNode = if (config.containerViewId != null) {
            findNodeByViewId(rootNode, config.containerViewId) ?: rootNode
        } else {
            rootNode
        }

        // 콘텐츠 문자열 수집
        val contentStrings = mutableListOf<String>()
        collectContentStrings(
            node = targetNode,
            config = config,
            depth = 0,
            contentStrings = contentStrings
        )

        // 해시 생성
        val concatenated = contentStrings.joinToString("|")
        val hash = concatenated.hashCode()

        Log.d(TAG, "Generated hash: $hash from ${contentStrings.size} strings")
        return hash
    }

    /**
     * View ID로 노드 찾기
     */
    private fun findNodeByViewId(
        node: AccessibilityNodeInfo,
        viewId: String
    ): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == viewId) {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val found = findNodeByViewId(child, viewId)
                if (found != null) {
                    child.recycle()
                    return found
                }
                child.recycle()
            }
        }

        return null
    }

    /**
     * 재귀적으로 콘텐츠 문자열 수집
     */
    private fun collectContentStrings(
        node: AccessibilityNodeInfo,
        config: HashConfig,
        depth: Int,
        contentStrings: MutableList<String>
    ) {
        // 최대 깊이 제한
        if (depth > config.maxDepth) {
            return
        }

        // 현재 노드의 View ID 체크
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // 제외 패턴에 해당하면 스킵
        if (config.excludedViewIdPatterns.any { pattern ->
                viewId.contains(pattern.lowercase())
            }) {
            return
        }

        // 포함 패턴에 해당하거나 패턴이 없으면 텍스트 수집
        val shouldCollect = config.includedViewIdPatterns.isEmpty() ||
                config.includedViewIdPatterns.any { pattern ->
                    viewId.contains(pattern.lowercase())
                }

        if (shouldCollect) {
            // 텍스트 수집
            val text = node.text?.toString()
            if (text != null && text.isNotBlank()) {
                if (isValidContentText(text, config)) {
                    contentStrings.add(text.trim())
                }
            }

            // ContentDescription 수집
            val contentDesc = node.contentDescription?.toString()
            if (contentDesc != null && contentDesc.isNotBlank()) {
                if (isValidContentText(contentDesc, config)) {
                    contentStrings.add(contentDesc.trim())
                }
            }
        }

        // 자식 노드 재귀 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectContentStrings(
                    node = child,
                    config = config,
                    depth = depth + 1,
                    contentStrings = contentStrings
                )
                child.recycle()
            }
        }
    }

    /**
     * 텍스트가 유효한 콘텐츠인지 검증
     *
     * - 너무 짧은 텍스트 제외 (3자 미만)
     * - 제외 패턴에 매칭되는 텍스트 제외
     */
    private fun isValidContentText(text: String, config: HashConfig): Boolean {
        // 너무 짧은 텍스트 제외
        if (text.length < 3) {
            return false
        }

        // 제외 패턴 체크
        for (pattern in config.excludedTextPatterns) {
            if (pattern.matches(text)) {
                return false
            }
        }

        return true
    }
}
