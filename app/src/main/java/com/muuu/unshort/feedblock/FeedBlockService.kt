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
 * 컨셉: 피드 진입 즉시 차단 화면 표시 + 60초 grace 기반 세션 정책.
 *
 * - 피드 첫 진입 → 즉시 overlay launch
 * - 사용자 "계속 볼래요" → 그 세션 자유
 * - 사용자 "그만 볼래요" → HOME 강제 이탈
 * - 다른 앱/HOME 60초 이내 복귀 → 같은 세션 유지
 * - 다른 앱/HOME 60초 초과 후 진입 → 다시 차단
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
        if (packageName !in FeedTargetRegistry.TARGET_PACKAGES) return

        val target = FeedTargetRegistry.getByPackage(packageName) ?: return
        if (!prefsManager.isAppEnabled(packageName)) return

        processingHandler?.post { handleEvent(target) }
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
                Log.d(TAG, "EXIT feed [${target.displayName}]")
                sessionManager.onExitFeed()
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
                sessionManager.reset()
                inFeed = false
                currentTarget = null
            }
        }
    }

    companion object {
        private const val TAG = "FeedBlockService"
        private const val OVERLAY_COOLDOWN_MS = 1_500L

        const val ACTION_FEED_BLOCK_STOP = "com.muuu.unshort.feedblock.STOP"
        const val ACTION_FEED_BLOCK_CONTINUE = "com.muuu.unshort.feedblock.CONTINUE"

        @Volatile
        var instance: FeedBlockService? = null
            private set
    }
}
