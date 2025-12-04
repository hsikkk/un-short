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
import com.muuu.shortblock.service.blocking.*
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.data.statistics.StatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

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
    private lateinit var sessionState: SessionStateManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var statisticsRepository: StatisticsRepository

    // 현재 처리 중인 패키지
    private var currentPackage: String = ""

    // 앱별 볼륨 저장
    private val savedVolumeByPackage = mutableMapOf<String, Int>()

    // 포그라운드 앱 추적
    private var lastForegroundPackage: String = ""
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var foregroundCheckRunnable: Runnable? = null
    private var pendingOverlayJob: Runnable? = null

    // Fresh start 감지용
    private var appStartTime: Long = 0

    // 타이머 완료 상태 추적 (통계 기록용)
    private var currentTimerCompleted: Boolean = false

    // 현재 세션이 스크롤 후 재진입인지 추적 (통계 기록용)
    private var isCurrentSessionFromScroll: Boolean = false

    // Screen state receiver
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // Managers 초기화
        prefsManager = PreferencesManager(this)
        statisticsRepository = StatisticsRepository(this)
        sessionState = SessionStateManager(
            context = this,
            isBlockingEnabled = { prefsManager.isBlockingEnabled },
            onSessionEnd = { info -> recordSessionFromStateTransition(info) }
        )

        // OverlayManager 초기화
        overlayManager = OverlayManager(
            context = this,
            onTimerCompleted = {
                // 타이머 완료 플래그 설정 (통계 기록용)
                currentTimerCompleted = true

                // 타이머 완료 → 확인 필요 상태로 전이
                sessionState.handleEvent(SessionEvent.TimerCompleted, currentPackage)
                stopForegroundCheck()
            },
            onSkip = {
                // "안볼래요" 버튼 - 뒤로 가기
                Log.d(TAG, "Skip button pressed - performing back action")

                restoreVolume(currentPackage)
                sessionState.handleEvent(SessionEvent.SkipConfirmed, currentPackage)

                prefsManager.clearCompletedSessionId()
                prefsManager.clearAllowedUntilScroll()

                performGlobalBackAction()
            },
            onWatch = {
                // "볼래요" 버튼 - 시청 허용 상태로 전이
                Log.d(TAG, "Watch button pressed - allowing watch")

                sessionState.handleEvent(SessionEvent.WatchConfirmed, currentPackage)
                stopForegroundCheck()

                prefsManager.isAllowedUntilScroll = true

                handler.postDelayed({
                    restoreVolume(currentPackage)
                    resumeMedia()
                }, 100)
            }
        )
        overlayManager.initialize()

        // 서비스 재시작 시 이전 세션 데이터 클리어
        clearStaleSessionData()

        // Persisted allowed state 복원
        if (prefsManager.isAllowedUntilScroll) {
            Log.d(TAG, "Restoring allowed state from SharedPreferences")
            sessionState.handleEvent(SessionEvent.TimerCompleted, packageName)  // 복원
        }

        // 30일 이전 통계 데이터 정리
        statisticsRepository.cleanOldData()

        // Screen state receiver 등록
        registerScreenStateReceiver()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        currentPackage = packageName  // 현재 패키지 저장

        // 차단 비활성화 시 오버레이만 제거하고 세션 추적은 계속
        if (!prefsManager.isBlockingEnabled) {
            if (overlayManager.isOverlayVisible(packageName)) {
                cancelPendingOverlay()
                overlayManager.hideOverlay(packageName)
            }
            // return 제거 - 세션 추적 계속 진행
        }

        // 이벤트 로깅
        Log.d(TAG, "=== Event: ${event.eventType}, Package: $packageName ===")
        Log.d(TAG, "Current state: ${sessionState.getCurrentState(packageName)}")

        // 오버레이 표시 중 포그라운드 앱 변경 감지
        if (overlayManager.isOverlayVisible(packageName)) {
            val currentForegroundPackage = rootInActiveWindow?.packageName?.toString()

            if (currentForegroundPackage != null && currentForegroundPackage != lastForegroundPackage) {
                lastForegroundPackage = currentForegroundPackage

                // 차단 대상 앱이 아닌 앱으로 전환
                if (currentForegroundPackage !in AppBlockingRegistry.TARGET_PACKAGES) {
                    Log.d(TAG, "Foreground changed to $currentForegroundPackage, dismissing overlay")
                    handleLeavingTargetApp(currentForegroundPackage)
                    return
                }
            }
        }

        // TYPE_WINDOW_STATE_CHANGED 이벤트로 앱 전환 감지
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Window state changed: $packageName")

            if (packageName !in AppBlockingRegistry.TARGET_PACKAGES && overlayManager.isOverlayVisible(packageName)) {
                Log.d(TAG, "Left target app to $packageName, dismissing overlay")
                handleLeavingTargetApp(packageName)
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
        val currentState = sessionState.getCurrentState(packageName)
        Log.d(TAG, ">>> isShorts=$isInShortsScreen, currentState=$currentState")

        // 상태 전이 처리
        handleStateTransitions(isInShortsScreen, packageName, rootNode, appConfig)

        // 상태에 따른 액션 수행
        handleStateActions(packageName)
    }

    /**
     * 상태 전이 처리
     */
    private fun handleStateTransitions(
        isInShortsScreen: Boolean,
        packageName: String,
        rootNode: AccessibilityNodeInfo,
        appConfig: AppBlockingConfig
    ) {
        val currentState = sessionState.getCurrentState(packageName)

        when (currentState) {
            // IDLE 상태
            ShortsSessionState.IDLE -> {
                if (isInShortsScreen) {
                    sessionState.handleEvent(SessionEvent.EnterShorts, packageName)
                    appStartTime = System.currentTimeMillis()
                }
            }

            // Foreground 쇼츠 상태
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER,
            ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION,
            ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL -> {
                if (!isInShortsScreen) {
                    // 쇼츠 이탈 - 어디로 갔는지 확인
                    val foregroundPkg = rootInActiveWindow?.packageName?.toString()

                    when {
                        // 같은 차단 대상 앱 내 다른 화면
                        foregroundPkg == packageName -> {
                            Log.d(TAG, "Exited shorts within same app")
                            sessionState.handleEvent(SessionEvent.ExitShorts, packageName)
                        }
                        // 다른 앱 = Background 전환
                        else -> {
                            Log.d(TAG, "Exited shorts to other app/home - entering background")
                            sessionState.handleEvent(SessionEvent.EnterBackground, packageName)
                        }
                    }

                    cancelPendingOverlay()
                    restoreVolume(packageName)

                    overlayManager.hideOverlay(packageName)
                } else {
                    // 쇼츠 화면 내에서 콘텐츠 변화 감지
                    Log.d(TAG, "Still in shorts, state=$currentState, checking for scroll...")

                    // 스크롤 감지 (ALLOWED 상태에서만)
                    if (currentState == ShortsSessionState.IN_SHORTS_ALLOWED_UNTIL_SCROLL) {
                        Log.d(TAG, "ALLOWED state - generating content hash")
                        val hash = hashGenerator.generateContentHash(rootNode, appConfig.hashConfig)
                        Log.d(TAG, "Generated hash: $hash")

                        if (hash != 0) {
                            Log.d(TAG, "Sending ContentHashChanged event")
                            sessionState.handleEvent(SessionEvent.ContentHashChanged(hash), packageName)

                            // 상태가 변경되었으면 스크롤 발생
                            val newState = sessionState.getCurrentState(packageName)
                            Log.d(TAG, "State after hash event: $newState")

                            if (newState == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_CONFIRMATION) {
                                Log.d(TAG, "Scroll detected - clearing state")

                                prefsManager.clearCompletedSessionId()
                                prefsManager.clearAllowedUntilScroll()
                            }
                        } else {
                            Log.w(TAG, "Hash is 0 - skipping")
                        }
                    } else {
                        Log.d(TAG, "Not in ALLOWED state ($currentState) - skipping scroll detection")
                    }
                }
            }

            // Background 상태에서 쇼츠 복귀
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_TIMER,
            ShortsSessionState.BACKGROUND_BLOCKED_NEED_CONFIRMATION,
            ShortsSessionState.BACKGROUND_ALLOWED_UNTIL_SCROLL -> {
                if (isInShortsScreen) {
                    sessionState.handleEvent(SessionEvent.ReturnToShorts, packageName)
                }
            }
        }
    }

    /**
     * 상태에 따른 액션 수행
     */
    private fun handleStateActions(packageName: String) {
        val overlayType = sessionState.getOverlayType(packageName)

        // 차단이 활성화되어 있고, 오버레이가 필요한 경우에만 표시
        if (prefsManager.isBlockingEnabled && overlayType != null && !overlayManager.isOverlayVisible(packageName)) {
            // 오버레이 표시 필요
            Log.d(TAG, "State requires overlay: $overlayType")
            showBlockOverlay(packageName, overlayType)
        } else if (!prefsManager.isBlockingEnabled) {
            Log.d(TAG, "Blocking disabled - skipping overlay display, session tracking continues")
        }
    }

    /**
     * 차단 대상 앱을 벗어났을 때 처리
     *
     * @param targetPackageName 이동한 대상 앱의 패키지명 (null이면 알 수 없음)
     */
    private fun handleLeavingTargetApp(targetPackageName: String? = null) {
        cancelPendingOverlay()
        overlayManager.hideOverlay(packageName)
        restoreVolume(packageName)

        // 우리 앱으로 전환된 경우 체크 (세션 기록은 SessionStateManager에서 처리)
        if (targetPackageName == this.packageName) {
            Log.d(TAG, "Switched to our app")
        }

        sessionState.handleEvent(SessionEvent.Reset, packageName)

        // SharedPreferences 클리어
        prefsManager.clearCompletedSessionId()
        prefsManager.clearAllowedUntilScroll()

        Log.d(TAG, "Cleared all state - left shorts app")
    }

    /**
     * 오버레이 표시
     */
    private fun showBlockOverlay(packageName: String, overlayType: OverlayType) {
        Log.d(TAG, "showBlockOverlay() called for $packageName with type: $overlayType")

        // 이미 pending job이 있으면 스킵 (중복 스케줄 방지)
        if (pendingOverlayJob != null) {
            Log.d(TAG, "Overlay job already pending - skipping duplicate schedule")
            return
        }

        // 이미 오버레이가 표시 중이면 스킵
        if (overlayManager.isOverlayVisible(packageName)) {
            Log.d(TAG, "Overlay already visible - skipping")
            return
        }

        // Activity 방식으로 변경되어 SYSTEM_ALERT_WINDOW 권한 불필요
        // (이전에는 WindowManager 오버레이를 사용했으나, 이제는 Activity를 사용)

        Log.d(TAG, "Scheduling activity launch with delay")

        val prevState = sessionState.getPreviousState(packageName)

        if (!prevState.isBackground) {
            saveAndMuteVolume(packageName)
        }

        // 딜레이 후 오버레이 표시
        pendingOverlayJob = Runnable {
            try {
                Log.d(TAG, "Executing pending overlay job for $packageName")

                // 현재 세션이 스크롤 후 재진입인지 확인 (통계 기록용)
                isCurrentSessionFromScroll = prefsManager.isAllowedUntilScroll
                Log.d(TAG, "Session from scroll: $isCurrentSessionFromScroll")

                // 세션 ID 생성
                val sessionId = java.util.UUID.randomUUID().toString()
                prefsManager.currentSessionId = sessionId
                Log.d(TAG, "Session created: $sessionId")

                if (!prevState.isBackground) {
                    Log.d(TAG, "First entry (from $prevState) - muting volume and attempting pauseMedia")
                    pauseMedia(packageName)
                } else {
                    Log.d(TAG, "Returned from background ($prevState) - skipping volume/pauseMedia")
                }

                // 오버레이 표시 (타입은 파라미터로 전달됨)
                overlayManager.showOverlay(packageName, sessionId, overlayType)

                // 포그라운드 체크 시작
                startForegroundCheck()

                pendingOverlayJob = null
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
                pendingOverlayJob = null

                restoreVolume(packageName)
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

                    if (currentForegroundPackage != null) {
                        when {
                            // 차단 대상 앱으로 전환 (쇼츠 복귀 가능)
                            currentForegroundPackage in AppBlockingRegistry.TARGET_PACKAGES -> {
                                // 계속 체크 (쇼츠 복귀 감지는 handleStateTransitions에서 처리)
                            }
                            // 우리 앱 (TimerActivity 등) - Background 상태로 전이
                            currentForegroundPackage == packageName -> {
                                Log.d(TAG, "Switched to our app ($currentForegroundPackage) - entering background")
                                sessionState.handleEvent(SessionEvent.EnterBackground, packageName)
                                overlayManager.hideOverlay(packageName)
                            }
                            // 다른 앱 - Background 상태로 전이
                            else -> {
                                Log.d(TAG, "Switched to other app ($currentForegroundPackage) - entering background")
                                sessionState.handleEvent(SessionEvent.EnterBackground, packageName)
                                overlayManager.hideOverlay(packageName)
                                stopForegroundCheck()
                                return
                            }
                        }
                    }

                    handler.postDelayed(this, 500)
                } catch (e: Exception) {
                    Log.d(TAG, "Error in foreground check", e)
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

            val state = sessionState.getCurrentState(packageName)
            val prevState = sessionState.getPreviousState(packageName)

            // Background 복귀면 스킵 (이미 일시정지 상태)
            if (prevState.isBackground) {
                Log.d(TAG, "From background ($prevState) - skipping pause")
                return
            }

            // 첫 진입(NEED_TIMER)일 때만 pause 시도
            if (state == ShortsSessionState.IN_SHORTS_BLOCKED_NEED_TIMER) {
                val isPlaying = isTargetAppPlayingMedia()
                Log.d(TAG, "NEED_TIMER state, playing=$isPlaying")

                if (isPlaying) {
                    Log.d(TAG, "Media playing - pausing")
                    performTapGesture()
                } else {
                    Log.d(TAG, "Media already paused - skipping tap")
                }
            } else {
                Log.d(TAG, "Not NEED_TIMER state ($state) - skipping pause")
            }
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
     * 오버레이 권한 요청 (더 이상 사용하지 않음 - Activity 방식으로 변경)
     */
    @Deprecated("Activity 방식으로 변경되어 더 이상 필요 없음")
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    
    /**
     * 오래된 세션 데이터 클리어
     */
    private fun clearStaleSessionData() {
        Log.d(TAG, "Clearing stale session data on service restart")

        sessionState.handleEvent(SessionEvent.Reset, packageName)
        appStartTime = 0

        prefsManager.clearSessionState()

        Log.d(TAG, "Stale session data cleared")
    }

    /**
     * 볼륨 저장 및 음소거
     */
    private fun saveAndMuteVolume(packageName: String) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
            audioManager?.let {
                val currentVolume = it.getStreamVolume(AudioManager.STREAM_MUSIC)
                savedVolumeByPackage[packageName] = currentVolume
                it.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                Log.d(TAG, "[$packageName] Volume saved ($currentVolume) and muted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$packageName] Error muting volume", e)
        }
    }

    /**
     * 볼륨 복원
     */
    private fun restoreVolume(packageName: String) {
        savedVolumeByPackage[packageName]?.let { savedVolume ->
            try {
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0)
                Log.d(TAG, "[$packageName] Volume restored to $savedVolume")
            } catch (e: Exception) {
                Log.e(TAG, "[$packageName] Error restoring volume", e)
            }
            savedVolumeByPackage.remove(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Screen state receiver 해제
        unregisterScreenStateReceiver()

        // 모든 저장된 볼륨 복원
        savedVolumeByPackage.keys.toList().forEach { pkg ->
            restoreVolume(pkg)
        }
        savedVolumeByPackage.clear()

        cancelPendingOverlay()
        stopForegroundCheck()
        overlayManager.cleanup()
        Log.d(TAG, "Service destroyed")
    }

    /**
     * Screen state receiver 등록
     */
    private fun registerScreenStateReceiver() {
        try {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            }
            registerReceiver(screenStateReceiver, filter)
            Log.d(TAG, "ScreenStateReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering ScreenStateReceiver", e)
        }
    }

    /**
     * Screen state receiver 해제
     */
    private fun unregisterScreenStateReceiver() {
        try {
            screenStateReceiver?.let {
                unregisterReceiver(it)
                screenStateReceiver = null
                Log.d(TAG, "ScreenStateReceiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering ScreenStateReceiver", e)
        }
    }

    /**
     * 화면 상태 변화 감지 BroadcastReceiver
     * - 화면 꺼짐 (ACTION_SCREEN_OFF)
     * - 화면 잠금 해제 (ACTION_USER_PRESENT)
     * - 홈 버튼/최근 앱 버튼 (ACTION_CLOSE_SYSTEM_DIALOGS)
     */
    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned off - hiding overlay")
                    handleScreenStateChange("SCREEN_OFF")
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Screen unlocked")
                    // 잠금 해제 시에는 오버레이를 숨기지 않음 (사용자가 다시 돌아온 것)
                }
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason")
                    Log.d(TAG, "System dialogs closed - reason: $reason")
                    when (reason) {
                        "homekey" -> {
                            Log.d(TAG, "Home button pressed - hiding overlay")
                            handleScreenStateChange("HOME_BUTTON")
                        }
                        "recentapps" -> {
                            Log.d(TAG, "Recent apps button pressed - hiding overlay")
                            handleScreenStateChange("RECENT_APPS")
                        }
                    }
                }
            }
        }

        private fun handleScreenStateChange(trigger: String) {
            // 오버레이가 표시 중일 때만 처리
            if (overlayManager.isOverlayVisible(currentPackage)) {
                Log.d(TAG, "[$trigger] Overlay visible - hiding and transitioning to background")

                // Pending overlay job 취소
                cancelPendingOverlay()

                // 오버레이 숨김
                overlayManager.hideOverlay(currentPackage)

                // 볼륨 복원
                restoreVolume(currentPackage)

                // Background 상태로 전이
                sessionState.handleEvent(SessionEvent.EnterBackground, currentPackage)

                // Foreground check 중지
                stopForegroundCheck()

                Log.d(TAG, "[$trigger] Overlay hidden and state transitioned to background")
            } else {
                Log.d(TAG, "[$trigger] No overlay visible - ignoring")
            }
        }
    }

    /**
     * 상태 전이로부터 세션 기록 (SessionStateManager 콜백)
     */
    private fun recordSessionFromStateTransition(info: SessionStateManager.SessionEndInfo) {
        val watchEnd = if (info.watchDurationMs != null && info.watchDurationMs > 0) {
            System.currentTimeMillis()
        } else {
            null
        }

        statisticsRepository.recordSession(
            packageName = info.packageName,
            didWatch = info.didWatch,
            timerCompleted = currentTimerCompleted,
            isScrollSession = isCurrentSessionFromScroll,
            watchStartTime = info.watchStartTime,
            watchEndTime = watchEnd,
            watchDurationMs = info.watchDurationMs
        )

        Log.d(TAG, "Session recorded: didWatch=${info.didWatch}, duration=${info.watchDurationMs}ms")

        currentTimerCompleted = false
        isCurrentSessionFromScroll = false
    }
}
