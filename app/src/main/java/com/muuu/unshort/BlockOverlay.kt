package com.muuu.unshort

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
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

class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val TAG = "BlockOverlay"

    private lateinit var skipButton: TextView
    private lateinit var watchButton: TextView
    private lateinit var startTimerButton: TextView
    private lateinit var mainMessage: TextView
    private lateinit var tipMessage: TextView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var prefsManager: PreferencesManager
    private lateinit var affiliateBannerContainer: LinearLayout
    private lateinit var brandWatermark: TextView

    private var onDismissListener: (() -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var onSkipListener: (() -> Unit)? = null
    private var onWatchListener: (() -> Unit)? = null
    private var currentSessionId: String = ""
    private var sourcePackageName: String = ""

    // Periodic check는 더 이상 사용하지 않음 (overlayType 파라미터로 대체)

    @SuppressLint("InflateParams")
    fun show(
        onDismiss: () -> Unit,
        onComplete: () -> Unit,
        onSkip: (() -> Unit)? = null,
        onWatch: (() -> Unit)? = null,
        sessionId: String = "",
        sourcePackage: String = "",
        overlayType: OverlayType = OverlayType.INITIAL
    ) {
        Log.d(TAG, "show() called with sessionId: $sessionId, overlayType: $overlayType")

        // 기존 오버레이가 있으면 먼저 제거 (타입 변경 시 재생성)
        if (overlayView != null) {
            Log.d(TAG, "Overlay already exists - dismissing to recreate with new type")
            dismiss()
        }

        this.onDismissListener = onDismiss
        this.onCompleteListener = onComplete
        this.onSkipListener = onSkip
        this.onWatchListener = onWatch
        this.currentSessionId = sessionId
        this.sourcePackageName = sourcePackage

        // 오버레이 뷰 생성
        Log.d(TAG, "Inflating overlay view")
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_flip_phone, null)
        skipButton = overlayView!!.findViewById(R.id.skipButton)
        watchButton = overlayView!!.findViewById(R.id.watchButton)
        startTimerButton = overlayView!!.findViewById(R.id.startTimerButton)
        mainMessage = overlayView!!.findViewById(R.id.mainMessage)
        tipMessage = overlayView!!.findViewById(R.id.tipMessage)
        buttonContainer = overlayView!!.findViewById(R.id.buttonContainer)
        affiliateBannerContainer = overlayView!!.findViewById(R.id.affiliateBannerContainer)
        brandWatermark = overlayView!!.findViewById(R.id.brandWatermark)

        Log.d(TAG, "Overlay view inflated successfully")

        // PreferencesManager 초기화 및 Tip 메시지 설정
        prefsManager = PreferencesManager(context)
        setupTipMessage()

        // Affiliate Banner 로드
        setupAffiliateBanner()

        // 오버레이 타입에 따라 UI 결정
        val isTimerCompleted = overlayType == OverlayType.CONFIRMATION

        Log.d(TAG, "Overlay type determines UI - isTimerCompleted: $isTimerCompleted")

        // Track overlay shown event
        AnalyticsManager.trackEvent(
            context,
            if (isTimerCompleted) AnalyticsEvent.OVERLAY_SHOWN_AFTER_TIMER else AnalyticsEvent.OVERLAY_SHOWN_BEFORE_TIMER
        )

        // Show appropriate buttons and messages based on timer status
        if (isTimerCompleted) {
            Log.d(TAG, ">>> UI: Timer completed mode - showing CONFIRMATION buttons")
            // Timer completed - swap button order
            // "아니요, 안 볼래요" becomes primary (white, top)
            // "네, 볼래요" becomes secondary (transparent, bottom)
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE
            Log.d(TAG, ">>> UI: startTimerButton GONE, watchButton VISIBLE")

            // Reorder buttons: skipButton first, then watchButton
            buttonContainer.removeAllViews()

            // Add skip button first (top position)
            skipButton.text = context.getString(R.string.block_button_no)
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
            watchParams.topMargin = context.resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(watchButton, watchParams)

            mainMessage.text = context.getString(R.string.block_message_after_timer)
            Log.d(TAG, ">>> UI: CONFIRMATION mode complete - skipButton/watchButton visible, startTimer GONE")
        } else {
            Log.d(TAG, ">>> UI: INITIAL mode - showing start timer button")
            // Timer not completed - default order
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
            skipButton.text = context.getString(R.string.block_button_close)
            Log.d(TAG, ">>> UI: startTimerButton VISIBLE, watchButton GONE")

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
            skipParams.topMargin = context.resources.displayMetrics.density.toInt() * 12
            buttonContainer.addView(skipButton, skipParams)

            mainMessage.text = context.getString(R.string.block_message_before_timer)
        }

        // "안볼래요" 버튼 클릭 리스너 설정
        skipButton.setOnClickListener {
            Log.d(TAG, ">>> Skip button clicked in BlockOverlay")

            // Track skip button click
            AnalyticsManager.trackEvent(context, AnalyticsEvent.OVERLAY_BUTTON_SKIP)

            Log.d(TAG, "Calling dismiss() from skip button")
            // 오버레이 닫기
            dismiss()
            Log.d(TAG, "Calling onSkipListener")
            // 백키 누르기 콜백 호출
            onSkipListener?.invoke()
            Log.d(TAG, "Calling onDismissListener")
            // dismiss 리스너는 마지막에 호출
            onDismissListener?.invoke()
            Log.d(TAG, "Skip button handler complete")
        }

        // "볼래요" 버튼 클릭 리스너 설정 (초기에는 숨겨져 있음)
        watchButton.setOnClickListener {
            Log.d(TAG, ">>> Watch button clicked in BlockOverlay")

            // Track watch button click
            AnalyticsManager.trackEvent(context, AnalyticsEvent.OVERLAY_BUTTON_WATCH)

            Log.d(TAG, "Calling dismiss() from watch button")
            // 오버레이 닫기
            dismiss()
            Log.d(TAG, "Calling onWatchListener")
            // 미디어 재생 콜백 호출
            onWatchListener?.invoke()
            Log.d(TAG, "Calling onDismissListener")
            // dismiss 리스너는 마지막에 호출
            onDismissListener?.invoke()
            Log.d(TAG, "Watch button handler complete")
        }

        // "타이머 시작하기" 버튼 클릭 리스너 설정
        startTimerButton.setOnClickListener {
            Log.d(TAG, ">>> Start Timer button clicked in BlockOverlay")

            // Track start timer button click
            AnalyticsManager.trackEvent(context, AnalyticsEvent.OVERLAY_BUTTON_START_TIMER)

            // ShortsBlockTimerActivity로 이동
            val intent = Intent(context, com.muuu.unshort.timer.ShortsBlockTimerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("session_id", currentSessionId)
                putExtra("source_package", sourcePackageName)
            }
            context.startActivity(intent)
            Log.d(TAG, "Started ShortsBlockTimerActivity with session: $currentSessionId, source: $sourcePackageName")

            // 오버레이 dismiss (TimerActivity가 보이도록)
            dismiss()
            Log.d(TAG, "Overlay dismissed to show TimerActivity")

            // onDismissListener는 호출하지 않음 (타이머로 이동한 것이므로 정상 플로우)
        }

        // 윈도우 매니저 파라미터 설정 - 실제 화면 + 상태바 + 네비게이션바 전체 덮기
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val statusBarHeight = getStatusBarHeight()
        val navigationBarHeight = getNavigationBarHeight()
        val totalHeight = screenHeight + statusBarHeight + navigationBarHeight
        Log.d(TAG, "Screen: $screenHeight, Status bar: $statusBarHeight, Nav bar: $navigationBarHeight, Total: $totalHeight")

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            totalHeight,  // 실제 화면 + 상태바 + 네비게이션바
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 모든 터치 완전 차단 + 홈/알림창 방지
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or  // 화면 꺼짐 방지
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or  // 잠금화면 위에도 표시
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,  // 화면 켜기
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        Log.d(TAG, "Adding view to window manager")
        try {
            windowManager.addView(overlayView, params)
            Log.d(TAG, "View added to window manager successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding view to window manager", e)
            throw e
        }

        // 뷰 자체를 클릭 가능하게 설정 (터치 이벤트 소비)
        overlayView!!.isClickable = true
        overlayView!!.isFocusable = true

        // 시스템 UI 숨기기 (알림창 차단)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ : WindowInsetsController 사용
            overlayView!!.windowInsetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // API 26-29 : systemUiVisibility 사용 (이 버전에서는 deprecated 아님)
            @Suppress("DEPRECATION")
            overlayView!!.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Periodic check 제거 - overlayType 파라미터로 UI 결정
        // (periodic check가 UI를 덮어씌우는 문제 해결)

        // Window focus listener 제거 - overlayType으로 UI 결정됨
    }


    fun dismiss() {
        Log.d(TAG, ">>> dismiss() called")

        // WebView 정리
        if (::affiliateBannerContainer.isInitialized) {
            // 컨테이너 내부의 WebView 정리
            for (i in 0 until affiliateBannerContainer.childCount) {
                val child = affiliateBannerContainer.getChildAt(i)
                if (child is WebView) {
                    child.loadUrl("about:blank")
                    child.destroy()
                }
            }
        }

        overlayView?.let {
            Log.d(TAG, "Removing overlay view from WindowManager")
            windowManager.removeView(it)
            overlayView = null
            Log.d(TAG, "overlayView set to null")
        }
    }

    fun isShowing(): Boolean {
        val showing = overlayView != null
        Log.d(TAG, "isShowing() = $showing")
        return showing
    }

    // updateButtonVisibility, startPeriodicCheck, stopPeriodicCheck 제거
    // overlayType 파라미터로 UI가 한 번 설정되면 변경하지 않음

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun setupTipMessage() {
        val tips = context.resources.getStringArray(R.array.overlay_tips)
        val randomIndex = tips.indices.random()
        val message = tips[randomIndex]

        tipMessage.text = message

        Log.d(TAG, "Tip message set: $message (random index: $randomIndex)")
    }

    /**
     * Affiliate 배너 설정 (Provider를 통해)
     */
    private fun setupAffiliateBanner() {
        try {
            // 프리미엄 유저는 배너 숨김
            if (PremiumManager.isPremium()) {
                affiliateBannerContainer.visibility = View.GONE
                brandWatermark.visibility = View.VISIBLE
                Log.d(TAG, "Premium user - affiliate banner hidden, watermark visible")
                return
            }

            // Affiliate Provider로부터 배너 뷰 생성 (Analytics 콜백 포함)
            val provider = AffiliateProviderFactory.create(
                context = context,
                onLinkClick = { url ->
                    // Affiliate 링크 클릭 시 Analytics 트래킹
                    AnalyticsManager.trackEvent(context, AnalyticsEvent.AFFILIATE_PRODUCT_CLICKED)
                    Log.d(TAG, "Affiliate link clicked: $url")

                    // 오버레이 닫기
                    dismiss()

                    onDismissListener?.invoke()
                }
            )
            val bannerView = provider.createBannerView(context)

            // 컨테이너에 배너 추가
            affiliateBannerContainer.removeAllViews()
            affiliateBannerContainer.addView(bannerView)
            affiliateBannerContainer.visibility = View.VISIBLE

            // Affiliate 배너가 있으면 워터마크 숨김 (레이아웃 유지)
            brandWatermark.visibility = View.INVISIBLE

            Log.d(TAG, "Affiliate banner loaded successfully via Provider, watermark hidden")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading affiliate banner", e)
            affiliateBannerContainer.visibility = View.GONE
            // 배너 로드 실패 시 워터마크 표시
            brandWatermark.visibility = View.VISIBLE
        }
    }
}
