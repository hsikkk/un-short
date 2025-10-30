package com.muuu.unshort

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val TAG = "BlockOverlay"

    private lateinit var skipButton: TextView
    private lateinit var watchButton: TextView
    private lateinit var startTimerButton: TextView
    private lateinit var mainMessage: TextView
    private lateinit var buttonContainer: LinearLayout

    private var onDismissListener: (() -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var onSkipListener: (() -> Unit)? = null
    private var onWatchListener: (() -> Unit)? = null
    private var currentSessionId: String = ""
    private var sourcePackageName: String = ""

    // Handler for periodic timer completion checks
    private var checkHandler: Handler? = null
    private var checkRunnable: Runnable? = null

    @SuppressLint("InflateParams")
    fun show(onDismiss: () -> Unit, onComplete: () -> Unit, onSkip: (() -> Unit)? = null, onWatch: (() -> Unit)? = null, sessionId: String = "", sourcePackage: String = "") {
        Log.d(TAG, "show() called with sessionId: $sessionId")
        if (overlayView != null) {
            Log.d(TAG, "Overlay already showing, ignoring")
            return
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
        buttonContainer = overlayView!!.findViewById(R.id.buttonContainer)
        Log.d(TAG, "Overlay view inflated successfully")

        // Check timer completion status
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val completedSessionId = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, "")
        val isTimerCompleted = sessionId.isNotEmpty() && sessionId == completedSessionId

        Log.d(TAG, "Timer completion check - currentSession: $sessionId, completedSession: $completedSessionId, isCompleted: $isTimerCompleted")

        // Show appropriate buttons and messages based on timer status
        if (isTimerCompleted) {
            // Timer completed - swap button order
            // "아니요, 안 볼래요" becomes primary (white, top)
            // "네, 볼래요" becomes secondary (transparent, bottom)
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE

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
        } else {
            // Timer not completed - default order
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
            skipButton.text = context.getString(R.string.block_button_close)

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
            // TimerActivity로 이동
            val intent = Intent(context, TimerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("session_id", currentSessionId)
                putExtra("source_package", sourcePackageName)
            }
            context.startActivity(intent)
            Log.d(TAG, "Started TimerActivity with session: $currentSessionId, source: $sourcePackageName")
            // 오버레이는 유지 (타이머 완료 후 돌아올 예정)
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
        overlayView!!.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Start periodic check for timer completion (fallback mechanism)
        startPeriodicCheck()

        // Add window focus listener for immediate check when returning
        overlayView!!.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                Log.d(TAG, "Window gained focus, checking timer completion state")
                updateButtonVisibility()
            }
        }
    }


    fun dismiss() {
        Log.d(TAG, ">>> dismiss() called")

        // Stop periodic checks
        stopPeriodicCheck()

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

    fun updateButtonVisibility() {
        Log.d(TAG, "updateButtonVisibility() called")

        if (overlayView == null) {
            Log.d(TAG, "updateButtonVisibility - overlayView is null, returning")
            return
        }

        // Ensure button references are valid
        if (!::startTimerButton.isInitialized || !::watchButton.isInitialized) {
            Log.d(TAG, "updateButtonVisibility - buttons not initialized, returning")
            return
        }

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val completedSessionId = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, "")

        // Timer is considered completed if session matches exactly
        // Since session ID is now hash-based (package + date), same app on same day will have same session
        val isTimerCompleted = currentSessionId.isNotEmpty() && currentSessionId == completedSessionId

        Log.d(TAG, "updateButtonVisibility - currentSession: '$currentSessionId', completedSession: '$completedSessionId'")
        Log.d(TAG, "updateButtonVisibility - isCompleted: $isTimerCompleted")

        try {
            if (isTimerCompleted) {
                // Timer completed - swap button order
                Log.d(TAG, "Timer completed - showing watch button, hiding timer button")
                startTimerButton.visibility = View.GONE
                watchButton.visibility = View.VISIBLE

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
            } else {
                // Timer not completed - default order
                Log.d(TAG, "Timer not completed - showing timer button, hiding watch button")
                startTimerButton.visibility = View.VISIBLE
                watchButton.visibility = View.GONE
                skipButton.text = context.getString(R.string.block_button_close)

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
            Log.d(TAG, "updateButtonVisibility completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating button visibility", e)
        }
    }

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

    private fun startPeriodicCheck() {
        // Stop any existing check first
        stopPeriodicCheck()

        Log.d(TAG, "Starting periodic check for session: $currentSessionId")

        checkHandler = Handler(Looper.getMainLooper())
        checkRunnable = object : Runnable {
            override fun run() {
                if (overlayView != null) {
                    Log.d(TAG, "Periodic check running for session: $currentSessionId")
                    // Check timer completion state every second
                    updateButtonVisibility()
                    checkHandler?.postDelayed(this, 1000) // Check every second
                } else {
                    Log.d(TAG, "Periodic check: overlayView is null, stopping check")
                }
            }
        }
        // Start checking immediately
        checkHandler?.post(checkRunnable!!)
        Log.d(TAG, "Started periodic timer completion check for session: $currentSessionId")
    }

    private fun stopPeriodicCheck() {
        checkRunnable?.let {
            checkHandler?.removeCallbacks(it)
        }
        checkHandler = null
        checkRunnable = null
        Log.d(TAG, "Stopped periodic timer completion check")
    }
}
