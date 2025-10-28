package com.shortblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var serviceStatusText: TextView
    private lateinit var overlayStatusText: TextView
    private lateinit var settingsButton: Button
    private lateinit var overlayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceStatusText = findViewById(R.id.serviceStatusText)
        overlayStatusText = findViewById(R.id.overlayStatusText)
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
        updateServiceStatus()
        updateOverlayStatus()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        if (isEnabled) {
            serviceStatusText.text = "접근성 서비스: 활성화 ✓"
            serviceStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            serviceStatusText.text = "접근성 서비스: 비활성화"
            serviceStatusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun updateOverlayStatus() {
        val isEnabled = Settings.canDrawOverlays(this)
        if (isEnabled) {
            overlayStatusText.text = "오버레이 권한: 허용됨 ✓"
            overlayStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            overlayStatusText.text = "오버레이 권한: 필요함"
            overlayStatusText.setTextColor(getColor(android.R.color.holo_red_dark))
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
