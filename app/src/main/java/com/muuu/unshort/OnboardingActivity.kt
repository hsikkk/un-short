package com.muuu.unshort

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var indicators: List<View>

    // 애니메이션 관련
    private var timerAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())

    // 5번째 페이지의 권한 설정 뷰들
    private var accessibilityCard: View? = null
    private var overlayCard: View? = null
    private var onboardingServiceStatusText: TextView? = null
    private var onboardingServiceDescription: TextView? = null
    private var onboardingOverlayStatusText: TextView? = null
    private var onboardingOverlayDescription: TextView? = null
    private var onboardingSettingsButton: Button? = null
    private var onboardingOverlayButton: Button? = null
    private var startButton: Button? = null
    private lateinit var permissionUIHelper: PermissionUIHelper

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

        // Enable edge-to-edge
        enableEdgeToEdge()

        // Check privacy consent first
        if (!hasValidPrivacyConsent()) {
            showPrivacyConsentDialog()
        }

        permissionUIHelper = PermissionUIHelper(this)
        viewPager = findViewById(R.id.viewPager)

        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3),
            findViewById(R.id.indicator4),
            findViewById(R.id.indicator5)
        )

        // ViewPager2 어댑터 설정
        viewPager.adapter = OnboardingAdapter(layouts)

        // ViewPager2 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)

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

    override fun onResume() {
        super.onResume()
        // 권한 설정에서 돌아왔을 때 UI 업데이트 (3페이지에 있을 때만)
        updatePermissionUI()
    }

    private fun updatePermissionUI() {
        // 접근성 서비스 카드 업데이트
        permissionUIHelper.updateAccessibilityCard(
            PermissionUIHelper.PermissionUIElements(
                card = accessibilityCard,
                statusText = onboardingServiceStatusText,
                descriptionText = onboardingServiceDescription,
                settingsButton = onboardingSettingsButton
            )
        )

        // 오버레이 권한 카드 업데이트
        permissionUIHelper.updateOverlayCard(
            PermissionUIHelper.PermissionUIElements(
                card = overlayCard,
                statusText = onboardingOverlayStatusText,
                descriptionText = onboardingOverlayDescription,
                settingsButton = onboardingOverlayButton
            )
        )

        // 시작하기 버튼 표시 업데이트
        permissionUIHelper.updateCompleteButton(startButton)
    }

    private fun finishOnboarding() {
        // SharedPreferences에 온보딩 완료 저장
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        // MainActivity로 이동 (백스택 완전히 클리어)
        val intent = Intent(this, MainActivity::class.java)
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
            // 5페이지(권한 설정 페이지)인 경우 버튼 리스너 설정
            if (position == 4) {
                setupPermissionPage(holder.itemView)
            }
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = position
    }

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun setupPermissionPage(view: View) {
        accessibilityCard = view.findViewById(R.id.onboardingAccessibilityCard)
        overlayCard = view.findViewById(R.id.onboardingOverlayCard)
        onboardingServiceStatusText = view.findViewById(R.id.onboardingServiceStatusText)
        onboardingServiceDescription = view.findViewById(R.id.onboardingServiceDescription)
        onboardingOverlayStatusText = view.findViewById(R.id.onboardingOverlayStatusText)
        onboardingOverlayDescription = view.findViewById(R.id.onboardingOverlayDescription)
        onboardingSettingsButton = view.findViewById(R.id.onboardingSettingsButton)
        onboardingOverlayButton = view.findViewById(R.id.onboardingOverlayButton)
        startButton = view.findViewById(R.id.startButton)

        Log.d("OnboardingActivity", "setupPermissionPage - Views found - settingsButton: ${onboardingSettingsButton != null}, overlayButton: ${onboardingOverlayButton != null}")

        // 버튼 리스너 설정
        onboardingSettingsButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Accessibility button clicked")
            PermissionUtils.openAccessibilitySettings(this)
        }

        onboardingOverlayButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Overlay button clicked")
            PermissionUtils.openOverlaySettings(this)
        }

        startButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Start button clicked")
            finishOnboarding()
        }

        // 초기 상태 업데이트
        updatePermissionUI()
    }

    private fun startPageAnimation(position: Int) {
        // 이전 애니메이션 정리
        timerAnimator?.cancel()

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

        // 페이드인 + 슬라이드업 애니메이션 (더 느리게)
        message?.let {
            it.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(300)
                .start()
        }

        buttons?.let {
            it.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(800)
                .start()
        }
    }

    private fun animatePage3Timer(position: Int) {
        val viewHolder = (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
            ?.findViewHolderForAdapterPosition(position) ?: return

        val timerNumber = viewHolder.itemView.findViewById<TextView>(R.id.previewTimerNumber)
        val progressRing = viewHolder.itemView.findViewById<ProgressBar>(R.id.previewProgressRing)
        val timerScreen = viewHolder.itemView.findViewById<View>(R.id.previewTimerScreen)
        val successScreen = viewHolder.itemView.findViewById<View>(R.id.previewSuccessScreen)

        // 타이머 카운트다운 애니메이션 (30.0 → 0.0) - Float으로 부드럽게
        timerAnimator = ValueAnimator.ofInt(1000, 0).apply {
            duration = 10000 // 10초 (3배속)
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
            timerScreen?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.withEndAction {
                    timerScreen.visibility = View.GONE
                    successScreen?.visibility = View.VISIBLE
                    successScreen?.alpha = 0f
                    successScreen?.animate()
                        ?.alpha(1f)
                        ?.setDuration(600)
                        ?.start()
                }
                ?.start()
        }, 10000) // 10초 후
    }

    /**
     * Check if user has given valid privacy consent
     */
    private fun hasValidPrivacyConsent(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedVersion = prefs.getInt(PrivacyPolicy.PREF_CONSENT_VERSION, 0)
        return savedVersion >= PrivacyPolicy.CURRENT_VERSION
    }

    /**
     * Show privacy consent dialog
     */
    private fun showPrivacyConsentDialog() {
        val dialog = PrivacyConsentDialog(
            context = this,
            onAgree = {
                // Save consent
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit().apply {
                    putInt(PrivacyPolicy.PREF_CONSENT_VERSION, PrivacyPolicy.CURRENT_VERSION)
                    putLong(PrivacyPolicy.PREF_CONSENT_TIMESTAMP, System.currentTimeMillis())
                    apply()
                }
                Log.d("OnboardingActivity", "Privacy consent given - version ${PrivacyPolicy.CURRENT_VERSION}")
            },
            onExit = {
                // User declined - exit app
                Log.d("OnboardingActivity", "Privacy consent declined - exiting app")
                finish()
                finishAffinity()
            }
        )
        dialog.show()
    }
}
