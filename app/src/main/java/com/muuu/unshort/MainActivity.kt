package com.muuu.unshort

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.shortblock.service.blocking.AppBlockingRegistry

class MainActivity : BaseActivity() {

    override fun isLightStatusBar(): Boolean = false // 검정 배경에 밝은 아이콘

    private lateinit var toggleArea: LinearLayout
    private lateinit var permissionWarning: LinearLayout
    private lateinit var permissionSettingsButton: com.google.android.material.button.MaterialButton
    private lateinit var toggleContainer: FrameLayout
    private lateinit var toggleCircle: CardView
    private lateinit var powerIcon: ImageView
    private lateinit var onText: TextView
    private lateinit var offText: TextView
    private lateinit var statusDot: View
    private lateinit var statusLabel: TextView
    private lateinit var settingsButton: ImageView
    private lateinit var settingsBadge: View
    private lateinit var settingsTipBanner: FrameLayout
    private lateinit var settingsTipBannerContent: LinearLayout
    private lateinit var settingsTipCloseButton: TextView
    private lateinit var bannerAdView: MuuuBannerAdView
    private lateinit var prefsManager: PreferencesManager
    private lateinit var blockedAppsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Managers 초기화
        prefsManager = PreferencesManager(this)

        // 온보딩 체크
        if (!prefsManager.isOnboardingCompleted) {
            // 온보딩 화면으로 이동
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Track app launch
        AnalyticsManager.trackEvent(this, AnalyticsEvent.APP_LAUNCHED)

        // Create Muuu banner ad unit
        val bannerAdUnit = MuuuBannerAdUnit(
            key = AdConfig.BANNER_HOME_BOTTOM,
            placement = "main_screen_bottom",
            bannerSize = MuuuBannerSize.Banner,
            refreshInterval = 7
        )

        // Create and setup Muuu banner ad view
        bannerAdView = MuuuBannerAdView(this, bannerAdUnit)

        // Add MuuuBannerAdView to container
        val adViewContainer = findViewById<FrameLayout>(R.id.adView)
        adViewContainer.addView(bannerAdView)

        // Load ad
        bannerAdView.loadAd()

        // View 초기화
        toggleArea = findViewById(R.id.toggleArea)
        permissionWarning = findViewById(R.id.permissionWarning)
        permissionSettingsButton = findViewById(R.id.permissionSettingsButton)
        toggleContainer = findViewById(R.id.toggleContainer)
        toggleCircle = findViewById(R.id.toggleCircle)
        powerIcon = findViewById(R.id.powerIcon)
        onText = findViewById(R.id.onText)
        offText = findViewById(R.id.offText)
        statusDot = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)
        settingsButton = findViewById(R.id.settingsButton)
        settingsBadge = findViewById(R.id.settingsBadge)
        settingsTipBanner = findViewById(R.id.settingsTipBanner)
        settingsTipBannerContent = findViewById(R.id.settingsTipBannerContent)
        settingsTipCloseButton = findViewById(R.id.settingsTipCloseButton)
        blockedAppsContainer = findViewById(R.id.blockedAppsContainer)

        // 설치된 앱만 동적으로 생성하여 표시
        populateBlockedApps()

        // 권한 설정 버튼 클릭 리스너
        permissionSettingsButton.setOnClickListener {
            val intent = Intent(this, PermissionSetupActivity::class.java)
            startActivity(intent)
        }

        // 설정 버튼 클릭 리스너
        settingsButton.setOnClickListener {
            prefsManager.hasVisitedSettings = true
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 설정 팁 배너 콘텐츠 영역 클릭 리스너 (설정으로 이동)
        settingsTipBannerContent.setOnClickListener {
            prefsManager.hasVisitedSettings = true
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 설정 팁 배너 닫기 버튼 클릭 리스너 (배너 숨김만)
        settingsTipCloseButton.setOnClickListener {
            prefsManager.hasSeenSettingsTip = true
            settingsTipBanner.visibility = View.GONE
        }

        // 토글 스위치 클릭 리스너
        toggleContainer.setOnClickListener {
            val currentState = prefsManager.isBlockingEnabled
            val newState = !currentState

            // If turning OFF, check if confirmation is required
            if (currentState && !newState) {
                if (prefsManager.isPreventImpulsiveDisable) {
                    // Show confirmation dialog
                    showDisableConfirmDialog()
                } else {
                    // Proceed immediately without confirmation
                    prefsManager.isBlockingEnabled = false

                    // Track blocking state change
                    AnalyticsManager.trackEvent(
                        this,
                        AnalyticsEvent.BLOCKING_DISABLED
                    )

                    // UI 업데이트 (애니메이션 포함)
                    updateUI(false, animate = true)
                }
            } else {
                // Turning ON - proceed immediately
                // 상태 저장
                prefsManager.isBlockingEnabled = true

                // Track blocking state change
                AnalyticsManager.trackEvent(
                    this,
                    AnalyticsEvent.BLOCKING_ENABLED
                )

                // UI 업데이트 (애니메이션 포함)
                updateUI(newState, animate = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateUI()
        updateSettingsTipBannerVisibility()
        updateSettingsBadgeVisibility()
        // 앱 설치/삭제 상황 반영
        populateBlockedApps()
    }

    private fun checkPermissionsAndUpdateUI() {
        val hasPermissions = checkPermissions()

        if (!hasPermissions) {
            // 권한 없음 - 토글 숨기고 경고 표시
            toggleArea.visibility = View.GONE
            permissionWarning.visibility = View.VISIBLE

            // 차단 상태는 유지 (권한 복구 시 이전 상태로 돌아감)
        } else {
            // 권한 있음 - 토글 표시
            toggleArea.visibility = View.VISIBLE
            permissionWarning.visibility = View.GONE

            // 저장된 차단 상태 불러오기
            updateUI(prefsManager.isBlockingEnabled)
        }
    }

    private fun updateSettingsTipBannerVisibility() {
        val shouldShow = !prefsManager.hasVisitedSettings && !prefsManager.hasSeenSettingsTip
        settingsTipBanner.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    private fun updateSettingsBadgeVisibility() {
        settingsBadge.visibility = if (!prefsManager.hasVisitedSettings) View.VISIBLE else View.INVISIBLE
    }

    private fun checkPermissions(): Boolean {
        return PermissionUtils.hasAllPermissions(this)
    }

    private fun updateUI(isEnabled: Boolean, animate: Boolean = false) {
        // dp to pixels conversion
        val density = resources.displayMetrics.density
        // Container width: 180dp, Circle width: 80dp, Both margins: 10dp
        // Translation distance: 180dp - 80dp - 10dp = 90dp
        val translationDistance = 90f * density

        if (animate) {
            // 토글 애니메이션
            // Circle은 기본적으로 오른쪽에 위치 (layout_gravity="end")
            // ON: 오른쪽 원위치 (0f), OFF: 왼쪽으로 이동 (negative translation)
            val targetTranslation = if (isEnabled) 0f else -translationDistance

            ValueAnimator.ofFloat(toggleCircle.translationX, targetTranslation).apply {
                duration = 400
                addUpdateListener { animation ->
                    toggleCircle.translationX = animation.animatedValue as Float
                }
                start()
            }

            // Scale animation for feedback
            toggleContainer.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    toggleContainer.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else {
            // 초기 상태 설정 (애니메이션 없이)
            // Circle은 기본적으로 오른쪽에 위치 (layout_gravity="end")
            // ON: 오른쪽 원위치 (0f), OFF: 왼쪽으로 이동 (negative translation)
            toggleCircle.translationX = if (isEnabled) 0f else -translationDistance
        }

        if (isEnabled) {
            // ON 상태
            toggleContainer.setBackgroundResource(R.drawable.toggle_track_background)

            // ON/OFF 텍스트 투명도
            onText.alpha = 0.6f
            offText.alpha = 0f

            // 아이콘 색상
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.BLACK
            )

            // Status Indicator
            statusDot.setBackgroundResource(R.drawable.status_dot_active)
            statusLabel.text = getString(R.string.status_active)
        } else {
            // OFF 상태
            toggleContainer.setBackgroundResource(R.drawable.toggle_track_inactive)

            // ON/OFF 텍스트 투명도
            onText.alpha = 0f
            offText.alpha = 0.6f

            // 아이콘 색상
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.parseColor("#BDBDBD")
            )

            // Status Indicator
            statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            statusLabel.text = getString(R.string.status_inactive)
        }
    }

    private fun showDisableConfirmDialog() {
        val dialog = DisableConfirmDialog(
            context = this,
            onConfirm = {
                // User confirmed - proceed with disabling
                prefsManager.isBlockingEnabled = false

                // Track blocking state change
                AnalyticsManager.trackEvent(
                    this,
                    AnalyticsEvent.BLOCKING_DISABLED
                )

                // UI 업데이트 (애니메이션 포함)
                updateUI(false, animate = true)
            },
            onCancel = {
                // User cancelled - do nothing, toggle stays in current position
            }
        )
        dialog.show()
    }

    /**
     * 설치된 앱만 동적으로 생성하여 표시
     */
    private fun populateBlockedApps() {
        // 컨테이너 초기화
        blockedAppsContainer.removeAllViews()

        // 설치된 앱만 필터링
        val installedConfigs = AppBlockingRegistry.ALL_CONFIGS
            .filter { isAppInstalled(it.packageName) }

        // 각 앱에 대해 View 동적 생성
        installedConfigs.forEachIndexed { index, config ->
            val appItemView = createAppItemView(config, isLast = index == installedConfigs.size - 1)
            blockedAppsContainer.addView(appItemView)
        }
    }

    /**
     * 앱 항목 View 생성 (가로 스크롤용 레이아웃)
     */
    private fun createAppItemView(config: com.muuu.shortblock.service.blocking.AppBlockingConfig, isLast: Boolean): View {
        val inflater = layoutInflater

        // 가로 스크롤용 레이아웃 사용 - 아이콘만 표시
        val itemView = inflater.inflate(R.layout.item_blocked_app_horizontal, blockedAppsContainer, false)

        // 아이콘 설정
        (itemView as ImageView).setImageResource(config.iconResId)

        return itemView
    }

    /**
     * 특정 패키지가 설치되어 있는지 확인
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

}
