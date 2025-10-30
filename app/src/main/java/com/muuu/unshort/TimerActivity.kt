package com.muuu.unshort

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 타이머 Activity - tmp/timer.html 디자인 기반
 */
class TimerActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private lateinit var timerText: TextView
    private lateinit var secondsLabel: TextView
    private lateinit var flipIndicator: View
    private lateinit var flipStatusText: TextView
    private lateinit var phoneIcon: View
    private lateinit var progressBar: ProgressBar
    private lateinit var skipButton: TextView
    private lateinit var motivationText: TextView
    private lateinit var mainContent: View
    private lateinit var successScreen: View
    private lateinit var continueButton: TextView

    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = 30
    private var timerDuration = 30 // 설정에서 읽어올 타이머 시간 (초)
    private var remainingMillis = 0L // 남은 시간 (밀리초)
    private var isTimerRunning = false
    private var isFlipped = false
    private var currentRotation = 0f // 현재 회전 각도 추적
    private lateinit var prefs: SharedPreferences
    private lateinit var currentSessionId: String
    private var sourcePackageName: String = ""
    private lateinit var flipDetector: FlipDetector
    private var forceCloseReceiver: BroadcastReceiver? = null

    private val TAG = "TimerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Screen will stay on during timer")

        // Get session ID and source package from intent
        currentSessionId = intent.getStringExtra("session_id") ?: ""
        sourcePackageName = intent.getStringExtra("source_package") ?: ""
        Log.d(TAG, "onCreate with session_id: $currentSessionId, source_package: $sourcePackageName")

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // 설정에서 타이머 시간 읽어오기 (기본값: 30초)
        timerDuration = prefs.getInt("wait_time", 30)
        remainingSeconds = timerDuration
        remainingMillis = (timerDuration * 1000).toLong()
        Log.d(TAG, "Timer duration set to: $timerDuration seconds")

        initViews()
        initFlipDetector()
        registerForceCloseReceiver()

        // 타이머는 폰이 뒤집혔을 때만 시작
        Log.d(TAG, "Waiting for phone to flip...")
    }

    private fun initViews() {
        timerText = findViewById(R.id.timerNumber)
        secondsLabel = findViewById(R.id.timerUnit)
        flipIndicator = findViewById(R.id.flipIndicator)
        flipStatusText = findViewById(R.id.flipText)
        phoneIcon = findViewById(R.id.phoneIcon)
        progressBar = findViewById(R.id.progressRing)
        skipButton = findViewById(R.id.skipButton)
        motivationText = findViewById(R.id.motivationText)
        mainContent = findViewById(R.id.mainContent)
        successScreen = findViewById(R.id.successScreen)
        continueButton = findViewById(R.id.continueButton)

        // Skip button (always visible) - "그냥 안볼래요"
        skipButton.setOnClickListener {
            // Cancel timer and trigger overlay's skip action (close overlay and return to app)
            countDownTimer?.cancel()
            flipDetector.stop()

            // Send explicit broadcast to close overlay and return to source app
            val intent = android.content.Intent(AppConstants.ACTION_CLOSE_OVERLAY)
            intent.setPackage(packageName)
            intent.putExtra("source_package", sourcePackageName)
            sendBroadcast(intent)
            Log.d(TAG, "Skip button clicked - broadcast sent to close overlay and return to $sourcePackageName")

            // Launch source app and finish this activity
            returnToSourceApp()
        }

        // Continue button (on success screen)
        continueButton.setOnClickListener {
            // Mark timer as completed for this session
            if (currentSessionId.isNotEmpty()) {
                prefs.edit().putString(AppConstants.PREF_COMPLETED_SESSION_ID, currentSessionId).apply()
                Log.d(TAG, "Timer completed for session: $currentSessionId")
            }

            // Launch source app and finish this activity
            returnToSourceApp()
        }

        // Set initial values
        timerText.text = timerDuration.toString()
        progressBar.max = timerDuration * 100 // 100x for ultra smooth animation
        progressBar.progress = timerDuration * 100
    }

    private fun initFlipDetector() {
        flipDetector = FlipDetector(this)
        flipDetector.start(object : FlipDetector.FlipListener {
            override fun onFlipDetected(flipped: Boolean) {
                isFlipped = flipped
                Log.d(TAG, "Flip detected: $flipped")

                if (flipped) {
                    // 폰이 뒤집혔을 때 - 반복 애니메이션 중단하고 180도로 고정
                    phoneIcon.animate().cancel()
                    phoneIcon.clearAnimation() // Clear pending callbacks
                    phoneIcon.animate()
                        .rotationY(180f)
                        .setDuration(400)
                        .start()

                    // Flip 인디케이터 페이드아웃 (공간 유지)
                    flipIndicator.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .start()

                    if (!isTimerRunning) {
                        // 타이머가 아직 시작 안 했으면 시작
                        startTimer()
                    }
                } else {
                    // 폰이 다시 앞면으로 돌아왔을 때 - 반복 애니메이션 재시작
                    phoneIcon.animate().cancel()
                    phoneIcon.clearAnimation() // Clear pending callbacks
                    phoneIcon.rotationY = 0f // Reset rotation immediately
                    currentRotation = 0f // Reset rotation counter

                    // Use postDelayed to ensure isFlipped state is stable
                    phoneIcon.postDelayed({
                        if (!isFlipped) { // Double-check state
                            Log.d(TAG, "Restarting animation after unflip")
                            animatePhoneIcon()
                        }
                    }, 100) // Small delay to ensure state is stable

                    // Flip 인디케이터 페이드인 (공간 유지)
                    flipIndicator.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()

                    flipStatusText.text = getString(R.string.timer_flip_instruction)
                    if (isTimerRunning) {
                        // 타이머 일시정지
                        pauseTimer()
                    }
                }
            }
        })

        // Start initial animation after view is ready
        phoneIcon.post {
            if (!isFlipped) {
                Log.d(TAG, "Starting initial animation")
                animatePhoneIcon()
            }
        }
    }

    private fun animatePhoneIcon() {
        if (isFlipped) {
            Log.d(TAG, "Animation cancelled - phone is flipped")
            return
        }

        Log.d(TAG, "Starting phone icon animation cycle - rotating to ${currentRotation + 360}deg")

        // 첫 번째 반바퀴: 0 → 180도, 투명 → 흰색
        val firstHalfRotation = currentRotation + 180f
        val colorToWhite = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.TRANSPARENT,
            Color.WHITE
        )
        colorToWhite.duration = 900
        colorToWhite.interpolator = AccelerateDecelerateInterpolator()
        colorToWhite.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            val drawable = phoneIcon.background as? GradientDrawable
            drawable?.setColor(color)
        }
        colorToWhite.start()

        phoneIcon.animate()
            .rotationY(firstHalfRotation)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator()) // 처음/끝 느리고 중간 빠르게
            .withEndAction {
                if (isFlipped) {
                    Log.d(TAG, "Animation stopped at 180deg - phone flipped")
                    colorToWhite.cancel()
                    return@withEndAction
                }

                // 두 번째 반바퀴: 180 → 360도, 흰색 → 투명
                currentRotation += 360f
                val colorToTransparent = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    Color.WHITE,
                    Color.TRANSPARENT
                )
                colorToTransparent.duration = 900
                colorToTransparent.interpolator = AccelerateDecelerateInterpolator()
                colorToTransparent.addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    val drawable = phoneIcon.background as? GradientDrawable
                    drawable?.setColor(color)
                }
                colorToTransparent.start()

                phoneIcon.animate()
                    .rotationY(currentRotation)
                    .setDuration(900)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (isFlipped) {
                            Log.d(TAG, "Animation stopped at 360deg - phone flipped")
                            colorToTransparent.cancel()
                            return@withEndAction
                        }
                        // 계속 반복
                        Log.d(TAG, "Animation cycle complete, restarting...")
                        animatePhoneIcon()
                    }
                    .start()
            }
            .start()
    }

    private fun startTimer() {
        if (isTimerRunning) return

        isTimerRunning = true
        Log.d(TAG, "Starting timer with ${remainingMillis}ms remaining")

        countDownTimer = object : CountDownTimer(remainingMillis, 10) { // Update every 10ms for ultra smooth animation
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                val progressValue = ((millisUntilFinished / 10).toInt())

                timerText.text = remainingSeconds.toString()
                progressBar.progress = progressValue
            }

            override fun onFinish() {
                isTimerRunning = false

                // Timer completed - show success screen
                timerText.text = "0"
                progressBar.progress = 0

                // Mark timer as completed for this session
                if (currentSessionId.isNotEmpty()) {
                    prefs.edit().putString(AppConstants.PREF_COMPLETED_SESSION_ID, currentSessionId).apply()
                    Log.d(TAG, "Timer completed for session: $currentSessionId")
                }

                // Stop flip detector
                flipDetector.stop()

                showSuccessScreen()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return

        isTimerRunning = false
        countDownTimer?.cancel()
        Log.d(TAG, "Timer paused with ${remainingMillis}ms remaining")
    }

    private fun showSuccessScreen() {
        mainContent.visibility = View.GONE
        successScreen.visibility = View.VISIBLE

        // Fade in animation for success screen
        successScreen.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )

        // Scale in animation for success icon
        val successIcon = successScreen.findViewById<View>(R.id.successIcon)
        successIcon.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_in)
        )
    }

    private fun returnToSourceApp() {
        Log.d(TAG, "returnToSourceApp called, source: $sourcePackageName")

        // Move this task to back and finish
        moveTaskToBack(true)

        // Finish this activity and all activities in this task
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
            Log.d(TAG, "finishAndRemoveTask() called")
        }, 100)
    }

    private fun registerForceCloseReceiver() {
        forceCloseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AppConstants.ACTION_TIMER_FORCE_CLOSE -> {
                        Log.d(TAG, "Received TIMER_FORCE_CLOSE broadcast, closing activity")
                        finish()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AppConstants.ACTION_TIMER_FORCE_CLOSE)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(forceCloseReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(forceCloseReceiver, filter)
            }
            Log.d(TAG, "Registered force close receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register force close receiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 화면 꺼짐 방지 플래그 제거
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Cleared keep screen on flag")

        countDownTimer?.cancel()
        flipDetector.stop()

        // Unregister force close receiver
        forceCloseReceiver?.let {
            try {
                unregisterReceiver(it)
                forceCloseReceiver = null
                Log.d(TAG, "Unregistered force close receiver")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering force close receiver", e)
            }
        }
    }
}
