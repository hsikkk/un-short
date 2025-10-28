package com.shortblock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
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

    private lateinit var timerText: TextView
    private lateinit var flipStatusText: TextView

    private var isPhoneFlipped = false
    private var remainingSeconds = 30
    private var onDismissListener: (() -> Unit)? = null

    @SuppressLint("InflateParams")
    fun show(onDismiss: () -> Unit) {
        if (overlayView != null) return

        this.onDismissListener = onDismiss

        // 오버레이 뷰 생성
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_flip_phone, null)
        timerText = overlayView!!.findViewById(R.id.timerText)
        flipStatusText = overlayView!!.findViewById(R.id.flipStatusText)

        // 윈도우 매니저 파라미터 설정
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        windowManager.addView(overlayView, params)

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
                // 30초 완료 - 오버레이 제거
                dismiss()
                onDismissListener?.invoke()
            }
        }.start()
    }

    private fun pauseCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun updateFlipStatus() {
        if (isPhoneFlipped) {
            flipStatusText.text = context.getString(R.string.flip_detected)
            flipStatusText.setTextColor(context.getColor(android.R.color.holo_green_dark))
        } else {
            flipStatusText.text = context.getString(R.string.flip_required)
            flipStatusText.setTextColor(context.getColor(android.R.color.holo_red_dark))
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
}
