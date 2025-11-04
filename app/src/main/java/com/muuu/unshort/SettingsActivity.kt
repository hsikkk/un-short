package com.muuu.unshort

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.muuu.unshort.admin.DeviceAdminManager
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.premium.PremiumManager

class SettingsActivity : BaseActivity() {

    private lateinit var backButton: ImageView
    private lateinit var premiumBanner: LinearLayout
    private lateinit var premiumBannerButton: com.google.android.material.button.MaterialButton
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
    private lateinit var feedbackItem: LinearLayout
    private lateinit var shareItem: LinearLayout
    private lateinit var reviewItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var versionText: TextView
    private lateinit var waitTimeProBadge: TextView
    private lateinit var allowFirstProBadge: TextView
    private lateinit var preventDisableProBadge: TextView
    private lateinit var deviceAdminProBadge: TextView

    private lateinit var deviceAdminManager: DeviceAdminManager
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Managers 초기화
        deviceAdminManager = DeviceAdminManager(this)
        prefsManager = PreferencesManager(this)

        // 설정 화면 방문 플래그 저장
        prefsManager.hasVisitedSettings = true

        // View 초기화
        backButton = findViewById(R.id.backButton)
        premiumBanner = findViewById(R.id.premiumBanner)
        premiumBannerButton = findViewById(R.id.premiumBannerButton)
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
        feedbackItem = findViewById(R.id.feedbackItem)
        shareItem = findViewById(R.id.shareItem)
        reviewItem = findViewById(R.id.reviewItem)
        versionItem = findViewById(R.id.versionItem)
        versionText = findViewById(R.id.versionText)
        waitTimeProBadge = findViewById(R.id.waitTimeProBadge)
        allowFirstProBadge = findViewById(R.id.allowFirstProBadge)
        preventDisableProBadge = findViewById(R.id.preventDisableProBadge)
        deviceAdminProBadge = findViewById(R.id.deviceAdminProBadge)

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

        // 스크롤한 쇼츠만 차단 설정 초기화
        allowFirstSwitch.isChecked = prefsManager.isBlockScrolledOnly

        // 스크롤한 쇼츠만 차단 스위치 리스너
        allowFirstSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!PremiumManager.isPremium()) {
                // 프리미엄 아니면 되돌리기
                allowFirstSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            prefsManager.isBlockScrolledOnly = isChecked
        }

        // 스크롤한 쇼츠만 차단 아이템 클릭 시 처리
        allowFirstItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                showPremiumRequiredDialog()
            } else {
                allowFirstSwitch.isChecked = !allowFirstSwitch.isChecked
            }
        }

        // 충동적 해제 방지 설정 초기화
        preventDisableSwitch.isChecked = prefsManager.isPreventImpulsiveDisable

        // 충동적 해제 방지 스위치 리스너
        preventDisableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!PremiumManager.isPremium()) {
                // 프리미엄 아니면 되돌리기
                preventDisableSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            prefsManager.isPreventImpulsiveDisable = isChecked
        }

        // 충동적 해제 방지 아이템 클릭 시 처리
        preventDisableItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                showPremiumRequiredDialog()
            } else {
                preventDisableSwitch.isChecked = !preventDisableSwitch.isChecked
            }
        }

        // Device Admin 스위치 초기 상태 설정 (리스너도 함께 설정됨)
        updateDeviceAdminSwitchState()

        // Device Admin 아이템 클릭 시 처리
        deviceAdminItem.setOnClickListener {
            if (!PremiumManager.isPremium()) {
                showPremiumRequiredDialog()
            } else {
                deviceAdminSwitch.isChecked = !deviceAdminSwitch.isChecked
            }
        }

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
        }

        // 프리미엄 배너 클릭
        premiumBanner.setOnClickListener {
            showPremiumRequiredDialog()
        }

        premiumBannerButton.setOnClickListener {
            showPremiumRequiredDialog()
        }

        // 대기 시간 설정
        waitTimeItem.setOnClickListener {
            showWaitTimeBottomSheet()
        }

        // 피드백 보내기
        feedbackItem.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("devmuuu@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
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
            showPremiumRequiredDialog()
            return
        }

        // 프리미엄 사용자: 커스텀 타이머 표시
        showCustomTimerBottomSheet()
    }

    /**
     * 프리미엄 필요 다이얼로그 표시
     */
    private fun showPremiumRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.premium_required_title))
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(getString(R.string.premium_button_upgrade)) { _, _ ->
                // 프리미엄 구매 플로우 시작
                PremiumManager.showPremiumPurchase(this) { success ->
                    if (success) {
                        // 구매 성공 시 커스텀 타이머 표시
                        showCustomTimerBottomSheet()
                    }
                }
            }
            .setNegativeButton(getString(R.string.premium_button_cancel), null)
            .show()
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

    /**
     * 프리미엄 UI 업데이트
     */
    private fun updatePremiumUI() {
        val isPremium = PremiumManager.isPremium()

        // Alpha 값 (더 흐리게)
        val premiumAlpha = if (isPremium) 1.0f else 0.4f

        // 배너
        premiumBanner.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE

        // 대기 시간
        waitTimeValue.visibility = if (isPremium) android.view.View.VISIBLE else android.view.View.GONE
        waitTimeProBadge.visibility = if (isPremium) android.view.View.GONE else android.view.View.VISIBLE
        waitTimeItem.alpha = premiumAlpha

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
        // 설정 화면에서 돌아왔을 때 Device Admin 상태 업데이트
        updateDeviceAdminSwitchState()
    }

    private fun showDeviceAdminDisableDialog() {
        val dialog = DisableConfirmDialog(
            context = this,
            titleResId = R.string.device_admin_disable_title,
            messageResId = R.string.device_admin_disable_message,
            requiredPhraseResId = R.string.device_admin_disable_phrase,
            onConfirm = {
                // User confirmed - proceed with disabling
                deviceAdminManager.removeAdmin()
            },
            onCancel = {
                // User cancelled - restore switch state
                updateDeviceAdminSwitchState()
            }
        )
        dialog.show()
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_DEVICE_ADMIN = 1001
    }
}
