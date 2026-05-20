package com.muuu.unshort.ui.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.config.AppConstants
import com.muuu.unshort.premium.PremiumManager
import com.muuu.unshort.promo.PromoCodeDialog
import com.muuu.unshort.ui.activity.PremiumUpgradeActivity
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.R

/**
 * 프리미엄 업그레이드 화면
 *
 * 프리미엄 기능 5가지를 비주얼하게 소개하고 업그레이드 유도
 * 더미 구현: 실제 결제 연동 전까지 Toast 메시지 표시
 */
class PremiumUpgradeActivity : BaseActivity() {

    override fun isLightStatusBar(): Boolean = true

    private lateinit var closeButton: ImageView
    private lateinit var upgradeButton: MaterialButton
    private lateinit var promoCodeButton: TextView

    private var entrySource: String = AppConstants.ENTRY_SOURCE_DIRECT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_upgrade)

        entrySource = intent?.getStringExtra(AppConstants.EXTRA_ENTRY_SOURCE)
            ?: AppConstants.ENTRY_SOURCE_DIRECT

        // View 초기화
        closeButton = findViewById(R.id.closeButton)
        upgradeButton = findViewById(R.id.upgradeButton)
        promoCodeButton = findViewById(R.id.promoCodeButton)

        // 가격 정보 로드
        loadPricing()

        // 닫기 버튼
        closeButton.setOnClickListener {
            finish()
        }

        // 업그레이드 버튼
        upgradeButton.setOnClickListener {
            // 업그레이드 버튼 클릭 추적
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.PREMIUM_UPGRADE_BUTTON_CLICKED,
                mapOf("entry_source" to entrySource)
            )

            // PremiumManager를 통해 실제 구매 플로우 시작
            // Dummy: Toast + 즉시 성공
            // Google Play Billing: 실제 결제 화면
            PremiumManager.showPremiumPurchase(this, entrySource) { success ->
                if (success) {
                    Toast.makeText(this, "프리미엄 활성화!", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        // 프로모션 코드 버튼
        promoCodeButton.setOnClickListener {
            // 프로모 코드 다이얼로그 오픈 추적
            AnalyticsManager.trackEvent(
                this,
                AnalyticsEvent.PROMO_CODE_DIALOG_OPENED,
                mapOf("entry_source" to entrySource)
            )

            showPromoCodeDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // 프리미엄 업그레이드 화면 조회 추적
        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.PREMIUM_UPGRADE_SCREEN_VIEWED,
            mapOf("entry_source" to entrySource)
        )
    }

    private fun showPromoCodeDialog() {
        val dialog = PromoCodeDialog(this) {
            // 성공 시 화면 종료
            Toast.makeText(this, "Lifetime Premium이 활성화되었습니다!", Toast.LENGTH_LONG).show()
            finish()
        }
        dialog.show()
    }

    /**
     * 가격 정보 로드 및 버튼 업데이트
     */
    private fun loadPricing() {
        // 로딩 상태
        upgradeButton.apply {
            text = getString(R.string.premium_button_loading)
            isEnabled = false
        }

        PremiumManager.queryPriceInfo { priceInfo ->
            runOnUiThread {
                val buttonText = when {
                    priceInfo == null -> {
                        // Fallback: 가격 로드 실패
                        getString(R.string.premium_button_fallback)
                    }
                    priceInfo.hasFreeTrial -> {
                        // 무료 체험 가능
                        getString(R.string.premium_button_with_trial, priceInfo.trialDays ?: 7)
                    }
                    else -> {
                        // 무료 체험 없음 (가격 표시)
                        getString(R.string.premium_button_without_trial, priceInfo.formattedPrice)
                    }
                }

                upgradeButton.apply {
                    text = buttonText
                    isEnabled = true
                }
            }
        }
    }
}
