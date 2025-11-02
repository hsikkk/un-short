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

    companion object {
        private const val PREF_KEY_ACCESSIBILITY_CONSENT = "accessibility_consent_agreed"
    }

    data class PermissionUIElements(
        val card: View?,
        val statusText: TextView?,
        val descriptionText: TextView?,
        val settingsButton: Button?
    )

    /**
     * 접근성 동의 여부 확인
     */
    fun isAccessibilityConsentGiven(): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_ACCESSIBILITY_CONSENT, false)
    }

    /**
     * 접근성 동의 저장
     */
    fun saveAccessibilityConsent() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_ACCESSIBILITY_CONSENT, true).apply()
    }

    /**
     * 접근성 동의 카드 UI 업데이트
     */
    fun updateConsentCard(elements: PermissionUIElements) {
        val isConsented = isAccessibilityConsentGiven()

        elements.apply {
            if (isConsented) {
                statusText?.text = context.getString(R.string.permission_consent_title_completed)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                descriptionText?.text = context.getString(R.string.permission_consent_completed)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                settingsButton?.visibility = View.GONE
                card?.alpha = 0.6f
            } else {
                statusText?.text = context.getString(R.string.permission_consent_title)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
                descriptionText?.text = context.getString(R.string.permission_not_set)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.error))
                settingsButton?.visibility = View.VISIBLE
                card?.alpha = 1.0f
            }
        }
    }

    /**
     * 접근성 서비스 카드 UI 업데이트
     */
    fun updateAccessibilityCard(elements: PermissionUIElements) {
        val isConsented = isAccessibilityConsentGiven()
        val isEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)

        elements.apply {
            if (isEnabled) {
                // 설정 완료
                statusText?.text = context.getString(R.string.permission_accessibility_completed)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                descriptionText?.text = context.getString(R.string.permission_completed)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                settingsButton?.visibility = View.GONE
                settingsButton?.isEnabled = false
                card?.alpha = 0.6f
            } else if (!isConsented) {
                // 동의 전 - 비활성화
                statusText?.text = context.getString(R.string.permission_accessibility)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_600))
                descriptionText?.text = context.getString(R.string.permission_consent_required)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.gray_600))
                settingsButton?.visibility = View.VISIBLE
                settingsButton?.isEnabled = false
                card?.alpha = 0.4f
            } else {
                // 동의 후, 설정 전 - 활성화
                statusText?.text = context.getString(R.string.permission_accessibility)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
                descriptionText?.text = context.getString(R.string.permission_not_set)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.error))
                settingsButton?.visibility = View.VISIBLE
                settingsButton?.isEnabled = true
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
                statusText?.text = context.getString(R.string.permission_overlay_completed)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                descriptionText?.text = context.getString(R.string.permission_completed)
                descriptionText?.setTextColor(ContextCompat.getColor(context, R.color.success))
                settingsButton?.visibility = View.GONE
                card?.alpha = 0.6f
            } else {
                statusText?.text = context.getString(R.string.permission_overlay)
                statusText?.setTextColor(ContextCompat.getColor(context, R.color.gray_900))
                descriptionText?.text = context.getString(R.string.permission_not_set)
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
