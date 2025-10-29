package com.muuu.unshort

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = AppConstants.FONT_SCALE
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private lateinit var toggleArea: LinearLayout
    private lateinit var permissionWarning: LinearLayout
    private lateinit var permissionSettingsButton: com.google.android.material.button.MaterialButton
    private lateinit var toggleContainer: FrameLayout
    private lateinit var toggleCircle: CardView
    private lateinit var powerIcon: ImageView
    private lateinit var onText: TextView
    private lateinit var offText: TextView
    private lateinit var statusDot: View
    private lateinit var statusLabel: TextView
    private lateinit var settingsButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 온보딩 체크
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        if (!onboardingCompleted) {
            // 온보딩 화면으로 이동
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // View 초기화
        toggleArea = findViewById(R.id.toggleArea)
        permissionWarning = findViewById(R.id.permissionWarning)
        permissionSettingsButton = findViewById(R.id.permissionSettingsButton)
        toggleContainer = findViewById(R.id.toggleContainer)
        toggleCircle = findViewById(R.id.toggleCircle)
        powerIcon = findViewById(R.id.powerIcon)
        onText = findViewById(R.id.onText)
        offText = findViewById(R.id.offText)
        statusDot = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)
        settingsButton = findViewById(R.id.settingsButton)

        // 권한 설정 버튼 클릭 리스너
        permissionSettingsButton.setOnClickListener {
            val intent = Intent(this, PermissionSetupActivity::class.java)
            startActivity(intent)
        }

        // 설정 버튼 클릭 리스너
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 토글 스위치 클릭 리스너
        toggleContainer.setOnClickListener {
            val currentState = prefs.getBoolean("blocking_enabled", true)
            val newState = !currentState

            // 상태 저장
            prefs.edit().putBoolean("blocking_enabled", newState).apply()

            // UI 업데이트 (애니메이션 포함)
            updateUI(newState, animate = true)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateUI()
    }

    private fun checkPermissionsAndUpdateUI() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasPermissions = checkPermissions()

        if (!hasPermissions) {
            // 권한 없음 - 토글 숨기고 경고 표시
            toggleArea.visibility = View.GONE
            permissionWarning.visibility = View.VISIBLE

            // 차단 자동 비활성화
            prefs.edit().putBoolean("blocking_enabled", false).apply()
        } else {
            // 권한 있음 - 토글 표시
            toggleArea.visibility = View.VISIBLE
            permissionWarning.visibility = View.GONE

            // 저장된 차단 상태 불러오기
            val isBlocking = prefs.getBoolean("blocking_enabled", true)
            updateUI(isBlocking)
        }
    }

    private fun checkPermissions(): Boolean {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)
        return accessibilityEnabled && overlayEnabled
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

    private fun updateUI(isEnabled: Boolean, animate: Boolean = false) {
        // dp to pixels conversion
        val density = resources.displayMetrics.density
        // Container width: 180dp, Circle width: 80dp, Both margins: 10dp
        // Translation distance: 180dp - 80dp - 10dp = 90dp
        val translationDistance = 90f * density

        if (animate) {
            // 토글 애니메이션
            // Circle은 기본적으로 오른쪽에 위치 (layout_gravity="end")
            // ON: 오른쪽 원위치 (0f), OFF: 왼쪽으로 이동 (negative translation)
            val targetTranslation = if (isEnabled) 0f else -translationDistance

            ValueAnimator.ofFloat(toggleCircle.translationX, targetTranslation).apply {
                duration = 400
                addUpdateListener { animation ->
                    toggleCircle.translationX = animation.animatedValue as Float
                }
                start()
            }

            // Scale animation for feedback
            toggleContainer.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    toggleContainer.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else {
            // 초기 상태 설정 (애니메이션 없이)
            // Circle은 기본적으로 오른쪽에 위치 (layout_gravity="end")
            // ON: 오른쪽 원위치 (0f), OFF: 왼쪽으로 이동 (negative translation)
            toggleCircle.translationX = if (isEnabled) 0f else -translationDistance
        }

        if (isEnabled) {
            // ON 상태
            toggleContainer.setBackgroundResource(R.drawable.toggle_track_background)

            // ON/OFF 텍스트 투명도
            onText.alpha = 0.6f
            offText.alpha = 0f

            // 아이콘 색상
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.BLACK
            )

            // Status Indicator
            statusDot.setBackgroundResource(R.drawable.status_dot_active)
            statusLabel.text = "ACTIVE"
        } else {
            // OFF 상태
            toggleContainer.setBackgroundResource(R.drawable.toggle_track_inactive)

            // ON/OFF 텍스트 투명도
            onText.alpha = 0f
            offText.alpha = 0.6f

            // 아이콘 색상
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.parseColor("#BDBDBD")
            )

            // Status Indicator
            statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            statusLabel.text = "INACTIVE"
        }
    }
}
