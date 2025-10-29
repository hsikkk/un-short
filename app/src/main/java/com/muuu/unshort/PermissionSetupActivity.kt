package com.muuu.unshort

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PermissionSetupActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private lateinit var backButton: ImageView
    private var accessibilityCard: View? = null
    private var overlayCard: View? = null
    private var serviceStatusText: TextView? = null
    private var serviceDescription: TextView? = null
    private var overlayStatusText: TextView? = null
    private var overlayDescription: TextView? = null
    private var settingsButton: Button? = null
    private var overlayButton: Button? = null
    private var completeButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        // View 초기화
        backButton = findViewById(R.id.backButton)
        accessibilityCard = findViewById(R.id.onboardingAccessibilityCard)
        overlayCard = findViewById(R.id.onboardingOverlayCard)
        serviceStatusText = findViewById(R.id.onboardingServiceStatusText)
        serviceDescription = findViewById(R.id.onboardingServiceDescription)
        overlayStatusText = findViewById(R.id.onboardingOverlayStatusText)
        overlayDescription = findViewById(R.id.onboardingOverlayDescription)
        settingsButton = findViewById(R.id.onboardingSettingsButton)
        overlayButton = findViewById(R.id.onboardingOverlayButton)
        completeButton = findViewById(R.id.startButton)

        // 완료 버튼 텍스트 변경
        completeButton?.text = "완료"

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
        }

        // 접근성 설정 버튼
        settingsButton?.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // 오버레이 설정 버튼
        overlayButton?.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // 완료 버튼
        completeButton?.setOnClickListener {
            finish()
        }

        // 초기 권한 상태 업데이트
        updatePermissionUI()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun updatePermissionUI() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)

        // 접근성 서비스 상태 업데이트
        if (accessibilityEnabled) {
            serviceStatusText?.text = "접근성 서비스 ✓"
            serviceStatusText?.setTextColor(getColor(R.color.success))
            serviceDescription?.text = "설정 완료"
            serviceDescription?.setTextColor(getColor(R.color.success))
            settingsButton?.visibility = View.GONE
            accessibilityCard?.alpha = 0.6f
        } else {
            serviceStatusText?.text = "접근성 서비스"
            serviceStatusText?.setTextColor(getColor(R.color.gray_900))
            serviceDescription?.text = "아직 설정되지 않았습니다"
            serviceDescription?.setTextColor(getColor(R.color.error))
            settingsButton?.visibility = View.VISIBLE
            accessibilityCard?.alpha = 1.0f
        }

        // 오버레이 권한 상태 업데이트
        if (overlayEnabled) {
            overlayStatusText?.text = "다른 앱 위에 표시 ✓"
            overlayStatusText?.setTextColor(getColor(R.color.success))
            overlayDescription?.text = "설정 완료"
            overlayDescription?.setTextColor(getColor(R.color.success))
            overlayButton?.visibility = View.GONE
            overlayCard?.alpha = 0.6f
        } else {
            overlayStatusText?.text = "다른 앱 위에 표시"
            overlayStatusText?.setTextColor(getColor(R.color.gray_900))
            overlayDescription?.text = "아직 설정되지 않았습니다"
            overlayDescription?.setTextColor(getColor(R.color.error))
            overlayButton?.visibility = View.VISIBLE
            overlayCard?.alpha = 1.0f
        }

        // 모든 권한 완료 시 완료 버튼 표시
        if (accessibilityEnabled && overlayEnabled) {
            completeButton?.visibility = View.VISIBLE
        } else {
            completeButton?.visibility = View.GONE
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
}
