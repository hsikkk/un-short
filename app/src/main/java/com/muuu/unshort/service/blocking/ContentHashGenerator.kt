package com.muuu.unshort.service.blocking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 콘텐츠 해시 생성기
 *
 * 기존 동작하던 로직을 그대로 사용
 */
class ContentHashGenerator {

    private val TAG = "ContentHashGenerator"
    private val stableExtractor = StableContentExtractor()  // 안정적인 콘텐츠 추출기

    /**
     * YouTube Shorts의 콘텐츠 핑거프린트 생성 (유사도 비교용)
     *
     * @param rootNode 루트 노드
     * @return 제목/채널/설명을 개별 해시로 가진 핑거프린트
     */
    fun generateContentFingerprint(rootNode: AccessibilityNodeInfo): ContentFingerprint {
        return stableExtractor.extractYouTubeFingerprint(rootNode)
    }

    /**
     * 디버깅용 view tree dump (오탐지 분석용)
     */
    fun dumpForDebug(rootNode: AccessibilityNodeInfo, tag: String) {
        stableExtractor.dumpNodeTree(rootNode, tag)
    }

    /**
     * 주어진 노드에서 콘텐츠 해시 생성
     */
    fun generateContentHash(
        rootNode: AccessibilityNodeInfo,
        config: HashConfig
    ): Int {
        // YouTube의 경우 안정적인 콘텐츠 추출 사용
        if (config.useStableExtractor && config.containerViewId?.contains("youtube") == true) {
            val stableContent = stableExtractor.extractYouTubeContent(rootNode)
            val hash = stableContent.hashCode()

            Log.d(TAG, "Using stable extractor - hash: $hash (content length: ${stableContent.length})")
            Log.d(TAG, "Stable content: $stableContent")

            return hash
        }

        // 기존 방식 (다른 앱들을 위해 유지)
        val targetNode = if (config.containerViewId != null) {
            rootNode.findAccessibilityNodeInfosByViewId(config.containerViewId)?.firstOrNull() ?: rootNode
        } else {
            rootNode
        }

        // StringBuilder로 콘텐츠 수집 (기존 방식)
        val contentBuilder = StringBuilder()

        collectContent(
            node = targetNode,
            config = config,
            depth = 0,
            contentBuilder = contentBuilder
        )

        val content = contentBuilder.toString()
        val hash = content.hashCode()
        val contentLength = content.length

        Log.d(TAG, "Generated hash: $hash (content length: $contentLength)")
        Log.d(TAG, "Content for hash: $content")

        return hash
    }

    /**
     * 재귀적으로 콘텐츠 수집 (기존 로직)
     */
    private fun collectContent(
        node: AccessibilityNodeInfo,
        config: HashConfig,
        depth: Int,
        contentBuilder: StringBuilder
    ) {
        // 최대 깊이 제한
        if (depth > config.maxDepth) {
            return
        }

        // Bounds 제한 체크 (우측 사이드바 제외)
        config.excludeBoundsRight?.let { rightLimit ->
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.left >= rightLimit) {
                return  // 우측 영역 노드는 스킵
            }
        }

        val viewId = node.viewIdResourceName ?: ""

        // Excluded viewId 패턴 체크 (기존과 동일)
        if (config.excludedViewIdPatterns.any { pattern ->
                viewId.contains(pattern, ignoreCase = true)
            }) {
            return
        }

        // 텍스트 수집 (앱별 검증 함수 사용)
        node.text?.toString()?.let { text ->
            if (text.isNotEmpty()) {
                // 앱별 텍스트 검증
                if (config.textValidator(text)) {
                    // Excluded text 패턴 체크
                    val shouldExclude = config.excludedTextPatterns.any { pattern ->
                        pattern.matches(text)
                    }

                    if (!shouldExclude) {
                        contentBuilder.append(text).append("|")
                    }
                }
            }
        }

        // ContentDescription 수집 (플래그 기반)
        if (config.includeContentDescription) {
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotEmpty() && desc.length > 5) {
                    // textValidator로 유효성 검증
                    if (config.textValidator(desc)) {
                        // Excluded text 패턴 체크
                        val shouldExclude = config.excludedTextPatterns.any { pattern ->
                            pattern.matches(desc)
                        }

                        if (!shouldExclude) {
                            contentBuilder.append(desc).append("|")
                        }
                    }
                }
            }
        }

        // 자식 노드 재귀 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectContent(
                    node = child,
                    config = config,
                    depth = depth + 1,
                    contentBuilder = contentBuilder
                )
                child.recycle()
            }
        }
    }
}
