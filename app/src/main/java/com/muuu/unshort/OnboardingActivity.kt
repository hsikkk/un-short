package com.muuu.unshort

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicators: List<View>

    // 3번째 페이지의 권한 설정 뷰들
    private var accessibilityCard: View? = null
    private var overlayCard: View? = null
    private var onboardingServiceStatusText: TextView? = null
    private var onboardingServiceDescription: TextView? = null
    private var onboardingOverlayStatusText: TextView? = null
    private var onboardingOverlayDescription: TextView? = null
    private var onboardingSettingsButton: Button? = null
    private var onboardingOverlayButton: Button? = null
    private var startButton: Button? = null

    private val layouts = listOf(
        R.layout.onboarding_page_1,
        R.layout.onboarding_page_2,
        R.layout.onboarding_page_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)

        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3)
        )

        // ViewPager2 어댑터 설정
        viewPager.adapter = OnboardingAdapter(layouts)

        // ViewPager2 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
            }
        })
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
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)

        // 접근성 서비스 상태 업데이트
        if (accessibilityEnabled) {
            onboardingServiceStatusText?.text = "접근성 서비스 ✓"
            onboardingServiceStatusText?.setTextColor(getColor(R.color.success))
            onboardingServiceDescription?.text = "설정 완료"
            onboardingServiceDescription?.setTextColor(getColor(R.color.success))
            onboardingSettingsButton?.visibility = View.GONE
            accessibilityCard?.alpha = 0.6f
        } else {
            onboardingServiceStatusText?.text = "접근성 서비스"
            onboardingServiceStatusText?.setTextColor(getColor(R.color.gray_900))
            onboardingServiceDescription?.text = "아직 설정되지 않았습니다"
            onboardingServiceDescription?.setTextColor(getColor(R.color.error))
            onboardingSettingsButton?.visibility = View.VISIBLE
            accessibilityCard?.alpha = 1.0f
        }

        // 오버레이 권한 상태 업데이트 (순서 상관없이 자유롭게)
        if (overlayEnabled) {
            onboardingOverlayStatusText?.text = "다른 앱 위에 표시 ✓"
            onboardingOverlayStatusText?.setTextColor(getColor(R.color.success))
            onboardingOverlayDescription?.text = "설정 완료"
            onboardingOverlayDescription?.setTextColor(getColor(R.color.success))
            onboardingOverlayButton?.visibility = View.GONE
            overlayCard?.alpha = 0.6f
        } else {
            onboardingOverlayStatusText?.text = "다른 앱 위에 표시"
            onboardingOverlayStatusText?.setTextColor(getColor(R.color.gray_900))
            onboardingOverlayDescription?.text = "아직 설정되지 않았습니다"
            onboardingOverlayDescription?.setTextColor(getColor(R.color.error))
            onboardingOverlayButton?.visibility = View.VISIBLE
            overlayCard?.alpha = 1.0f
        }

        // 모든 권한 완료 시 시작하기 버튼 표시
        if (accessibilityEnabled && overlayEnabled) {
            startButton?.visibility = View.VISIBLE
        } else {
            startButton?.visibility = View.GONE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains("${packageName}/${ShortsBlockService::class.java.name}") == true
        }

        return false
    }

    private fun finishOnboarding() {
        // SharedPreferences에 온보딩 완료 저장
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        // MainActivity로 이동
        val intent = Intent(this, MainActivity::class.java)
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
            // 3페이지(권한 설정 페이지)인 경우 버튼 리스너 설정
            if (position == 2) {
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
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        onboardingOverlayButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Overlay button clicked")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        startButton?.setOnClickListener {
            Log.d("OnboardingActivity", "Start button clicked")
            finishOnboarding()
        }

        // 초기 상태 업데이트
        updatePermissionUI()
    }
}
