package com.muuu.unshort.ui.activity

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.muuu.unshort.admin.DeviceAdminManager
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.util.NotificationPermissionHelper
import com.muuu.unshort.ui.activity.PremiumUpgradeActivity
import com.muuu.unshort.ui.activity.SubscriptionManagementActivity
import com.muuu.unshort.ui.activity.SettingsActivity
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.receiver.DailyReportReceiver
import com.muuu.unshort.receiver.SleepModeReceiver
import com.muuu.unshort.util.ThreeStepProtectionHelper
import com.muuu.unshort.util.WarningConfig
import com.muuu.unshort.util.ConfirmConfig
import com.muuu.unshort.BuildConfig
import com.muuu.unshort.R
import com.muuu.unshort.ui.dialog.WarningDialog
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var backButton: ImageView
    private lateinit var premiumBanner: LinearLayout
    private lateinit var premiumBannerButton: com.google.android.material.button.MaterialButton
    private lateinit var subscriptionManagementItem: LinearLayout
    private lateinit var waitTimeValue: TextView
    private lateinit var waitTimeItem: LinearLayout
    private lateinit var hapticSwitch: Switch
    private lateinit var hapticItem: LinearLayout
    private lateinit var allowFirstSwitch: Switch
    private lateinit var allowFirstItem: LinearLayout
    private lateinit var preventDisableSwitch: Switch
    private lateinit var preventDisableItem: LinearLayout
    private lateinit var deviceAdminSwitch: Switch
    private lateinit var deviceAdminItem: LinearLayout
    private lateinit var feedBlockSettingsItem: LinearLayout
    private lateinit var feedbackItem: LinearLayout
    private lateinit var shareItem: LinearLayout
    private lateinit var reviewItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var versionText: TextView
    private lateinit var waitTimeProBadge: TextView
    private lateinit var allowFirstProBadge: TextView
    private lateinit var preventDisableProBadge: TextView
    private lateinit var deviceAdminProBadge: TextView
    private lateinit var customMessageItem: LinearLayout
    private lateinit var customMessageProBadge: TextView

    // Notification Settings
    private lateinit var dailyNotificationItem: LinearLayout
    private lateinit var dailyNotificationSwitch: Switch
    private lateinit var dailyNotificationTimeItem: LinearLayout
    private lateinit var dailyNotificationTimeValue: TextView

    // Daily Limit
    private lateinit var dailyLimitItem: LinearLayout
    private lateinit var dailyLimitDesc: TextView
    private lateinit var dailyLimitSwitch: Switch
    private lateinit var dailyLimitProBadge: TextView
    private lateinit var dailyLimitWarningItem: LinearLayout
    private lateinit var dailyLimitWarningSwitch: Switch
    private lateinit var dailyLimitMonitorItem: LinearLayout
    private lateinit var dailyLimitMonitorSwitch: Switch

    // Sleep Mode
    private lateinit var sleepModeItem: LinearLayout
    private lateinit var sleepModeDesc: TextView
    private lateinit var sleepModeSwitch: Switch

    private lateinit var deviceAdminManager: DeviceAdminManager
    private lateinit var prefsManager: PreferencesManager

    // 3단계 보호 플로우 헬퍼
    private lateinit var protectionHelper: ThreeStepProtectionHelper
    private lateinit var disableConfirmTimerLauncher: ActivityResultLauncher<Intent>

    // Notification permission helper
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Managers 초기화
        deviceAdminManager = DeviceAdminManager(this)
        prefsManager = PreferencesManager(this)

        // 3단계 보호 플로우 헬퍼 초기화
        protectionHelper = ThreeStepProtectionHelper(this)

        // Register activity result launcher for timer
        disableConfirmTimerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Helper에게 결과 전달 (저장된 config와 action 사용)
            protectionHelper.handleTimerResult(result.resultCode)
        }

        protectionHelper.registerTimerLauncher(disableConfirmTimerLauncher)

        // Initialize notification permission helper
        notificationPermissionHelper = NotificationPermissionHelper(this, prefsManager)
        notificationPermissionHelper.registerLauncher(
            onGranted = {
                // 권한 허용 → ON + 알람 스케줄링
                prefsManager.isDailyNotificationsEnabled = true
                dailyNotificationSwitch.isChecked = true
                DailyReportReceiver.scheduleDailyReport(this)
            },
            onDenied = {
                // 권한 거부 → OFF 유지
                dailyNotificationSwitch.isChecked = false
                prefsManager.isDailyNotificationsEnabled = false
            }
        )

        // 설정 화면 방문 플래그 저장
        prefsManager.hasVisitedSettings = true

        // Header setup
        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.settings_title)

        // View 초기화
        backButton = findViewById(R.id.backButton)
        premiumBanner = findViewById(R.id.premiumBanner)
        premiumBannerButton = findViewById(R.id.premiumBannerButton)
        subscriptionManagementItem = findViewById(R.id.subscriptionManagementItem)
        waitTimeValue = findViewById(R.id.waitTimeValue)
        waitTimeItem = findViewById(R.id.waitTimeItem)
        hapticSwitch = findViewById(R.id.hapticSwitch)
        hapticItem = findViewById(R.id.hapticItem)
        allowFirstSwitch = findViewById(R.id.allowFirstSwitch)
        allowFirstItem = findViewById(R.id.allowFirstItem)
        preventDisableSwitch = findViewById(R.id.preventDisableSwitch)
        preventDisableItem = findViewById(R.id.preventDisableItem)
        deviceAdminSwitch = findViewById(R.id.deviceAdminSwitch)
        deviceAdminItem = findViewById(R.id.deviceAdminItem)
        feedBlockSettingsItem = findViewById(R.id.feedBlockSettingsItem)
        feedbackItem = findViewById(R.id.feedbackItem)
        shareItem = findViewById(R.id.shareItem)
        reviewItem = findViewById(R.id.reviewItem)
        versionItem = findViewById(R.id.versionItem)
        versionText = findViewById(R.id.versionText)
        waitTimeProBadge = findViewById(R.id.waitTimeProBadge)
        allowFirstProBadge = findViewById(R.id.allowFirstProBadge)
        preventDisableProBadge = findViewById(R.id.preventDisableProBadge)
        deviceAdminProBadge = findViewById(R.id.deviceAdminProBadge)
        customMessageItem = findViewById(R.id.customMessageItem)
        customMessageProBadge = findViewById(R.id.customMessageProBadge)

        // Notification Settings
        dailyNotificationItem = findViewById(R.id.dailyNotificationItem)
        dailyNotificationSwitch = findViewById(R.id.dailyNotificationSwitch)
        dailyNotificationTimeItem = findViewById(R.id.dailyNotificationTimeItem)
        dailyNotificationTimeValue = findViewById(R.id.dailyNotificationTimeValue)

        // Daily Limit
        dailyLimitItem = findViewById(R.id.dailyLimitItem)
        dailyLimitDesc = findViewById(R.id.dailyLimitDesc)
        dailyLimitSwitch = findViewById(R.id.dailyLimitSwitch)
        dailyLimitProBadge = findViewById(R.id.dailyLimitProBadge)
        dailyLimitWarningItem = findViewById(R.id.dailyLimitWarningItem)
        dailyLimitWarningSwitch = findViewById(R.id.dailyLimitWarningSwitch)
        dailyLimitMonitorItem = findViewById(R.id.dailyLimitMonitorItem)
        dailyLimitMonitorSwitch = findViewById(R.id.dailyLimitMonitorSwitch)

        // Sleep Mode
        sleepModeItem = findViewById(R.id.sleepModeItem)
        sleepModeDesc = findViewById(R.id.sleepModeDesc)
        sleepModeSwitch = findViewById(R.id.sleepModeSwitch)

        // 버전 정보 설정
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            versionText.text = getString(R.string.settings_version_value, version)
        } catch (e: Exception) {
            versionText.text = getString(R.string.settings_version_default)
        }

        // 저장된 설정 표시
        updateWaitTimeDisplay(prefsManager.waitTime)

        // 프리미엄 UI 초기화
        updatePremiumUI()

        // 햅틱 피드백 설정 초기화
        hapticSwitch.isChecked = prefsManager.isHapticEnabled

        // 햅틱 스위치 리스너
        hapticSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.isHapticEnabled = isChecked
        }

        // 햅틱 아이템 클릭 시 스위치 토글
        hapticItem.setOnClickListener {
            hapticSwitch.isChecked = !hapticSwitch.isChecked
        }

        // "스크롤한 쇼츠만 차단" 토글은 deprecate.
        // 일일 즉시 해제 한도 시스템이 등가 효과(클릭 진입 시 "바로 보기"로 즉시 해제)를 자동 제공.
        // 기존 사용자 데이터(prefsManager.isBlockScrolledOnly)는 SessionStateManager에서 그대로 사용.
        allowFirstItem.visibility = View.GONE

        // 충동적 해제 방지 설정 초기화
        preventDisableSwitch.isChecked = prefsManager.isPreventImpulsiveDisable

        // 충동적 해제 방지 스위치 리스너
        setupPreventDisableListener()

        // 충동적 해제 방지 아이템 클릭 시 처리
        preventDisableItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                startActivity(Intent(this, PremiumUpgradeActivity::class.java))
            } else {
                preventDisableSwitch.isChecked = !preventDisableSwitch.isChecked
            }
        }

        // Device Admin 스위치 초기 상태 설정 (리스너도 함께 설정됨)
        updateDeviceAdminSwitchState()

        // Device Admin 아이템 클릭 시 처리
        deviceAdminItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                startActivity(Intent(this, PremiumUpgradeActivity::class.java))
            } else {
                deviceAdminSwitch.isChecked = !deviceAdminSwitch.isChecked
            }
        }

        // 피드 차단 (베타) 진입 - BottomSheet
        feedBlockSettingsItem.setOnClickListener {
            com.muuu.unshort.feedblock.settings.AdvancedSettingsBottomSheet.show(this)
        }

        // 일일 제한 초기화
        setupDailyLimit()

        // 수면 모드 초기화
        setupSleepMode()

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
        }

        // 프리미엄 배너 클릭
        premiumBanner.setOnClickListener {
            startActivity(Intent(this, PremiumUpgradeActivity::class.java))
        }

        premiumBannerButton.setOnClickListener {
            startActivity(Intent(this, PremiumUpgradeActivity::class.java))
        }

        // 구독 관리
        subscriptionManagementItem.setOnClickListener {
            startActivity(Intent(this, SubscriptionManagementActivity::class.java))
        }

        // 대기 시간 설정
        waitTimeItem.setOnClickListener {
            showWaitTimeBottomSheet()
        }

        // 차단 화면 문구 변경
        customMessageItem.setOnClickListener {
            showCustomMessageBottomSheet()
        }

        // 피드백 보내기
        feedbackItem.setOnClickListener {
            val appInfo = "\n\n---\nApp: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nDevice: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("devmuuu@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                putExtra(Intent.EXTRA_TEXT, appInfo)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.toast_email_app_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        // 앱 공유하기
        shareItem.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, playStoreUrl))
            }
            startActivity(Intent.createChooser(shareIntent, "공유하기"))
        }

        // 리뷰 남기기
        reviewItem.setOnClickListener {
            val uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }

        // 버전 정보 클릭 (이스터 에그 등 추가 가능)
        versionItem.setOnClickListener {
            // 버전 정보 클릭 시 동작 (필요시 추가)
        }
    }

    private fun updateWaitTimeDisplay(seconds: Int) {
        waitTimeValue.text = getString(R.string.settings_delay_time_value, seconds)
    }

    private fun showWaitTimeBottomSheet() {
        // 프리미엄 체크
        if (!PremiumManager.isPremium()) {
            startActivity(Intent(this, PremiumUpgradeActivity::class.java))
            return
        }

        // 프리미엄 사용자: 커스텀 타이머 표시
        showCustomTimerBottomSheet()
    }

    /**
     * 커스텀 타이머 Bottom Sheet (프리미엄 전용)
     */
    private fun showCustomTimerBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_custom_timer, null)

        val currentWaitTime = prefsManager.waitTime

        // View 찾기
        val timerValueText = view.findViewById<TextView>(R.id.timerValueText)
        val timerSeekBar = view.findViewById<SeekBar>(R.id.timerSeekBar)
        val preset15 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset15)
        val preset30 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset30)
        val preset60 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset60)
        val doneButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.doneButton)

        // SeekBar 설정 (5~300초, 5초 단위)
        // progress: 0~295 → 실제값: 5~300초
        timerSeekBar.max = 295
        timerSeekBar.progress = currentWaitTime - 5
        timerValueText.text = getString(R.string.settings_delay_time_value, currentWaitTime)

        // SeekBar 리스너
        timerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 5초 단위로 스냅
                val snappedProgress = (progress / 5) * 5
                val seconds = snappedProgress + 5
                timerValueText.text = getString(R.string.settings_delay_time_value, seconds)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 5초 단위로 스냅
                val snappedProgress = (seekBar!!.progress / 5) * 5
                seekBar.progress = snappedProgress
            }
        })

        // 프리셋 버튼들
        preset15.setOnClickListener {
            timerSeekBar.progress = 10 // 15 - 5 = 10
            timerValueText.text = getString(R.string.settings_delay_time_value, 15)
        }

        preset30.setOnClickListener {
            timerSeekBar.progress = 25 // 30 - 5 = 25
            timerValueText.text = getString(R.string.settings_delay_time_value, 30)
        }

        preset60.setOnClickListener {
            timerSeekBar.progress = 55 // 60 - 5 = 55
            timerValueText.text = getString(R.string.settings_delay_time_value, 60)
        }

        // 완료 버튼
        doneButton.setOnClickListener {
            val finalProgress = (timerSeekBar.progress / 5) * 5
            val seconds = finalProgress + 5
            prefsManager.waitTime = seconds
            updateWaitTimeDisplay(seconds)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showCustomMessageBottomSheet() {
        if (!PremiumManager.isPremium()) {
            startActivity(Intent(this, PremiumUpgradeActivity::class.java))
            return
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_custom_message, null)

        val editBefore = view.findViewById<EditText>(R.id.editBeforeTimer)
        val editAfter = view.findViewById<EditText>(R.id.editAfterTimer)
        val resetButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.resetButton)
        val doneButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.doneButton)

        val currentBefore = prefsManager.customMessageBeforeTimer
        val currentAfter = prefsManager.customMessageAfterTimer
        if (currentBefore.isNotEmpty()) editBefore.setText(currentBefore)
        if (currentAfter.isNotEmpty()) editAfter.setText(currentAfter)

        resetButton.setOnClickListener {
            editBefore.text.clear()
            editAfter.text.clear()
        }

        doneButton.setOnClickListener {
            prefsManager.customMessageBeforeTimer = editBefore.text.toString().trim()
            prefsManager.customMessageAfterTimer = editAfter.text.toString().trim()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnShowListener {
            @Suppress("DEPRECATION")
            bottomSheetDialog.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        bottomSheetDialog.show()
    }

    /**
     * 프리미엄 UI 업데이트
     */
    private fun updatePremiumUI() {
        val isPremium = PremiumManager.isPremium()

        // Alpha 값 (더 흐리게)
        val premiumAlpha = if (isPremium) 1.0f else 0.4f

        // 배너
        premiumBanner.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE

        // 구독 관리 (프리미엄 사용자에게만 표시)
        subscriptionManagementItem.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE

        // 대기 시간
        waitTimeValue.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        waitTimeProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        waitTimeItem.alpha = premiumAlpha

        // 차단 화면 문구 변경
        customMessageProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        customMessageItem.alpha = premiumAlpha

        // 스크롤한 쇼츠만 차단
        allowFirstSwitch.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        allowFirstProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        allowFirstItem.alpha = premiumAlpha

        // 충동적 해제 방지
        preventDisableSwitch.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        preventDisableProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        preventDisableItem.alpha = premiumAlpha

        // 앱 삭제 방지
        deviceAdminSwitch.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        deviceAdminProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        deviceAdminItem.alpha = premiumAlpha

        // 일일 제한
        dailyLimitSwitch.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        dailyLimitProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        dailyLimitItem.alpha = premiumAlpha
    }

    /**
     * Device Admin 스위치 상태 업데이트
     */
    private fun updateDeviceAdminSwitchState() {
        val isActive = deviceAdminManager.isDeviceAdminActive()
        // 리스너를 일시적으로 제거하고 상태 업데이트
        deviceAdminSwitch.setOnCheckedChangeListener(null)
        deviceAdminSwitch.isChecked = isActive
        // 리스너 다시 설정
        deviceAdminSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!PremiumManager.isPremium()) {
                // 프리미엄 아니면 되돌리기
                deviceAdminSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                deviceAdminManager.requestActivation(this, REQUEST_CODE_ENABLE_DEVICE_ADMIN)
            } else {
                // OFF로 시도 - 확인 다이얼로그 표시
                showDeviceAdminDisableDialog()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ENABLE_DEVICE_ADMIN) {
            // Device Admin 활성화 결과 처리
            updateDeviceAdminSwitchState()

            if (resultCode == Activity.RESULT_OK) {
                // 활성화 성공
                Toast.makeText(this, R.string.toast_device_admin_enabled, Toast.LENGTH_SHORT).show()
            } else {
                // 활성화 취소
                Toast.makeText(this, R.string.toast_device_admin_cancelled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 설정 화면 조회 추적
        val entrySource = intent?.getStringExtra(com.muuu.unshort.config.AppConstants.EXTRA_ENTRY_SOURCE)
            ?: com.muuu.unshort.config.AppConstants.ENTRY_SOURCE_MAIN_BUTTON
        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.SETTINGS_SCREEN_VIEWED,
            mapOf("entry_source" to entrySource)
        )

        // 설정 화면에서 돌아왔을 때 Device Admin 상태 업데이트
        updateDeviceAdminSwitchState()

        // 프리미엄 업그레이드 후 돌아왔을 때 UI 업데이트
        updatePremiumUI()

        // Daily Notification Switch - 권한 체크 포함
        setupDailyNotificationSwitch()

        // 시간 설정 UI 초기화
        updateNotificationTimeDisplay()
        dailyNotificationTimeItem.setOnClickListener {
            showNotificationTimePicker()
        }

        // 아이템 클릭 시 토글
        dailyNotificationItem.setOnClickListener {
            dailyNotificationSwitch.isChecked = !dailyNotificationSwitch.isChecked
        }
    }

    private fun showDeviceAdminDisableDialog() {
        // 3단계 보호 플로우 시작
        protectionHelper.start(
            warningConfig = WarningConfig(
                titleResId = R.string.disable_warning_title,
                messageResId = R.string.disable_warning_message,
                positiveTextResId = R.string.disable_warning_confirm,
                negativeTextResId = R.string.disable_warning_cancel
            ),
            confirmConfig = ConfirmConfig(
                titleResId = R.string.device_admin_disable_title,
                messageResId = R.string.device_admin_disable_message,
                requiredPhraseResId = R.string.device_admin_disable_phrase,
                onCancel = {
                    // 2단계 취소 시 스위치 복원
                    updateDeviceAdminSwitchState()
                },
                onWarningCancel = {
                    // 0단계 취소 시 스위치 복원
                    updateDeviceAdminSwitchState()
                }
            ),
            finalAction = {
                deviceAdminManager.removeAdmin()
            },
            protectionContext = com.muuu.unshort.timer.DisableConfirmTimerActivity.CONTEXT_DEVICE_ADMIN_OFF
        )
    }

    private fun setupPreventDisableListener() {
        preventDisableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!PremiumManager.isPremium()) {
                // 프리미엄 아니면 되돌리기
                preventDisableSwitch.setOnCheckedChangeListener(null)
                preventDisableSwitch.isChecked = false
                setupPreventDisableListener()
                return@setOnCheckedChangeListener
            }

            if (!isChecked && prefsManager.isPreventImpulsiveDisable) {
                // OFF로 전환 시도 - 확인 다이얼로그 표시
                showPreventDisableOffDialog()
            } else {
                // ON으로 전환 - 즉시 저장
                prefsManager.isPreventImpulsiveDisable = isChecked
            }
        }
    }

    private fun showPreventDisableOffDialog() {
        // 3단계 보호 플로우 시작
        protectionHelper.start(
            warningConfig = WarningConfig(
                titleResId = R.string.disable_warning_title,
                messageResId = R.string.disable_warning_message,
                positiveTextResId = R.string.disable_warning_confirm,
                negativeTextResId = R.string.disable_warning_cancel
            ),
            confirmConfig = ConfirmConfig(
                titleResId = R.string.prevent_impulsive_disable_off_title,
                messageResId = R.string.prevent_impulsive_disable_off_message,
                requiredPhraseResId = R.string.prevent_impulsive_disable_off_phrase,
                onCancel = {
                    // 2단계 취소 시 스위치 복원
                    preventDisableSwitch.setOnCheckedChangeListener(null)
                    preventDisableSwitch.isChecked = true
                    setupPreventDisableListener()
                },
                onWarningCancel = {
                    // 0단계 취소 시 스위치 복원
                    preventDisableSwitch.setOnCheckedChangeListener(null)
                    preventDisableSwitch.isChecked = true
                    setupPreventDisableListener()
                }
            ),
            finalAction = {
                prefsManager.isPreventImpulsiveDisable = false
                preventDisableSwitch.setOnCheckedChangeListener(null)
                preventDisableSwitch.isChecked = false
                setupPreventDisableListener()
            },
            protectionContext = com.muuu.unshort.timer.DisableConfirmTimerActivity.CONTEXT_PREVENT_DISABLE_OFF
        )
    }

    /**
     * Setup daily notification switch with permission handling
     */
    private fun setupDailyNotificationSwitch() {
        // 기존 리스너 제거 (중복 호출 방지)
        dailyNotificationSwitch.setOnCheckedChangeListener(null)

        // 권한 여부에 따른 스위치 상태 설정
        val hasPermission = notificationPermissionHelper.hasPermission()
        dailyNotificationSwitch.isChecked = if (hasPermission) {
            prefsManager.isDailyNotificationsEnabled
        } else {
            false  // 권한 없으면 무조건 OFF
        }

        // 스위치 상태에 따라 시간 설정 UI 표시/숨김
        updateNotificationTimeVisibility(dailyNotificationSwitch.isChecked)

        // 리스너 설정
        dailyNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // ON 시도
                if (notificationPermissionHelper.hasPermission()) {
                    // 권한 있음 - 바로 활성화
                    prefsManager.isDailyNotificationsEnabled = true
                    DailyReportReceiver.scheduleDailyReport(this)
                } else {
                    // 권한 없음 - 권한 요청
                    dailyNotificationSwitch.isChecked = false  // 시각적 토글 방지
                    notificationPermissionHelper.requestPermission(skipIfAsked = false)
                }
            } else {
                // OFF - 권한 무관
                prefsManager.isDailyNotificationsEnabled = false
                DailyReportReceiver.cancelDailyReport(this)
            }

            // 시간 설정 UI 표시/숨김
            updateNotificationTimeVisibility(isChecked)
        }
    }

    /**
     * 알림 ON/OFF 상태에 따라 시간 설정 UI 표시/숨김
     */
    private fun updateNotificationTimeVisibility(isEnabled: Boolean) {
        dailyNotificationTimeItem.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    /**
     * 현재 설정된 알림 시간을 UI에 표시
     */
    private fun updateNotificationTimeDisplay() {
        val hour = prefsManager.dailyNotificationHour
        val minute = prefsManager.dailyNotificationMinute
        val timeString = String.format("%02d:%02d", hour, minute)
        dailyNotificationTimeValue.text = timeString
    }

    /**
     * 알림 시간 선택 다이얼로그 표시
     */
    private fun showNotificationTimePicker() {
        val currentHour = prefsManager.dailyNotificationHour
        val currentMinute = prefsManager.dailyNotificationMinute

        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // 시간 저장
                prefsManager.dailyNotificationHour = hourOfDay
                prefsManager.dailyNotificationMinute = minute

                // UI 업데이트
                updateNotificationTimeDisplay()

                // 알람 재스케줄링
                if (prefsManager.isDailyNotificationsEnabled) {
                    DailyReportReceiver.scheduleDailyReport(this)
                    Log.d("SettingsActivity", "Rescheduled notification for $hourOfDay:$minute")
                }
            },
            currentHour,
            currentMinute,
            true  // 24시간 형식
        )

        timePicker.show()
    }

    private fun setupDailyLimit() {
        updateDailyLimitDisplay()

        dailyLimitSwitch.isChecked = prefsManager.isDailyLimitEnabled
        dailyLimitWarningSwitch.isChecked = prefsManager.isDailyLimitWarningEnabled
        dailyLimitMonitorSwitch.isChecked = prefsManager.isDailyLimitMonitorEnabled

        setupDailyLimitSwitchListener()

        dailyLimitItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                startActivity(Intent(this, PremiumUpgradeActivity::class.java))
                return@setOnClickListener
            }
            if (prefsManager.isDailyLimitEnabled) {
                showDailyLimitBottomSheet()
            } else {
                dailyLimitSwitch.isChecked = true
            }
        }

        // 80% 경고 토글
        dailyLimitWarningSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.isDailyLimitWarningEnabled = isChecked
        }
        dailyLimitWarningItem.setOnClickListener {
            dailyLimitWarningSwitch.isChecked = !dailyLimitWarningSwitch.isChecked
        }

        // 모니터링 토글
        dailyLimitMonitorSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.isDailyLimitMonitorEnabled = isChecked
            if (isChecked) {
                showDailyLimitMonitoringNotification(prefsManager.dailyLimitMinutes)
            } else {
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.cancel(com.muuu.unshort.config.AppConstants.NOTIFICATION_ID_DAILY_LIMIT_MONITOR)
            }
        }
        dailyLimitMonitorItem.setOnClickListener {
            dailyLimitMonitorSwitch.isChecked = !dailyLimitMonitorSwitch.isChecked
        }

        updateDailyLimitSubItems()
    }

    private fun showDailyLimitMonitoringNotification(limitMinutes: Int) {
        val notifier = com.muuu.unshort.util.DailyLimitNotifier(this)
        val limitMs = limitMinutes * 60 * 1000L
        kotlinx.coroutines.MainScope().launch {
            val stats = com.muuu.unshort.data.statistics.StatisticsRepository(this@SettingsActivity).getTodayStats()
            notifier.updateMonitoringNotification(stats.watchTimeMs, limitMs)
        }
    }

    private fun updateDailyLimitSubItems() {
        val visible = if (prefsManager.isDailyLimitEnabled) View.VISIBLE else View.GONE
        dailyLimitWarningItem.visibility = visible
        dailyLimitMonitorItem.visibility = visible
    }

    private fun updateDailyLimitDisplay() {
        if (prefsManager.isDailyLimitEnabled) {
            dailyLimitDesc.text = getString(R.string.daily_limit_value_format, prefsManager.dailyLimitMinutes)
        } else {
            dailyLimitDesc.text = getString(R.string.settings_daily_limit_desc)
        }
    }

    private fun showDailyLimitBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_daily_limit, null)

        val valueText = view.findViewById<TextView>(R.id.dailyLimitValueText)
        val seekBar = view.findViewById<SeekBar>(R.id.dailyLimitSeekBar)
        val doneButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.dailyLimitDoneButton)
        val preset30 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset30)
        val preset60 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset60)
        val preset120 = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.preset120)

        var tempMinutes = prefsManager.dailyLimitMinutes

        fun minutesToProgress(minutes: Int): Int = (minutes / 15).coerceIn(1, 16)
        fun progressToMinutes(progress: Int): Int = progress * 15

        fun updateDisplay() {
            valueText.text = getString(R.string.daily_limit_value_format, tempMinutes)
        }

        seekBar.progress = minutesToProgress(tempMinutes)
        updateDisplay()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempMinutes = progressToMinutes(progress)
                updateDisplay()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        preset30.setOnClickListener {
            tempMinutes = 30; seekBar.progress = minutesToProgress(30); updateDisplay()
        }
        preset60.setOnClickListener {
            tempMinutes = 60; seekBar.progress = minutesToProgress(60); updateDisplay()
        }
        preset120.setOnClickListener {
            tempMinutes = 120; seekBar.progress = minutesToProgress(120); updateDisplay()
        }

        fun applySettings() {
            prefsManager.dailyLimitMinutes = tempMinutes

            if (prefsManager.isDailyLimitMonitorEnabled) {
                showDailyLimitMonitoringNotification(tempMinutes)
            }

            updateDailyLimitDisplay()
            bottomSheetDialog.dismiss()
        }

        doneButton.setOnClickListener {
            val oldMinutes = prefsManager.dailyLimitMinutes

            if (tempMinutes != oldMinutes) {
                kotlinx.coroutines.MainScope().launch {
                    val stats = com.muuu.unshort.data.statistics.StatisticsRepository(this@SettingsActivity).getTodayStats()
                    val currentLimitMs = oldMinutes * 60 * 1000L
                    val watchedRatio = if (currentLimitMs > 0) stats.watchTimeMs.toDouble() / currentLimitMs else 0.0

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (watchedRatio >= 0.8) {
                            androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                                .setTitle(getString(R.string.daily_limit_change_title))
                                .setMessage(getString(R.string.daily_limit_change_message))
                                .setPositiveButton(getString(R.string.changelog_confirm)) { _, _ -> applySettings() }
                                .setNegativeButton(getString(android.R.string.cancel), null)
                                .show()
                        } else {
                            applySettings()
                        }
                    }
                }
            } else {
                applySettings()
            }
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun setupSleepMode() {
        updateSleepModeDisplay()
        syncSleepModeSwitch(prefsManager.isSleepModeEnabled)

        sleepModeItem.setOnClickListener {
            if (prefsManager.isSleepModeEnabled) {
                if (prefsManager.isSleepTime()) {
                    showSleepModeDeactivateWarning()
                    return@setOnClickListener
                }
                showSleepModeBottomSheet()
            } else {
                sleepModeSwitch.isChecked = true
            }
        }
    }

    private fun updateSleepModeDisplay() {
        if (prefsManager.isSleepModeEnabled) {
            sleepModeDesc.text = prefsManager.getSleepTimeDisplayString()
        } else {
            sleepModeDesc.text = getString(R.string.settings_sleep_mode_desc)
        }
    }

    private fun showSleepModeBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_sleep_mode, null)

        val startTimeText = view.findViewById<TextView>(R.id.sleepStartTimeText)
        val endTimeText = view.findViewById<TextView>(R.id.sleepEndTimeText)
        val startTimeRow = view.findViewById<View>(R.id.sleepStartTimeRow)
        val endTimeRow = view.findViewById<View>(R.id.sleepEndTimeRow)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.sleepModeSaveButton)

        var tempStartHour = prefsManager.sleepStartHour
        var tempStartMinute = prefsManager.sleepStartMinute
        var tempEndHour = prefsManager.sleepEndHour
        var tempEndMinute = prefsManager.sleepEndMinute

        fun updateTimeTexts() {
            startTimeText.text = String.format("%02d:%02d", tempStartHour, tempStartMinute)
            endTimeText.text = String.format("%02d:%02d", tempEndHour, tempEndMinute)
        }

        updateTimeTexts()

        startTimeRow.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    if (hourOfDay == tempEndHour && minute == tempEndMinute) {
                        Toast.makeText(this, getString(R.string.sleep_mode_invalid_time), Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }
                    tempStartHour = hourOfDay
                    tempStartMinute = minute
                    updateTimeTexts()
                },
                tempStartHour,
                tempStartMinute,
                true
            ).show()
        }

        endTimeRow.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    if (hourOfDay == tempStartHour && minute == tempStartMinute) {
                        Toast.makeText(this, getString(R.string.sleep_mode_invalid_time), Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }
                    tempEndHour = hourOfDay
                    tempEndMinute = minute
                    updateTimeTexts()
                },
                tempEndHour,
                tempEndMinute,
                true
            ).show()
        }

        saveButton.setOnClickListener {
            val wasEnabled = prefsManager.isSleepModeEnabled

            val now = java.time.LocalTime.now()
            val start = java.time.LocalTime.of(tempStartHour, tempStartMinute)
            val end = java.time.LocalTime.of(tempEndHour, tempEndMinute)
            val isCurrentlyInSleepTime = if (start > end) {
                now >= start || now < end
            } else if (start == end) {
                false
            } else {
                now in start..<end
            }

            if (isCurrentlyInSleepTime && (!wasEnabled || !prefsManager.isSleepTime())) {
                showSleepModeActivateConfirmDialog(
                    tempStartHour, tempStartMinute,
                    tempEndHour, tempEndMinute,
                    bottomSheetDialog
                )
                return@setOnClickListener
            }

            applySleepModeSettings(
                true,
                tempStartHour, tempStartMinute,
                tempEndHour, tempEndMinute
            )
            bottomSheetDialog.dismiss()
        }

        // 저장 안 하고 닫으면 토글 복원
        val wasEnabled = prefsManager.isSleepModeEnabled
        bottomSheetDialog.setOnDismissListener {
            if (!prefsManager.isSleepModeEnabled && !wasEnabled) {
                syncSleepModeSwitch(false)
            }
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showSleepModeActivateConfirmDialog(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int,
        bottomSheetDialog: BottomSheetDialog
    ) {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.sleep_mode_confirm_activate_title)
            .setMessage(R.string.sleep_mode_confirm_activate_message)
            .setPositiveButton(R.string.sleep_mode_confirm_activate_positive) { _, _ ->
                applySleepModeSettings(true, startHour, startMinute, endHour, endMinute)
                bottomSheetDialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSleepModeDeactivateWarning() {
        protectionHelper.start(
            warningConfig = WarningConfig(
                titleResId = R.string.disable_warning_title,
                messageResId = R.string.disable_warning_message,
                positiveTextResId = R.string.disable_warning_confirm,
                negativeTextResId = R.string.disable_warning_cancel
            ),
            confirmConfig = ConfirmConfig(
                titleResId = R.string.prevent_impulsive_disable_off_title,
                messageResId = R.string.prevent_impulsive_disable_off_message,
                requiredPhraseResId = R.string.prevent_impulsive_disable_off_phrase,
                onCancel = { },
                onWarningCancel = { }
            ),
            finalAction = {
                applySleepModeSettings(
                    false,
                    prefsManager.sleepStartHour,
                    prefsManager.sleepStartMinute,
                    prefsManager.sleepEndHour,
                    prefsManager.sleepEndMinute
                )
            },
            protectionContext = com.muuu.unshort.timer.DisableConfirmTimerActivity.CONTEXT_SLEEP_MODE_OFF
        )
    }

    private fun setupDailyLimitSwitchListener() {
        dailyLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!PremiumManager.isPremium()) {
                dailyLimitSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                prefsManager.isDailyLimitEnabled = true
                updateDailyLimitDisplay()
                updateDailyLimitSubItems()
                com.muuu.unshort.receiver.DailyLimitResetReceiver.scheduleReset(this)
                if (prefsManager.isDailyLimitMonitorEnabled) {
                    showDailyLimitMonitoringNotification(prefsManager.dailyLimitMinutes)
                }
            } else {
                dailyLimitSwitch.setOnCheckedChangeListener(null)
                dailyLimitSwitch.isChecked = true
                setupDailyLimitSwitchListener()
                protectionHelper.start(
                    warningConfig = WarningConfig(
                        titleResId = R.string.disable_warning_title,
                        messageResId = R.string.disable_warning_message,
                        positiveTextResId = R.string.disable_warning_confirm,
                        negativeTextResId = R.string.disable_warning_cancel
                    ),
                    confirmConfig = ConfirmConfig(
                        titleResId = R.string.prevent_impulsive_disable_off_title,
                        messageResId = R.string.prevent_impulsive_disable_off_message,
                        requiredPhraseResId = R.string.prevent_impulsive_disable_off_phrase,
                        onCancel = { },
                        onWarningCancel = { }
                    ),
                    finalAction = {
                        prefsManager.isDailyLimitEnabled = false
                        updateDailyLimitDisplay()
                        updateDailyLimitSubItems()
                        com.muuu.unshort.receiver.DailyLimitResetReceiver.cancelReset(this)
                        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                        nm.cancel(com.muuu.unshort.config.AppConstants.NOTIFICATION_ID_DAILY_LIMIT_MONITOR)
                        dailyLimitSwitch.setOnCheckedChangeListener(null)
                        dailyLimitSwitch.isChecked = false
                        setupDailyLimitSwitchListener()
                    },
                    protectionContext = com.muuu.unshort.timer.DisableConfirmTimerActivity.CONTEXT_DAILY_LIMIT_OFF
                )
            }
        }
    }

    private fun applySleepModeSettings(
        enabled: Boolean,
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    ) {
        prefsManager.isSleepModeEnabled = enabled
        prefsManager.sleepStartHour = startHour
        prefsManager.sleepStartMinute = startMinute
        prefsManager.sleepEndHour = endHour
        prefsManager.sleepEndMinute = endMinute

        if (enabled) {
            SleepModeReceiver.scheduleAlarms(this)
        } else {
            SleepModeReceiver.cancelAlarms(this)
        }

        updateSleepModeDisplay()
        syncSleepModeSwitch(enabled)
    }

    private fun syncSleepModeSwitch(enabled: Boolean) {
        sleepModeSwitch.setOnCheckedChangeListener(null)
        sleepModeSwitch.isChecked = enabled
        sleepModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showSleepModeBottomSheet()
            } else {
                if (prefsManager.isSleepModeEnabled && prefsManager.isSleepTime()) {
                    sleepModeSwitch.isChecked = true
                    showSleepModeDeactivateWarning()
                    return@setOnCheckedChangeListener
                }
                applySleepModeSettings(false, prefsManager.sleepStartHour, prefsManager.sleepStartMinute, prefsManager.sleepEndHour, prefsManager.sleepEndMinute)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_DEVICE_ADMIN = 1001
    }
}
