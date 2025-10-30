package com.muuu.unshort

import android.content.res.Configuration
import android.os.Bundle
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
    private lateinit var permissionUIHelper: PermissionUIHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        permissionUIHelper = PermissionUIHelper(this)

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
            PermissionUtils.openAccessibilitySettings(this)
        }

        // 오버레이 설정 버튼
        overlayButton?.setOnClickListener {
            PermissionUtils.openOverlaySettings(this)
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
        // 접근성 서비스 카드 업데이트
        permissionUIHelper.updateAccessibilityCard(
            PermissionUIHelper.PermissionUIElements(
                card = accessibilityCard,
                statusText = serviceStatusText,
                descriptionText = serviceDescription,
                settingsButton = settingsButton
            )
        )

        // 오버레이 권한 카드 업데이트
        permissionUIHelper.updateOverlayCard(
            PermissionUIHelper.PermissionUIElements(
                card = overlayCard,
                statusText = overlayStatusText,
                descriptionText = overlayDescription,
                settingsButton = overlayButton
            )
        )

        // 완료 버튼 표시 업데이트
        permissionUIHelper.updateCompleteButton(completeButton)
    }
}
