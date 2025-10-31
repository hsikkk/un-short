package com.muuu.unshort

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShortsBlockService : AccessibilityService() {

    private var blockOverlay: BlockOverlay? = null
    private val TAG = "ShortsBlockService"
    private var allowedUntilScroll = false  // 15초 완료 후 스크롤 전까지 허용
    private var justScrolled = false  // 방금 스크롤했는지 여부 (오버레이 표시 후 바로 닫히는 것 방지)
    private var lastShortsContentHash: Int = 0  // 이전 쇼츠 화면의 해시값
    private var stableHashCount = 0  // 같은 해시값이 연속으로 나타난 횟수
    private var wasInShortsScreen = false  // 이전에 쇼츠 화면에 있었는지 여부
    private var overlayWasShown = false  // 현재 쇼츠에 대해 오버레이가 표시된 적이 있는지
    private var leftViaHomeButton = false  // 홈/백 버튼으로 나갔는지 여부
    private var lastForegroundPackage: String = ""  // 마지막 포그라운드 앱
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var foregroundCheckRunnable: Runnable? = null
    private var pendingOverlayJob: Runnable? = null  // pending 중인 오버레이 표시 job
    private var appStartTime: Long = 0  // 앱 시작 시간 (fresh start 감지용)

    // Timer-related variables
    private var currentSessionId: String = ""
    private var timerReceiver: BroadcastReceiver? = null

    // 차단 대상 앱 패키지명
    private val TARGET_APPS = setOf(
        "com.google.android.youtube",
        "com.instagram.android"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Privacy consent check - blocking disabled if not consented
        if (!hasValidPrivacyConsent()) {
            Log.w(TAG, "Privacy consent not given - blocking disabled")
            return
        }

        // 차단 활성화 상태 확인
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBlockingEnabled = prefs.getBoolean("blocking_enabled", true)

        // 차단이 비활성화되어 있으면 아무 작업도 하지 않음
        if (!isBlockingEnabled) {
            // 오버레이가 표시 중이면 제거
            if (blockOverlay?.isShowing() == true) {
                cancelPendingOverlay()
                blockOverlay?.dismiss()
                blockOverlay = null
            }
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // 이벤트 로깅 (디버깅용)
        Log.d(TAG, "=== Event: ${event.eventType}, Package: $packageName ===")
        Log.d(TAG, "State - allowedUntilScroll: $allowedUntilScroll, overlayWasShown: $overlayWasShown")
        Log.d(TAG, "State - blockOverlay?.isShowing(): ${blockOverlay?.isShowing()}")

        // 오버레이가 표시 중일 때, 포그라운드 앱 변경 감지
        if (blockOverlay?.isShowing() == true) {
            // rootInActiveWindow로 현재 포그라운드 앱 확인
            val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()

            if (currentForegroundPackage != null && currentForegroundPackage != lastForegroundPackage) {
                lastForegroundPackage = currentForegroundPackage

                // 차단 대상 앱이 아닌 앱으로 전환 (홈/백 버튼 포함)
                if (currentForegroundPackage !in TARGET_APPS) {
                    Log.d(TAG, "Foreground changed to $currentForegroundPackage, dismissing overlay")
                    cancelPendingOverlay()  // pending job 취소
                    blockOverlay?.dismiss()
                    blockOverlay = null
                    leftViaHomeButton = true  // 백그라운드로 나갔음을 표시

                    // 타이머 완료 플래그 초기화 (쇼츠 앱을 떠나면 리셋)
                    prefs.edit().remove(AppConstants.PREF_COMPLETED_SESSION_ID).apply()
                    Log.d(TAG, "Cleared timer completion flag - left shorts app")

                    // 모든 상태 플래그 초기화 (플래그 꼬임 방지)
                    allowedUntilScroll = false
                    overlayWasShown = false
                    Log.d(TAG, "Reset all flags - left shorts app")

                    return
                }
            }
        }

        // TYPE_WINDOW_STATE_CHANGED 이벤트로 앱 전환 감지 (추가 보험)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Window state changed: $packageName")

            // 차단 대상 앱에서 다른 앱으로 전환 (홈/백 버튼 포함)
            if (packageName !in TARGET_APPS && blockOverlay?.isShowing() == true) {
                Log.d(TAG, "Left target app to $packageName (home/back/app switch), dismissing overlay")
                cancelPendingOverlay()  // pending job 취소
                blockOverlay?.dismiss()
                blockOverlay = null
                leftViaHomeButton = true  // 백그라운드로 나갔음을 표시

                // 타이머 완료 플래그 초기화 (쇼츠 앱을 떠나면 리셋)
                prefs.edit().remove(AppConstants.PREF_COMPLETED_SESSION_ID).apply()
                Log.d(TAG, "Cleared timer completion flag - left shorts app")

                // 모든 상태 플래그 초기화 (플래그 꼬임 방지)
                allowedUntilScroll = false
                overlayWasShown = false
                Log.d(TAG, "Reset all flags - left shorts app")

                return
            }
        }

        // 차단 대상 앱이 아니면 무시
        if (packageName !in TARGET_APPS) {
            return
        }

        // event.source를 사용하여 이벤트가 발생한 실제 뷰의 정보를 가져옴
        val sourceNode = event.source

        // 쇼츠/릴스 화면인지 먼저 확인
        val isShorts = isShortsScreen(packageName, event, sourceNode)
        Log.d(TAG, ">>> isShorts = $isShorts")

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
                        overlayWasShown = false  // 새 영상이므로 플래그 초기화

                        // 스크롤 후 오버레이 표시
                        if (blockOverlay?.isShowing() != true && !overlayWasShown) {
                            showBlockOverlay(packageName)
                            overlayWasShown = true  // 오버레이 표시됨
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
            // 쇼츠 화면에 처음 진입했는지 확인 (이전 화면이 쇼츠가 아니었거나, 백그라운드에서 돌아온 경우)
            if (!wasInShortsScreen) {
                Log.d(TAG, "Entering Shorts screen (wasInShortsScreen=false)")
                overlayWasShown = false  // 새로 쇼츠에 진입했으므로 초기화
            }

            // 백그라운드에서 돌아온 경우
            if (leftViaHomeButton) {
                Log.d(TAG, "Returned to Shorts screen from background")
                leftViaHomeButton = false
                overlayWasShown = false  // 백그라운드에서 돌아왔으므로 초기화
                appStartTime = System.currentTimeMillis()  // Fresh start 시간 기록
                Log.d(TAG, "Recorded app start time for fresh start detection")
            }

            // 15초 완료 후 허용 상태면 차단하지 않음
            if (allowedUntilScroll) {
                Log.d(TAG, "Allowed to view current shorts (allowedUntilScroll=true)")
                wasInShortsScreen = true
                return
            }

            Log.d(TAG, "Checking overlay display conditions:")
            Log.d(TAG, "  - blockOverlay?.isShowing(): ${blockOverlay?.isShowing()}")
            Log.d(TAG, "  - overlayWasShown: $overlayWasShown")
            Log.d(TAG, "  - Should show overlay: ${blockOverlay?.isShowing() != true && !overlayWasShown}")

            // 쇼츠 화면이고 오버레이가 없으면 표시
            if (blockOverlay?.isShowing() != true && !overlayWasShown) {
                Log.d(TAG, "Shorts screen detected in $packageName")
                Log.d(TAG, ">>> Calling showBlockOverlay()")
                showBlockOverlay(packageName)
                Log.d(TAG, ">>> overlayWasShown set to true")
                overlayWasShown = true
            } else {
                Log.d(TAG, "Not showing overlay - already shown or currently showing")
            }

            wasInShortsScreen = true
        } else {
            // 쇼츠 화면을 벗어남 (앱 내에서 다른 화면으로 이동)
            wasInShortsScreen = false

            // 방금 스크롤한 경우는 무시 (스크롤 직후 일시적으로 isShorts가 false가 될 수 있음)
            if (justScrolled) {
                Log.d(TAG, "Ignoring 'shorts closed' event right after scroll")
                return
            }

            // 쇼츠 화면 벗어남 - pending job 취소
            cancelPendingOverlay()

            // 쇼츠 화면이 아닌데 오버레이가 표시 중이면 제거
            if (blockOverlay?.isShowing() == true) {
                Log.d(TAG, "Shorts screen closed (within app navigation), dismissing overlay")
                blockOverlay?.dismiss()
                blockOverlay = null

                // leftViaHomeButton이 true이면 홈/백 버튼으로 나간 것이므로 상태 유지
                if (!leftViaHomeButton) {
                    Log.d(TAG, "Within app navigation - clearing all state")

                    allowedUntilScroll = false
                    lastShortsContentHash = 0
                    stableHashCount = 0
                    overlayWasShown = false

                    // 우리 앱이 아닌 다른 곳으로 갔으면 TimerActivity 종료
                    val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()
                    if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
                        Log.d(TAG, "Left to external app/home ($currentForegroundPackage), closing TimerActivity")
                        sendTimerForceClose()
                    }
                } else {
                    Log.d(TAG, "Left via home/back button - keeping state for resume")
                }
            }
        }
    }

    private fun isShortsScreen(packageName: String, event: AccessibilityEvent, sourceNode: AccessibilityNodeInfo?): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        return when (packageName) {
            "com.google.android.youtube" -> detectYouTubeShorts(rootNode)
            "com.instagram.android" -> detectInstagramReels(rootNode)
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
        Log.d(TAG, "detectTikTok called - package: ${node.packageName}")

        // TikTok 메인 피드(For You/Following) 감지
        // ViewPager로 구성되어 있으므로 이를 탐지

        // 1. ViewPager ID 확인 (메인 피드) - 여러 가능한 ID들
        val possibleViewPagerIds = listOf(
            "com.zhiliaoapp.musically:id/viewpager",
            "com.ss.android.ugc.trill:id/viewpager",
            "com.zhiliaoapp.musically:id/view_pager",
            "com.ss.android.ugc.trill:id/view_pager"
        )

        for (id in possibleViewPagerIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "TikTok: Found ViewPager with ID: $id")
                return true
            }
        }

        // 2. 메인 피드 컨테이너 확인
        val possibleContainerIds = listOf(
            "com.zhiliaoapp.musically:id/main_tab_container",
            "com.ss.android.ugc.trill:id/main_tab_container"
        )

        for (id in possibleContainerIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "TikTok: Found main tab container: $id")
                return true
            }
        }

        // 3. "For You" 또는 "Following" 또는 한글 텍스트 확인
        val forYouNodes = findNodesByText(node, "For You")
        val followingNodes = findNodesByText(node, "Following")
        val koreanForYou = findNodesByText(node, "추천") // 한국어 "추천"
        val koreanFollowing = findNodesByText(node, "팔로잉") // 한국어 "팔로잉"

        if (forYouNodes.isNotEmpty() || followingNodes.isNotEmpty() ||
            koreanForYou.isNotEmpty() || koreanFollowing.isNotEmpty()) {
            Log.d(TAG, "TikTok: Found main feed tab text")
            return true
        }

        // 4. 프로필 페이지나 다른 화면이면 false
        val profileNodes = findNodesByText(node, "Profile")
        val profileKorean = findNodesByText(node, "프로필")
        val discoverNodes = findNodesByText(node, "Discover")
        val discoverKorean = findNodesByText(node, "검색")

        if (profileNodes.isNotEmpty() || discoverNodes.isNotEmpty() ||
            profileKorean.isNotEmpty() || discoverKorean.isNotEmpty()) {
            Log.d(TAG, "TikTok: In Profile or Discover screen, not blocking")
            return false
        }

        // 디버깅: 현재 화면의 View ID들 출력 (처음 몇 개만)
        logViewIds(node, 0, 3)

        // 기본적으로 true 반환 (틱톡은 기본이 피드 화면)
        Log.d(TAG, "TikTok: Defaulting to true (main feed assumed)")
        return true
    }

    private fun logViewIds(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return

        val viewId = node.viewIdResourceName
        if (viewId != null && viewId.isNotEmpty()) {
            Log.d(TAG, "TikTok ViewID [depth=$depth]: $viewId")
        }

        for (i in 0 until minOf(node.childCount, 5)) { // 처음 5개만
            node.getChild(i)?.let { child ->
                logViewIds(child, depth + 1, maxDepth)
            }
        }
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

            // UI 관련 요소들 제외 (재생 상태에 따라 변하는 것들)
            if (viewId.contains("comment") ||
                viewId.contains("like") ||
                viewId.contains("engagement") ||
                viewId.contains("actions") ||
                viewId.contains("button") ||
                viewId.contains("progress") ||
                viewId.contains("time") ||
                viewId.contains("duration") ||
                viewId.contains("seek") ||
                viewId.contains("player_control") ||
                viewId.contains("pause") ||
                viewId.contains("play")) {
                return
            }

            // 핵심 콘텐츠만 포함 (제목, 채널명, 설명)
            val isContentNode = viewId.contains("title") ||
                               viewId.contains("channel") ||
                               viewId.contains("author") ||
                               viewId.contains("description") ||
                               viewId.contains("reel_metadata") ||
                               viewId.contains("video_metadata")

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                // 숫자만 있거나 너무 짧은 텍스트는 제외
                if (text.isNotEmpty() && text.length > 2 && !text.all { it.isDigit() || it == ':' || it == '/' }) {
                    // 재생 시간 패턴 제외 (예: "0:15", "1:30")
                    if (!text.matches(Regex("\\d+:\\d+"))) {
                        contentBuilder.append(text).append("|")
                    }
                }
            }

            // contentDescription은 주요 노드에서만 수집
            if (isContentNode) {
                node.contentDescription?.toString()?.let { desc ->
                    if (desc.isNotEmpty() && desc.length > 5) {
                        contentBuilder.append(desc).append("|")
                    }
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

            // UI 관련 요소들 제외 (재생 상태에 따라 변하는 것들)
            if (viewId.contains("comment") ||
                viewId.contains("like") ||
                viewId.contains("share") ||
                viewId.contains("action_bar") ||
                viewId.contains("button") ||
                viewId.contains("progress") ||
                viewId.contains("time") ||
                viewId.contains("duration") ||
                viewId.contains("seek") ||
                viewId.contains("player_control") ||
                viewId.contains("pause") ||
                viewId.contains("play") ||
                viewId.contains("heart") ||
                viewId.contains("save")) {
                return
            }

            // 핵심 콘텐츠만 포함 (계정명, 설명, 캡션)
            val isContentNode = viewId.contains("username") ||
                               viewId.contains("caption") ||
                               viewId.contains("description") ||
                               viewId.contains("user_name") ||
                               viewId.contains("text_content") ||
                               viewId.contains("primary_text")

            // 텍스트나 콘텐츠 설명이 있으면 추가
            node.text?.toString()?.let { text ->
                // 숫자만 있거나 너무 짧은 텍스트는 제외
                if (text.isNotEmpty() && text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }) {
                    // 숫자 포맷 제외 (예: "1.2K", "345", "10M")
                    if (!text.matches(Regex("\\d+[KMB]?")) && !text.matches(Regex("\\d+\\.\\d+[KMB]?"))) {
                        contentBuilder.append(text).append("|")
                    }
                }
            }

            // contentDescription은 주요 노드에서만 수집
            if (isContentNode) {
                node.contentDescription?.toString()?.let { desc ->
                    if (desc.isNotEmpty() && desc.length > 5) {
                        contentBuilder.append(desc).append("|")
                    }
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

    private fun showBlockOverlay(packageName: String) {
        Log.d(TAG, "showBlockOverlay() called for $packageName")

        // allowedUntilScroll이 true이면 오버레이 표시하지 않음
        if (allowedUntilScroll) {
            Log.d(TAG, "Allowed until scroll - skipping overlay")
            return
        }

        // 이미 오버레이가 표시 중이면 무시
        if (blockOverlay?.isShowing() == true) {
            Log.d(TAG, "Overlay already showing - skipping")
            return
        }

        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            requestOverlayPermission()
            return
        }

        Log.d(TAG, "Overlay permission granted, scheduling overlay with delay")

        // 기존 pending job 취소
        cancelPendingOverlay()

        // 딜레이 후 pauseMedia + 오버레이 표시
        pendingOverlayJob = Runnable {
            try {
                Log.d(TAG, "Executing pending overlay job for $packageName")

                // Generate session ID using only content hash
                // This ensures all shorts get blocked, not just the first one
                val contentHash = if (lastShortsContentHash != 0) lastShortsContentHash else 0
                currentSessionId = generateSessionId(packageName, contentHash)
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putString(AppConstants.PREF_CURRENT_SESSION_ID, currentSessionId).apply()
                Log.d(TAG, "Session created with content hash $contentHash: $currentSessionId")

                // 미디어 일시정지 시도
                pauseMedia(packageName)

                blockOverlay = BlockOverlay(this)
                Log.d(TAG, "BlockOverlay created, calling show()")
                blockOverlay?.show(
                    onDismiss = {
                        // 오버레이 제거 시
                        Log.d(TAG, "Overlay dismissed")
                        stopForegroundCheck()
                        blockOverlay = null
                    },
                    onComplete = {
                        // 타이머 완료 - 버튼 전환만 (오버레이는 유지)
                        Log.d(TAG, "Timer completed, showing watch button")
                        // 포그라운드 체크 중지 (사용자 선택 대기)
                        stopForegroundCheck()
                    },
                    onSkip = {
                        // "안볼래요" 버튼 클릭 - 백키 누르기
                        Log.d(TAG, "Skip button pressed, performing back action")
                        // 세션 초기화 (쇼츠를 나갔으므로)
                        overlayWasShown = false
                        allowedUntilScroll = false
                        currentSessionId = ""

                        // 타이머 완료 플래그도 초기화 ("안볼래요" 선택 시 다음 쇼츠도 처음부터)
                        prefs.edit().remove(AppConstants.PREF_COMPLETED_SESSION_ID).apply()
                        Log.d(TAG, "Cleared timer completion flag - user chose to skip")

                        performGlobalBackAction()
                    },
                    onWatch = {
                        // "볼래요" 버튼 클릭 - 미디어 재생 재개
                        Log.d(TAG, "Watch button pressed, resuming media")
                        allowedUntilScroll = true
                        overlayWasShown = true  // 이미 표시된 것으로 유지
                        stopForegroundCheck()

                        // 짧은 지연 후 미디어 재생 (오버레이 dismiss 애니메이션 완료 후)
                        handler.postDelayed({
                            resumeMedia()
                        }, 100)
                    },
                    sessionId = currentSessionId,
                    sourcePackage = packageName
                )
                Log.d(TAG, "BlockOverlay show() completed")

                // 주기적으로 포그라운드 앱 체크 시작
                startForegroundCheck()

                pendingOverlayJob = null
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
                pendingOverlayJob = null
            }
        }

        // Fresh start 후 500ms 이내면 500ms 딜레이, 이후는 300ms
        val timeSinceStart = System.currentTimeMillis() - appStartTime
        val delay = if (timeSinceStart < 500) {
            500L // Fresh start 후 500ms 이내: 500ms 딜레이
        } else {
            300L // 일반: 300ms 딜레이
        }

        handler.postDelayed(pendingOverlayJob!!, delay)
        Log.d(TAG, "Overlay job scheduled with ${delay}ms delay (timeSinceStart: ${timeSinceStart}ms)")
    }

    private fun cancelPendingOverlay() {
        pendingOverlayJob?.let {
            handler.removeCallbacks(it)
            Log.d(TAG, "Cancelled pending overlay job")
            pendingOverlayJob = null
        }
    }

    private fun isTargetAppPlayingMedia(): Boolean {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return false

            // 현재 음악이 재생 중인지 확인 (YouTube, TikTok)
            val isMusicActive = audioManager.isMusicActive

            // 미디어 모드 확인
            val mode = audioManager.mode

            Log.d(TAG, "Audio state - isMusicActive: $isMusicActive, mode: $mode")

            // 음악이 재생 중이면 확실히 재생 중
            if (isMusicActive) {
                Log.d(TAG, "Media is playing (music active)")
                return true
            }

            // 음악이 재생 중이 아니지만 모드가 NORMAL이 아니면 재생 중일 가능성
            // MODE_NORMAL = 0, MODE_IN_CALL = 2, MODE_IN_COMMUNICATION = 3
            if (mode != AudioManager.MODE_NORMAL) {
                Log.d(TAG, "Media might be playing (mode: $mode)")
                return true
            }

            Log.d(TAG, "No media playing")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking media state", e)
            // 에러 시 안전하게 true 반환 (pause 시도)
            return true
        }
    }

    private fun pauseMedia(packageName: String) {
        try {
            Log.d(TAG, "pauseMedia called for $packageName")

            // 빈 패키지명이면 무조건 pause 시도
            if (packageName.isEmpty()) {
                Log.d(TAG, "Empty package name - always attempting pause")
                performTapGesture()
                return
            }

            // overlayWasShown이 false이면 첫 감지 → 무조건 pause 시도
            // (앱 열자마자 Shorts가 켜진 경우 대응)
            if (!overlayWasShown) {
                Log.d(TAG, "First detection - attempting pause regardless of audio state")
                performTapGesture()
                return
            }

            // 이후부터는 미디어가 실제로 재생 중인지 확인
            val isPlaying = isTargetAppPlayingMedia()
            Log.d(TAG, "Media state check - isPlaying: $isPlaying")

            if (!isPlaying) {
                Log.d(TAG, "Media not playing, skipping tap gesture to avoid resume")
                return
            }

            Log.d(TAG, "Media is playing, sending tap gesture to pause")
            performTapGesture()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media", e)
        }
    }

    private fun performTapGesture() {
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
    }

    private fun performGlobalBackAction() {
        try {
            Log.d(TAG, "Performing global back action")
            val backPerformed = performGlobalAction(GLOBAL_ACTION_BACK)
            if (backPerformed) {
                Log.d(TAG, "Back action completed successfully")
            } else {
                Log.w(TAG, "Back action failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing back action", e)
        }
    }

    private fun resumeMedia() {
        try {
            Log.d(TAG, "Resuming media playback")

            // 미디어가 이미 재생 중인지 확인
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && audioManager.isMusicActive) {
                Log.d(TAG, "Media already playing, skipping tap")
                return
            }

            // 미디어가 일시정지 상태면 탭하여 재생
            Log.d(TAG, "Media paused, sending tap gesture to resume")
            performTapGesture()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming media", e)
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

        // Clear stale session data on service start/restart
        clearStaleSessionData()

        // Register broadcast receiver for timer events
        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AppConstants.ACTION_TIMER_COMPLETED -> {
                        val sessionId = intent.getStringExtra("session_id") ?: ""
                        Log.d(TAG, "Timer completed broadcast received for session: $sessionId")

                        // Update overlay buttons if it's showing and session matches
                        if (blockOverlay?.isShowing() == true && sessionId == currentSessionId) {
                            Log.d(TAG, "Updating overlay buttons for completed timer")
                            blockOverlay?.updateButtonVisibility()
                        }
                    }
                    AppConstants.ACTION_TIMER_CANCELLED -> {
                        Log.d(TAG, "Timer cancelled broadcast received")
                    }
                    AppConstants.ACTION_CLOSE_OVERLAY -> {
                        Log.d(TAG, "Close overlay broadcast received - dismissing and returning to app")
                        // Dismiss overlay
                        blockOverlay?.dismiss()
                        blockOverlay = null
                        currentSessionId = ""

                        // Simulate back key press to return to original app
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            Log.d(TAG, "Back key pressed")
                        }, 100)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AppConstants.ACTION_TIMER_COMPLETED)
            addAction(AppConstants.ACTION_TIMER_CANCELLED)
            addAction(AppConstants.ACTION_CLOSE_OVERLAY)
        }

        // Register BroadcastReceiver with proper error handling
        try {
            // Android 13+ requires explicit export flag for BroadcastReceivers
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(timerReceiver, filter)
            }
            Log.d(TAG, "BroadcastReceiver registered for timer events")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register BroadcastReceiver - will use fallback mechanism", e)
            // Fallback: overlay will check SharedPreferences directly via periodic checks
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error registering BroadcastReceiver", e)
        }
    }

    private fun startForegroundCheck() {
        stopForegroundCheck()  // 기존 체크 중지

        foregroundCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    // 현재 포그라운드 앱 확인
                    val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()

                    if (currentForegroundPackage != null && currentForegroundPackage !in TARGET_APPS) {
                        Log.d(TAG, "Foreground check: switched to $currentForegroundPackage, dismissing overlay")
                        blockOverlay?.dismiss()
                        blockOverlay = null
                        leftViaHomeButton = true
                        stopForegroundCheck()
                        return
                    }

                    // 계속 체크
                    handler.postDelayed(this, 500)  // 500ms마다 체크
                } catch (e: Exception) {
                    Log.e(TAG, "Error in foreground check", e)
                }
            }
        }

        handler.post(foregroundCheckRunnable!!)
        Log.d(TAG, "Started foreground check")
    }

    private fun stopForegroundCheck() {
        foregroundCheckRunnable?.let {
            handler.removeCallbacks(it)
            foregroundCheckRunnable = null
            Log.d(TAG, "Stopped foreground check")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPendingOverlay()
        stopForegroundCheck()
        blockOverlay?.dismiss()
        blockOverlay = null

        // Unregister broadcast receiver
        timerReceiver?.let {
            unregisterReceiver(it)
            timerReceiver = null
        }
    }

    /**
     * Send broadcast to force close TimerActivity
     */
    private fun sendTimerForceClose() {
        try {
            val intent = Intent(AppConstants.ACTION_TIMER_FORCE_CLOSE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sendBroadcast(intent)
            } else {
                sendBroadcast(intent)
            }
            Log.d(TAG, "Sent TIMER_FORCE_CLOSE broadcast")

            // 세션 ID 초기화
            currentSessionId = ""
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().remove(AppConstants.PREF_CURRENT_SESSION_ID).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending TIMER_FORCE_CLOSE broadcast", e)
        }
    }

    /**
     * Generate a session ID based on package name and date
     * This ensures the same session ID for the same app on the same day
     */
    private fun generateSessionId(packageName: String, contentHash: Int = 0): String {
        // Use only content hash for session ID - this way all shorts get blocked
        val input = contentHash.toString()

        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(input.toByteArray())
            // Convert to hex string (first 16 bytes for shorter ID)
            val sessionId = hashBytes.take(16).joinToString("") { "%02x".format(it) }
            Log.d(TAG, "Generated session ID for content hash '$contentHash': $sessionId")
            sessionId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate hash-based session ID", e)
            // Fallback to UUID if hashing fails
            UUID.randomUUID().toString()
        }
    }

    /**
     * Clear stale session data on service restart
     */
    private fun clearStaleSessionData() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Clear all session-related data
        prefs.edit().apply {
            remove(AppConstants.PREF_CURRENT_SESSION_ID)
            remove(AppConstants.PREF_COMPLETED_SESSION_ID)
            remove("session_created_time")
            apply()
        }

        // Reset in-memory session state
        currentSessionId = ""
        allowedUntilScroll = false
        overlayWasShown = false
        leftViaHomeButton = false

        Log.d(TAG, "Cleared all session data on service restart")
    }

    /**
     * Check if user has given valid privacy consent
     * @return true if consent version is current, false otherwise
     */
    private fun hasValidPrivacyConsent(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedVersion = prefs.getInt(PrivacyPolicy.PREF_CONSENT_VERSION, 0)
        return savedVersion >= PrivacyPolicy.CURRENT_VERSION
    }
}
