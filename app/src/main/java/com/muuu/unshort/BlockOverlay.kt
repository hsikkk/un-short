package com.muuu.unshort

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val TAG = "BlockOverlay"

    private lateinit var skipButton: TextView
    private lateinit var watchButton: TextView
    private lateinit var startTimerButton: TextView
    private lateinit var buttonSpacer: View

    private var onDismissListener: (() -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var onSkipListener: (() -> Unit)? = null
    private var onWatchListener: (() -> Unit)? = null
    private var currentSessionId: String = ""

    @SuppressLint("InflateParams")
    fun show(onDismiss: () -> Unit, onComplete: () -> Unit, onSkip: (() -> Unit)? = null, onWatch: (() -> Unit)? = null, sessionId: String = "") {
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

        // 오버레이 뷰 생성
        Log.d(TAG, "Inflating overlay view")
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_flip_phone, null)
        skipButton = overlayView!!.findViewById(R.id.skipButton)
        watchButton = overlayView!!.findViewById(R.id.watchButton)
        startTimerButton = overlayView!!.findViewById(R.id.startTimerButton)
        buttonSpacer = overlayView!!.findViewById(R.id.buttonSpacer)
        Log.d(TAG, "Overlay view inflated successfully")

        // Check timer completion status
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val completedSessionId = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, "")
        val isTimerCompleted = sessionId.isNotEmpty() && sessionId == completedSessionId

        Log.d(TAG, "Timer completion check - currentSession: $sessionId, completedSession: $completedSessionId, isCompleted: $isTimerCompleted")

        // Show appropriate buttons based on timer status
        if (isTimerCompleted) {
            // Timer completed, show watch button
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE
        } else {
            // Timer not completed, show timer button
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
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
            }
            context.startActivity(intent)
            Log.d(TAG, "Started TimerActivity with session: $currentSessionId")
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
    }


    fun dismiss() {
        Log.d(TAG, ">>> dismiss() called")

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
        if (overlayView == null) return

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val completedSessionId = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, "")
        val isTimerCompleted = currentSessionId.isNotEmpty() && currentSessionId == completedSessionId

        Log.d(TAG, "updateButtonVisibility - session: $currentSessionId, completed: $completedSessionId, isCompleted: $isTimerCompleted")

        if (isTimerCompleted) {
            // Timer completed, show watch button
            startTimerButton.visibility = View.GONE
            watchButton.visibility = View.VISIBLE
        } else {
            // Timer not completed, show timer button
            startTimerButton.visibility = View.VISIBLE
            watchButton.visibility = View.GONE
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
}
