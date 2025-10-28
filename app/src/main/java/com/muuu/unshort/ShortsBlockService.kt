package com.muuu.unshort

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
    private var justScrolled = false  // 방금 스크롤했는지 여부 (오버레이 표시 후 바로 닫히는 것 방지)
    private var lastShortsContentHash: Int = 0  // 이전 쇼츠 화면의 해시값

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

        // 컨텐츠 변경 감지로 스크롤 판단 (YouTube Shorts는 TYPE_VIEW_SCROLLED를 발생시키지 않음)
        if (isShorts && allowedUntilScroll) {
            // 현재 쇼츠 화면의 해시값 계산
            val currentContentHash = getCurrentShortsContentHash()

            if (currentContentHash != 0 && currentContentHash != lastShortsContentHash) {
                // 화면 내용이 변경됨 = 다음 쇼츠로 스크롤
                Log.d(TAG, "Shorts content changed (scroll detected), blocking next shorts")
                allowedUntilScroll = false
                justScrolled = true
                lastShortsContentHash = currentContentHash

                // 스크롤 후 오버레이 표시
                if (blockOverlay?.isShowing() != true) {
                    showBlockOverlay()
                    // 오버레이 표시 후 justScrolled 플래그 해제 (바로 닫히는 것 방지)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        justScrolled = false
                        Log.d(TAG, "JustScrolled flag reset after delay")
                    }, 100) // 100ms 후 플래그 해제
                }
                return
            } else if (lastShortsContentHash == 0) {
                // 첫 번째 쇼츠 - 해시값 저장
                lastShortsContentHash = currentContentHash
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
            // 방금 스크롤한 경우는 무시 (스크롤 직후 일시적으로 isShorts가 false가 될 수 있음)
            if (justScrolled) {
                Log.d(TAG, "Ignoring 'shorts closed' event right after scroll")
                return
            }

            // 쇼츠 화면이 아닌데 오버레이가 표시 중이면 제거
            if (blockOverlay?.isShowing() == true) {
                Log.d(TAG, "Shorts screen closed, dismissing overlay")
                blockOverlay?.dismiss()
                blockOverlay = null
                allowedUntilScroll = false  // 허용 상태 초기화
                lastShortsContentHash = 0  // 해시값 초기화
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

    private fun getCurrentShortsContentHash(): Int {
        val rootNode = rootInActiveWindow ?: return 0

        // RecyclerView 또는 ViewPager에서 현재 보이는 아이템의 해시값 계산
        // 텍스트와 콘텐츠 설명을 조합하여 고유한 해시값 생성
        val contentBuilder = StringBuilder()

        fun collectContent(node: AccessibilityNodeInfo, depth: Int = 0) {
            // 너무 깊이 탐색하지 않도록 제한
            if (depth > 10) return

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                if (text.isNotEmpty()) {
                    contentBuilder.append(text).append("|")
                }
            }
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotEmpty()) {
                    contentBuilder.append(desc).append("|")
                }
            }

            // 자식 노드 탐색
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectContent(child, depth + 1)
                }
            }
        }

        collectContent(rootNode)
        return contentBuilder.toString().hashCode()
    }

    private fun showBlockOverlay() {
        Log.d(TAG, "showBlockOverlay() called")

        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            requestOverlayPermission()
            return
        }

        Log.d(TAG, "Overlay permission granted, creating BlockOverlay")

        try {
            blockOverlay = BlockOverlay(this)
            Log.d(TAG, "BlockOverlay created, calling show()")
            blockOverlay?.show(
                onDismiss = {
                    // 오버레이 제거 시
                    Log.d(TAG, "Overlay dismissed")
                    blockOverlay?.dismiss()
                    blockOverlay = null
                },
                onComplete = {
                    // 15초 완료 - 현재 쇼츠까지는 허용
                    allowedUntilScroll = true
                    Log.d(TAG, "Timer completed, allowing current shorts")
                }
            )
            Log.d(TAG, "BlockOverlay show() completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
        }
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
