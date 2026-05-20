package com.muuu.unshort.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.prefs.PreferencesManager
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
    private lateinit var prefsManager: PreferencesManager
    private var fromOnboarding = false

    /**
     * onResume 호출 진입 시점에 모든 권한이 이미 충족된 적이 있었는지.
     * 새 권한 전환을 감지하기 위한 1회 latch.
     */
    private var onboardingCompletedTracked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        permissionUIHelper = PermissionUIHelper(this)
        prefsManager = PreferencesManager(this)

        // 온보딩에서 진입했는지 확인
        fromOnboarding = intent.getBooleanExtra("from_onboarding", false)

        // 진입 추적
        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.PERMISSION_SETUP_VIEWED,
            mapOf("from_onboarding" to fromOnboarding)
        )

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
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.PERMISSION_SETTINGS_OPENED,
                mapOf("type" to "overlay")
            )
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
        detectPermissionTransitions()
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
     * 직전 onResume과 비교하여 권한 전환을 감지하고 이벤트 발화.
     * Settings 화면 갔다 돌아왔을 때 호출됨.
     */
    private fun detectPermissionTransitions() {
        val accessibilityNow = PermissionUtils.isAccessibilityServiceEnabled(this)
        val overlayNow = PermissionUtils.canDrawOverlays(this)

        val accessibilityBefore = prefsManager.lastKnownAccessibilityPermission
        val overlayBefore = prefsManager.lastKnownOverlayPermission

        if (accessibilityBefore != accessibilityNow) {
            val eventName = if (accessibilityNow) {
                AnalyticsEvent.PERMISSION_GRANTED
            } else {
                AnalyticsEvent.PERMISSION_DENIED_RETURNED
            }
            AnalyticsManager.trackEvent(
                this,
                eventName,
                mapOf(
                    "type" to "accessibility",
                    "from_onboarding" to fromOnboarding
                )
            )
            prefsManager.lastKnownAccessibilityPermission = accessibilityNow
        }

        if (overlayBefore != overlayNow) {
            val eventName = if (overlayNow) {
                AnalyticsEvent.PERMISSION_GRANTED
            } else {
                AnalyticsEvent.PERMISSION_DENIED_RETURNED
            }
            AnalyticsManager.trackEvent(
                this,
                eventName,
                mapOf(
                    "type" to "overlay",
                    "from_onboarding" to fromOnboarding
                )
            )
            prefsManager.lastKnownOverlayPermission = overlayNow
        }

        // 모든 권한 충족 시 setup completed 1회 발화 + 온보딩 완료 마킹
        if (accessibilityNow && overlayNow && !onboardingCompletedTracked) {
            onboardingCompletedTracked = true
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.PERMISSION_SETUP_COMPLETED,
                mapOf("from_onboarding" to fromOnboarding)
            )
            AnalyticsManager.markOnboardingCompleted(this)
        }
    }

    /**
     * 접근성 권한 동의 다이얼로그 표시 → 동의 → 설정 이동 + 토스트
     */
    private fun showAccessibilityConsentDialog() {
        AnalyticsManager.trackEvent(this, AnalyticsEvent.ACCESSIBILITY_CONSENT_DIALOG_SHOWN)
        val dialog = PrivacyConsentDialog(
            context = this,
            onAgree = {
                AnalyticsManager.trackEvent(this, AnalyticsEvent.ACCESSIBILITY_CONSENT_AGREED)
                AnalyticsManager.trackEvent(
                    this,
                    AnalyticsEvent.PERMISSION_SETTINGS_OPENED,
                    mapOf("type" to "accessibility")
                )
                // 제조사별 추가 안내가 필요한 경우에만 토스트 표시
                if (PermissionUtils.needsAccessibilityGuide()) {
                    val guideText = PermissionUtils.getAccessibilityGuide(this)
                    Toast.makeText(this, guideText, Toast.LENGTH_LONG).show()
                }
                // 시스템 설정으로 이동
                PermissionUtils.openAccessibilitySettings(this)
            },
            onExit = {
                AnalyticsManager.trackEvent(this, AnalyticsEvent.ACCESSIBILITY_CONSENT_DECLINED)
            },
            exitButtonTextResId = R.string.privacy_btn_disagree
        )
        dialog.show()
    }
}
