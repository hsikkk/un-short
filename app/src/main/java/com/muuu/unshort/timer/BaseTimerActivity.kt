package com.muuu.unshort.timer

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuNativeAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.ad.core.model.nativetemplate.MuuuNativeAdTemplate
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.view.MuuuNativeAdView
import com.muuu.unshort.AdConfig
import com.muuu.unshort.AppConstants
import com.muuu.unshort.BaseActivity
import com.muuu.unshort.FlipDetector
import com.muuu.unshort.R
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.prefs.PreferencesManager

/**
 * 타이머 기반 Activity의 베이스 클래스
 *
 * 책임:
 * - 30초 카운트다운 타이머
 * - 폰 뒤집기 감지
 * - UI 애니메이션 (프로그레스, 폰 아이콘)
 * - 햅틱 피드백
 * - 광고 표시 (프리미엄에서 제거)
 *
 * 하위 클래스 구현 필요:
 * - getLayoutResourceId(): 레이아웃 리소스 ID
 * - onTimerCompleted(): 타이머 완료 시 액션
 * - onSkipClicked(): 스킵 버튼 클릭 액션
 */
abstract class BaseTimerActivity : BaseActivity() {

    protected companion object {
        const val TAG = "BaseTimerActivity"
    }

    override fun isLightStatusBar(): Boolean = false

    // Views
    protected lateinit var timerText: TextView
    protected lateinit var secondsLabel: TextView
    protected lateinit var flipIndicator: View
    protected lateinit var flipStatusText: TextView
    protected lateinit var phoneIcon: View
    protected lateinit var progressBar: ProgressBar
    protected lateinit var skipButton: TextView
    protected lateinit var motivationText: TextView
    protected lateinit var mainContent: View
    protected lateinit var successScreen: View
    protected lateinit var continueButton: TextView
    protected var bannerAdView: MuuuBannerAdView? = null
    protected var nativeAdView: MuuuNativeAdView? = null

    // Timer state
    private var countDownTimer: CountDownTimer? = null
    protected var remainingSeconds = 30
    protected var timerDuration = 30
    private var remainingMillis = 0L
    private var isTimerRunning = false
    protected var isFlipped = false
    private var accumulatedRotation = 0f

    // Utils
    protected lateinit var prefsManager: PreferencesManager
    protected lateinit var flipDetector: FlipDetector
    private var forceCloseReceiver: BroadcastReceiver? = null

    // 추상 메서드: 하위 클래스에서 구현 필요
    protected abstract fun getLayoutResourceId(): Int
    protected abstract fun provideTimerDuration(): Int
    protected abstract fun onTimerCompleted()
    protected abstract fun onSkipClicked()
    protected open fun shouldShowAd(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())

        // 화면 꺼짐 방지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefsManager = PreferencesManager(this)

        // 타이머 시간 설정 (하위 클래스가 결정)
        timerDuration = provideTimerDuration()
        remainingSeconds = timerDuration
        remainingMillis = (timerDuration * 1000).toLong()

        setupAds()
        initViews()
        initFlipDetector()
        registerForceCloseReceiver()
    }

    private fun setupAds() {
        if (!shouldShowAd()) return

        // 네이티브 광고 (상단)
        val nativeAdContainer = findViewById<FrameLayout>(R.id.nativeAd)
        nativeAdView = AdManager.setupNativeAd(
            activity = this,
            container = nativeAdContainer,
            adUnit = MuuuNativeAdUnit(
                key = AdConfig.NATIVE_TIMER_TOP,
                placement = "timer_screen_top",
                refreshInterval = 7
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
            activity = this,
            container = adViewContainer,
            adUnit = MuuuBannerAdUnit(
                key = AdConfig.BANNER_TIMER_BOTTOM,
                placement = "timer_screen_bottom",
                bannerSize = MuuuBannerSize.Banner,
                refreshInterval = 4
            )
        )
    }

    protected open fun initViews() {
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

        // Skip button
        skipButton.setOnClickListener {
            countDownTimer?.cancel()
            flipDetector.stop()
            onSkipClicked()
        }

        // Continue button (on success screen)
        continueButton.setOnClickListener {
            onContinueClicked()
        }

        // Set initial values
        timerText.text = timerDuration.toString()
        progressBar.max = timerDuration * 100
        progressBar.progress = timerDuration * 100
    }

    protected open fun onContinueClicked() {
        // 하위 클래스에서 오버라이드 가능
        finish()
    }

    private fun initFlipDetector() {
        flipDetector = FlipDetector(this)
        flipDetector.start(object : FlipDetector.FlipListener {
            override fun onFlipDetected(flipped: Boolean) {
                isFlipped = flipped
                Log.d(TAG, "Flip detected: $flipped")

                if (flipped) {
                    onPhoneFlipped()
                } else {
                    onPhoneUnflipped()
                }
            }
        })

        // Start initial animation
        phoneIcon.post {
            if (!isFlipped) {
                animatePhoneIcon()
            }
        }
    }

    protected open fun onPhoneFlipped() {
        // 폰이 뒤집혔을 때 - 반복 애니메이션 중단하고 180도로 고정
        phoneIcon.animate().cancel()
        phoneIcon.clearAnimation()
        phoneIcon.animate()
            .rotationY(180f)
            .setDuration(400)
            .start()

        // Flip 인디케이터 페이드아웃
        flipIndicator.animate()
            .alpha(0f)
            .setDuration(300)
            .start()

        if (!isTimerRunning) {
            startTimer()
        }
    }

    protected open fun onPhoneUnflipped() {
        // 폰이 다시 앞면으로 돌아왔을 때 - 반복 애니메이션 재시작
        phoneIcon.animate().cancel()
        phoneIcon.clearAnimation()
        phoneIcon.rotationY = 0f
        accumulatedRotation = 0f

        phoneIcon.postDelayed({
            if (!isFlipped) {
                animatePhoneIcon()
            }
        }, 100)

        // Flip 인디케이터 페이드인
        flipIndicator.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        flipStatusText.text = getString(R.string.timer_flip_instruction)
        if (isTimerRunning) {
            pauseTimer()
        }
    }

    private fun animatePhoneIcon() {
        if (isFlipped) return

        // 첫 번째 반바퀴: 0 → 180도, 투명 → 흰색
        val firstHalfRotation = accumulatedRotation + 180f
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
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isFlipped) {
                    colorToWhite.cancel()
                    return@withEndAction
                }

                // 두 번째 반바퀴: 180 → 360도, 흰색 → 투명
                accumulatedRotation += 360f
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
                    .rotationY(accumulatedRotation)
                    .setDuration(900)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (isFlipped) {
                            colorToTransparent.cancel()
                            return@withEndAction
                        }
                        animatePhoneIcon()
                    }
                    .start()
            }
            .start()
    }

    protected fun startTimer() {
        if (isTimerRunning) return

        isTimerRunning = true
        Log.d(TAG, "Starting timer with ${remainingMillis}ms remaining")

        countDownTimer = object : CountDownTimer(remainingMillis, 10) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                val progressValue = ((millisUntilFinished / 10).toInt())

                timerText.text = remainingSeconds.toString()
                progressBar.progress = progressValue
            }

            override fun onFinish() {
                isTimerRunning = false
                timerText.text = "0"
                progressBar.progress = 0

                flipDetector.stop()
                triggerHapticFeedback()
                showSuccessScreen()

                // 하위 클래스의 완료 액션 호출
                onTimerCompleted()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return

        isTimerRunning = false
        countDownTimer?.cancel()
        Log.d(TAG, "Timer paused with ${remainingMillis}ms remaining")
    }

    protected fun triggerHapticFeedback() {
        if (!prefsManager.isHapticEnabled) {
            Log.d(TAG, "Haptic feedback disabled in settings")
            return
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 50, 200, 50, 100)
                val amplitudes = intArrayOf(0, 128, 0, 255, 0, 128)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 50, 200, 50, 100), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger haptic feedback", e)
        }
    }

    protected fun showSuccessScreen() {
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register force close receiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        countDownTimer?.cancel()
        flipDetector.stop()

        forceCloseReceiver?.let {
            try {
                unregisterReceiver(it)
                forceCloseReceiver = null
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering force close receiver", e)
            }
        }
    }
}
