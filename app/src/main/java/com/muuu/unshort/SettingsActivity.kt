package com.muuu.unshort

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.muuu.unshort.admin.DeviceAdminManager

class SettingsActivity : BaseActivity() {

    private lateinit var backButton: ImageView
    private lateinit var waitTimeValue: TextView
    private lateinit var waitTimeItem: LinearLayout
    private lateinit var hapticSwitch: Switch
    private lateinit var allowFirstSwitch: Switch
    private lateinit var preventDisableSwitch: Switch
    private lateinit var deviceAdminSwitch: Switch
    private lateinit var deviceAdminItem: LinearLayout
    private lateinit var feedbackItem: LinearLayout
    private lateinit var shareItem: LinearLayout
    private lateinit var reviewItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var versionText: TextView

    private lateinit var deviceAdminManager: DeviceAdminManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // View 초기화
        backButton = findViewById(R.id.backButton)
        waitTimeValue = findViewById(R.id.waitTimeValue)
        waitTimeItem = findViewById(R.id.waitTimeItem)
        hapticSwitch = findViewById(R.id.hapticSwitch)
        allowFirstSwitch = findViewById(R.id.allowFirstSwitch)
        preventDisableSwitch = findViewById(R.id.preventDisableSwitch)
        deviceAdminSwitch = findViewById(R.id.deviceAdminSwitch)
        deviceAdminItem = findViewById(R.id.deviceAdminItem)
        feedbackItem = findViewById(R.id.feedbackItem)
        shareItem = findViewById(R.id.shareItem)
        reviewItem = findViewById(R.id.reviewItem)
        versionItem = findViewById(R.id.versionItem)
        versionText = findViewById(R.id.versionText)

        // DeviceAdminManager 초기화
        deviceAdminManager = DeviceAdminManager(this)

        // 버전 정보 설정
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            versionText.text = getString(R.string.settings_version_value, version)
        } catch (e: Exception) {
            versionText.text = getString(R.string.settings_version_default)
        }

        // 저장된 설정 표시
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val waitTime = prefs.getInt("wait_time", 30)
        updateWaitTimeDisplay(waitTime)

        // 햅틱 피드백 설정 초기화
        val isHapticEnabled = prefs.getBoolean("haptic_enabled", true)
        hapticSwitch.isChecked = isHapticEnabled

        // 햅틱 스위치 리스너
        hapticSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("haptic_enabled", isChecked).apply()
        }

        // 스크롤한 쇼츠만 차단 설정 초기화
        val isBlockScrolledOnly = prefs.getBoolean("block_scrolled_only", false)
        allowFirstSwitch.isChecked = isBlockScrolledOnly

        // 스크롤한 쇼츠만 차단 스위치 리스너
        allowFirstSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("block_scrolled_only", isChecked).apply()
        }

        // 충동적 해제 방지 설정 초기화
        val isPreventDisable = prefs.getBoolean("prevent_impulsive_disable", false)
        preventDisableSwitch.isChecked = isPreventDisable

        // 충동적 해제 방지 스위치 리스너
        preventDisableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("prevent_impulsive_disable", isChecked).apply()
        }

        // Device Admin 스위치 초기 상태 설정
        updateDeviceAdminSwitchState()

        // Device Admin 스위치 리스너
        deviceAdminSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // ON으로 시도 - 바로 Device Admin 권한 요청
                deviceAdminManager.requestActivation(this, REQUEST_CODE_ENABLE_DEVICE_ADMIN)
            } else {
                // OFF로 시도 - 확인 다이얼로그 표시
                showDeviceAdminDisableDialog()
            }
        }

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
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
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_delay_time, null)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentWaitTime = prefs.getInt("wait_time", 30)

        // 라디오 버튼 찾기
        val radio15 = view.findViewById<RadioButton>(R.id.radio15)
        val radio30 = view.findViewById<RadioButton>(R.id.radio30)
        val radio60 = view.findViewById<RadioButton>(R.id.radio60)

        // 현재 설정된 값에 따라 라디오 버튼 체크
        when (currentWaitTime) {
            15 -> radio15.isChecked = true
            30 -> radio30.isChecked = true
            60 -> radio60.isChecked = true
        }

        // 옵션 클릭 리스너들
        view.findViewById<LinearLayout>(R.id.option15).setOnClickListener {
            // 모든 라디오 버튼 해제 후 선택
            radio15.isChecked = true
            radio30.isChecked = false
            radio60.isChecked = false

            prefs.edit().putInt("wait_time", 15).apply()
            updateWaitTimeDisplay(15)
            bottomSheetDialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.option30).setOnClickListener {
            // 모든 라디오 버튼 해제 후 선택
            radio15.isChecked = false
            radio30.isChecked = true
            radio60.isChecked = false

            prefs.edit().putInt("wait_time", 30).apply()
            updateWaitTimeDisplay(30)
            bottomSheetDialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.option60).setOnClickListener {
            // 모든 라디오 버튼 해제 후 선택
            radio15.isChecked = false
            radio30.isChecked = false
            radio60.isChecked = true

            prefs.edit().putInt("wait_time", 60).apply()
            updateWaitTimeDisplay(60)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
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
            if (isChecked) {
                deviceAdminManager.requestActivation(this, REQUEST_CODE_ENABLE_DEVICE_ADMIN)
            } else {
                deviceAdminManager.removeAdmin()
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
