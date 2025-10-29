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
    private var stableHashCount = 0  // 같은 해시값이 연속으로 나타난 횟수

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

        // event.source를 사용하여 이벤트가 발생한 실제 뷰의 정보를 가져옴
        val sourceNode = event.source

        // 쇼츠/릴스 화면인지 먼저 확인
        val isShorts = isShortsScreen(packageName, event, sourceNode)

        // 컨텐츠 변경 감지로 스크롤 판단 (YouTube Shorts는 TYPE_VIEW_SCROLLED를 발생시키지 않음)
        if (isShorts && allowedUntilScroll) {
            // 현재 쇼츠 화면의 해시값 계산
            val currentContentHash = getCurrentShortsContentHash()

            if (currentContentHash != 0) {
                if (currentContentHash == lastShortsContentHash) {
                    // 같은 해시값 - 같은 영상
                    stableHashCount++
                    Log.d(TAG, "Same content hash, stable count: $stableHashCount")
                } else {
                    // 해시값이 변경됨
                    if (lastShortsContentHash == 0) {
                        // 첫 번째 쇼츠 - 해시값 저장
                        Log.d(TAG, "First shorts, hash: $currentContentHash")
                        lastShortsContentHash = currentContentHash
                        stableHashCount = 1
                    } else if (stableHashCount >= 2) {
                        // 이전 해시값이 2회 이상 안정적으로 나타났고, 지금 변경됨 = 실제 스크롤
                        Log.d(TAG, "Shorts content changed (scroll detected), old hash: $lastShortsContentHash, new hash: $currentContentHash")
                        allowedUntilScroll = false
                        justScrolled = true
                        lastShortsContentHash = currentContentHash
                        stableHashCount = 1

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
                    } else {
                        // 해시값이 변경되었지만 아직 안정적이지 않음 (UI 변경일 수 있음)
                        Log.d(TAG, "Hash changed but not stable yet, old: $lastShortsContentHash, new: $currentContentHash, count: $stableHashCount")
                        lastShortsContentHash = currentContentHash
                        stableHashCount = 1
                    }
                }
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
                stableHashCount = 0  // 안정성 카운터 초기화
            }
        }
    }

    private fun isShortsScreen(packageName: String, event: AccessibilityEvent, sourceNode: AccessibilityNodeInfo?): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        return when (packageName) {
            "com.google.android.youtube" -> detectYouTubeShorts(rootNode)
            "com.instagram.android" -> detectInstagramReels(rootNode)
            "com.zhiliaoapp.musically" -> detectTikTok(rootNode)
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

    private fun detectTikTok(node: AccessibilityNodeInfo): Boolean {
        // TikTok은 기본적으로 전체가 쇼츠 형식
        // 하지만 프로필 페이지, 검색 페이지 등은 제외
        // 여기서는 단순히 true를 반환 (댓글 화면은 isCommentScreenOpen에서 처리)
        return true
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

        // 패키지명 확인
        val packageName = rootNode.packageName?.toString() ?: ""

        return when {
            packageName.contains("youtube") -> getYouTubeShortsHash(rootNode)
            packageName.contains("instagram") -> getInstagramReelsHash(rootNode)
            packageName.contains("musically") -> getTikTokHash(rootNode)
            else -> 0
        }
    }

    private fun getYouTubeShortsHash(rootNode: AccessibilityNodeInfo): Int {
        // YouTube Shorts의 경우 reel_player_page_container 내부만 확인
        val shortsContainer = rootNode.findAccessibilityNodeInfosByViewId(
            "com.google.android.youtube:id/reel_player_page_container"
        ).firstOrNull()

        val targetNode = shortsContainer ?: rootNode
        val contentBuilder = StringBuilder()

        // 영상 제목, 채널명 등 주요 정보만 수집 (댓글, 좋아요 수 등은 제외)
        fun collectVideoContent(node: AccessibilityNodeInfo, depth: Int = 0) {
            // 너무 깊이 탐색하지 않도록 제한
            if (depth > 8) return

            val viewId = node.viewIdResourceName ?: ""

            // 댓글, 좋아요 관련 뷰는 제외
            if (viewId.contains("comment") ||
                viewId.contains("like") ||
                viewId.contains("engagement") ||
                viewId.contains("actions") ||
                viewId.contains("button")) {
                return
            }

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                if (text.isNotEmpty() && text.length > 2) { // 너무 짧은 텍스트는 제외
                    contentBuilder.append(text).append("|")
                }
            }
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotEmpty() && desc.length > 5) { // 너무 짧은 설명은 제외
                    contentBuilder.append(desc).append("|")
                }
            }

            // 자식 노드 탐색
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectVideoContent(child, depth + 1)
                }
            }
        }

        collectVideoContent(targetNode)
        val hash = contentBuilder.toString().hashCode()
        Log.d(TAG, "YouTube hash: $hash (content length: ${contentBuilder.length})")
        return hash
    }

    private fun getInstagramReelsHash(rootNode: AccessibilityNodeInfo): Int {
        // Instagram Reels의 경우 clips_viewer_view_pager 내부만 확인
        val reelsContainer = rootNode.findAccessibilityNodeInfosByViewId(
            "com.instagram.android:id/clips_viewer_view_pager"
        ).firstOrNull()

        val targetNode = reelsContainer ?: rootNode
        val contentBuilder = StringBuilder()

        fun collectVideoContent(node: AccessibilityNodeInfo, depth: Int = 0) {
            if (depth > 8) return

            val viewId = node.viewIdResourceName ?: ""

            // 댓글, 좋아요, 공유 등 인터랙션 관련 뷰는 제외
            if (viewId.contains("comment") ||
                viewId.contains("like") ||
                viewId.contains("share") ||
                viewId.contains("action_bar") ||
                viewId.contains("button")) {
                return
            }

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                if (text.isNotEmpty() && text.length > 2) {
                    contentBuilder.append(text).append("|")
                }
            }
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotEmpty() && desc.length > 5) {
                    contentBuilder.append(desc).append("|")
                }
            }

            // 자식 노드 탐색
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectVideoContent(child, depth + 1)
                }
            }
        }

        collectVideoContent(targetNode)
        val hash = contentBuilder.toString().hashCode()
        Log.d(TAG, "Instagram hash: $hash (content length: ${contentBuilder.length})")
        return hash
    }

    private fun getTikTokHash(rootNode: AccessibilityNodeInfo): Int {
        // TikTok의 경우 ViewPager 기반으로 동작
        val contentBuilder = StringBuilder()

        fun collectVideoContent(node: AccessibilityNodeInfo, depth: Int = 0) {
            if (depth > 8) return

            val viewId = node.viewIdResourceName ?: ""

            // 댓글, 좋아요, 공유 등 인터랙션 관련 뷰는 제외
            if (viewId.contains("comment") ||
                viewId.contains("digg") ||  // TikTok의 좋아요 버튼
                viewId.contains("share") ||
                viewId.contains("download") ||
                viewId.contains("button")) {
                return
            }

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                if (text.isNotEmpty() && text.length > 2) {
                    contentBuilder.append(text).append("|")
                }
            }
            node.contentDescription?.toString()?.let { desc ->
                if (desc.isNotEmpty() && desc.length > 5) {
                    contentBuilder.append(desc).append("|")
                }
            }

            // 자식 노드 탐색
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectVideoContent(child, depth + 1)
                }
            }
        }

        collectVideoContent(rootNode)
        val hash = contentBuilder.toString().hashCode()
        Log.d(TAG, "TikTok hash: $hash (content length: ${contentBuilder.length})")
        return hash
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
            // 오버레이 표시 전에 미디어 일시정지
            pauseMedia()

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

    private fun pauseMedia() {
        try {
            // 미디어 일시정지를 위해 Back 버튼 액션을 수행하거나
            // 화면을 탭하여 영상을 일시정지 시도
            // AccessibilityService에서는 직접적인 미디어 제어가 제한적이므로
            // 대신 화면 중앙을 탭하는 제스처를 수행하여 영상을 일시정지
            val rootNode = rootInActiveWindow ?: return

            // 화면 중앙 좌표 계산
            val displayMetrics = resources.displayMetrics
            val centerX = displayMetrics.widthPixels / 2f
            val centerY = displayMetrics.heightPixels / 2f

            // 중앙을 클릭하는 제스처 생성 (Android 7.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = android.accessibilityservice.GestureDescription.StrokeDescription(
                    android.graphics.Path().apply {
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY)
                    },
                    0,
                    100 // 100ms 동안 클릭
                )
                val gesture = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(path)
                    .build()

                dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                        Log.d(TAG, "Pause gesture completed")
                    }

                    override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                        Log.d(TAG, "Pause gesture cancelled")
                    }
                }, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media", e)
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
