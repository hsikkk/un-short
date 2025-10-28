package com.shortblock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ShortsBlockService : AccessibilityService() {

    private var blockOverlay: BlockOverlay? = null
    private val TAG = "ShortsBlockService"
    private var allowedUntilScroll = false  // 15초 완료 후 스크롤 전까지 허용

    // 차단 대상 앱 패키지명
    private val TARGET_APPS = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.zhiliaoapp.musically" // TikTok
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 차단 대상 앱이 아니면 무시
        if (packageName !in TARGET_APPS) return

        // 쇼츠/릴스 화면인지 먼저 확인
        val isShorts = isShortsScreen(packageName, event)

        // 스크롤 이벤트 감지 (다음 쇼츠로 이동)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && isShorts) {
            if (allowedUntilScroll) {
                Log.d(TAG, "Scroll detected, blocking next shorts")
                allowedUntilScroll = false

                // 스크롤 후 오버레이 표시
                if (blockOverlay?.isShowing() != true) {
                    showBlockOverlay()
                }
                return
            }
        }

        if (isShorts) {
            // 15초 완료 후 허용 상태면 차단하지 않음
            if (allowedUntilScroll) {
                Log.d(TAG, "Allowed to view current shorts")
                return
            }

            // 쇼츠 화면이고 오버레이가 없으면 표시
            if (blockOverlay?.isShowing() != true) {
                Log.d(TAG, "Shorts screen detected in $packageName")
                showBlockOverlay()
            }
        } else {
            // 쇼츠 화면이 아닌데 오버레이가 표시 중이면 제거
            if (blockOverlay?.isShowing() == true) {
                Log.d(TAG, "Shorts screen closed, dismissing overlay")
                blockOverlay?.dismiss()
                blockOverlay = null
                allowedUntilScroll = false  // 허용 상태 초기화
            }
        }
    }

    private fun isShortsScreen(packageName: String, event: AccessibilityEvent): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        return when (packageName) {
            "com.google.android.youtube" -> detectYouTubeShorts(rootNode)
            "com.instagram.android" -> detectInstagramReels(rootNode)
            "com.zhiliaoapp.musically" -> true // TikTok은 전체가 쇼츠 형식
            else -> false
        }
    }

    private fun detectYouTubeShorts(node: AccessibilityNodeInfo): Boolean {
        // YouTube Shorts 감지
        // 1. shorts_player 같은 View ID 찾기
        val shortsNodes = node.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/reel_player_page_container"
        )
        if (shortsNodes.isNotEmpty()) return true

        // 2. "Shorts" 텍스트 찾기
        val textNodes = findNodesByText(node, "Shorts")
        if (textNodes.isNotEmpty()) {
            // Shorts 탭이 선택되어 있는지 확인
            return textNodes.any { it.parent?.isSelected == true || it.isSelected }
        }

        return false
    }

    private fun detectInstagramReels(node: AccessibilityNodeInfo): Boolean {
        // Instagram Reels 감지
        // 1. reels_viewer 같은 View ID
        val reelsNodes = node.findAccessibilityNodeInfosByViewId(
            "com.instagram.android:id/clips_viewer_view_pager"
        )
        if (reelsNodes.isNotEmpty()) return true

        // 2. Content Description으로 찾기
        return findNodesByContentDescription(node, "Reels").isNotEmpty()
    }

    private fun findNodesByText(node: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                result.addAll(findNodesByText(child, text))
            }
        }

        return result
    }

    private fun findNodesByContentDescription(
        node: AccessibilityNodeInfo,
        description: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        if (node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                result.addAll(findNodesByContentDescription(child, description))
            }
        }

        return result
    }

    private fun showBlockOverlay() {
        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            requestOverlayPermission()
            return
        }

        blockOverlay = BlockOverlay(this)
        blockOverlay?.show(
            onDismiss = {
                // 오버레이 제거 시
                blockOverlay?.dismiss()
                blockOverlay = null
            },
            onComplete = {
                // 15초 완료 - 현재 쇼츠까지는 허용
                allowedUntilScroll = true
                Log.d(TAG, "Timer completed, allowing current shorts")
            }
        )
    }

    private fun pauseVideo() {
        try {
            val rootNode = rootInActiveWindow ?: return

            // 방법 1: 클릭 가능한 노드를 재귀적으로 찾아서 클릭
            val clickableNode = findClickableNode(rootNode)
            if (clickableNode != null) {
                val success = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked on clickable node: success=$success")
                return
            }

            // 방법 2: 루트 노드 클릭 시도
            val success = rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                Log.d(TAG, "Video pause action performed on root")
            } else {
                Log.w(TAG, "Video pause action failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause video", e)
        }
    }

    private fun findClickableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 클릭 가능한 노드 찾기
        if (node.isClickable) {
            return node
        }

        // 자식 노드 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val clickable = findClickableNode(child)
                if (clickable != null) {
                    return clickable
                }
            }
        }

        return null
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        blockOverlay?.dismiss()
        blockOverlay = null
    }
}
