package com.muuu.unshort

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class BlockOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var flipDetector: FlipDetector? = null
    private var countDownTimer: CountDownTimer? = null
    private val TAG = "BlockOverlay"

    private lateinit var timerText: TextView
    private lateinit var secondsLabel: TextView
    private lateinit var flipStatusText: TextView
    private lateinit var skipButton: TextView
    private lateinit var watchButton: TextView
    private lateinit var buttonSpacer: View
    private lateinit var flipStatusIndicator: View

    private var isPhoneFlipped = false
    private var remainingSeconds = 15
    private var onDismissListener: (() -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var onSkipListener: (() -> Unit)? = null
    private var onWatchListener: (() -> Unit)? = null

    @SuppressLint("InflateParams")
    fun show(onDismiss: () -> Unit, onComplete: () -> Unit, onSkip: (() -> Unit)? = null, onWatch: (() -> Unit)? = null) {
        Log.d(TAG, "show() called")
        if (overlayView != null) {
            Log.d(TAG, "Overlay already showing, ignoring")
            return
        }

        this.onDismissListener = onDismiss
        this.onCompleteListener = onComplete
        this.onSkipListener = onSkip
        this.onWatchListener = onWatch

        // SharedPreferences에서 설정된 딜레이 시간 읽기 (기본값 30초)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        remainingSeconds = prefs.getInt("wait_time", 30)

        // 오버레이 뷰 생성
        Log.d(TAG, "Inflating overlay view")
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_flip_phone, null)
        timerText = overlayView!!.findViewById(R.id.timerText)
        secondsLabel = overlayView!!.findViewById(R.id.secondsLabel)
        flipStatusText = overlayView!!.findViewById(R.id.flipStatusText)
        skipButton = overlayView!!.findViewById(R.id.skipButton)
        watchButton = overlayView!!.findViewById(R.id.watchButton)
        buttonSpacer = overlayView!!.findViewById(R.id.buttonSpacer)
        flipStatusIndicator = overlayView!!.findViewById(R.id.flipStatusIndicator)
        Log.d(TAG, "Overlay view inflated successfully")

        // "안볼래요" 버튼 클릭 리스너 설정
        skipButton.setOnClickListener {
            Log.d(TAG, "Skip button clicked, dismissing overlay and pressing back")
            // 오버레이 닫기
            dismiss()
            // 백키 누르기 콜백 호출
            onSkipListener?.invoke()
            // dismiss 리스너는 마지막에 호출
            onDismissListener?.invoke()
        }

        // "볼래요" 버튼 클릭 리스너 설정 (초기에는 숨겨져 있음)
        watchButton.setOnClickListener {
            Log.d(TAG, "Watch button clicked, dismissing overlay and resuming media")
            // 오버레이 닫기
            dismiss()
            // 미디어 재생 콜백 호출
            onWatchListener?.invoke()
            // dismiss 리스너는 마지막에 호출
            onDismissListener?.invoke()
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

        // 초기 타이머 표시
        timerText.text = remainingSeconds.toString()

        // 폰 뒤집기 감지 시작
        flipDetector = FlipDetector(context)
        flipDetector?.start(object : FlipDetector.FlipListener {
            override fun onFlipDetected(isFlipped: Boolean) {
                isPhoneFlipped = isFlipped
                updateFlipStatus()

                if (isFlipped && countDownTimer == null) {
                    startCountdown()
                } else if (!isFlipped && countDownTimer != null) {
                    pauseCountdown()
                }
            }
        })

        updateFlipStatus()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                timerText.text = remainingSeconds.toString()

                // 폰이 다시 뒤집어지면 일시정지
                if (!isPhoneFlipped) {
                    pauseCountdown()
                }
            }

            override fun onFinish() {
                // 타이머 완료 - 버튼 전환
                Log.d(TAG, "Timer finished, showing watch button")
                onTimerComplete()
            }
        }.start()
    }

    private fun pauseCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun onTimerComplete() {
        // 타이머 텍스트를 "완료"로 변경
        timerText.text = "✓"
        timerText.textSize = 72f
        timerText.setTextColor(context.getColor(android.R.color.holo_green_light))

        // "초" 라벨 숨기기
        secondsLabel.visibility = View.GONE

        // 햅틱 피드백 제공
        provideHapticFeedback()

        // 플립 감지 중지
        flipDetector?.stop()

        // 플립 상태 UI 숨기기
        flipStatusText.visibility = View.GONE
        flipStatusIndicator.visibility = View.GONE

        // 두 버튼 모두 표시: 안볼래요 유지하고 볼래요 추가
        skipButton.visibility = View.VISIBLE  // 안볼래요 버튼 유지
        buttonSpacer.visibility = View.VISIBLE  // 버튼 사이 간격
        watchButton.visibility = View.VISIBLE  // 볼래요 버튼 표시

        // 완료 콜백 호출
        onCompleteListener?.invoke()
    }

    private fun provideHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Android 8.0 이상: VibrationEffect 사용 (짧고 부드러운 진동)
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Android 8.0 미만: 기본 진동
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
                Log.d(TAG, "Haptic feedback provided")
            } else {
                Log.d(TAG, "Vibrator not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error providing haptic feedback", e)
        }
    }

    private fun updateFlipStatus() {
        if (isPhoneFlipped) {
            flipStatusText.text = context.getString(R.string.flip_detected)
            // 녹색으로 인디케이터 변경
            flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
            flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(android.R.color.holo_green_dark)
            )
        } else {
            flipStatusText.text = context.getString(R.string.flip_required)
            // 빨간색으로 인디케이터 변경
            flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
            flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(android.R.color.holo_red_dark)
            )
        }
    }

    fun dismiss() {
        countDownTimer?.cancel()
        countDownTimer = null

        flipDetector?.stop()
        flipDetector = null

        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    fun isShowing(): Boolean = overlayView != null

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
