package com.muuu.unshort

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.muuu.unshort.service.blocking.AppBlockingConfig
import com.muuu.unshort.service.blocking.AppBlockingRegistry
import com.muuu.unshort.service.blocking.BlockingStage
import com.muuu.unshort.service.blocking.ContentHashGenerator
import com.muuu.unshort.service.blocking.OverlayManager
import com.muuu.unshort.service.blocking.SessionEvent
import com.muuu.unshort.service.blocking.SessionStateManager
import com.muuu.unshort.service.blocking.ShortsDetectionEngine
import com.muuu.unshort.service.blocking.ShortsLocation
import com.muuu.unshort.service.blocking.ShortsUsageMonitor
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.config.OverlayType
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.util.BlockingReminderNotifier
import java.util.UUID

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

    companion object {
        /**
         * Service instance (Activity에서 접근 가능)
         *
         * Service가 실행되지 않은 경우 null
         */
        var instance: ShortsBlockService? = null
            private set

        private const val MAX_AUTO_EXIT_ATTEMPTS = 5
        private const val AUTO_EXIT_FALLBACK_DELAY = 500L
        private const val AUTO_EXIT_COOLDOWN_MS = 2000L
    }

    private val TAG = "ShortsBlockService"

    // 새로운 컴포넌트들
    private val detectionEngine = ShortsDetectionEngine()
    private val hashGenerator = ContentHashGenerator()
    private lateinit var sessionState: SessionStateManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var statisticsRepository: StatisticsRepository
    private lateinit var usageMonitor: ShortsUsageMonitor
    private lateinit var reminderNotifier: BlockingReminderNotifier
    private lateinit var dailyLimitManager: com.muuu.unshort.service.dailylimit.DailyLimitManager
    private lateinit var dailyLimitNotifier: com.muuu.unshort.util.DailyLimitNotifier

    // 현재 처리 중인 패키지
    private var currentPackage: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var pendingOverlayJob: Runnable? = null

    /**
     * 액세서빌리티 트리 순회/해시 등 무거운 binder 호출을 처리하는 백그라운드 스레드
     *
     * 메인 스레드에서 처리 시 ANR 발생 (rootInActiveWindow + 재귀 트리 순회 = 수십 ms 이상)
     */
    private var processingThread: HandlerThread? = null
    private var processingHandler: Handler? = null

    private val PAUSE_MEDIA_TIMEOUT_MS = 1500L       // 미디어 정지 대기 최대 시간
    private val PAUSE_MEDIA_SETTLE_DELAY_MS = 200L   // 정지 확인 후 시스템 안정화 대기
    private val PAUSE_MEDIA_PRE_TAP_DELAY_MS = 500L  // 인디케이터 미정의 앱(Naver 등)에서 탭 전 대기 (UI 안정화)

    /**
     * 진행 중인 pause UI 확인 요청
     *
     * 탭 후 다음 AccessibilityEvent에서 pauseIndicator가 발견되면 onConfirmed() 콜백 호출
     */
    @Volatile
    private var pendingPauseConfirmation: PauseConfirmation? = null

    private data class PauseConfirmation(
        val packageName: String,
        val indicator: com.muuu.unshort.service.blocking.PauseIndicator,
        val onConfirmed: () -> Unit,
        val timeoutRunnable: Runnable
    )

    // Fresh start 감지용
    private var appStartTime: Long = 0

    // 타이머 완료 상태 추적 (통계 기록용)
    private var currentTimerCompleted: Boolean = false

    // 현재 세션이 스크롤 후 재진입인지 추적 (통계 기록용)
    private var isCurrentSessionFromScroll: Boolean = false

    // 차단 화면이 뜬 트리거 방식 (UI 분기용 — 통계용 isCurrentSessionFromScroll과 별개)
    // - true: 스크롤로 인한 차단 (ContentHashChanged → NEED_CONFIRMATION 전이)
    // - false: 외부 진입에 의한 차단 (EnterShorts → NEED_TIMER 전이) = 클릭 진입
    private var lastBlockOriginScroll: Boolean = false

    // Skip 후 쇼츠 완전 이탈까지 반복 back press 상태
    private var isAutoExitingShorts = false
    private var autoExitAttempts = 0
    private var autoExitPendingRunnable: Runnable? = null
    // Auto-exit 완료 후 쿨다운 (과도기 화면에서 false positive 방지)
    private var autoExitCooldownUntil: Long = 0

    // Screen state receiver
    private var screenStateReceiver: ScreenStateReceiver? = null

    // Overlay action receiver
    private var overlayActionReceiver: OverlayActionReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // 백그라운드 처리 스레드 시작 (ANR 방지: 트리 순회/해시 등 무거운 작업 전용)
        processingThread = HandlerThread("ShortsBlock-Processing").apply {
            start()
        }
        processingHandler = Handler(processingThread!!.looper)

        // Service instance 설정
        instance = this

        // Managers 초기화
        prefsManager = PreferencesManager(this)
        statisticsRepository = StatisticsRepository(this)

        // ReminderNotifier 초기화
        reminderNotifier = BlockingReminderNotifier(this)

        // UsageMonitor 초기화
        usageMonitor = ShortsUsageMonitor(
          context = this,
          prefsManager = prefsManager,
          onThresholdExceeded = {
            // 임계값 초과 시 알림 발송
            reminderNotifier.sendReminderNotification()
          }
        )

        // DailyLimit 초기화
        dailyLimitNotifier = com.muuu.unshort.util.DailyLimitNotifier(this)
        dailyLimitManager = com.muuu.unshort.service.dailylimit.DailyLimitManager(
            context = this,
            prefsManager = prefsManager,
            statisticsRepository = statisticsRepository,
            onWarningThreshold = { dailyLimitNotifier.sendWarningNotification() },
            onLimitExceeded = {
                prefsManager.isBlockingEnabled = true
                dailyLimitNotifier.sendLimitExceededNotification()
            },
            onMonitoringUpdate = { currentMs, limitMs ->
                dailyLimitNotifier.updateMonitoringNotification(currentMs, limitMs)
            }
        )

        // Daily limit reset 알람 스케줄
        if (prefsManager.isDailyLimitEnabled) {
            com.muuu.unshort.receiver.DailyLimitResetReceiver.scheduleReset(this)
        }

        // SessionStateManager 초기화 (usageMonitor 의존)
        sessionState = SessionStateManager(
          context = this,
          isBlockingEnabled = { prefsManager.isBlockingEnabled },
          onSessionEnd = { info -> recordSessionFromStateTransition(info) },
          onScrollDetected = { packageName ->
            // 스크롤 감지 시 연속 시청 체크
            usageMonitor.checkOnEnterShorts()
          }
        )

        // OverlayManager 초기화
        overlayManager = OverlayManager(
          context = this,
          onTimerCompleted = {
            // 타이머 완료 플래그 설정 (통계 기록용)
            currentTimerCompleted = true

            // Note: TimerCompleted event is now sent by TimerActivity directly
          }
        )
        overlayManager.initialize()

        // 서비스 재시작 시 이전 세션 데이터 클리어
        clearStaleSessionData()

        // 30일 이전 통계 데이터 정리
        statisticsRepository.cleanOldData()

        // Receivers 등록
        registerScreenStateReceiver()
        registerOverlayActionReceiver()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        currentPackage = packageName  // 현재 패키지 저장

        // 이벤트 객체는 메서드 반환 후 재활용되므로 필요한 정보만 추출 (event.eventType은 primitive)
        val eventType = event.eventType
        val eventTimestamp = System.currentTimeMillis()

        // 이벤트 상세 로깅 (뷰 변화 모니터링) - 메인 스레드에서 가벼운 정보만 추출
        logAccessibilityEventDetails(event, packageName)

        // 무거운 작업(rootInActiveWindow, 트리 순회, 해시)은 백그라운드 스레드로 위임
        val worker = processingHandler ?: return
        worker.post {
            processAccessibilityEvent(packageName, eventType, eventTimestamp)
        }
    }

    /**
     * 백그라운드 스레드에서 실행되는 액세서빌리티 이벤트 처리
     *
     * 메인 스레드 ANR 방지를 위해 다음 작업을 워커 스레드로 분리:
     * - rootInActiveWindow (binder call)
     * - detectionEngine.detectShortsScreen (재귀 트리 순회)
     * - hashGenerator.generateContentHash (재귀 트리 순회)
     * - sessionState.handleEvent (@Synchronized로 보호됨)
     */
    private fun processAccessibilityEvent(
        packageName: String,
        eventType: Int,
        eventTimestamp: Long
    ) {
        // Skip 후 반복 back press 진행 중: 쇼츠 이탈 감지만 수행 (오버레이/상태 전이는 스킵)
        if (isAutoExitingShorts) {
            if (packageName !in AppBlockingRegistry.TARGET_PACKAGES) return
            val appConfig = AppBlockingRegistry.getConfigByPackageName(packageName) ?: return
            val rootNode = rootInActiveWindow ?: return
            val stillInShorts = detectionEngine.detectShortsScreen(rootNode, appConfig)

            if (!stillInShorts) {
                Log.d(TAG, "Auto-exit: shorts exited (event-driven), clearing flag")
                cancelAutoExit()
            } else if (autoExitAttempts < MAX_AUTO_EXIT_ATTEMPTS) {
                autoExitAttempts++
                Log.d(TAG, "Auto-exit: still in shorts (event-driven), back press attempt $autoExitAttempts/$MAX_AUTO_EXIT_ATTEMPTS")
                cancelPendingAutoExitFallback()
                performGlobalBackAction()
                scheduleAutoExitFallback(packageName, appConfig)
            } else {
                Log.w(TAG, "Auto-exit: max attempts reached (event-driven), giving up")
                cancelAutoExit()
            }
            return
        }

        Log.d(TAG, "=== Event: $eventType, Package: $packageName ===")
        Log.d(TAG, "Current state: ${sessionState.getCurrentState(packageName)}")

        // TikTok 디버깅
        if (packageName.contains("trill", ignoreCase = true) ||
            packageName.contains("tiktok", ignoreCase = true) ||
            packageName.contains("aweme", ignoreCase = true)) {
            Log.e(TAG, "!!! TIKTOK DETECTED: $packageName !!!")
            Log.e(TAG, "!!! In TARGET_PACKAGES: ${packageName in AppBlockingRegistry.TARGET_PACKAGES}")
        }

        if (packageName !in AppBlockingRegistry.TARGET_PACKAGES) return

        if (!prefsManager.isAppBlockingEnabled(packageName)) {
            Log.d(TAG, "App blocking disabled for $packageName - ignoring")
            return
        }

        if (System.currentTimeMillis() < autoExitCooldownUntil) {
            Log.d(TAG, "Auto-exit cooldown active, skipping event processing")
            return
        }

        val appConfig = AppBlockingRegistry.getConfigByPackageName(packageName) ?: return
        val rootNode = rootInActiveWindow ?: return

        // pause UI 인디케이터 검사 (이벤트 기반 미디어 정지 확인)
        // 탭 후 도착한 첫 이벤트에서 인디케이터가 보이면 정지 적용으로 판단
        val pauseConfirmation = pendingPauseConfirmation
        if (pauseConfirmation != null && pauseConfirmation.packageName == packageName) {
            if (isPauseIndicatorVisible(rootNode, pauseConfirmation.indicator)) {
                Log.d(TAG, "Pause UI confirmed via accessibility event for $packageName")
                processingHandler?.removeCallbacks(pauseConfirmation.timeoutRunnable)
                pendingPauseConfirmation = null
                processingHandler?.postDelayed({
                    pauseConfirmation.onConfirmed()
                }, PAUSE_MEDIA_SETTLE_DELAY_MS)
            }
        }

        val isInShortsScreen = detectionEngine.detectShortsScreen(rootNode, appConfig)
        val currentState = sessionState.getCurrentState(packageName)
        Log.d(TAG, ">>> isShorts=$isInShortsScreen, currentState=$currentState")

        handleStateTransitions(isInShortsScreen, packageName, rootNode, appConfig)
        handleStateActions(packageName)
    }

    /**
     * AccessibilityEvent 상세 로깅 (뷰 변화 모니터링용)
     *
     * YouTube pause 등 UI 변화로 인한 false positive 디버깅을 위해
     * 이벤트 타입, 소스 정보, 변경된 내용을 상세히 기록
     */
    private fun logAccessibilityEventDetails(event: AccessibilityEvent, packageName: String) {
        if (packageName !in AppBlockingRegistry.TARGET_PACKAGES) return

        val eventTypeName = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "VIEW_LONG_CLICKED"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> "VIEW_SELECTED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIFICATION_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "VIEW_TEXT_SELECTION_CHANGED"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> "ANNOUNCEMENT"
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "VIEW_ACCESSIBILITY_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> "VIEW_ACCESSIBILITY_FOCUS_CLEARED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
            else -> "UNKNOWN(${event.eventType})"
        }

        val sourceViewId = event.source?.viewIdResourceName ?: "null"
        val sourceClassName = event.className?.toString() ?: "null"
        val sourceText = event.text?.joinToString(", ") ?: "null"
        val contentDesc = event.contentDescription?.toString() ?: "null"

        Log.d(TAG, "┌─── AccessibilityEvent Detail ───")
        Log.d(TAG, "│ Type: $eventTypeName (${event.eventType})")
        Log.d(TAG, "│ Package: $packageName")
        Log.d(TAG, "│ ClassName: $sourceClassName")
        Log.d(TAG, "│ ViewId: $sourceViewId")
        Log.d(TAG, "│ Text: $sourceText")
        Log.d(TAG, "│ ContentDesc: $contentDesc")

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val contentChangeTypes = event.contentChangeTypes
            val changeTypeNames = mutableListOf<String>()
            if (contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0) changeTypeNames.add("SUBTREE")
            if (contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT != 0) changeTypeNames.add("TEXT")
            if (contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION != 0) changeTypeNames.add("CONTENT_DESCRIPTION")
            if (contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION != 0) changeTypeNames.add("STATE_DESCRIPTION")
            if (contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED != 0) changeTypeNames.add("UNDEFINED")
            if (changeTypeNames.isEmpty()) changeTypeNames.add("NONE($contentChangeTypes)")
            Log.d(TAG, "│ ContentChangeTypes: ${changeTypeNames.joinToString(", ")}")
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            Log.d(TAG, "│ ScrollX: ${event.scrollX}, ScrollY: ${event.scrollY}")
            Log.d(TAG, "│ FromIndex: ${event.fromIndex}, ToIndex: ${event.toIndex}")
            Log.d(TAG, "│ ItemCount: ${event.itemCount}")
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "│ WindowId: ${event.windowId}")
        }

        event.source?.recycle()

        Log.d(TAG, "└─────────────────────────────────")
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

        when {
            // IDLE 상태 (쇼츠 화면 밖)
            currentState.shortsLocation == ShortsLocation.OUTSIDE -> {
                if (isInShortsScreen) {
                    // OUTSIDE → IN_SHORTS = 외부에서 진입 (클릭 진입)
                    lastBlockOriginScroll = false
                    sessionState.handleEvent(SessionEvent.EnterShorts, packageName)
                    appStartTime = System.currentTimeMillis()

                    // 쇼츠 진입 시 연속 시청 체크
                    usageMonitor.checkOnEnterShorts()
                }
            }

            // IN_SHORTS 상태 (모든 blocking stage)
            currentState.shortsLocation == ShortsLocation.IN_SHORTS -> {
                if (!isInShortsScreen) {
                    // 쇼츠 이탈 - 어디로 갔는지 확인
                    val foregroundPkg = rootInActiveWindow?.packageName?.toString()

                    when {
                        // 같은 차단 대상 앱 내 다른 화면
                        foregroundPkg == packageName -> {
                            Log.d(TAG, "Exited shorts within same app")
                            sessionState.handleEvent(SessionEvent.ExitShorts, packageName)
                        }
                        // 다른 앱 = Background 전환 (deprecated event)
                        else -> {
                            Log.d(TAG, "Exited shorts to other app/home - entering background")
                            @Suppress("DEPRECATION")
                            sessionState.handleEvent(SessionEvent.EnterBackground, packageName)
                        }
                    }

                    cancelPendingOverlay()
                } else {
                    // 쇼츠 화면 내에서 콘텐츠 변화 감지
                    Log.d(TAG, "Still in shorts, state=$currentState, checking for scroll...")

                    // 스크롤 감지 (WATCHING 상태에서만)
                    if (currentState.isWatching()) {
                        Log.d(TAG, "WATCHING state - generating content hash")
                        val hash = hashGenerator.generateContentHash(rootNode, appConfig.hashConfig)
                        Log.d(TAG, "Generated hash: $hash")

                        if (hash != 0) {
                            // YouTube의 경우 핑거프린트도 함께 생성 (유사도 비교용)
                            val fingerprint = if (packageName == "com.google.android.youtube") {
                                hashGenerator.generateContentFingerprint(rootNode)
                            } else {
                                null
                            }

                            Log.d(TAG, "Sending ContentHashChanged event with fingerprint: $fingerprint")
                            sessionState.handleEvent(SessionEvent.ContentHashChanged(hash, fingerprint), packageName)

                            // 상태가 변경되었으면 스크롤 발생
                            val newState = sessionState.getCurrentState(packageName)
                            Log.d(TAG, "State after hash event: $newState")

                            // 스크롤 감지 = WATCHING → NEED_TIMER 전이 (SessionStateManager.handleContentHashChanged 참고)
                            if (newState.blockingStage == BlockingStage.NEED_TIMER) {
                                Log.d(TAG, "Scroll detected - clearing state")

                                prefsManager.clearCompletedSessionId()
                                prefsManager.clearAllowedUntilScroll()

                                // 스크롤로 인한 차단 (UI 분기용)
                                lastBlockOriginScroll = true
                            }
                        } else {
                            Log.w(TAG, "Hash is 0 - skipping")
                        }
                    } else {
                        Log.d(TAG, "Not in ALLOWED state ($currentState) - skipping scroll detection")
                    }
                }
            }

            // Note: BACKGROUND states are deprecated - Activity lifecycle handles this now
        }
    }

    /**
     * 상태에 따른 액션 수행
     */
    private fun handleStateActions(packageName: String) {
        val overlayType = sessionState.getOverlayType(packageName)

        // 차단이 활성화되어 있고, 오버레이가 필요한 경우에만 표시
        if (prefsManager.isBlockingEnabled && overlayType != null) {
            // 오버레이 표시 필요
            Log.d(TAG, "State requires overlay: $overlayType")
            showBlockOverlay(packageName, overlayType)
        } else if (!prefsManager.isBlockingEnabled) {
            Log.d(TAG, "Blocking disabled - skipping overlay display, session tracking continues")
        }
    }

    /**
     * 오버레이 표시
     *
     * 워커 스레드에서 실행되므로 pauseMedia는 동기적으로 미디어 정지를 대기할 수 있다.
     * (메인 스레드 차단 없음 → ANR 방지)
     */
    private fun showBlockOverlay(packageName: String, overlayType: OverlayType) {
        Log.d(TAG, "showBlockOverlay() called for $packageName with type: $overlayType")

        // 이미 pending job이 있으면 스킵 (중복 스케줄 방지)
        if (pendingOverlayJob != null) {
            Log.d(TAG, "Overlay job already pending - skipping duplicate schedule")
            return
        }

        val worker = processingHandler ?: return

        Log.d(TAG, "Scheduling activity launch with delay (on worker thread)")

        // 딜레이 후 오버레이 표시 - 워커 스레드에서 실행
        // 미디어 정지가 필요한 앱은 pauseIndicator 기반 이벤트 콜백 후 오버레이 표시 (비동기)
        pendingOverlayJob = Runnable {
            try {
                Log.d(TAG, "Executing pending overlay job for $packageName")

                isCurrentSessionFromScroll = prefsManager.isAllowedUntilScroll
                Log.d(TAG, "Session from scroll: $isCurrentSessionFromScroll")

                val sessionId = UUID.randomUUID().toString()
                prefsManager.currentSessionId = sessionId
                Log.d(TAG, "Session created: $sessionId")

                val showOverlayAction = {
                    try {
                        showOverlayAndStartCheck(packageName, sessionId, overlayType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing overlay action", e)
                        pendingOverlayJob = null
                    }
                }

                val appConfig = AppBlockingRegistry.getConfigByPackageName(packageName)
                if (appConfig?.controlsMedia == true) {
                    val indicator = appConfig.pauseIndicator
                    if (indicator != null) {
                        Log.d(TAG, "Pausing media and awaiting UI indicator")
                        pauseAndAwaitUiConfirmation(packageName, indicator) {
                            showOverlayAction()
                        }
                    } else {
                        // pauseIndicator 미정의 앱(예: Naver): 무조건 탭 (audio 체크 신뢰 불가)
                        // Naver Shorts는 무음/짧은 무음 구간이 있어 isMusicActive로는 재생 여부 판별 불가
                        // → 항상 탭하여 일시정지 보장
                        Log.d(TAG, "Pause without indicator - always tap (audio check unreliable)")
                        processingHandler?.postDelayed({
                            handler.post { performTapGesture() }
                            processingHandler?.postDelayed({
                                showOverlayAction()
                            }, PAUSE_MEDIA_SETTLE_DELAY_MS)
                        }, PAUSE_MEDIA_PRE_TAP_DELAY_MS)
                    }
                } else {
                    Log.d(TAG, "Skipping media pause (controlsMedia=false or config not found)")
                    showOverlayAction()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
                pendingOverlayJob = null
            }
        }

        // Fresh start 후 500ms 이내면 300ms 딜레이, 이후는 100ms
        val timeSinceStart = System.currentTimeMillis() - appStartTime
        val delay = if (timeSinceStart < 500) 300L else 100L

        worker.postDelayed(pendingOverlayJob!!, delay)
        Log.d(TAG, "Overlay job scheduled with ${delay}ms delay")
    }

    /**
     * Pending 오버레이 취소
     */
    private fun cancelPendingOverlay() {
        pendingOverlayJob?.let {
            // 워커/메인 양쪽에서 안전하게 제거
            processingHandler?.removeCallbacks(it)
            handler.removeCallbacks(it)
            Log.d(TAG, "Cancelled pending overlay job")
            pendingOverlayJob = null
        }
    }

    /**
     * 오버레이 표시 및 포그라운드 체크 시작
     */
    private fun showOverlayAndStartCheck(packageName: String, sessionId: String, overlayType: OverlayType) {
        // 진입 방식 판단: SessionStateManager의 마지막 결정으로부터 동기화
        // - lastBlockOriginScroll = true: 스크롤로 인한 차단 (ContentHashChanged → NEED_CONFIRMATION)
        // - lastBlockOriginScroll = false: 외부 진입(EnterShorts → NEED_TIMER)
        // 통계용 isCurrentSessionFromScroll(=isAllowedUntilScroll)은 클릭 진입 판단에 부적합하므로 사용 X.
        overlayManager.showOverlay(packageName, sessionId, overlayType, lastBlockOriginScroll)
        pendingOverlayJob = null
    }


    /**
     * 단일 탭 후 pauseIndicator UI가 노출될 때까지 대기 (이벤트 기반, 비동기 콜백)
     *
     * 흐름:
     * 1. 이미 정지 상태면 즉시 onConfirmed() 호출
     * 2. 탭 제스처 1회 dispatch (toggle이므로 절대 재탭 금지)
     * 3. pendingPauseConfirmation 등록 → 다음 AccessibilityEvent에서 인디케이터 검색
     * 4. processAccessibilityEvent가 인디케이터를 발견하면 onConfirmed() 호출 + settle delay
     * 5. PAUSE_MEDIA_TIMEOUT_MS 내에 못 찾으면 timeout으로 onConfirmed() 호출
     *
     * 메인 스레드 차단 없음 + polling 없음 → ANR 안전
     */
    private fun pauseAndAwaitUiConfirmation(
        packageName: String,
        indicator: com.muuu.unshort.service.blocking.PauseIndicator,
        onConfirmed: () -> Unit
    ) {
        val worker = processingHandler ?: run {
            onConfirmed()
            return
        }

        val state = sessionState.getCurrentState(packageName)
        if (state.blockingStage != BlockingStage.NEED_TIMER) {
            Log.d(TAG, "Not NEED_TIMER state (${state.blockingStage}) - skipping pause")
            onConfirmed()
            return
        }

        if (!isTargetAppPlayingMedia()) {
            Log.d(TAG, "Media already paused - skipping tap")
            onConfirmed()
            return
        }

        // 이전 대기 취소
        cancelPendingPauseConfirmation()

        val timeoutRunnable = Runnable {
            val pc = pendingPauseConfirmation
            if (pc != null && pc.packageName == packageName) {
                Log.w(TAG, "pauseAndAwaitUiConfirmation: timeout - proceeding without UI confirmation")
                pendingPauseConfirmation = null
                pc.onConfirmed()
            }
        }

        pendingPauseConfirmation = PauseConfirmation(
            packageName = packageName,
            indicator = indicator,
            onConfirmed = onConfirmed,
            timeoutRunnable = timeoutRunnable
        )

        Log.d(TAG, "Media playing - sending single tap to pause, awaiting UI indicator")
        // 탭 제스처는 메인 스레드에서 dispatch (시스템 요구사항)
        handler.post { performTapGesture() }

        // 타임아웃 안전장치
        worker.postDelayed(timeoutRunnable, PAUSE_MEDIA_TIMEOUT_MS)
    }

    /**
     * 진행 중인 pause UI 확인 대기 취소
     */
    private fun cancelPendingPauseConfirmation() {
        val pc = pendingPauseConfirmation ?: return
        processingHandler?.removeCallbacks(pc.timeoutRunnable)
        pendingPauseConfirmation = null
    }

    /**
     * rootNode에서 pauseIndicator 일치 노드가 존재하는지 검사
     */
    private fun isPauseIndicatorVisible(
        rootNode: AccessibilityNodeInfo,
        indicator: com.muuu.unshort.service.blocking.PauseIndicator
    ): Boolean {
        // viewId 기반 매칭 (정확)
        for (viewId in indicator.viewIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId) ?: continue
            val visible = nodes.any { it.isVisibleToUser }
            nodes.forEach { @Suppress("DEPRECATION") it.recycle() }
            if (visible) return true
        }

        // contentDescription 기반 매칭 (부분, 대소문자 무시)
        for (desc in indicator.contentDescriptions) {
            if (findVisibleNodeByContentDescription(rootNode, desc, 0)) return true
        }

        return false
    }

    /**
     * contentDescription에 target이 포함된 가시 노드를 재귀 검색 (깊이 30 제한)
     */
    private fun findVisibleNodeByContentDescription(
        node: AccessibilityNodeInfo,
        target: String,
        depth: Int
    ): Boolean {
        if (depth > 30) return false

        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains(target, ignoreCase = true) && node.isVisibleToUser) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (findVisibleNodeByContentDescription(child, target, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
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

            // 1000ms 후 플래그 자동 해제 (tap gesture로 인한 지연된 이벤트 무시)
            handler.postDelayed({
                Log.d(TAG, "resumeMedia completed, accepting accessibility events again")
            }, 1000)
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
            val path = GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(centerX, centerY)
                    lineTo(centerX, centerY)
                },
                0,
                100
            )
            val gesture = GestureDescription.Builder()
                .addStroke(path)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Tap gesture completed")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
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
     * Skip 후 첫 back press 실행 + fallback 타이머 시작
     *
     * 이벤트 기반(onAccessibilityEvent)과 타이머 기반(fallback) 두 경로로 쇼츠 이탈을 감지한다.
     * - 이벤트 기반: onAccessibilityEvent에서 쇼츠 이탈 감지 시 즉시 back press 또는 종료
     * - 타이머 기반: 이벤트가 안 오는 경우를 대비한 fallback (500ms 후 직접 체크)
     */
    private fun startAutoExitShorts(
        sourcePackage: String,
        appConfig: AppBlockingConfig
    ) {
        isAutoExitingShorts = true
        autoExitAttempts = 1
        Log.d(TAG, "Auto-exit: starting, first back press (attempt 1/$MAX_AUTO_EXIT_ATTEMPTS)")
        performGlobalBackAction()
        scheduleAutoExitFallback(sourcePackage, appConfig)
    }

    /**
     * Fallback 타이머: onAccessibilityEvent가 오지 않을 경우를 대비하여
     * 직접 rootInActiveWindow를 폴링한다.
     *
     * 트리 순회는 무거운 binder 호출이므로 워커 스레드에서 실행
     */
    private fun scheduleAutoExitFallback(
        sourcePackage: String,
        appConfig: AppBlockingConfig
    ) {
        cancelPendingAutoExitFallback()
        val runnable = Runnable {
            if (!isAutoExitingShorts) return@Runnable

            // 트리 순회는 워커 스레드에서 실행하여 메인 스레드 ANR 방지
            val worker = processingHandler
            if (worker == null) {
                cancelAutoExit()
                return@Runnable
            }

            worker.post {
                try {
                    if (!isAutoExitingShorts) return@post

                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val stillInShorts = detectionEngine.detectShortsScreen(rootNode, appConfig)
                        if (stillInShorts) {
                            if (autoExitAttempts < MAX_AUTO_EXIT_ATTEMPTS) {
                                autoExitAttempts++
                                Log.d(TAG, "Auto-exit: still in shorts (fallback), back press attempt $autoExitAttempts/$MAX_AUTO_EXIT_ATTEMPTS")
                                handler.post { performGlobalBackAction() }
                                scheduleAutoExitFallback(sourcePackage, appConfig)
                            } else {
                                Log.w(TAG, "Auto-exit: max attempts reached (fallback), giving up")
                                cancelAutoExit()
                            }
                        } else {
                            Log.d(TAG, "Auto-exit: shorts exited (fallback)")
                            cancelAutoExit()
                        }
                    } else {
                        Log.d(TAG, "Auto-exit: root node null (fallback), assuming exited")
                        cancelAutoExit()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-exit fallback worker error", e)
                    cancelAutoExit()
                }
            }
            return@Runnable
        }
        autoExitPendingRunnable = runnable
        handler.postDelayed(runnable, AUTO_EXIT_FALLBACK_DELAY)
    }

    private fun cancelPendingAutoExitFallback() {
        autoExitPendingRunnable?.let { handler.removeCallbacks(it) }
        autoExitPendingRunnable = null
    }

    private fun cancelAutoExit() {
        isAutoExitingShorts = false
        autoExitAttempts = 0
        cancelPendingAutoExitFallback()
        // 쿨다운: 과도기 화면에서 false positive로 오버레이 재표시 방지
        autoExitCooldownUntil = System.currentTimeMillis() + AUTO_EXIT_COOLDOWN_MS
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

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Service instance 해제
        instance = null

        // Receivers 해제
        unregisterScreenStateReceiver()
        unregisterOverlayActionReceiver()

        cancelPendingOverlay()
        overlayManager.cleanup()

        // 백그라운드 스레드 종료 (남은 작업 마무리 후 안전하게 종료)
        processingHandler?.removeCallbacksAndMessages(null)
        processingThread?.quitSafely()
        processingHandler = null
        processingThread = null

        Log.d(TAG, "Service destroyed")
    }

    /**
     * SessionStateManager 접근자 (Activity에서 사용)
     */
    fun getSessionStateManager(): SessionStateManager = sessionState

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
     * Overlay action receiver 등록
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerOverlayActionReceiver() {
        try {
            overlayActionReceiver = OverlayActionReceiver()
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_CLOSE_OVERLAY)
                addAction(AppConstants.ACTION_WATCH_CONFIRMED)
                addAction(AppConstants.ACTION_INSTANT_UNBLOCK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(overlayActionReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(overlayActionReceiver, filter)
            }
            Log.d(TAG, "OverlayActionReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering OverlayActionReceiver", e)
        }
    }

    /**
     * Overlay action receiver 해제
     */
    private fun unregisterOverlayActionReceiver() {
        try {
            overlayActionReceiver?.let {
                unregisterReceiver(it)
                overlayActionReceiver = null
                Log.d(TAG, "OverlayActionReceiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering OverlayActionReceiver", e)
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
            Log.d(TAG, "[$trigger] Screen state changed - transitioning to background")

            // Pending overlay job 취소
            cancelPendingOverlay()

            // Note: EnterBackground is deprecated, Activity lifecycle events handle state now
            @Suppress("DEPRECATION")
            sessionState.handleEvent(SessionEvent.EnterBackground, currentPackage)

            Log.d(TAG, "[$trigger] State transitioned to background")
        }
    }

    /**
     * 오버레이 액션 BroadcastReceiver
     * - 안볼래요 버튼 (ACTION_CLOSE_OVERLAY)
     * - 볼래요 버튼 (ACTION_WATCH_CONFIRMED)
     */
    private inner class OverlayActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AppConstants.ACTION_CLOSE_OVERLAY -> {
                    val sourcePackage = intent.getStringExtra("source_package") ?: currentPackage
                    Log.d(TAG, "Skip button pressed - performing back action after overlay dismissal")

                    sessionState.handleEvent(SessionEvent.SkipConfirmed, sourcePackage)

                    prefsManager.clearCompletedSessionId()
                    prefsManager.clearAllowedUntilScroll()

                    // 딜레이 동안 이벤트 처리를 차단하여 쇼츠 재감지 방지
                    autoExitCooldownUntil = System.currentTimeMillis() + AUTO_EXIT_COOLDOWN_MS
                    Log.d(TAG, "Skip cooldown set - autoExitCooldownUntil for ${AUTO_EXIT_COOLDOWN_MS}ms")

                    // Overlay Activity가 완전히 종료되고 쇼츠로 복귀한 후 액션 수행
                    handler.postDelayed({
                        // 앱별 설정에 따라 적절한 종료 액션 수행
                        val appConfig = AppBlockingRegistry.getConfigByPackageName(sourcePackage)
                        val exitAction = appConfig?.exitAction ?: com.muuu.unshort.service.blocking.ExitAction.BACK

                        when (exitAction) {
                            com.muuu.unshort.service.blocking.ExitAction.HOME -> {
                                Log.d(TAG, "Performing home action for $sourcePackage")
                                val homePerformed = performGlobalAction(GLOBAL_ACTION_HOME)
                                if (homePerformed) {
                                    Log.d(TAG, "Home action completed successfully")
                                } else {
                                    Log.w(TAG, "Home action failed")
                                }
                            }
                            com.muuu.unshort.service.blocking.ExitAction.BACK -> {
                                Log.d(TAG, "Performing repeated back action until shorts exit")
                                if (appConfig != null) {
                                    startAutoExitShorts(sourcePackage, appConfig)
                                } else {
                                    performGlobalBackAction()
                                }
                            }
                        }
                    }, 300)
                }
                AppConstants.ACTION_WATCH_CONFIRMED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    val sourcePackage = intent.getStringExtra("source_package") ?: currentPackage
                    Log.d(TAG, "Watch button pressed - allowing watch (session=$sessionId)")

                    // WatchConfirmed 이벤트 전송 (상태 전이 + scrollData 초기화)
                    sessionState.handleEvent(SessionEvent.WatchConfirmed, sourcePackage)

                    prefsManager.isAllowedUntilScroll = true

                    // 미디어 재생 재개 (앱 설정에 따라)
                    val appConfig = AppBlockingRegistry.getConfigByPackageName(sourcePackage)
                    if (appConfig?.controlsMedia == true) {
                        handler.postDelayed({
                            resumeMedia()
                        }, 400)  // Activity 완전 제거 보장
                    } else {
                        Log.d(TAG, "Skipping resumeMedia (controlsMedia=false or config not found)")
                    }
                }
                AppConstants.ACTION_INSTANT_UNBLOCK -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    val sourcePackage = intent.getStringExtra("source_package") ?: currentPackage
                    Log.d(TAG, "Instant unblock pressed - allowing watch immediately (session=$sessionId)")

                    // ACTION_WATCH_CONFIRMED와 동일 처리. 한도 차감은 Activity가 이미 수행.
                    sessionState.handleEvent(SessionEvent.WatchConfirmed, sourcePackage)

                    prefsManager.isAllowedUntilScroll = true

                    val appConfig = AppBlockingRegistry.getConfigByPackageName(sourcePackage)
                    if (appConfig?.controlsMedia == true) {
                        handler.postDelayed({
                            resumeMedia()
                        }, 400)
                    } else {
                        Log.d(TAG, "Skipping resumeMedia (controlsMedia=false or config not found)")
                    }
                }
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

        // 일일 제한 체크 (방금 기록한 세션의 duration을 전달하여 race condition 방지)
        dailyLimitManager.checkAfterSessionEnd(info.watchDurationMs)

        currentTimerCompleted = false
        isCurrentSessionFromScroll = false
    }
}
