package com.muuu.unshort

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.muuu.shortblock.service.blocking.*

/**
 * 쇼츠 차단 AccessibilityService
 *
 * 책임:
 * - AccessibilityEvent 수신 및 라우팅
 * - 차단 활성화 상태 확인
 * - 포그라운드 앱 변경 감지
 * - 미디어 컨트롤 (일시정지/재생)
 * - 글로벌 액션 수행 (뒤로 가기)
 */
class ShortsBlockService : AccessibilityService() {

    private val TAG = "ShortsBlockService"

    // 새로운 컴포넌트들
    private val detectionEngine = ShortsDetectionEngine()
    private val hashGenerator = ContentHashGenerator()
    private val scrollManager = ScrollDetectionManager()
    private lateinit var overlayManager: OverlayManager

    // 포그라운드 앱 추적
    private var lastForegroundPackage: String = ""
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var foregroundCheckRunnable: Runnable? = null
    private var pendingOverlayJob: Runnable? = null

    // Fresh start 감지용
    private var appStartTime: Long = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // OverlayManager 초기화
        overlayManager = OverlayManager(
            context = this,
            onTimerCompleted = {
                scrollManager.onTimerCompleted()
                stopForegroundCheck()
            },
            onSkip = {
                // "안볼래요" 버튼 - 뒤로 가기
                Log.d(TAG, "Skip button pressed - performing back action")
                scrollManager.clearSession()

                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().apply {
                    remove(AppConstants.PREF_COMPLETED_SESSION_ID)
                    remove("allowed_until_scroll")
                    apply()
                }

                performGlobalBackAction()
            },
            onWatch = {
                // "볼래요" 버튼 - 미디어 재생 재개
                Log.d(TAG, "Watch button pressed - resuming media")
                scrollManager.onTimerCompleted()
                stopForegroundCheck()

                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("allowed_until_scroll", true).apply()

                handler.postDelayed({
                    resumeMedia()
                }, 100)
            }
        )
        overlayManager.initialize()

        // 서비스 재시작 시 이전 세션 데이터 클리어
        clearStaleSessionData()

        // Persisted allowed state 복원
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val persistedAllowedUntilScroll = prefs.getBoolean("allowed_until_scroll", false)
        if (persistedAllowedUntilScroll) {
            Log.d(TAG, "Restoring allowed state from SharedPreferences")
            scrollManager.onTimerCompleted()  // 복원
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 차단 활성화 상태 확인
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBlockingEnabled = prefs.getBoolean("blocking_enabled", true)

        if (!isBlockingEnabled) {
            // 차단 비활성화 시 오버레이 제거
            if (overlayManager.isOverlayVisible()) {
                cancelPendingOverlay()
                overlayManager.hideOverlay()
            }
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // 이벤트 로깅
        Log.d(TAG, "=== Event: ${event.eventType}, Package: $packageName ===")
        Log.d(TAG, "State - allowedUntilScroll: ${scrollManager.isAllowedUntilScroll()}, overlayShown: ${scrollManager.isOverlayShown()}")

        // 오버레이 표시 중 포그라운드 앱 변경 감지
        if (overlayManager.isOverlayVisible()) {
            val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()

            if (currentForegroundPackage != null && currentForegroundPackage != lastForegroundPackage) {
                lastForegroundPackage = currentForegroundPackage

                // 차단 대상 앱이 아닌 앱으로 전환
                if (currentForegroundPackage !in AppBlockingRegistry.TARGET_PACKAGES) {
                    Log.d(TAG, "Foreground changed to $currentForegroundPackage, dismissing overlay")
                    handleLeavingTargetApp(prefs)
                    return
                }
            }
        }

        // TYPE_WINDOW_STATE_CHANGED 이벤트로 앱 전환 감지
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Window state changed: $packageName")

            if (packageName !in AppBlockingRegistry.TARGET_PACKAGES && overlayManager.isOverlayVisible()) {
                Log.d(TAG, "Left target app to $packageName, dismissing overlay")
                handleLeavingTargetApp(prefs)
                return
            }
        }

        // 차단 대상 앱이 아니면 무시
        if (packageName !in AppBlockingRegistry.TARGET_PACKAGES) {
            return
        }

        // 앱 설정 가져오기
        val appConfig = AppBlockingRegistry.getConfigByPackageName(packageName) ?: return
        val rootNode = rootInActiveWindow ?: return

        // 쇼츠 화면 감지
        val isInShortsScreen = detectionEngine.detectShortsScreen(rootNode, appConfig)
        Log.d(TAG, ">>> isShorts = $isInShortsScreen")

        // 콘텐츠 해시 기반 스크롤 감지
        if (isInShortsScreen && scrollManager.isAllowedUntilScroll()) {
            val currentContentHash = hashGenerator.generateContentHash(rootNode, appConfig.hashConfig)

            if (currentContentHash != 0) {
                val scrollDetected = scrollManager.onContentHashChanged(currentContentHash)

                if (scrollDetected) {
                    Log.d(TAG, "Scroll detected - clearing session and showing overlay")

                    // 세션 데이터 클리어
                    prefs.edit().apply {
                        remove(AppConstants.PREF_COMPLETED_SESSION_ID)
                        remove(AppConstants.PREF_CURRENT_SESSION_ID)
                        remove("allowed_until_scroll")
                        apply()
                    }

                    // 오버레이 표시
                    if (!overlayManager.isOverlayVisible() && scrollManager.shouldShowOverlay(isInShortsScreen)) {
                        showBlockOverlay(packageName)
                        scrollManager.markOverlayShown()
                    }
                    return
                }
            } else {
                Log.w(TAG, "Hash generation failed - keeping current state")
            }
        }

        if (isInShortsScreen) {
            // 쇼츠 화면 진입 감지
            val justEntered = scrollManager.checkShortsScreenEntry(isInShortsScreen)
            if (justEntered) {
                Log.d(TAG, "Just entered shorts screen")
                appStartTime = System.currentTimeMillis()
            }

            // 허용 상태면 차단하지 않음
            if (scrollManager.isAllowedUntilScroll()) {
                Log.d(TAG, "Allowed to view current shorts")
                return
            }

            Log.d(TAG, "Checking overlay display conditions:")
            Log.d(TAG, "  - overlayVisible: ${overlayManager.isOverlayVisible()}")
            Log.d(TAG, "  - should show: ${scrollManager.shouldShowOverlay(isInShortsScreen)}")

            // 오버레이 표시 조건 확인
            if (scrollManager.shouldShowOverlay(isInShortsScreen)) {
                Log.d(TAG, "Shorts screen detected in $packageName")
                showBlockOverlay(packageName)
                scrollManager.markOverlayShown()
            } else {
                Log.d(TAG, "Not showing overlay - conditions not met")
            }
        } else {
            // 쇼츠 화면 이탈
            val justExited = scrollManager.checkShortsScreenExit(isInShortsScreen)
            if (justExited) {
                Log.d(TAG, "Just exited shorts screen")
                cancelPendingOverlay()

                if (overlayManager.isOverlayVisible()) {
                    Log.d(TAG, "Dismissing overlay - left shorts screen")
                    overlayManager.hideOverlay()
                    scrollManager.onExitShortsScreen()

                    // 외부 앱으로 전환되었으면 TimerActivity 종료
                    val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()
                    if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
                        Log.d(TAG, "Left to external app ($currentForegroundPackage), closing TimerActivity")
                        sendTimerForceClose()
                    }
                }
            }
        }
    }

    /**
     * 차단 대상 앱을 벗어났을 때 처리
     */
    private fun handleLeavingTargetApp(prefs: android.content.SharedPreferences) {
        cancelPendingOverlay()
        overlayManager.hideOverlay()
        scrollManager.clearSession()

        // SharedPreferences 클리어
        prefs.edit().apply {
            remove(AppConstants.PREF_COMPLETED_SESSION_ID)
            remove("allowed_until_scroll")
            apply()
        }
        Log.d(TAG, "Cleared all state - left shorts app")
    }

    /**
     * 오버레이 표시
     */
    private fun showBlockOverlay(packageName: String) {
        Log.d(TAG, "showBlockOverlay() called for $packageName")

        // 허용 상태면 오버레이 표시하지 않음
        if (scrollManager.isAllowedUntilScroll()) {
            Log.d(TAG, "Allowed until scroll - skipping overlay")
            return
        }

        // 이미 오버레이가 표시 중이면 무시
        if (overlayManager.isOverlayVisible()) {
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

        // 딜레이 후 오버레이 표시
        pendingOverlayJob = Runnable {
            try {
                Log.d(TAG, "Executing pending overlay job for $packageName")

                // 세션 ID 생성
                val sessionId = java.util.UUID.randomUUID().toString()
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().putString(AppConstants.PREF_CURRENT_SESSION_ID, sessionId).apply()
                Log.d(TAG, "Session created: $sessionId")

                // 미디어 일시정지
                pauseMedia(packageName)

                // 오버레이 표시
                overlayManager.showOverlay(packageName, sessionId)

                // 포그라운드 체크 시작
                startForegroundCheck()

                pendingOverlayJob = null
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
                pendingOverlayJob = null
            }
        }

        // Fresh start 후 500ms 이내면 500ms 딜레이, 이후는 300ms
        val timeSinceStart = System.currentTimeMillis() - appStartTime
        val delay = if (timeSinceStart < 500) 500L else 300L

        handler.postDelayed(pendingOverlayJob!!, delay)
        Log.d(TAG, "Overlay job scheduled with ${delay}ms delay")
    }

    /**
     * Pending 오버레이 취소
     */
    private fun cancelPendingOverlay() {
        pendingOverlayJob?.let {
            handler.removeCallbacks(it)
            Log.d(TAG, "Cancelled pending overlay job")
            pendingOverlayJob = null
        }
    }

    /**
     * 포그라운드 체크 시작
     */
    private fun startForegroundCheck() {
        stopForegroundCheck()

        foregroundCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()

                    if (currentForegroundPackage != null && currentForegroundPackage !in AppBlockingRegistry.TARGET_PACKAGES) {
                        Log.d(TAG, "Foreground check: switched to $currentForegroundPackage, dismissing overlay")
                        overlayManager.hideOverlay()
                        scrollManager.clearSession()
                        stopForegroundCheck()
                        return
                    }

                    handler.postDelayed(this, 500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in foreground check", e)
                }
            }
        }

        handler.post(foregroundCheckRunnable!!)
        Log.d(TAG, "Started foreground check")
    }

    /**
     * 포그라운드 체크 중지
     */
    private fun stopForegroundCheck() {
        foregroundCheckRunnable?.let {
            handler.removeCallbacks(it)
            foregroundCheckRunnable = null
            Log.d(TAG, "Stopped foreground check")
        }
    }

    /**
     * 미디어 일시정지
     */
    private fun pauseMedia(packageName: String) {
        try {
            Log.d(TAG, "pauseMedia called for $packageName")

            // 첫 감지 시 무조건 pause
            if (!scrollManager.isOverlayShown()) {
                Log.d(TAG, "First detection - attempting pause")
                performTapGesture()
                return
            }

            // 미디어 재생 중인지 확인
            val isPlaying = isTargetAppPlayingMedia()
            Log.d(TAG, "Media state check - isPlaying: $isPlaying")

            if (!isPlaying) {
                Log.d(TAG, "Media not playing, skipping tap gesture")
                return
            }

            Log.d(TAG, "Media is playing, sending tap gesture to pause")
            performTapGesture()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media", e)
        }
    }

    /**
     * 미디어 재생 중인지 확인
     */
    private fun isTargetAppPlayingMedia(): Boolean {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return false
            val isMusicActive = audioManager.isMusicActive
            val mode = audioManager.mode

            Log.d(TAG, "Audio state - isMusicActive: $isMusicActive, mode: $mode")

            if (isMusicActive) {
                Log.d(TAG, "Media is playing (music active)")
                return true
            }

            if (mode != AudioManager.MODE_NORMAL) {
                Log.d(TAG, "Media might be playing (mode: $mode)")
                return true
            }

            Log.d(TAG, "No media playing")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking media state", e)
            return true  // 에러 시 안전하게 true 반환
        }
    }

    /**
     * 미디어 재생 재개
     */
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

    /**
     * 화면 중앙 탭 제스처 수행
     */
    private fun performTapGesture() {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val centerY = displayMetrics.heightPixels / 2f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.accessibilityservice.GestureDescription.StrokeDescription(
                android.graphics.Path().apply {
                    moveTo(centerX, centerY)
                    lineTo(centerX, centerY)
                },
                0,
                100
            )
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(path)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Tap gesture completed")
                }

                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Tap gesture cancelled")
                }
            }, null)
        }
    }

    /**
     * 뒤로 가기 액션 수행
     */
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

    /**
     * 오버레이 권한 요청
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * TimerActivity 강제 종료 브로드캐스트 전송
     */
    private fun sendTimerForceClose() {
        try {
            val intent = Intent(AppConstants.ACTION_TIMER_FORCE_CLOSE)
            sendBroadcast(intent)
            Log.d(TAG, "Sent TIMER_FORCE_CLOSE broadcast")

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().remove(AppConstants.PREF_CURRENT_SESSION_ID).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending TIMER_FORCE_CLOSE broadcast", e)
        }
    }

    /**
     * 오래된 세션 데이터 클리어
     */
    private fun clearStaleSessionData() {
        Log.d(TAG, "Clearing stale session data on service restart")

        scrollManager.clearSession()
        appStartTime = 0

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            remove(AppConstants.PREF_CURRENT_SESSION_ID)
            remove(AppConstants.PREF_COMPLETED_SESSION_ID)
            remove("session_created_time")
            remove("allowed_until_scroll")
            apply()
        }

        Log.d(TAG, "Stale session data cleared")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPendingOverlay()
        stopForegroundCheck()
        overlayManager.cleanup()
        Log.d(TAG, "Service destroyed")
    }
}
