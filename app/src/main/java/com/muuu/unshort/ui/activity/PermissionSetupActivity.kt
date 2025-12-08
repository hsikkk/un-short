package com.muuu.unshort.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.util.PermissionUIHelper
import com.muuu.unshort.ui.activity.MainActivity
import com.muuu.unshort.util.PermissionUtils
import com.muuu.unshort.ui.dialog.PrivacyConsentDialog
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.ui.activity.PermissionSetupActivity
import com.muuu.unshort.R

class PermissionSetupActivity : BaseActivity() {

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
    private var fromOnboarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        permissionUIHelper = PermissionUIHelper(this)

        // 온보딩에서 진입했는지 확인
        fromOnboarding = intent.getBooleanExtra("from_onboarding", false)

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

        // 온보딩에서 진입한 경우 뒤로가기 버튼 숨김
        if (fromOnboarding) {
            backButton.visibility = View.GONE
        } else {
            // 뒤로 가기 버튼
            backButton.setOnClickListener {
                finish()
            }
        }

        // 접근성 Enable Permission 버튼 - 다이얼로그 → 동의 → 설정 이동
        settingsButton?.setOnClickListener {
            showAccessibilityConsentDialog()
        }

        // 오버레이 설정 버튼
        overlayButton?.setOnClickListener {
            PermissionUtils.openOverlaySettings(this)
        }

        // 완료 버튼
        completeButton?.setOnClickListener {
            if (fromOnboarding) {
                // 온보딩에서 진입한 경우 MainActivity로 이동
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }

        // 초기 권한 상태 업데이트
        updatePermissionUI()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    override fun onBackPressed() {
        if (fromOnboarding) {
            // 온보딩에서 진입한 경우 뒤로가기 무시
            // 사용자가 권한 설정을 건너뛰려면 완료 버튼을 사용해야 함
        } else {
            super.onBackPressed()
        }
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

        // 권한 상태가 변경되었을 수 있으므로 User Property 업데이트
        AnalyticsManager.updatePermissionUserProperties(this)
    }

    /**
     * 접근성 권한 동의 다이얼로그 표시 → 동의 → 설정 이동 + 토스트
     */
    private fun showAccessibilityConsentDialog() {
        val dialog = PrivacyConsentDialog(
            context = this,
            onAgree = {
                // 제조사별 추가 안내가 필요한 경우에만 토스트 표시
                if (PermissionUtils.needsAccessibilityGuide()) {
                    val guideText = PermissionUtils.getAccessibilityGuide(this)
                    Toast.makeText(this, guideText, Toast.LENGTH_LONG).show()
                }
                // 시스템 설정으로 이동
                PermissionUtils.openAccessibilitySettings(this)
            },
            onExit = {
                // 동의 거부 시 아무 것도 하지 않음
            },
            exitButtonTextResId = R.string.privacy_btn_disagree
        )
        dialog.show()
    }
}
