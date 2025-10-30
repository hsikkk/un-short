package com.muuu.unshort

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class TimerActivity : AppCompatActivity() {

    private lateinit var flipDetector: FlipDetector
    private var countDownTimer: CountDownTimer? = null
    private var isPhoneFlipped = false
    private var remainingSeconds = 30
    private var timerCompleted = false
    private lateinit var prefs: SharedPreferences
    private lateinit var currentSessionId: String

    // UI elements
    private lateinit var timerText: TextView
    private lateinit var secondsLabel: TextView
    private lateinit var flipStatusText: TextView
    private lateinit var flipStatusCard: CardView
    private lateinit var flipStatusIndicator: View
    private lateinit var instructionText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cancelButton: TextView
    private lateinit var completeButton: TextView

    private val TAG = "TimerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // Get session ID from intent
        currentSessionId = intent.getStringExtra("session_id") ?: ""
        if (currentSessionId.isEmpty()) {
            Log.e(TAG, "No session ID provided")
            finish()
            return
        }

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Check if timer already completed for this session
        val completedSessionId = prefs.getString(AppConstants.PREF_COMPLETED_SESSION_ID, "")
        if (completedSessionId == currentSessionId) {
            // Timer already completed for this session
            showAlreadyCompleted()
            return
        }

        initViews()
        setupTimer()
        startFlipDetection()
    }

    private fun initViews() {
        timerText = findViewById(R.id.timerText)
        secondsLabel = findViewById(R.id.secondsLabel)
        flipStatusText = findViewById(R.id.flipStatusText)
        flipStatusCard = findViewById(R.id.flipStatusCard)
        flipStatusIndicator = findViewById(R.id.flipStatusIndicator)
        instructionText = findViewById(R.id.instructionText)
        progressBar = findViewById(R.id.timerProgressBar)
        cancelButton = findViewById(R.id.cancelButton)
        completeButton = findViewById(R.id.completeButton)

        // Initially hide complete button
        completeButton.visibility = View.GONE

        // Set up cancel button
        cancelButton.setOnClickListener {
            showCancelConfirmation()
        }

        // Set up complete button
        completeButton.setOnClickListener {
            returnToOverlay()
        }

        // Set initial timer text
        timerText.text = remainingSeconds.toString()
        progressBar.max = remainingSeconds
        progressBar.progress = remainingSeconds
    }

    private fun setupTimer() {
        // Check for existing timer state
        val savedRemainingSeconds = prefs.getInt("timer_remaining_seconds_$currentSessionId", -1)
        if (savedRemainingSeconds > 0) {
            remainingSeconds = savedRemainingSeconds
            timerText.text = remainingSeconds.toString()
            progressBar.progress = remainingSeconds
        } else {
            // Get wait time from settings
            remainingSeconds = prefs.getInt("wait_time", 30)
            progressBar.max = remainingSeconds
            progressBar.progress = remainingSeconds
        }
    }

    private fun startFlipDetection() {
        flipDetector = FlipDetector(this)
        flipDetector.start(object : FlipDetector.FlipListener {
            override fun onFlipDetected(isFlipped: Boolean) {
                runOnUiThread {
                    isPhoneFlipped = isFlipped
                    updateFlipStatus()

                    if (isFlipped && countDownTimer == null && !timerCompleted) {
                        startCountdown()
                    } else if (!isFlipped && countDownTimer != null) {
                        pauseCountdown()
                    }
                }
            }
        })
        updateFlipStatus()
    }

    private fun updateFlipStatus() {
        if (timerCompleted) {
            flipStatusText.text = "완료!"
            flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
            flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.holo_green_dark)
            )
            instructionText.text = "타이머가 완료되었습니다"
            return
        }

        if (isPhoneFlipped) {
            flipStatusText.text = "진행중"
            flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
            flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.holo_green_dark)
            )
            instructionText.text = "폰을 뒤집어 놓은 상태를 유지하세요"
        } else {
            flipStatusText.text = "대기중"
            flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
            flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(android.R.color.holo_red_dark)
            )
            instructionText.text = "폰을 뒤집어 놓으면\n타이머가 시작됩니다"
        }
    }

    private fun startCountdown() {
        // Haptic feedback removed for flip transitions

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                timerText.text = remainingSeconds.toString()
                progressBar.progress = remainingSeconds

                // Save progress
                prefs.edit()
                    .putInt("timer_remaining_seconds_$currentSessionId", remainingSeconds)
                    .apply()

                // Check if phone is still flipped
                if (!isPhoneFlipped) {
                    pauseCountdown()
                }
            }

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
    }

    private fun pauseCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null

        // Haptic feedback removed for flip transitions
    }

    private fun onTimerComplete() {
        timerCompleted = true

        // Update UI
        timerText.text = "✓"
        timerText.textSize = 72f
        timerText.setTextColor(getColor(android.R.color.holo_green_light))
        secondsLabel.visibility = View.GONE
        progressBar.progress = 0

        // Stop flip detection
        flipDetector.stop()

        // Update flip status with completion message
        flipStatusText.text = "완료!"
        flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
        flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
            getColor(android.R.color.holo_green_dark)
        )
        instructionText.text = "타이머가 완료되었습니다!\n이제 쇼츠를 볼 수 있습니다."

        // Show complete button, hide cancel button
        completeButton.visibility = View.VISIBLE
        cancelButton.visibility = View.GONE

        // Save completion state with timestamp
        prefs.edit()
            .putString(AppConstants.PREF_COMPLETED_SESSION_ID, currentSessionId)
            .putLong("timer_completed_time", System.currentTimeMillis()) // Save completion timestamp
            .remove("timer_remaining_seconds_$currentSessionId")
            .apply()

        // Send broadcast to service
        val intent = Intent(AppConstants.ACTION_TIMER_COMPLETED)
        intent.putExtra("session_id", currentSessionId)
        sendBroadcast(intent)

        // Strong haptic feedback
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    private fun provideHapticFeedback() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isHapticEnabled = prefs.getBoolean("haptic_enabled", true)

        if (!isHapticEnabled) return

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error providing haptic feedback", e)
        }
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("타이머 취소")
            .setMessage("타이머를 취소하시겠습니까?\n쇼츠를 보려면 처음부터 다시 시작해야 합니다.")
            .setPositiveButton("취소하기") { _, _ ->
                // Cancel timer
                countDownTimer?.cancel()
                flipDetector.stop()

                // Clear timer state
                prefs.edit()
                    .remove("timer_remaining_seconds_$currentSessionId")
                    .apply()

                // Send broadcast
                val intent = Intent(AppConstants.ACTION_TIMER_CANCELLED)
                intent.putExtra("session_id", currentSessionId)
                sendBroadcast(intent)

                finish()
            }
            .setNegativeButton("계속하기", null)
            .show()
    }

    private fun showAlreadyCompleted() {
        // Timer already completed, show return button
        setContentView(R.layout.activity_timer)
        initViews()

        timerCompleted = true
        timerText.text = "✓"
        timerText.textSize = 72f
        timerText.setTextColor(getColor(android.R.color.holo_green_light))
        secondsLabel.visibility = View.GONE
        progressBar.progress = 0

        completeButton.visibility = View.VISIBLE
        cancelButton.visibility = View.GONE

        flipStatusText.text = "완료!"
        flipStatusIndicator.setBackgroundResource(R.drawable.circle_shape)
        flipStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
            getColor(android.R.color.holo_green_dark)
        )
        instructionText.text = "이미 타이머를 완료했습니다!\n쇼츠로 돌아갈 수 있습니다."
    }

    private fun returnToOverlay() {
        // If timer is completed, ensure broadcast is sent before returning
        if (timerCompleted) {
            // Re-send broadcast to ensure overlay updates
            val intent = Intent(AppConstants.ACTION_TIMER_COMPLETED)
            intent.putExtra("session_id", currentSessionId)
            sendBroadcast(intent)
            Log.d(TAG, "Re-sent timer completion broadcast before returning")
        }

        // Simply finish the activity to return to overlay
        finish()
    }

    override fun onResume() {
        super.onResume()
        // If timer is already completed, send broadcast to update overlay
        if (timerCompleted) {
            val intent = Intent(AppConstants.ACTION_TIMER_COMPLETED)
            intent.putExtra("session_id", currentSessionId)
            sendBroadcast(intent)
            Log.d(TAG, "Timer already completed, sent broadcast in onResume")
        }
    }

    override fun onBackPressed() {
        if (!timerCompleted) {
            showCancelConfirmation()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (::flipDetector.isInitialized) {
            flipDetector.stop()
        }
    }
}