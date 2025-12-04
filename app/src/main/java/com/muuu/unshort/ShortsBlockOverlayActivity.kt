package com.muuu.unshort

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import com.muuu.affiliate.AffiliateProviderFactory
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager

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
    private lateinit var tipMessage: TextView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var affiliateBannerContainer: LinearLayout
    private lateinit var brandWatermark: TextView

    // State
    private lateinit var prefsManager: PreferencesManager
    private var currentSessionId: String = ""
    private var sourcePackageName: String = ""
    private var overlayType: OverlayType = OverlayType.INITIAL

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

        // Get extras from intent
        currentSessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
        sourcePackageName = intent.getStringExtra(EXTRA_SOURCE_PACKAGE) ?: ""
        overlayType = OverlayType.valueOf(
            intent.getStringExtra(EXTRA_OVERLAY_TYPE) ?: OverlayType.INITIAL.name
        )

        Log.d(TAG, "onCreate: session=$currentSessionId, source=$sourcePackageName, type=$overlayType")

        // Setup fullscreen flags
        setupFullscreen()

        setContentView(R.layout.overlay_flip_phone)

        // Initialize PreferencesManager
        prefsManager = PreferencesManager(this)

        // Initialize views
        initViews()

        // Setup Tip message
        setupTipMessage()

        // Setup Affiliate banner
        setupAffiliateBanner()

        // Update UI based on overlay type
        updateUI()

        // Register timer receiver
        registerTimerReceiver()

        // Track analytics
        AnalyticsManager.trackEvent(
            this,
            if (overlayType == OverlayType.CONFIRMATION) {
                AnalyticsEvent.OVERLAY_SHOWN_AFTER_TIMER
            } else {
                AnalyticsEvent.OVERLAY_SHOWN_BEFORE_TIMER
            }
        )
    }

    private fun setupFullscreen() {
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Show when locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Fullscreen mode
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    private fun initViews() {
        skipButton = findViewById(R.id.skipButton)
        watchButton = findViewById(R.id.watchButton)
        startTimerButton = findViewById(R.id.startTimerButton)
        mainMessage = findViewById(R.id.mainMessage)
        tipMessage = findViewById(R.id.tipMessage)
        buttonContainer = findViewById(R.id.buttonContainer)
        affiliateBannerContainer = findViewById(R.id.affiliateBannerContainer)
        brandWatermark = findViewById(R.id.brandWatermark)

        // Setup button listeners
        skipButton.setOnClickListener {
            Log.d(TAG, "Skip button clicked")
            AnalyticsManager.trackEvent(this, AnalyticsEvent.OVERLAY_BUTTON_SKIP)
            handleSkip()
        }

        watchButton.setOnClickListener {
            Log.d(TAG, "Watch button clicked")
            AnalyticsManager.trackEvent(this, AnalyticsEvent.OVERLAY_BUTTON_WATCH)
            handleWatch()
        }

        startTimerButton.setOnClickListener {
            Log.d(TAG, "Start timer button clicked")
            AnalyticsManager.trackEvent(this, AnalyticsEvent.OVERLAY_BUTTON_START_TIMER)
            handleStartTimer()
        }
    }

    private fun updateUI() {
        val isTimerCompleted = overlayType == OverlayType.CONFIRMATION

        Log.d(TAG, "updateUI: isTimerCompleted=$isTimerCompleted")

        if (isTimerCompleted) {
            // Timer completed - show CONFIRMATION buttons
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE

            // Reorder buttons: skipButton first, then watchButton
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

            // Add watch button second (bottom position)
            watchButton.setTextColor(0xFF8A8A8A.toInt())
            watchButton.setBackgroundResource(android.R.color.transparent)
            val watchParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            watchParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(watchButton, watchParams)

            mainMessage.text = getString(R.string.block_message_after_timer)
        } else {
            // Timer not completed - show INITIAL buttons
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
            skipButton.text = getString(R.string.block_button_close)

            // Reorder buttons: startTimerButton first, then skipButton
            buttonContainer.removeAllViews()

            // Add start timer button first
            val startParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            buttonContainer.addView(startTimerButton, startParams)

            // Add skip button second
            skipButton.setTextColor(0xFF8A8A8A.toInt())
            skipButton.setBackgroundResource(android.R.color.transparent)
            val skipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            skipParams.topMargin = resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(skipButton, skipParams)

            mainMessage.text = getString(R.string.block_message_before_timer)
        }
    }

    private fun handleSkip() {
        // Send broadcast to close overlay and perform back action
        val intent = Intent(AppConstants.ACTION_CLOSE_OVERLAY)
        intent.setPackage(packageName)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)

        // Finish this activity
        finishAndRemoveTask()
    }

    private fun handleWatch() {
        // Send broadcast to allow watching
        val intent = Intent(AppConstants.ACTION_WATCH_CONFIRMED)
        intent.setPackage(packageName)
        intent.putExtra("session_id", currentSessionId)
        intent.putExtra("source_package", sourcePackageName)
        sendBroadcast(intent)

        // Finish this activity
        finishAndRemoveTask()
    }

    private fun handleStartTimer() {
        // Start ShortsBlockTimerActivity
        val intent = Intent(this, com.muuu.unshort.timer.ShortsBlockTimerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("session_id", currentSessionId)
            putExtra("source_package", sourcePackageName)
        }
        startActivity(intent)

        // Finish this activity (timer activity will take over)
        finish()
    }

    private fun setupTipMessage() {
        val tips = resources.getStringArray(R.array.overlay_tips)
        val randomIndex = tips.indices.random()
        val message = tips[randomIndex]

        tipMessage.text = message
        Log.d(TAG, "Tip message set: $message")
    }

    private fun setupAffiliateBanner() {
        try {
            // RemoteConfig + Premium check
            val showAffiliateBanner = UnshortApplication.remoteConfig.getBoolean(AppConstants.RC_SHOW_AFFILIATE_BANNER)

            if (!showAffiliateBanner || PremiumManager.isPremium()) {
                affiliateBannerContainer.visibility = View.GONE
                brandWatermark.visibility = View.VISIBLE
                Log.d(TAG, "Affiliate banner hidden - RemoteConfig: $showAffiliateBanner, Premium: ${PremiumManager.isPremium()}")
                return
            }

            // Affiliate Provider로부터 배너 뷰 생성
            val provider = AffiliateProviderFactory.create(
                context = this,
                onLinkClick = { url ->
                    // Affiliate 링크 클릭 시 Analytics 트래킹
                    AnalyticsManager.trackEvent(this, AnalyticsEvent.AFFILIATE_PRODUCT_CLICKED)
                    Log.d(TAG, "Affiliate link clicked: $url")

                    // Finish activity
                    finishAndRemoveTask()
                }
            )
            val bannerView = provider.createBannerView(this)

            // 컨테이너에 배너 추가
            affiliateBannerContainer.removeAllViews()
            affiliateBannerContainer.addView(bannerView)
            affiliateBannerContainer.visibility = View.VISIBLE

            // Affiliate 배너가 있으면 워터마크 숨김
            brandWatermark.visibility = View.INVISIBLE

            Log.d(TAG, "Affiliate banner loaded successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading affiliate banner", e)
            affiliateBannerContainer.visibility = View.GONE
            brandWatermark.visibility = View.VISIBLE
        }
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

        // Hide system UI
        hideSystemUI()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ : WindowInsetsController 사용
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // API 26-29 : systemUiVisibility 사용
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    override fun onBackPressed() {
        // Disable back button
        Log.d(TAG, "Back button pressed - ignored")
        // Do nothing (차단 강제성 유지)
    }

    override fun onDestroy() {
        super.onDestroy()

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

        // Clean up WebView
        if (::affiliateBannerContainer.isInitialized) {
            for (i in 0 until affiliateBannerContainer.childCount) {
                val child = affiliateBannerContainer.getChildAt(i)
                if (child is WebView) {
                    child.loadUrl("about:blank")
                    child.destroy()
                }
            }
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
