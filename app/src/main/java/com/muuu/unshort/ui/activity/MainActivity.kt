package com.muuu.unshort.ui.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.data.statistics.AppDatabase
import com.muuu.unshort.ui.dialog.WarningDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuInterstitialAdUnit
import com.muuu.ad.core.listener.MuuuInterstitialAdListener
import com.muuu.ad.core.listener.MuuuInterstitialAdLoaderListener
import com.muuu.ad.core.model.MuuuAdDisplayFailError
import com.muuu.ad.core.model.MuuuAdInfo
import com.muuu.ad.core.model.MuuuAdLoadError
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.ad.core.model.MuuuInterstitialAd
import com.muuu.ad.loader.MuuuInterstitialAdLoader
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.service.blocking.AppBlockingRegistry
import com.muuu.unshort.ui.activity.ReportActivity
import com.muuu.unshort.util.NotificationPermissionHelper
import com.muuu.unshort.util.ThreeStepProtectionHelper
import com.muuu.unshort.util.WarningConfig
import com.muuu.unshort.util.ConfirmConfig
import com.muuu.unshort.config.AdConfig
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.util.InAppReviewHelper
import com.muuu.unshort.ui.activity.MainActivity
import com.muuu.unshort.ui.dialog.DisableTypeDialog
import com.muuu.unshort.ui.dialog.TimerRequiredDialog
import com.muuu.unshort.receiver.TempDisableReceiver
import com.muuu.unshort.ui.activity.SettingsActivity
import com.muuu.unshort.util.PermissionUtils
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.receiver.DailyReportReceiver
import com.muuu.unshort.ui.dialog.ExitConfirmationDialog
import com.muuu.unshort.ui.dialog.ChangelogBottomSheet
import com.muuu.unshort.changelog.ChangelogRegistry
import com.muuu.unshort.ui.activity.PermissionSetupActivity
import com.muuu.unshort.BuildConfig
import com.muuu.unshort.R

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
    private lateinit var shortsEntryNumber: TextView
    private lateinit var shortsConsumptionNumber: TextView
    private lateinit var watchTimeText: TextView
    private lateinit var settingsButton: ImageView
    private lateinit var settingsBadge: View
    private lateinit var statisticsBadge: View
    private lateinit var premiumBadge: TextView
    private var bannerAdView: MuuuBannerAdView? = null
    private lateinit var prefsManager: PreferencesManager
    private lateinit var blockedAppsContainer: LinearLayout
    private lateinit var blockedAppsSettingsButton: ImageView
    private lateinit var loadingOverlay: FrameLayout

    // 3단계 보호 플로우 헬퍼
    private lateinit var protectionHelper: ThreeStepProtectionHelper
    private lateinit var disableConfirmTimerLauncher: ActivityResultLauncher<Intent>

    // 앱별 차단 해제 타이머
    private lateinit var appDisableTimerLauncher: ActivityResultLauncher<Intent>
    private var pendingDisablePackageName: String? = null
    private var appBlockingSettingsDialog: com.muuu.unshort.ui.dialog.AppBlockingSettingsDialog? = null

    // 노티피케이션 권한 Helper
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper

    // In-App Review Helper
    private lateinit var inAppReviewHelper: InAppReviewHelper

    // SharedPreferences 변경 리스너 (SleepModeReceiver 등 외부에서 변경 시 즉시 반영)
    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AppConstants.PREF_BLOCKING_ENABLED) {
            checkPermissionsAndUpdateUI()
            updateSleepModeLabel()
        }
    }

    // 일시 해제 상태 업데이트용 핸들러
    private val tempDisableHandler = Handler(Looper.getMainLooper())
    private val tempDisableUpdateRunnable = object : Runnable {
        override fun run() {
            updateTempDisableStatus()
            // 1분마다 업데이트
            tempDisableHandler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Managers 초기화
        prefsManager = PreferencesManager(this)

        // 3단계 보호 플로우 헬퍼 초기화
        protectionHelper = ThreeStepProtectionHelper(this)

        // Register activity result launcher for timer
        disableConfirmTimerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // 타이머 완료 - 해제 타입 선택
                showDisableTypeDialog()
            } else {
                // 타이머 취소 - UI 복원
                updateUI(true, animate = false)
            }
        }

        protectionHelper.registerTimerLauncher(disableConfirmTimerLauncher)

        // Register activity result launcher for app-specific disable timer
        appDisableTimerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // 타이머 완료 - 앱별 차단 해제
                pendingDisablePackageName?.let { packageName ->
                    prefsManager.setAppBlockingEnabled(packageName, false)
                    populateBlockedApps()
                    // 다이얼로그가 떠있다면 스위치 갱신
                    appBlockingSettingsDialog?.refreshSwitches()
                    Log.d("MainActivity", "App blocking disabled for $packageName after timer")
                }
            } else {
                // 타이머 취소 - 아무 작업 안함
                Log.d("MainActivity", "App disable timer cancelled")
            }
            pendingDisablePackageName = null
        }

        // Initialize notification permission helper
        notificationPermissionHelper = NotificationPermissionHelper(this, prefsManager)
        notificationPermissionHelper.registerLauncher(
            onGranted = {
                prefsManager.hasAskedNotificationPermission = true
                Log.d("MainActivity", "Notification permission granted")
            },
            onDenied = {
                prefsManager.hasAskedNotificationPermission = true
                Log.d("MainActivity", "Notification permission denied")
            }
        )

        // Initialize in-app review helper
        inAppReviewHelper = InAppReviewHelper(this)

        // Record app installed time (first launch only)
        if (prefsManager.appInstalledAt == 0L) {
            prefsManager.appInstalledAt = System.currentTimeMillis()
            prefsManager.markChangelogSeen(BuildConfig.VERSION_CODE)
        } else if (prefsManager.lastSeenVersionCode == 0) {
            prefsManager.lastSeenVersionCode = BuildConfig.VERSION_CODE - 1
        }

        setContentView(R.layout.activity_main)

        // 백버튼 처리 - 종료 확인 다이얼로그
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 로딩 중에는 뒤로가기 무시
                if (loadingOverlay.visibility == View.VISIBLE) {
                    return
                }
                showExitConfirmDialog()
            }
        })

        // Track app launch
        AnalyticsManager.trackEvent(this, AnalyticsEvent.APP_LAUNCHED)

        // Schedule daily report notification (only if enabled)
        if (prefsManager.isDailyNotificationsEnabled) {
            DailyReportReceiver.scheduleDailyReport(this)
        }

        // Request notification permission for Android 13+ (once only)
        notificationPermissionHelper.requestPermission(skipIfAsked = true)

        // 광고 설정 (AdManager가 프리미엄 체크 및 자동 제거 처리)
        val adViewContainer = findViewById<FrameLayout>(R.id.adView)
        bannerAdView = AdManager.setupBannerAd(
            context = this,
            container = adViewContainer,
            adUnit = MuuuBannerAdUnit(
                key = AdConfig.BANNER_HOME_BOTTOM,
                placement = "main_screen_bottom",
                bannerSize = MuuuBannerSize.Banner,
                refreshInterval = 7
            )
        )

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
        shortsEntryNumber = findViewById(R.id.shortsEntryNumber)
        shortsConsumptionNumber = findViewById(R.id.shortsConsumptionNumber)
        watchTimeText = findViewById(R.id.watchTimeText)
        settingsButton = findViewById(R.id.settingsButton)
        settingsBadge = findViewById(R.id.settingsBadge)
        statisticsBadge = findViewById(R.id.statisticsBadge)
        premiumBadge = findViewById(R.id.premiumBadge)
        blockedAppsContainer = findViewById(R.id.blockedAppsContainer)
        blockedAppsSettingsButton = findViewById(R.id.blockedAppsSettingsButton)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        // 설치된 앱만 동적으로 생성하여 표시
        populateBlockedApps()

        // 권한 설정 버튼 클릭 리스너
        permissionSettingsButton.setOnClickListener {
            val intent = Intent(this, PermissionSetupActivity::class.java)
            startActivity(intent)
        }

        // 통계 버튼 클릭 리스너
        findViewById<ImageView>(R.id.statsButton).setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        // 차단 앱 설정 버튼 클릭 리스너
        blockedAppsSettingsButton.setOnClickListener {
            showAppBlockingSettingsDialog()
        }

        // 설정 버튼 클릭 리스너
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 토글 스위치 클릭 리스너
        toggleContainer.setOnClickListener {
            val currentState = prefsManager.isBlockingEnabled
            val newState = !currentState

            // If turning OFF, check sleep mode first
            if (currentState && !newState) {
                if (prefsManager.isSleepTime()) {
                    showSleepModeBlockDialog()
                    return@setOnClickListener
                }
                // 최근 10분 내 쇼츠 진입 시도 확인
                lifecycleScope.launch {
                    val now = System.currentTimeMillis()
                    val tenMinutesAgo = now - (10 * 60 * 1000)
                    val repository = StatisticsRepository(this@MainActivity)
                    val recentAttempts = repository.getSessionCountForDate(tenMinutesAgo, now)

                    withContext(Dispatchers.Main) {
                        if (recentAttempts > 0) {
                            // 최근 10분 내 진입 시도 있음 - 타이머 필요
                            showTimerRequiredDialog()
                        } else if (prefsManager.isPreventImpulsiveDisable) {
                            // Show 3-step protection flow first
                            showDisableConfirmDialog()
                        } else {
                            // 진입 시도 없음 - 바로 해제 타입 선택
                            showDisableTypeDialog()
                        }
                    }
                }
            } else {
                // Turning ON - proceed immediately
                // Clear any temporary disable state
                prefsManager.clearTempDisable()
                TempDisableReceiver.cancelAlarm(this)

                // 상태 저장
                prefsManager.isBlockingEnabled = true

                // Track blocking state change
                AnalyticsManager.trackEvent(
                    this,
                    AnalyticsEvent.BLOCKING_ENABLED
                )

                // UI 업데이트 (애니메이션 포함)
                updateUI(true, animate = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 리뷰 요청 체크 (조건 충족 시 표시)
        inAppReviewHelper.checkAndShowReviewIfEligible()

        // 일시 해제 만료 체크
        checkTempDisableExpiration()

        checkPermissionsAndUpdateUI()
        updateSettingsBadgeVisibility()
        updateStatisticsBadgeVisibility()
        updatePremiumBadgeVisibility()
        updateStatisticsSummary()
        updateSleepModeLabel()
        // 앱 설치/삭제 상황 반영
        populateBlockedApps()

        // 일시 해제 상태 업데이트 시작
        if (prefsManager.isTemporarilyDisabled()) {
            tempDisableHandler.post(tempDisableUpdateRunnable)
        }

        // SharedPreferences 변경 리스너 등록
        getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefChangeListener)

        // 업데이트 후 첫 실행 시 changelog 표시
        showChangelogIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // 일시 해제 상태 업데이트 중지
        tempDisableHandler.removeCallbacks(tempDisableUpdateRunnable)

        // SharedPreferences 변경 리스너 해제
        getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefChangeListener)
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

        // 권한 상태를 User Property로 업데이트
        AnalyticsManager.updatePermissionUserProperties(this)
    }

    private fun updateSettingsBadgeVisibility() {
        settingsBadge.visibility = if (!prefsManager.hasVisitedSettings) View.VISIBLE else View.INVISIBLE
    }

    private fun updateStatisticsBadgeVisibility() {
        statisticsBadge.visibility = if (!prefsManager.hasVisitedStatistics) View.VISIBLE else View.INVISIBLE
    }

    private fun updatePremiumBadgeVisibility() {
        premiumBadge.visibility = if (PremiumManager.isPremium()) View.VISIBLE else View.GONE
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
            statusLabel.setTextColor(Color.BLACK)
            statusLabel.alpha = 0.5f
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
            statusLabel.setTextColor(Color.BLACK)
            statusLabel.alpha = 0.5f
        }
    }

    private fun showDisableConfirmDialog() {
        // 3단계 보호 플로우 시작
        protectionHelper.start(
            warningConfig = WarningConfig(
                titleResId = R.string.disable_warning_title,
                messageResId = R.string.disable_warning_message,
                positiveTextResId = R.string.disable_warning_confirm,
                negativeTextResId = R.string.disable_warning_cancel
            ),
            confirmConfig = ConfirmConfig(
                titleResId = R.string.disable_dialog_title,
                messageResId = R.string.disable_dialog_message,
                requiredPhraseResId = R.string.disable_dialog_phrase,
                onCancel = {
                    // 2단계 취소 시 토글 UI를 ON 상태로 복원
                    updateUI(true, animate = false)
                },
                onWarningCancel = {
                    // 0단계 취소 시에도 토글 UI 복원
                    updateUI(true, animate = false)
                }
            ),
            finalAction = {
                // 3단계 보호 플로우 완료 후 해제 타입 선택 다이얼로그 표시
                showDisableTypeDialog()
            }
        )
    }

    private fun showTimerRequiredDialog() {
        // 최근 10분 내 진입 시도가 있어 타이머 완료 필요
        TimerRequiredDialog(
            context = this,
            showAdButton = !PremiumManager.isPremium(), // 프리미엄 유저는 광고 버튼 숨김
            onWatchAd = {
                showInterstitialAdForDisable()
            },
            onStartTimer = {
                // 타이머 시작
                val intent = Intent(this, com.muuu.unshort.timer.DisableConfirmTimerActivity::class.java)
                disableConfirmTimerLauncher.launch(intent)
            },
            onCancel = {
                // 취소 - UI 복원
                updateUI(true, animate = false)
            }
        ).show()
    }

    private fun showInterstitialAdForDisable() {
        showLoading()

        MuuuInterstitialAdLoader(
            context = this,
            adUnit = MuuuInterstitialAdUnit(
                key = AdConfig.INTERSTITIAL_TURN_OFF,
                placement = "interstitial_turn_off"
            )
        ).apply {
            setListener(
                object : MuuuInterstitialAdLoaderListener {
                    override fun onAdLoadFail(err: MuuuAdLoadError) {
                        hideLoading()
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.ad_load_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onAdLoadSuccess(
                        ad: MuuuInterstitialAd,
                        adInfo: MuuuAdInfo
                    ) {
                        ad.setListener(object : MuuuInterstitialAdListener {
                            override fun onDismiss(adInfo: MuuuAdInfo) {}

                            override fun onFailedToShow(
                                adInfo: MuuuAdInfo,
                                error: MuuuAdDisplayFailError
                            ) {
                                hideLoading()
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.ad_display_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onShown(adInfo: MuuuAdInfo) {
                                hideLoading()
                                onAdCompleted()
                            }
                        })

                        ad.show(this@MainActivity)
                    }
                }
            )

            load()
        }
    }

    private fun onAdCompleted() {
        // 광고 시청 완료 후 해제 타입 선택
        showDisableTypeDialog()
    }

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    private fun showDisableTypeDialog() {
        DisableTypeDialog(
            context = this,
            onPermanentDisable = {
                // 영구 해제
                prefsManager.clearTempDisable()
                TempDisableReceiver.cancelAlarm(this)
                prefsManager.isBlockingEnabled = false
                AnalyticsManager.trackEvent(this, AnalyticsEvent.BLOCKING_DISABLED)
                updateUI(false, animate = true)
            },
            onTemporaryDisable = { durationMinutes ->
                // 일시 해제
                val triggerAtMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
                prefsManager.tempDisableUntil = triggerAtMillis
                prefsManager.isBlockingEnabled = false

                // 알람 스케줄링
                TempDisableReceiver.scheduleReEnable(this, triggerAtMillis)

                AnalyticsManager.trackEvent(this, AnalyticsEvent.BLOCKING_DISABLED)
                updateUI(false, animate = true)

                // 일시 해제 상태 업데이트 시작
                tempDisableHandler.post(tempDisableUpdateRunnable)
            },
            onCancel = {
                // 취소 시 UI 복원
                updateUI(true, animate = false)
            }
        ).show()
    }

    /**
     * 일시 해제 만료 체크 및 자동 재활성화
     */
    private fun checkTempDisableExpiration() {
        if (prefsManager.tempDisableUntil > 0 && !prefsManager.isTemporarilyDisabled()) {
            // 일시 해제 시간이 지났으면 재활성화
            prefsManager.clearTempDisable()
            prefsManager.isBlockingEnabled = true
        }
    }

    /**
     * 일시 해제 상태 UI 업데이트
     */
    private fun updateTempDisableStatus() {
        if (!prefsManager.isTemporarilyDisabled()) {
            // 일시 해제 상태 아님 - 핸들러 중지하고 일반 UI 복원
            tempDisableHandler.removeCallbacks(tempDisableUpdateRunnable)
            checkTempDisableExpiration()
            checkPermissionsAndUpdateUI()
            return
        }

        // 남은 시간 계산
        val remainingMs = prefsManager.getTempDisableRemainingTime()
        val remainingMinutes = (remainingMs / 60_000).toInt()

        // Status Label 업데이트
        statusDot.setBackgroundResource(R.drawable.status_dot_temp_disabled)
        statusLabel.text = getString(R.string.status_temp_disabled, formatRemainingTime(remainingMinutes))
        statusLabel.setTextColor(getColor(R.color.warning))
        statusLabel.alpha = 1.0f
    }

    /**
     * 남은 시간을 사람이 읽기 쉬운 형식으로 포맷
     */
    private fun formatRemainingTime(minutes: Int): String {
        return when {
            minutes < 1 -> getString(R.string.time_less_than_minute)
            minutes < 60 -> getString(R.string.time_minutes_format, minutes)
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins == 0) {
                    getString(R.string.time_hours_format, hours)
                } else {
                    getString(R.string.time_hours_minutes_format, hours, mins)
                }
            }
        }
    }

    /**
     * 설치된 앱만 동적으로 생성하여 표시
     */
    private fun populateBlockedApps() {
        // 컨테이너 초기화
        blockedAppsContainer.removeAllViews()

        // 설치된 앱 중 활성화된 앱만 필터링
        val installedConfigs = AppBlockingRegistry.ALL_CONFIGS
            .filter { isAppInstalled(it.packageName) }
            .filter { prefsManager.isAppBlockingEnabled(it.packageName) }

        // 각 앱에 대해 View 동적 생성
        installedConfigs.forEachIndexed { index, config ->
            val appItemView = createAppItemView(config, isLast = index == installedConfigs.size - 1)
            blockedAppsContainer.addView(appItemView)
        }
    }

    /**
     * 앱 항목 View 생성 (가로 스크롤용 레이아웃)
     */
    private fun createAppItemView(config: com.muuu.unshort.service.blocking.AppBlockingConfig, isLast: Boolean): View {
        val inflater = layoutInflater

        // 가로 스크롤용 레이아웃 사용 - 아이콘만 표시
        val itemView = inflater.inflate(R.layout.item_blocked_app_horizontal, blockedAppsContainer, false)

        // 실제 앱 아이콘 가져오기
        val appIcon = try {
            packageManager.getApplicationIcon(config.packageName)
        } catch (e: Exception) {
            // 앱을 찾을 수 없으면 기본 아이콘 사용
            ResourcesCompat.getDrawable(resources, config.iconResId, null)
        }

        // 아이콘 설정
        (itemView as ImageView).setImageDrawable(appIcon)

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

    /**
     * 오늘 쇼츠 진입/소비 통계 업데이트
     */
    private fun updateStatisticsSummary() {
        lifecycleScope.launch {
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = System.currentTimeMillis()

                // DB 조회를 IO 스레드에서 실행
                val stats = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val repository = StatisticsRepository(this@MainActivity)
                    repository.getTodayStats()
                }

                // UI 업데이트는 메인 스레드에서
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // 쇼츠 진입 숫자 업데이트
                    shortsEntryNumber.text = stats.attemptCount.toString()

                    // 숫자가 0이면 회색, 아니면 primary_dark
                    if (stats.attemptCount == 0) {
                        shortsEntryNumber.setTextColor(Color.parseColor("#9E9E9E"))
                    } else {
                        shortsEntryNumber.setTextColor(ResourcesCompat.getColor(resources, R.color.primary_dark, null))
                    }

                    // 쇼츠 소비 숫자 업데이트
                    shortsConsumptionNumber.text = stats.watchedCount.toString()

                    // 숫자가 0이면 회색, 아니면 primary_dark
                    if (stats.watchedCount == 0) {
                        shortsConsumptionNumber.setTextColor(Color.parseColor("#9E9E9E"))
                    } else {
                        shortsConsumptionNumber.setTextColor(ResourcesCompat.getColor(resources, R.color.primary_dark, null))
                    }

                    // 시청 시간 업데이트
                    watchTimeText.text = formatWatchTime(stats.watchTimeMs)

                    // 시간이 0이면 회색, 아니면 primary_dark
                    if (stats.watchTimeMs == 0L) {
                        watchTimeText.setTextColor(Color.parseColor("#9E9E9E"))
                    } else {
                        watchTimeText.setTextColor(ResourcesCompat.getColor(resources, R.color.primary_dark, null))
                    }
                }

            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    shortsEntryNumber.text = "0"
                    shortsConsumptionNumber.text = "0"
                    watchTimeText.text = "0m"
                    shortsEntryNumber.setTextColor(Color.parseColor("#9E9E9E"))
                    shortsConsumptionNumber.setTextColor(Color.parseColor("#9E9E9E"))
                    watchTimeText.setTextColor(Color.parseColor("#9E9E9E"))
                }
            }
        }
    }

    /**
     * 시청 시간을 사람이 읽기 쉬운 형식으로 포맷
     *
     * @param milliseconds 시청 시간 (밀리초)
     * @return 포맷된 시간 문자열 (예: "5m", "1h 30m", "2h")
     */
    private fun formatWatchTime(milliseconds: Long): String {
        val minuteUnit = getString(R.string.time_unit_minute)
        val hourUnit = getString(R.string.time_unit_hour)

        if (milliseconds == 0L) return "0$minuteUnit"

        val totalMinutes = (milliseconds / 60000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0 -> "$minutes$minuteUnit"
            minutes == 0 -> "$hours$hourUnit"
            else -> "$hours$hourUnit $minutes$minuteUnit"
        }
    }

    private fun updateSleepModeLabel() {
        if (prefsManager.isSleepTime()) {
            statusLabel.text = getString(R.string.sleep_mode_active_label)
            powerIcon.setImageResource(R.drawable.ic_moon)
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.parseColor("#F59E0B")
            )
        } else {
            powerIcon.setImageResource(R.drawable.ic_power)
            updateUI(prefsManager.isBlockingEnabled)
        }
    }

    private fun showSleepModeBlockDialog() {
        WarningDialog(
            context = this,
            titleResId = R.string.sleep_mode_dialog_title,
            messageResId = R.string.sleep_mode_dialog_message,
            positiveTextResId = R.string.sleep_mode_dialog_confirm,
            onPositive = { }
        ).show()
    }

    private fun showChangelogIfNeeded() {
        if (prefsManager.shouldShowChangelog(BuildConfig.VERSION_CODE)) {
            val changelog = ChangelogRegistry.getChangelogForVersion(BuildConfig.VERSION_CODE)
                ?: ChangelogRegistry.getLatestChangelog()
                ?: return

            ChangelogBottomSheet.show(this, changelog) {
                prefsManager.markChangelogSeen(BuildConfig.VERSION_CODE)
            }
        }
    }

    /**
     * 앱 종료 확인 다이얼로그 표시
     */
    private fun showExitConfirmDialog() {
        ExitConfirmationDialog(
            context = this,
            onConfirm = {
                finish()
            },
            onCancel = {
                // 아무것도 하지 않음 (다이얼로그 닫기)
            }
        ).show()
    }

    /**
     * 차단 앱 설정 다이얼로그 표시
     */
    private fun showAppBlockingSettingsDialog() {
        appBlockingSettingsDialog = com.muuu.unshort.ui.dialog.AppBlockingSettingsDialog(
            activity = this,
            onSettingsChanged = {
                // 설정 변경 시 아이콘 리스트 새로고침
                populateBlockedApps()
                appBlockingSettingsDialog = null
            },
            onRequestTimer = { packageName ->
                // 타이머 시작 요청
                startAppDisableTimer(packageName)
            }
        )
        appBlockingSettingsDialog?.show()
    }

    /**
     * 앱별 차단 해제 타이머 시작
     *
     * @param packageName 차단 해제할 앱의 패키지명
     */
    fun startAppDisableTimer(packageName: String) {
        pendingDisablePackageName = packageName
        // 설정 변경과 동일한 60초 타이머 사용
        val intent = Intent(this, com.muuu.unshort.timer.DisableConfirmTimerActivity::class.java)
        appDisableTimerLauncher.launch(intent)
        Log.d("MainActivity", "Starting 60s app disable timer for $packageName")
    }

}
