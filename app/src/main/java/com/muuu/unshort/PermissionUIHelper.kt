package com.muuu.unshort

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * 권한 설정 UI 업데이트 헬퍼
 * OnboardingActivity와 PermissionSetupActivity의 공통 UI 업데이트 로직
 */
class PermissionUIHelper(private val context: Context) {

    data class PermissionUIElements(
        val card: View?,
        val statusText: TextView?,
        val descriptionText: TextView?,
        val settingsButton: Button?
    )

    /**
     * 접근성 서비스 카드 UI 업데이트
     */
    fun updateAccessibilityCard(elements: PermissionUIElements) {
        val isEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)

        elements.apply {
            if (isEnabled) {
                statusText?.text = "접근성 서비스 ✓"
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                descriptionText?.text = "설정 완료"
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                settingsButton?.visibility = View.GONE
                card?.alpha = 0.6f
            } else {
                statusText?.text = "접근성 서비스"
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
                descriptionText?.text = "아직 설정되지 않았습니다"
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.error))
                settingsButton?.visibility = View.VISIBLE
                card?.alpha = 1.0f
            }
        }
    }

    /**
     * 오버레이 권한 카드 UI 업데이트
     */
    fun updateOverlayCard(elements: PermissionUIElements) {
        val isEnabled = PermissionUtils.canDrawOverlays(context)

        elements.apply {
            if (isEnabled) {
                statusText?.text = "다른 앱 위에 표시 ✓"
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                descriptionText?.text = "설정 완료"
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                settingsButton?.visibility = View.GONE
                card?.alpha = 0.6f
            } else {
                statusText?.text = "다른 앱 위에 표시"
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
                descriptionText?.text = "아직 설정되지 않았습니다"
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.error))
                settingsButton?.visibility = View.VISIBLE
                card?.alpha = 1.0f
            }
        }
    }

    /**
     * 완료/시작 버튼 표시 여부 업데이트
     */
    fun updateCompleteButton(button: Button?) {
        if (PermissionUtils.hasAllPermissions(context)) {
            button?.visibility = View.VISIBLE
        } else {
            button?.visibility = View.GONE
        }
    }
}
