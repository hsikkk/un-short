package com.muuu.unshort

import android.os.Bundle
import android.widget.ImageView
import com.google.android.material.button.MaterialButton

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

        // 업그레이드 버튼 (더미)
        upgradeButton.setOnClickListener {
            // 더미: 항상 성공으로 처리 (DummyPremiumProvider에서 onResult(true) 호출)
            // Activity 종료하면 PremiumManager의 Callback이 자동으로 UI 갱신
            finish()
        }
    }
}
