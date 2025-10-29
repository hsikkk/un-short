package com.muuu.unshort

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var powerButton: FrameLayout
    private lateinit var powerButtonBackground: View
    private lateinit var powerIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusDescription: TextView
    private lateinit var toggleCard: MaterialCardView

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
        powerButtonBackground = findViewById(R.id.powerButtonBackground)
        powerIcon = findViewById(R.id.powerIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusDescription = findViewById(R.id.statusDescription)
        toggleCard = findViewById(R.id.toggleCard)

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
            animateButtonPress(it)

            // UI 업데이트
            updateUI(newState)
        }
    }

    private fun animateButtonPress(view: View) {
        // 버튼을 살짝 눌렸다가 원래대로 돌아오는 애니메이션
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.92f).apply {
            duration = 100
        }
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f).apply {
            duration = 100
        }
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.92f, 1f).apply {
            duration = 100
            startDelay = 100
        }
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.92f, 1f).apply {
            duration = 100
            startDelay = 100
        }

        scaleDown.start()
        scaleDownY.start()
        scaleUp.start()
        scaleUpY.start()
    }

    private fun updateUI(isEnabled: Boolean) {
        if (isEnabled) {
            // 활성화 상태 - 녹색
            statusTitle.text = "차단 활성화됨"
            statusDescription.text = "쇼츠 앱을 열면 차단이 작동합니다"
            toggleCard.strokeColor = ContextCompat.getColor(this, R.color.success)

            // 전원 버튼 - 녹색
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, R.color.success))
            }
            powerButtonBackground.background = drawable

            // 아이콘 색상 - 흰색
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
        } else {
            // 비활성화 상태 - 회색
            statusTitle.text = "차단 비활성화됨"
            statusDescription.text = "쇼츠 앱을 자유롭게 사용할 수 있습니다"
            toggleCard.strokeColor = ContextCompat.getColor(this, R.color.gray_300)

            // 전원 버튼 - 회색
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@MainActivity, R.color.gray_400))
            }
            powerButtonBackground.background = drawable

            // 아이콘 색상 - 흰색
            DrawableCompat.setTint(
                DrawableCompat.wrap(powerIcon.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
        }
    }
}
