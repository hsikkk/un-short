package com.muuu.unshort.feedblock.overlay

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuNativeAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.ad.core.model.nativetemplate.MuuuNativeAdTemplate
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.view.MuuuNativeAdView
import com.muuu.unshort.R
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.config.AdConfig
import com.muuu.unshort.feedblock.FeedBlockService
import com.muuu.unshort.ui.activity.BaseActivity

/**
 * 피드 차단 오버레이 (베타)
 *
 * 진입 즉시 표시되며 OK/Close 두 버튼만 제공한다 (대기 시간/폰 뒤집기 없음).
 *
 * - 계속 볼래요(OK): CONTINUE 브로드캐스트 → 세션 통과
 * - 그만 볼래요(Close): STOP 브로드캐스트 → HOME 강제 이탈
 */
class FeedBlockOverlayActivity : BaseActivity() {

    override fun isLightStatusBar(): Boolean = false

    private lateinit var mainMessage: TextView
    private lateinit var continueButton: TextView
    private lateinit var stopButton: TextView
    private lateinit var buttonContainer: LinearLayout

    private var displayName: String = "Feed"

    private var bannerAdView: MuuuBannerAdView? = null
    private var nativeAdView: MuuuNativeAdView? = null

    private val countdownHandler = Handler(Looper.getMainLooper())
    private var continueDelaySecondsLeft: Int = CONTINUE_DELAY_SECONDS
    private val countdownTick = object : Runnable {
        override fun run() {
            if (continueDelaySecondsLeft <= 0) {
                continueButton.text = getString(R.string.feed_block_overlay_continue)
                continueButton.isEnabled = true
                continueButton.alpha = 1f
                return
            }
            continueButton.text = getString(
                R.string.feed_block_overlay_continue_countdown,
                continueDelaySecondsLeft
            )
            continueDelaySecondsLeft--
            countdownHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.overlay_flip_phone)

        displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: "Feed"

        mainMessage = findViewById(R.id.mainMessage)
        buttonContainer = findViewById(R.id.buttonContainer)

        continueButton = findViewById(R.id.startTimerButton)
        stopButton = findViewById(R.id.skipButton)
        findViewById<View>(R.id.watchButton).visibility = View.GONE

        renderUi()
        setupAds()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 백버튼 무시 (마찰 유지)
            }
        })
    }

    private fun setupAds() {
        val nativeAdContainer = findViewById<FrameLayout>(R.id.nativeAd)
        nativeAdView = AdManager.setupNativeAd(
            context = this,
            container = nativeAdContainer,
            adUnit = MuuuNativeAdUnit(
                key = AdConfig.NATIVE_OVERLAY_TOP,
                placement = "feed_overlay_top",
                refreshInterval = 4
            ),
            template = MuuuNativeAdTemplate.Line(
                backgroundColor = getColor(R.color.primary_dark),
                contentColor = getColor(R.color.white),
                adMarkLabelBackgroundColor = getColor(R.color.gray_700),
                adMarkLabelTextColor = getColor(R.color.white)
            )
        )

        val bannerAdContainer = findViewById<FrameLayout>(R.id.adView)
        bannerAdView = AdManager.setupBannerAd(
            context = this,
            container = bannerAdContainer,
            adUnit = MuuuBannerAdUnit(
                key = AdConfig.BANNER_OVERLAY_BOTTOM,
                placement = "feed_overlay_bottom",
                bannerSize = MuuuBannerSize.Banner,
                refreshInterval = 4
            )
        )
    }

    private fun renderUi() {
        mainMessage.text = getString(R.string.feed_block_overlay_message, displayName)

        buttonContainer.removeAllViews()

        continueButton.visibility = View.VISIBLE
        continueButton.isEnabled = false
        continueButton.alpha = 0.5f
        continueButton.text = getString(
            R.string.feed_block_overlay_continue_countdown,
            CONTINUE_DELAY_SECONDS
        )
        buttonContainer.addView(
            continueButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        stopButton.text = getString(R.string.feed_block_overlay_stop)
        stopButton.setTextColor(0xFF8A8A8A.toInt())
        stopButton.setBackgroundResource(android.R.color.transparent)
        val stopParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        stopParams.topMargin = (resources.displayMetrics.density * 12).toInt()
        buttonContainer.addView(stopButton, stopParams)

        continueButton.setOnClickListener { handleContinue() }
        stopButton.setOnClickListener { handleStop() }

        countdownHandler.removeCallbacks(countdownTick)
        continueDelaySecondsLeft = CONTINUE_DELAY_SECONDS
        countdownHandler.post(countdownTick)
    }

    private fun handleContinue() {
        Log.d(TAG, "continue clicked")
        sendBroadcast(
            Intent(FeedBlockService.ACTION_FEED_BLOCK_CONTINUE).setPackage(packageName)
        )
        finishAndRemoveTask()
    }

    private fun handleStop() {
        Log.d(TAG, "stop clicked")
        sendBroadcast(
            Intent(FeedBlockService.ACTION_FEED_BLOCK_STOP).setPackage(packageName)
        )
        finishAndRemoveTask()
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing) {
            finishAndRemoveTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownHandler.removeCallbacks(countdownTick)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val TAG = "FeedBlockOverlay"
        private const val CONTINUE_DELAY_SECONDS = 5
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
    }
}
