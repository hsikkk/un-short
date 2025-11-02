package com.muuu.unshort

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager

class OnboardingActivity : BaseActivity() {

    override fun getStatusBarColor(): Int = Color.TRANSPARENT
    override fun isLightStatusBar(): Boolean = true

    private lateinit var viewPager: ViewPager2
    private lateinit var indicators: List<View>
    private lateinit var skipButton: TextView

    // 애니메이션 관련
    private var timerAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

    private val layouts = listOf(
        R.layout.onboarding_page_1,
        R.layout.onboarding_page_2,
        R.layout.onboarding_page_3,
        R.layout.onboarding_page_4,
        R.layout.onboarding_page_5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Track onboarding started
        AnalyticsManager.trackEvent(this, AnalyticsEvent.ONBOARDING_STARTED)

        viewPager = findViewById(R.id.viewPager)
        skipButton = findViewById(R.id.skipButton)

        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3),
            findViewById(R.id.indicator4),
            findViewById(R.id.indicator5)
        )

        // Skip button click listener
        skipButton.setOnClickListener {
            finishOnboarding()
        }

        // ViewPager2 어댑터 설정
        viewPager.adapter = OnboardingAdapter(layouts)

        // ViewPager2 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)

                // Skip button visibility: 페이지 1-4에만 표시, 5번째 페이지는 숨김
                skipButton.visibility = if (position < 4) View.VISIBLE else View.GONE

                // 페이지별 애니메이션 시작
                handler.postDelayed({
                    startPageAnimation(position)
                }, 300) // ViewPager transition 완료 후 애니메이션 시작
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        timerAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, view ->
            if (index == position) {
                view.setBackgroundResource(R.drawable.indicator_active)
            } else {
                view.setBackgroundResource(R.drawable.indicator_inactive)
            }
        }
    }

    private fun finishOnboarding() {
        // SharedPreferences에 온보딩 완료 저장
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        // Track onboarding completed
        AnalyticsManager.trackEvent(this, AnalyticsEvent.ONBOARDING_COMPLETED)

        // 권한 설정 화면으로 이동 (백스택 완전히 클리어)
        val intent = Intent(this, PermissionSetupActivity::class.java)
        intent.putExtra("from_onboarding", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ViewPager2 어댑터
    private inner class OnboardingAdapter(private val layouts: List<Int>) :
        RecyclerView.Adapter<OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(layouts[viewType], parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            // 5페이지(시작 페이지)인 경우 버튼 리스너 설정
            if (position == 4) {
                setupStartPage(holder.itemView)
            }
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = position
    }

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun setupStartPage(view: View) {
        val startButton = view.findViewById<Button>(R.id.startButton)
        startButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Start button clicked")
            finishOnboarding()
        }
    }

    private fun startPageAnimation(position: Int) {
        // 이전 애니메이션 정리
        timerAnimator?.cancel()
        handler.removeCallbacksAndMessages(null) // 모든 Handler 콜백 제거

        when (position) {
            1 -> animatePage2or4(position) // Page 2
            2 -> animatePage3Timer(position) // Page 3 (Timer)
            3 -> animatePage2or4(position) // Page 4
        }
    }

    private fun animatePage2or4(position: Int) {
        val viewHolder = (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
            ?.findViewHolderForAdapterPosition(position) ?: return

        val message = viewHolder.itemView.findViewById<TextView>(R.id.previewMessage)
        val buttons = viewHolder.itemView.findViewById<View>(R.id.previewButtons)

        // 이전 애니메이션 취소 및 초기 상태 설정
        message?.apply {
            animate().cancel()
            alpha = 0f
            translationY = 50f
        }
        buttons?.apply {
            animate().cancel()
            alpha = 0f
            translationY = 50f
        }

        // 페이드인 + 슬라이드업 애니메이션 (더 느리게)
        message?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(800)
            ?.setStartDelay(50)
            ?.start()

        buttons?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(800)
            ?.setStartDelay(700)
            ?.start()
    }

    private fun animatePage3Timer(position: Int) {
        val viewHolder = (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
            ?.findViewHolderForAdapterPosition(position) ?: return

        val timerNumber = viewHolder.itemView.findViewById<TextView>(R.id.previewTimerNumber)
        val progressRing = viewHolder.itemView.findViewById<ProgressBar>(R.id.previewProgressRing)
        val timerScreen = viewHolder.itemView.findViewById<View>(R.id.previewTimerScreen)
        val successScreen = viewHolder.itemView.findViewById<View>(R.id.previewSuccessScreen)

        // 초기 상태 리셋
        timerScreen?.visibility = View.VISIBLE
        timerScreen?.alpha = 1f
        successScreen?.visibility = View.GONE
        successScreen?.alpha = 0f
        timerNumber?.text = "10"
        progressRing?.progress = 1000

        val timerDuration = 5000L

        // 타이머 카운트다운 애니메이션 (30.0 → 0.0) - Float으로 부드럽게
        timerAnimator = ValueAnimator.ofInt(1000, 0).apply {
            duration = timerDuration
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                timerNumber?.text = (value / 100).toString()
                progressRing?.setProgress(value)
            }
            interpolator = LinearInterpolator()
            start()
        }

        // 타이머 완료 후 성공 화면 표시
        handler.postDelayed({
            // 현재 페이지가 여전히 3페이지인지 확인
            if (viewPager.currentItem != position) return@postDelayed

            timerScreen?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.withEndAction {
                    // 애니메이션 완료 시점에도 페이지 확인
                    if (viewPager.currentItem != position) return@withEndAction

                    timerScreen.visibility = View.GONE
                    successScreen?.visibility = View.VISIBLE
                    successScreen?.alpha = 0f
                    successScreen?.animate()
                        ?.alpha(1f)
                        ?.setDuration(600)
                        ?.withEndAction {
                            // 2초 후 다시 타이머로 복귀 (무한 반복)
                            handler.postDelayed({
                                restartTimerAnimation(position)
                            }, 2000)
                        }
                        ?.start()
                }
                ?.start()
        }, timerDuration)
    }

    private fun restartTimerAnimation(position: Int) {
        // 현재 페이지가 여전히 3페이지인지 확인 (사용자가 스와이프했을 수 있음)
        if (viewPager.currentItem == position) {
            animatePage3Timer(position)
        }
    }
}
