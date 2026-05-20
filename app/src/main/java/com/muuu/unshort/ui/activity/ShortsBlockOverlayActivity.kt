package com.muuu.unshort.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuNativeAdUnit
import com.muuu.ad.core.adunit.MuuuRewardedAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.ad.core.model.nativetemplate.MuuuNativeAdTemplate
import com.muuu.unshort.service.blocking.ActivityType
import com.muuu.unshort.service.blocking.SessionEvent
import com.muuu.unshort.service.blocking.BlockingStage
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.ad.DailyUnblockQuotaManager
import com.muuu.unshort.ad.RewardResult
import com.muuu.unshort.ui.dialog.QuotaExhaustedDialog
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.config.AdConfig
import com.muuu.unshort.config.OverlayType
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.R
import com.muuu.unshort.ShortsBlockService

/**
 * 쇼츠 차단 오버레이 Activity
 *
 * 책임:
 * - 쇼츠 감지 시 차단 화면 표시
 * - 타이머 시작 전/후 UI 변경
 * - 사용자 선택 처리 (타이머 시작, 안볼래요, 볼래요)
 */
class ShortsBlockOverlayActivity : BaseActivity() {

    companion object {
        private const val TAG = "ShortsBlockOverlayActivity"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SOURCE_PACKAGE = "source_package"
        const val EXTRA_OVERLAY_TYPE = "overlay_type"
}

    override fun isLightStatusBar(): Boolean = false

    // Views
    private lateinit var skipButton: TextView
    private lateinit var watchButton: TextView
    private lateinit var startTimerButton: TextView
    private lateinit var mainMessage: TextView
    private lateinit var buttonContainer: LinearLayout
    private var bannerAdView: com.muuu.ad.view.MuuuBannerAdView? = null
    private var nativeAdView: com.muuu.ad.view.MuuuNativeAdView? = null

    // 바로 보기 — 클릭 진입 시 노출. N초 후 활성화 (skipButton 패턴)
    private lateinit var instantUnblockButton: TextView
    private var isFromScroll: Boolean = false
    private var rewardedAdInProgress: Boolean = false
    private var instantUnblockCountdown: Int = 2

    // Utils
    private lateinit var prefsManager: PreferencesManager

    // State
    private lateinit var sessionStateManager: com.muuu.unshort.service.blocking.SessionStateManager
    private var currentSessionId: String = ""
    private var sourcePackageName: String = ""
    private var overlayType: OverlayType = OverlayType.INITIAL
    private var isTimerActivityLaunched: Boolean = false
    private var skipButtonCountdown: Int = 2
    private val handler = Handler(Looper.getMainLooper())
    private var activityCreatedTime: Long = 0

    // Activity Result API for timer completion
    private val timerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isTimerActivityLaunched = false
        if (result.resultCode == RESULT_OK) {
            // 타이머 완료
            Log.d(TAG, "Timer completed - switching to CONFIRMATION mode")
            overlayType = OverlayType.CONFIRMATION
            updateUI()
        } else {
            Log.d(TAG, "Timer cancelled")
        }
    }

    // Receiver for timer completion
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AppConstants.ACTION_TIMER_COMPLETED -> {
                    val sessionId = intent.getStringExtra("session_id") ?: ""
                    Log.d(TAG, "Timer completed broadcast: $sessionId")

                    if (sessionId == currentSessionId) {
                        Log.d(TAG, "Session matches - switching to CONFIRMATION mode")
                        overlayType = OverlayType.CONFIRMATION
                        updateUI()
                    }
                }
            }
        }
    }

    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Record activity creation time
        activityCreatedTime = System.currentTimeMillis()
        prefsManager = PreferencesManager(this)

        // Get extras from intent
        currentSessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
        sourcePackageName = intent.getStringExtra(EXTRA_SOURCE_PACKAGE) ?: ""
        overlayType = OverlayType.valueOf(
            intent.getStringExtra(EXTRA_OVERLAY_TYPE) ?: OverlayType.INITIAL.name
        )
        isFromScroll = intent.getBooleanExtra(AppConstants.EXTRA_ENTRY_FROM_SCROLL, false)

        Log.d(TAG, "onCreate: session=$currentSessionId, source=$sourcePackageName, type=$overlayType")

        // Setup fullscreen flags
        setupScreenKeepOn()

        setContentView(R.layout.overlay_flip_phone)

        // Initialize SessionStateManager from Service
        sessionStateManager = ShortsBlockService.instance?.getSessionStateManager()
            ?: throw IllegalStateException("ShortsBlockService is not running")

        // Initialize views
        initViews()

        // Setup Ads
        setupAds()

        // Update UI based on overlay type
        updateUI()

        // Setup bottom action area (즉시 해제 / 광고 충전)
        setupBottomActionUi()

        if (prefsManager.isSleepTime()) {
            mainMessage.text = getString(R.string.sleep_mode_motivation)
        }

        // Register timer receiver
        registerTimerReceiver()

        // Disable back button (최신 API 사용)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - 뒤로가기 버튼 무시 (차단 강제성 유지)
                Log.d(TAG, "Back button pressed - ignored")
            }
        })

        // Notify SessionStateManager that Activity started
        sessionStateManager.handleEvent(
            com.muuu.unshort.service.blocking.SessionEvent.ActivityStarted(
                com.muuu.unshort.service.blocking.ActivityType.OVERLAY,
                currentSessionId
            ),
            sourcePackageName
        )
        Log.d(TAG, "Activity started event sent: session=$currentSessionId")

        // Track analytics
        AnalyticsManager.trackEvent(
            this,
            if (overlayType == OverlayType.CONFIRMATION) {
                AnalyticsEvent.OVERLAY_SHOWN_AFTER_TIMER
            } else {
                AnalyticsEvent.OVERLAY_SHOWN_BEFORE_TIMER
            },
            overlayContextProperties()
        )
    }

    /**
     * 오버레이 관련 이벤트에 공통으로 붙는 컨텍스트
     */
    private fun overlayContextProperties(extra: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        return mapOf(
            "source_package" to sourcePackageName,
            "source_app" to AnalyticsManager.sourceAppOf(sourcePackageName),
            "entry_type" to if (isFromScroll) "scroll" else "click",
            "session_id" to currentSessionId,
            "overlay_type" to overlayType.name
        ) + extra
    }

    private fun timeOnOverlayMs(): Long {
        return if (activityCreatedTime > 0) {
            System.currentTimeMillis() - activityCreatedTime
        } else 0L
    }

    private fun setupScreenKeepOn() {
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Note: setShowWhenLocked 제거 - 잠금화면 위로 오버레이가 뜨는 것을 방지
    }

    private fun initViews() {
        skipButton = findViewById(R.id.skipButton)
        watchButton = findViewById(R.id.watchButton)
        startTimerButton = findViewById(R.id.startTimerButton)
        mainMessage = findViewById(R.id.mainMessage)
        buttonContainer = findViewById(R.id.buttonContainer)

        instantUnblockButton = findViewById(R.id.instantUnblockButton)

        // Setup button listeners
        skipButton.setOnClickListener {
            Log.d(TAG, "Skip button clicked")
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.OVERLAY_BUTTON_SKIP,
                overlayContextProperties(mapOf("time_on_overlay_ms" to timeOnOverlayMs()))
            )
            handleSkip()
        }

        watchButton.setOnClickListener {
            Log.d(TAG, "Watch button clicked")
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.OVERLAY_BUTTON_WATCH,
                overlayContextProperties(mapOf("time_on_overlay_ms" to timeOnOverlayMs()))
            )
            handleWatch()
        }

        startTimerButton.setOnClickListener {
            Log.d(TAG, "Start timer button clicked")
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.OVERLAY_BUTTON_START_TIMER,
                overlayContextProperties(mapOf("time_on_overlay_ms" to timeOnOverlayMs()))
            )
            handleStartTimer()
        }

        instantUnblockButton.setOnClickListener {
            Log.d(TAG, "Instant unblock button clicked")
            onInstantUnblockClicked()
        }
    }

    private fun startSkipButtonCountdown() {
        // Only start countdown for INITIAL overlay (before timer)
        if (overlayType != OverlayType.INITIAL) {
            return
        }

        skipButtonCountdown = 2
        skipButton.isEnabled = false
        skipButton.alpha = 0.5f

        updateSkipButtonText()

        handler.postDelayed({
            skipButtonCountdown = 1
            updateSkipButtonText()

            handler.postDelayed({
                skipButtonCountdown = 0
                skipButton.isEnabled = true
                skipButton.alpha = 1.0f
                updateSkipButtonText()
            }, 1000)
        }, 1000)
    }

    private fun updateSkipButtonText() {
        when {
            skipButtonCountdown > 0 -> {
                skipButton.text = getString(R.string.block_button_close_countdown, skipButtonCountdown)
            }
            else -> {
                skipButton.text = getString(R.string.block_button_close)
            }
        }
    }

    private fun updateUI() {
        val isTimerCompleted = overlayType == OverlayType.CONFIRMATION

        Log.d(TAG, "updateUI: isTimerCompleted=$isTimerCompleted")

        if (isTimerCompleted) {
            // Timer completed - show CONFIRMATION buttons
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE

            // 순서: skipButton(메인 액션) → bottomAction → watchButton(가벼운 옵션)
            buttonContainer.removeAllViews()

            // Add skip button first (top position)
            skipButton.text = getString(R.string.block_button_no)
            skipButton.setTextColor(0xFF000000.toInt())
            skipButton.setBackgroundResource(R.drawable.btn_timer_skip_white_solid)
            val skipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            buttonContainer.addView(skipButton, skipParams)

            // Add bottom action container (즉시 해제 / 광고 충전, skipButton 바로 아래)
            // instantUnblockButton (마진 포함)
            val instantParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            instantParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(instantUnblockButton, instantParams)

            // Add watch button last
            watchButton.setTextColor(0xFF8A8A8A.toInt())
            watchButton.setBackgroundResource(android.R.color.transparent)
            val watchParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            watchParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(watchButton, watchParams)

            mainMessage.text = prefsManager.customMessageAfterTimer.ifEmpty {
                getString(R.string.block_message_after_timer)
            }
        } else {
            // Timer not completed - show INITIAL buttons
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
            skipButton.text = getString(R.string.block_button_close)

            // 순서: startTimerButton(메인) → bottomAction(secondary) → skipButton(취소)
            buttonContainer.removeAllViews()

            // Add start timer button first ("한번 더 생각해보기")
            val startParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            buttonContainer.addView(startTimerButton, startParams)

            // Add bottom action container ("바로 보기 / 광고 보고 +N회") — 메인과 그만 닫기 사이
            // instantUnblockButton (마진 포함)
            val instantParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            instantParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(instantUnblockButton, instantParams)

            // Add skip button last ("그만 볼래요")
            skipButton.setTextColor(0xFF8A8A8A.toInt())
            skipButton.setBackgroundResource(android.R.color.transparent)
            val skipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            skipParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(skipButton, skipParams)

            mainMessage.text = prefsManager.customMessageBeforeTimer.ifEmpty {
                getString(R.string.block_message_before_timer)
            }
        }
    }

    private fun handleSkip() {
        // Send broadcast to ShortsBlockService
        val intent = Intent(AppConstants.ACTION_CLOSE_OVERLAY)
        intent.setPackage(packageName)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)
        Log.d(TAG, "Skip button clicked - broadcast sent")

        // Finish this activity (ActivityDestroyed will be sent in onDestroy)
        finishAndRemoveTask()
    }

    private fun handleWatch() {
        // Send broadcast to ShortsBlockService
        val intent = Intent(AppConstants.ACTION_WATCH_CONFIRMED)
        intent.setPackage(packageName)
        intent.putExtra("session_id", currentSessionId)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)
        Log.d(TAG, "Watch button clicked - broadcast sent")

        // Finish this activity (ActivityDestroyed will be sent in onDestroy)
        finishAndRemoveTask()
    }

    private fun handleStartTimer() {
        // Ignore button clicks within 1 second of activity creation
        val timeSinceCreation = System.currentTimeMillis() - activityCreatedTime
        if (timeSinceCreation < 1000) {
            Log.d(TAG, "Timer button clicked too early (${timeSinceCreation}ms) - ignoring")
            return
        }

        // Cancel any pending handler callbacks (e.g., skip button countdown)
        handler.removeCallbacksAndMessages(null)

        // Timer Activity 시작 (result 기대)
        val intent = Intent(this, com.muuu.unshort.timer.ShortsBlockTimerActivity::class.java).apply {
            putExtra("session_id", currentSessionId)
            putExtra("source_package", sourcePackageName)
        }

        // Activity를 background로 이동 (finish 제거)
        isTimerActivityLaunched = true
        timerLauncher.launch(intent)

        Log.d(TAG, "Timer activity launched, overlay moved to background")
    }

    /**
     * 차단 화면 하단 "바로 보기" 노출 결정
     *
     * - 스크롤 진입: GONE
     * - 클릭 진입: 노출 + N초 카운트다운
     * 잔여 횟수 텍스트는 노출하지 않음. 한도 도달 시점도 별도 표기 X (클릭 시 다이얼로그로 처리).
     */
    private fun setupBottomActionUi() {
        if (isFromScroll) {
            instantUnblockButton.visibility = View.GONE
            return
        }
        instantUnblockButton.visibility = View.VISIBLE
        startInstantUnblockCountdown()
    }

    /**
     * "바로 보기" N초 후 활성화 (skipButton과 동일 패턴)
     */
    private fun startInstantUnblockCountdown() {
        instantUnblockCountdown = 2
        instantUnblockButton.isEnabled = false
        instantUnblockButton.alpha = 0.5f
        updateInstantUnblockButtonText()

        handler.postDelayed({
            instantUnblockCountdown = 1
            updateInstantUnblockButtonText()

            handler.postDelayed({
                instantUnblockCountdown = 0
                instantUnblockButton.isEnabled = true
                instantUnblockButton.alpha = 1.0f
                updateInstantUnblockButtonText()
            }, 1000)
        }, 1000)
    }

    private fun updateInstantUnblockButtonText() {
        instantUnblockButton.text = if (instantUnblockCountdown > 0) {
            getString(R.string.instant_unblock_countdown, instantUnblockCountdown)
        } else {
            getString(R.string.instant_unblock_label)
        }
    }

    /**
     * "바로 보기" 클릭 시 분기:
     *  - 한도 ≥ 1 또는 프리미엄 → 즉시 해제
     *  - 한도 = 0 → 다이얼로그 (광고 / 프리미엄 / 닫기)
     */
    private fun onInstantUnblockClicked() {
        val isPremium = PremiumManager.isPremium()
        val remaining = DailyUnblockQuotaManager.getRemainingQuota(this)

        if (isPremium || remaining >= 1) {
            performInstantUnblock()
        } else {
            showQuotaExhaustedDialog()
        }
    }

    private fun performInstantUnblock() {
        val isPremium = PremiumManager.isPremium()
        val remainingBefore = DailyUnblockQuotaManager.getRemainingQuota(this)
        DailyUnblockQuotaManager.consumeOnInstantUnblock(this)
        val remainingAfter = DailyUnblockQuotaManager.getRemainingQuota(this)
        val totalTodayAfter = DailyUnblockQuotaManager.getTotalUnblockToday(this)

        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.INSTANT_UNBLOCK_CLICKED,
            overlayContextProperties(
                mapOf(
                    "remaining_before" to remainingBefore,
                    "remaining_after" to remainingAfter,
                    "total_today_after" to totalTodayAfter,
                    "via_friction_no_charge" to isPremium
                )
            )
        )
        // 누적 즉시해제 카운터 갱신
        AnalyticsManager.incrementUnblockLifetimeAndUpdate(this)

        val intent = Intent(AppConstants.ACTION_INSTANT_UNBLOCK)
        intent.setPackage(packageName)
        intent.putExtra("session_id", currentSessionId)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)
        Log.d(TAG, "Instant unblock - broadcast sent")

        finishAndRemoveTask()
    }

    private fun showQuotaExhaustedDialog() {
        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.UNBLOCK_QUOTA_EXHAUSTED,
            overlayContextProperties(
                mapOf(
                    "total_today" to DailyUnblockQuotaManager.getTotalUnblockToday(this),
                    "ads_watched_today" to DailyUnblockQuotaManager.getTotalAdsWatchedToday(this)
                )
            )
        )

        QuotaExhaustedDialog(
            context = this,
            rechargeAmount = AppConstants.DEFAULT_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT,
            onWatchAd = { watchRewardedAdAndRecharge() },
            onPremium = {
                startActivity(Intent(this, PremiumUpgradeActivity::class.java))
            },
            onCancel = { /* dismiss */ }
        ).show()
    }

    private fun watchRewardedAdAndRecharge() {
        if (rewardedAdInProgress) return
        rewardedAdInProgress = true

        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.AD_RECHARGE_CLICKED,
            overlayContextProperties(
                mapOf(
                    "placement" to "rewarded_unblock_quota_recharge",
                    "ads_watched_today" to DailyUnblockQuotaManager.getTotalAdsWatchedToday(this)
                )
            )
        )

        AdManager.setupRewardedAd(
            activity = this,
            adUnit = MuuuRewardedAdUnit(
                key = AdConfig.REWARDED_UNBLOCK_QUOTA_RECHARGE,
                placement = "rewarded_unblock_quota_recharge"
            )
        ) { result ->
            rewardedAdInProgress = false
            when (result) {
                is RewardResult.Earned -> {
                    val charged = DailyUnblockQuotaManager.rechargeFromAd(this)
                    AnalyticsManager.trackEvent(
                        this,
                        AnalyticsEvent.AD_RECHARGE_QUOTA_ADDED,
                        overlayContextProperties(
                            mapOf(
                                "placement" to "rewarded_unblock_quota_recharge",
                                "amount" to charged,
                                "remaining_after" to DailyUnblockQuotaManager.getRemainingQuota(this),
                                "ads_watched_today" to DailyUnblockQuotaManager.getTotalAdsWatchedToday(this)
                            )
                        )
                    )
                    // 광고 시청 완료 → 즉시 해제로 이어짐 (한도 차감)
                    performInstantUnblock()
                }
                is RewardResult.Failed -> {
                    AnalyticsManager.trackEvent(
                        this,
                        AnalyticsEvent.AD_RECHARGE_AD_FAILED,
                        overlayContextProperties(
                            mapOf("placement" to "rewarded_unblock_quota_recharge")
                        )
                    )
                    Toast.makeText(
                        this,
                        getString(R.string.ad_recharge_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is RewardResult.Cancelled -> {
                    Log.d(TAG, "Rewarded ad cancelled (no reward)")
                }
                is RewardResult.NotApplicable -> {
                    // 광고 시청 도중 프리미엄 활성화 등 → 즉시 해제
                    performInstantUnblock()
                }
            }
        }
    }

    private fun setupAds() {
        // 네이티브 광고 (상단)
        val nativeAdContainer = findViewById<FrameLayout>(R.id.nativeAd)
        nativeAdView = AdManager.setupNativeAd(
            context = this,
            container = nativeAdContainer,
            adUnit = MuuuNativeAdUnit(
                key = AdConfig.NATIVE_OVERLAY_TOP,
                placement = "overlay_screen_top",
                refreshInterval = 4
            ),
            template = MuuuNativeAdTemplate.Line(
                backgroundColor = getColor(R.color.primary_dark),
                contentColor = getColor(R.color.white),
                adMarkLabelBackgroundColor = getColor(R.color.gray_700),
                adMarkLabelTextColor = getColor(R.color.white)
            )
        )

        // 배너 광고 (하단)
        val adViewContainer = findViewById<FrameLayout>(R.id.adView)
        bannerAdView = AdManager.setupBannerAd(
            context = this,
            container = adViewContainer,
            adUnit = MuuuBannerAdUnit(
                key = AdConfig.BANNER_OVERLAY_BOTTOM,
                placement = "overlay_screen_bottom",
                bannerSize = MuuuBannerSize.Banner,
                refreshInterval = 4
            )
        )
    }

    private fun registerTimerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(AppConstants.ACTION_TIMER_COMPLETED)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(timerReceiver, filter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "Timer receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering receiver", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Sync from SessionState
        val currentState = sessionStateManager.getCurrentState(sourcePackageName)
        Log.d(TAG, "onResume: syncing from SessionState: $currentState")

        when (currentState.blockingStage) {
            com.muuu.unshort.service.blocking.BlockingStage.NEED_CONFIRMATION -> {
                if (overlayType != OverlayType.CONFIRMATION) {
                    Log.d(TAG, "State changed to NEED_CONFIRMATION - updating UI")
                    overlayType = OverlayType.CONFIRMATION
                    updateUI()
                }
            }
            com.muuu.unshort.service.blocking.BlockingStage.NEED_TIMER -> {
                if (overlayType != OverlayType.INITIAL) {
                    Log.d(TAG, "State changed to NEED_TIMER - updating UI")
                    overlayType = OverlayType.INITIAL
                    updateUI()
                }
            }
            else -> {
                Log.d(TAG, "State is ${currentState.blockingStage} - finishing activity")
                finishAndRemoveTask()
                return
            }
        }

        // Resume skip button countdown if in INITIAL state (e.g., returning from timer)
        if (overlayType == OverlayType.INITIAL && skipButtonCountdown != 0) {
            startSkipButtonCountdown()
        }
    }

    override fun onStop() {
        super.onStop()

        // 타이머 Activity가 실행 중이 아니면 Activity를 종료
        // (홈키, 다른 앱으로 전환 등의 경우)
        if (!isTimerActivityLaunched) {
            Log.d(TAG, "onStop: timer not launched - finishing activity")
            finish()
        } else {
            Log.d(TAG, "onStop: timer launched - keeping activity alive")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove all pending handler callbacks
        handler.removeCallbacksAndMessages(null)

        // Notify SessionStateManager that Activity destroyed
        ShortsBlockService.instance?.getSessionStateManager()?.handleEvent(
            SessionEvent.ActivityDestroyed(
                ActivityType.OVERLAY
            ),
            sourcePackageName
        )
        Log.d(TAG, "Activity destroyed event sent")

        // Unregister timer receiver
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(timerReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Timer receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
