package com.muuu.unshort

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
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
    private lateinit var progressBar: ProgressBar
    private lateinit var skipButton: TextView
    private lateinit var motivationText: TextView
    private lateinit var mainContent: View
    private lateinit var successScreen: View
    private lateinit var continueButton: TextView

    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = 30
    private lateinit var prefs: SharedPreferences
    private lateinit var currentSessionId: String

    private val TAG = "TimerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // Get session ID from intent
        currentSessionId = intent.getStringExtra("session_id") ?: ""
        Log.d(TAG, "onCreate with session_id: $currentSessionId")

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        initViews()
        startTimer()
    }

    private fun initViews() {
        timerText = findViewById(R.id.timerNumber)
        secondsLabel = findViewById(R.id.timerUnit)
        flipIndicator = findViewById(R.id.flipIndicator)
        flipStatusText = findViewById(R.id.flipText)
        progressBar = findViewById(R.id.progressRing)
        skipButton = findViewById(R.id.skipButton)
        motivationText = findViewById(R.id.motivationText)
        mainContent = findViewById(R.id.mainContent)
        successScreen = findViewById(R.id.successScreen)
        continueButton = findViewById(R.id.continueButton)

        // Skip button (always visible)
        skipButton.setOnClickListener {
            // Cancel timer
            countDownTimer?.cancel()
            finish()
        }

        // Continue button (on success screen)
        continueButton.setOnClickListener {
            // Mark timer as completed for this session
            if (currentSessionId.isNotEmpty()) {
                prefs.edit().putString(AppConstants.PREF_COMPLETED_SESSION_ID, currentSessionId).apply()
                Log.d(TAG, "Timer completed for session: $currentSessionId")
            }
            finish()
        }

        // Set initial values
        timerText.text = "30"
        progressBar.max = 30 * 100 // 100x for ultra smooth animation
        progressBar.progress = 30 * 100
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(30000, 10) { // Update every 10ms for ultra smooth animation
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                val progressValue = ((millisUntilFinished / 10).toInt())

                timerText.text = remainingSeconds.toString()
                progressBar.progress = progressValue
            }

            override fun onFinish() {
                // Timer completed - show success screen
                timerText.text = "0"
                progressBar.progress = 0

                // Mark timer as completed for this session
                if (currentSessionId.isNotEmpty()) {
                    prefs.edit().putString(AppConstants.PREF_COMPLETED_SESSION_ID, currentSessionId).apply()
                    Log.d(TAG, "Timer completed for session: $currentSessionId")
                }

                showSuccessScreen()
            }
        }.start()
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

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
