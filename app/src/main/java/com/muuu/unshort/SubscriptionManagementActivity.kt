package com.muuu.unshort

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.muuu.unshort.premium.PremiumManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 구독 관리 화면
 *
 * 구독 정보 표시 및 해지 기능 제공
 */
class SubscriptionManagementActivity : BaseActivity() {

    private lateinit var backButton: ImageView
    private lateinit var subscriptionStatus: TextView
    private lateinit var subscriptionStartDate: TextView
    private lateinit var subscriptionNextBilling: TextView
    private lateinit var subscriptionAutoRenew: TextView
    private lateinit var cancelSubscriptionButton: MaterialButton

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription_management)

        // View 초기화
        backButton = findViewById(R.id.backButton)
        subscriptionStatus = findViewById(R.id.subscriptionStatus)
        subscriptionStartDate = findViewById(R.id.subscriptionStartDate)
        subscriptionNextBilling = findViewById(R.id.subscriptionNextBilling)
        subscriptionAutoRenew = findViewById(R.id.subscriptionAutoRenew)
        cancelSubscriptionButton = findViewById(R.id.cancelSubscriptionButton)

        // 뒤로 가기 버튼
        backButton.setOnClickListener {
            finish()
        }

        // 구독 해지 버튼
        cancelSubscriptionButton.setOnClickListener {
            PremiumManager.openSubscriptionManagement(this)
        }

        // 구독 정보 로드
        loadSubscriptionInfo()
    }

    /**
     * 구독 정보 로드 및 UI 업데이트
     */
    private fun loadSubscriptionInfo() {
        // Lifetime Premium 확인 (최우선)
        if (PremiumManager.isLifetimePremium()) {
            val lifetimeInfo = PremiumManager.getLifetimePremiumInfo()

            // Lifetime Premium 상태 표시
            subscriptionStatus.text = getString(R.string.promo_lifetime_status)
            subscriptionStatus.setTextColor(getColor(R.color.success))

            // 활성화 날짜
            if (lifetimeInfo != null) {
                subscriptionStartDate.text = dateFormat.format(Date(lifetimeInfo.redeemedAt))
            } else {
                subscriptionStartDate.text = "-"
            }

            // Lifetime은 만료일 없음
            subscriptionNextBilling.text = getString(R.string.promo_no_expiry)

            // 자동 갱신 없음
            subscriptionAutoRenew.text = getString(R.string.promo_not_applicable)

            // 구독 해지 버튼 숨김 (Lifetime은 해지 불가)
            cancelSubscriptionButton.visibility = View.GONE

            return
        }

        // Google Play 구독 정보 확인
        val subscriptionInfo = PremiumManager.getSubscriptionInfo()

        if (subscriptionInfo != null) {
            // 구독 상태
            subscriptionStatus.text = if (subscriptionInfo.isActive) {
                getString(R.string.subscription_status_active)
            } else {
                getString(R.string.subscription_status_expired)
            }
            subscriptionStatus.setTextColor(
                if (subscriptionInfo.isActive) {
                    getColor(R.color.success)
                } else {
                    getColor(R.color.error)
                }
            )

            // 구독 시작일
            subscriptionStartDate.text = dateFormat.format(subscriptionInfo.purchaseDate)

            // 다음 결제일
            if (subscriptionInfo.estimatedExpiryDate != null) {
                subscriptionNextBilling.text = dateFormat.format(subscriptionInfo.estimatedExpiryDate)
            } else {
                subscriptionNextBilling.text = "-"
            }

            // 자동 갱신 상태
            subscriptionAutoRenew.text = if (subscriptionInfo.autoRenewing) {
                getString(R.string.subscription_auto_renew_on)
            } else {
                getString(R.string.subscription_auto_renew_off)
            }

            cancelSubscriptionButton.visibility = View.VISIBLE
        } else {
            // 구독 정보 없음 (비구독자)
            subscriptionStatus.text = getString(R.string.subscription_status_expired)
            subscriptionStatus.setTextColor(getColor(R.color.error))
            subscriptionStartDate.text = "-"
            subscriptionNextBilling.text = "-"
            subscriptionAutoRenew.text = "-"
            cancelSubscriptionButton.visibility = View.GONE
        }
    }
}
