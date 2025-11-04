package com.muuu.unshort

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.muuu.unshort.premium.PremiumManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium_upgrade)

        // View 초기화
        closeButton = findViewById(R.id.closeButton)
        upgradeButton = findViewById(R.id.upgradeButton)

        // 닫기 버튼
        closeButton.setOnClickListener {
            finish()
        }

        // 업그레이드 버튼
        upgradeButton.setOnClickListener {
            // PremiumManager를 통해 실제 구매 플로우 시작
            // Dummy: Toast + 즉시 성공
            // Google Play Billing: 실제 결제 화면
            PremiumManager.showPremiumPurchase(this) { success ->
                if (success) {
                    Toast.makeText(this, "프리미엄 활성화!", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }
}
