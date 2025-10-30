package com.muuu.unshort

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private lateinit var backButton: ImageView
    private lateinit var waitTimeValue: TextView
    private lateinit var waitTimeItem: LinearLayout
    private lateinit var hapticSwitch: Switch
    private lateinit var feedbackItem: LinearLayout
    private lateinit var shareItem: LinearLayout
    private lateinit var reviewItem: LinearLayout
    private lateinit var versionItem: LinearLayout
    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // View 초기화
        backButton = findViewById(R.id.backButton)
        waitTimeValue = findViewById(R.id.waitTimeValue)
        waitTimeItem = findViewById(R.id.waitTimeItem)
        hapticSwitch = findViewById(R.id.hapticSwitch)
        feedbackItem = findViewById(R.id.feedbackItem)
        shareItem = findViewById(R.id.shareItem)
        reviewItem = findViewById(R.id.reviewItem)
        versionItem = findViewById(R.id.versionItem)
        versionText = findViewById(R.id.versionText)

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
}