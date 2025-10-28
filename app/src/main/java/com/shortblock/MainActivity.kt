package com.shortblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var readyStatusCard: MaterialCardView
    private lateinit var statusCard: MaterialCardView
    private lateinit var serviceStatusText: TextView
    private lateinit var serviceDescription: TextView
    private lateinit var overlayStatusText: TextView
    private lateinit var overlayDescription: TextView
    private lateinit var settingsButton: Button
    private lateinit var overlayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        readyStatusCard = findViewById(R.id.readyStatusCard)
        statusCard = findViewById(R.id.statusCard)
        serviceStatusText = findViewById(R.id.serviceStatusText)
        serviceDescription = findViewById(R.id.serviceDescription)
        overlayStatusText = findViewById(R.id.overlayStatusText)
        overlayDescription = findViewById(R.id.overlayDescription)
        settingsButton = findViewById(R.id.settingsButton)
        overlayButton = findViewById(R.id.overlayButton)

        settingsButton.setOnClickListener {
            // 접근성 설정으로 이동
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        overlayButton.setOnClickListener {
            // 오버레이 권한 설정으로 이동
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)

        // 모든 권한이 허용되었는지 확인
        val allReady = accessibilityEnabled && overlayEnabled

        if (allReady) {
            // 모두 완료되면 준비 완료 카드 표시, 상태 카드 숨김
            readyStatusCard.visibility = View.VISIBLE
            statusCard.visibility = View.GONE
        } else {
            // 권한이 부족하면 상태 카드 표시, 준비 완료 카드 숨김
            readyStatusCard.visibility = View.GONE
            statusCard.visibility = View.VISIBLE

            updateServiceStatus(accessibilityEnabled)
            updateOverlayStatus(overlayEnabled)
        }
    }

    private fun updateServiceStatus(isEnabled: Boolean) {
        if (isEnabled) {
            serviceStatusText.text = "접근성 서비스 ✓"
            serviceStatusText.setTextColor(getColor(R.color.success))
            serviceDescription.text = "활성화됨"
            settingsButton.visibility = View.GONE
        } else {
            serviceStatusText.text = "접근성 서비스"
            serviceStatusText.setTextColor(getColor(R.color.gray_900))
            serviceDescription.text = "쇼츠 앱 감지를 위해 필요합니다"
            settingsButton.visibility = View.VISIBLE
        }
    }

    private fun updateOverlayStatus(isEnabled: Boolean) {
        if (isEnabled) {
            overlayStatusText.text = "오버레이 권한 ✓"
            overlayStatusText.setTextColor(getColor(R.color.success))
            overlayDescription.text = "활성화됨"
            overlayButton.visibility = View.GONE
        } else {
            overlayStatusText.text = "오버레이 권한"
            overlayStatusText.setTextColor(getColor(R.color.gray_900))
            overlayDescription.text = "차단 화면 표시를 위해 필요합니다"
            overlayButton.visibility = View.VISIBLE
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
