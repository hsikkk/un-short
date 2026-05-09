package com.muuu.unshort.feedblock

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.muuu.unshort.feedblock.detection.FeedDetectionEngine
import com.muuu.unshort.feedblock.lifecycle.FeedSessionManager
import com.muuu.unshort.feedblock.overlay.FeedBlockOverlayActivity
import com.muuu.unshort.feedblock.prefs.FeedBlockPreferences

/**
 * 피드 차단 AccessibilityService (베타)
 *
 * 컨셉: 피드 진입 즉시 차단 화면 표시 + 외부 앱 이탈 기반 grace 세션 정책.
 *
 * - 피드 첫 진입 → 즉시 overlay launch (진입 후 짧은 polling으로 화면 그려지자마자 차단)
 * - 사용자 "계속 볼래요" → 그 세션 자유 (Passed)
 * - 사용자 "그만 볼래요" → HOME 강제 이탈 (Idle)
 * - 같은 앱 내 화면 이동 (피드 ↔ 메시지/스토리/프로필) → 세션 유지 (Passed)
 * - unshort 자체 진입(차단 화면 등) → 외부 이탈로 카운트하지 않음
 * - 외부 앱(타깃이 아닌 다른 앱)으로 이탈 60초 이내 복귀 → 같은 세션 유지
 * - 외부 앱 이탈 60초 초과 후 진입 → 다시 차단
 */
class FeedBlockService : AccessibilityService() {

    private lateinit var prefsManager: FeedBlockPreferences
    private val detectionEngine = FeedDetectionEngine()
    private val sessionManager = FeedSessionManager()

    private var processingThread: HandlerThread? = null
    private var processingHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var inFeed: Boolean = false

    @Volatile
    private var currentTarget: FeedTarget? = null
    private var lastOverlayShownAt: Long = 0L

    @Volatile
    private var lastForegroundPackage: String? = null

    private var pollingRunnable: Runnable? = null

    private var overlayActionReceiver: OverlayActionReceiver? = null
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "service connected")
        instance = this

        prefsManager = FeedBlockPreferences(this)

        processingThread = HandlerThread("FeedBlock-Processing").apply { start() }
        processingHandler = Handler(processingThread!!.looper)

        registerOverlayActionReceiver()
        registerScreenStateReceiver()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (!prefsManager.isBetaEnabled) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handlePackageChange(packageName)
        }

        if (packageName !in FeedTargetRegistry.TARGET_PACKAGES) return

        val target = FeedTargetRegistry.getByPackage(packageName) ?: return
        if (!prefsManager.isAppEnabled(packageName)) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            startInstantBlockPolling(target)
        }

        processingHandler?.post { handleEvent(target) }
    }

    /**
     * 포그라운드 패키지 변경 추적.
     *
     * - unshort 자체 진입: lastForegroundPackage 업데이트 안 함 (외부 이탈로 카운트하지 않음)
     * - 같은 패키지 재진입: 무시
     * - 타깃 앱에서 다른 앱(타깃이든 외부든)으로 전환: onLeaveApp 호출하여 grace 시작
     */
    private fun handlePackageChange(newPackage: String) {
        if (newPackage == this.packageName) {
            Log.d(TAG, "Transition to self ($newPackage) - ignored")
            return
        }

        val previous = lastForegroundPackage
        if (previous == newPackage) return

        lastForegroundPackage = newPackage

        if (previous == null) return

        val previousIsTarget = previous in FeedTargetRegistry.TARGET_PACKAGES
        if (!previousIsTarget) return

        Log.d(TAG, "Left target app $previous → $newPackage, starting grace")
        sessionManager.onLeaveApp()
        inFeed = false
        currentTarget = null
        cancelInstantBlockPolling()
    }

    /**
     * 타깃 앱 진입 직후 화면이 아직 그려지지 않아 detection이 false인 경우를 보완하기 위해
     * 짧은 간격으로 재시도. inFeed가 true가 되면 즉시 종료.
     */
    private fun startInstantBlockPolling(target: FeedTarget) {
        val handler = processingHandler ?: return
        cancelInstantBlockPolling()

        val runnable = object : Runnable {
            private var attempts = 0

            override fun run() {
                if (inFeed) return
                handleEvent(target)
                attempts++
                if (!inFeed && attempts < INSTANT_POLL_MAX_ATTEMPTS) {
                    handler.postDelayed(this, INSTANT_POLL_INTERVAL_MS)
                }
            }
        }
        pollingRunnable = runnable
        handler.post(runnable)
    }

    private fun cancelInstantBlockPolling() {
        val handler = processingHandler ?: return
        pollingRunnable?.let { handler.removeCallbacks(it) }
        pollingRunnable = null
    }

    private fun handleEvent(target: FeedTarget) {
        val rootNode = rootInActiveWindow ?: return
        val isFeed = detectionEngine.detectFeed(rootNode, target)

        when {
            isFeed && !inFeed -> {
                inFeed = true
                currentTarget = target
                Log.d(TAG, "ENTER feed [${target.displayName}]")
                if (sessionManager.onEnterFeed(target)) {
                    triggerBlockOverlay(target)
                }
            }
            !isFeed && inFeed -> {
                Log.d(TAG, "EXIT feed (same app navigation) [${target.displayName}]")
                inFeed = false
                currentTarget = null
            }
        }
    }

    private fun triggerBlockOverlay(target: FeedTarget) {
        val now = System.currentTimeMillis()
        if (now - lastOverlayShownAt < OVERLAY_COOLDOWN_MS) return
        lastOverlayShownAt = now

        Log.i(TAG, "Launching overlay for ${target.displayName}")

        mainHandler.post {
            val intent = Intent(this, FeedBlockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(FeedBlockOverlayActivity.EXTRA_PACKAGE, target.packageName)
                putExtra(FeedBlockOverlayActivity.EXTRA_DISPLAY_NAME, target.displayName)
            }
            startActivity(intent)
        }
    }

    fun handleUserContinue() {
        Log.d(TAG, "user chose to continue")
        sessionManager.onUserContinue()
    }

    fun handleUserStop() {
        Log.d(TAG, "user chose to stop - performing HOME action")
        sessionManager.onUserStop()
        inFeed = false
        currentTarget = null
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d(TAG, "service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterOverlayActionReceiver()
        unregisterScreenStateReceiver()

        cancelInstantBlockPolling()
        processingHandler?.removeCallbacksAndMessages(null)
        processingThread?.quitSafely()
        processingHandler = null
        processingThread = null

        if (instance === this) {
            instance = null
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerOverlayActionReceiver() {
        overlayActionReceiver = OverlayActionReceiver()
        val filter = IntentFilter().apply {
            addAction(ACTION_FEED_BLOCK_STOP)
            addAction(ACTION_FEED_BLOCK_CONTINUE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayActionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayActionReceiver, filter)
        }
    }

    private fun unregisterOverlayActionReceiver() {
        try {
            overlayActionReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        overlayActionReceiver = null
    }

    private fun registerScreenStateReceiver() {
        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private fun unregisterScreenStateReceiver() {
        try {
            screenStateReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        screenStateReceiver = null
    }

    private inner class OverlayActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_FEED_BLOCK_STOP -> handleUserStop()
                ACTION_FEED_BLOCK_CONTINUE -> handleUserContinue()
            }
        }
    }

    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off - resetting session")
                cancelInstantBlockPolling()
                sessionManager.reset()
                inFeed = false
                currentTarget = null
                lastForegroundPackage = null
            }
        }
    }

    companion object {
        private const val TAG = "FeedBlockService"
        private const val OVERLAY_COOLDOWN_MS = 1_500L
        private const val INSTANT_POLL_INTERVAL_MS = 250L
        private const val INSTANT_POLL_MAX_ATTEMPTS = 8

        const val ACTION_FEED_BLOCK_STOP = "com.muuu.unshort.feedblock.STOP"
        const val ACTION_FEED_BLOCK_CONTINUE = "com.muuu.unshort.feedblock.CONTINUE"

        @Volatile
        var instance: FeedBlockService? = null
            private set
    }
}
