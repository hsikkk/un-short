package com.muuu.unshort

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class MainActivity : AppCompatActivity() {

    private lateinit var powerButton: FrameLayout
    private lateinit var powerButtonCard: CardView
    private lateinit var powerIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusDot: View
    private lateinit var statusLabel: TextView

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
        powerButton = findViewById(R.id.powerButton)
        powerButtonCard = powerButton.parent as CardView
        powerIcon = findViewById(R.id.powerIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusDot = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)

        // 저장된 차단 상태 불러오기
        val isBlocking = prefs.getBoolean("blocking_enabled", true)
        updateUI(isBlocking)

        // 전원 버튼 클릭 리스너
        powerButton.setOnClickListener {
            val currentState = prefs.getBoolean("blocking_enabled", true)
            val newState = !currentState

            // 상태 저장
            prefs.edit().putBoolean("blocking_enabled", newState).apply()

            // 버튼 애니메이션
            animateButtonPress(powerButton.parent as View)

            // UI 업데이트
            updateUI(newState)
        }
    }

    private fun animateButtonPress(view: View) {
        // 버튼을 살짝 눌렸다가 원래대로 돌아오는 애니메이션
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f).apply {
            duration = 150
        }
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f).apply {
            duration = 150
        }
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f).apply {
            duration = 150
            startDelay = 150
        }
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f).apply {
            duration = 150
            startDelay = 150
        }

        scaleDown.start()
        scaleDownY.start()
        scaleUp.start()
        scaleUpY.start()
    }

    private fun updateUI(isEnabled: Boolean) {
        if (isEnabled) {
            // 활성화 상태 - 검은색 버튼
            statusTitle.text = "차단 활성화됨"
            statusTitle.alpha = 0.8f

            // CardView 배경 - 검은색
            powerButtonCard.setCardBackgroundColor(Color.parseColor("#000000"))
            powerButtonCard.cardElevation = 24f

            // 아이콘 색상 - 흰색
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.WHITE
            )

            // 상태 인디케이터
            statusDot.setBackgroundResource(R.drawable.status_dot_active)
            statusLabel.text = "ACTIVE"
            statusLabel.alpha = 0.4f
        } else {
            // 비활성화 상태 - 흰색 버튼 + 얇은 테두리
            statusTitle.text = "차단 비활성화됨"
            statusTitle.alpha = 0.4f

            // CardView 배경 - 흰색
            powerButtonCard.setCardBackgroundColor(Color.WHITE)
            powerButtonCard.cardElevation = 8f

            // 아이콘 색상 - 검은색 30%
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                Color.parseColor("#4D000000")
            )

            // 상태 인디케이터
            statusDot.setBackgroundResource(R.drawable.status_dot_inactive)
            statusLabel.text = "INACTIVE"
            statusLabel.alpha = 0.3f
        }
    }
}
