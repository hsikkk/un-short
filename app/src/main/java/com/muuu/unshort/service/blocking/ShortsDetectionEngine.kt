package com.muuu.unshort.service.blocking

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 통합 쇼츠 감지 엔진
 *
 * 책임:
 * - AccessibilityNodeInfo 트리에서 쇼츠 화면 감지
 * - 앱별 설정(AppBlockingConfig)을 기반으로 감지 로직 실행
 * - View ID, 텍스트, ContentDescription 기반 감지 지원
 */
class ShortsDetectionEngine {

    private val TAG = "ShortsDetectionEngine"
    private val MAX_TREE_DEPTH = 30

    /**
     * 주어진 노드에서 쇼츠 화면 여부 감지
     *
     * @param node 분석할 AccessibilityNodeInfo
     * @param config 앱별 차단 설정
     * @return 쇼츠 화면이면 true
     */
    fun detectShortsScreen(
        node: AccessibilityNodeInfo,
        config: AppBlockingConfig
    ): Boolean {
        Log.d(TAG, "[${config.displayName}] Starting detection...")

        // 1단계: View ID 기반 감지 (우선순위 높음)
        for (detector in config.viewIdDetectors) {
            Log.d(TAG, "[${config.displayName}] Checking View ID: ${detector.viewId}")
            val found = findNodesByViewId(node, detector.viewId)
            Log.d(TAG, "[${config.displayName}] Found ${found.size} nodes for View ID: ${detector.viewId}")
            if (found.isNotEmpty()) {
                // 실제로 화면에 보이는 노드만 유효한 감지로 처리
                val visibleNodes = found.filter { it.isVisibleToUser }
                Log.d(TAG, "[${config.displayName}] Visible nodes: ${visibleNodes.size}/${found.size} for View ID: ${detector.viewId}")
                found.forEach { it.recycle() }
                if (visibleNodes.isNotEmpty()) {
                    Log.d(TAG, "[${config.displayName}] ✓ Detected via View ID: ${detector.viewId}")
                    return true
                } else {
                    Log.d(TAG, "[${config.displayName}] ✗ View ID found but not visible to user: ${detector.viewId}")
                }
            }
        }

        // 2단계: 텍스트/ContentDescription 기반 감지 (폴백)
        for (detector in config.textDetectors) {
            Log.d(TAG, "[${config.displayName}] Checking ${detector.searchType}: ${detector.text}")
            val nodes = when (detector.searchType) {
                SearchType.TEXT -> findNodesByText(node, detector.text)
                SearchType.CONTENT_DESCRIPTION -> findNodesByContentDescription(node, detector.text)
            }
            Log.d(TAG, "[${config.displayName}] Found ${nodes.size} nodes for ${detector.searchType}: ${detector.text}")

            if (nodes.isNotEmpty()) {
                // requiresSelection이 true인 경우 isSelected도 체크
                val matched = if (detector.requiresSelection) {
                    val result = nodes.any { it.isSelected }
                    Log.d(TAG, "[${config.displayName}] Selection check: $result")
                    result
                } else {
                    true
                }

                nodes.forEach { it.recycle() }

                if (matched) {
                    Log.d(TAG, "[${config.displayName}] ✓ Detected via ${detector.searchType}: ${detector.text}")
                    return true
                }
            }
        }

        Log.d(TAG, "[${config.displayName}] ✗ Not detected")
        return false
    }

    /**
     * View ID로 노드 검색 (Android SDK 내장 메서드 사용)
     */
    private fun findNodesByViewId(
        node: AccessibilityNodeInfo,
        viewId: String
    ): List<AccessibilityNodeInfo> {
        // Android SDK의 최적화된 메서드 사용
        return node.findAccessibilityNodeInfosByViewId(viewId)?.toList() ?: emptyList()
    }

    /**
     * 텍스트로 노드 검색 (재귀, 깊이 제한)
     */
    private fun findNodesByText(
        node: AccessibilityNodeInfo,
        searchText: String,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_TREE_DEPTH) return emptyList()

        val results = mutableListOf<AccessibilityNodeInfo>()

        // 현재 노드 체크
        val nodeText = node.text?.toString() ?: ""
        if (nodeText.contains(searchText, ignoreCase = true)) {
            results.add(AccessibilityNodeInfo.obtain(node))
        }

        // 자식 노드 재귀 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                results.addAll(findNodesByText(child, searchText, depth + 1))
                child.recycle()
            }
        }

        return results
    }

    /**
     * ContentDescription으로 노드 검색 (재귀, 깊이 제한)
     */
    private fun findNodesByContentDescription(
        node: AccessibilityNodeInfo,
        searchText: String,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_TREE_DEPTH) return emptyList()

        val results = mutableListOf<AccessibilityNodeInfo>()

        // 현재 노드 체크
        val contentDesc = node.contentDescription?.toString() ?: ""
        if (contentDesc.contains(searchText, ignoreCase = true)) {
            results.add(AccessibilityNodeInfo.obtain(node))
        }

        // 자식 노드 재귀 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                results.addAll(findNodesByContentDescription(child, searchText, depth + 1))
                child.recycle()
            }
        }

        return results
    }
}
