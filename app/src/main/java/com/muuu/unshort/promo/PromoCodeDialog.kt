package com.muuu.unshort.promo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.muuu.unshort.R
import com.muuu.unshort.premium.PremiumManager

/**
 * 프로모션 코드 입력 다이얼로그
 *
 * Lifetime Premium 활성화를 위한 프로모션 코드를 입력받습니다.
 */
class PromoCodeDialog(
    context: Context,
    private val onSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var promoCodeInput: TextInputEditText
    private lateinit var errorMessage: TextView
    private lateinit var successMessage: TextView
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_promo_code)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        promoCodeInput = findViewById(R.id.promoCodeInput)
        errorMessage = findViewById(R.id.errorMessage)
        successMessage = findViewById(R.id.successMessage)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupListeners() {
        confirmButton.setOnClickListener {
            val code = promoCodeInput.text.toString().trim()

            if (code.isEmpty()) {
                showError("프로모션 코드를 입력해주세요")
                return@setOnClickListener
            }

            validatePromoCode(code)
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun validatePromoCode(code: String) {
        // Hide previous messages
        hideMessages()

        val result = PremiumManager.activatePromoCode(code)

        when (result) {
            is PromoCodeValidator.ValidationResult.Valid -> {
                showSuccess("Lifetime Premium이 활성화되었습니다!")
                confirmButton.isEnabled = false
                promoCodeInput.isEnabled = false

                // 1.5초 후 다이얼로그 닫기 및 콜백 호출
                promoCodeInput.postDelayed({
                    dismiss()
                    onSuccess()
                }, 500)
            }

            is PromoCodeValidator.ValidationResult.InvalidFormat -> {
                showError("올바른 형식이 아닙니다\n(예: UNSHORT-XXXXX-XXXXXXXX)")
            }

            is PromoCodeValidator.ValidationResult.InvalidSignature -> {
                showError("유효하지 않은 코드입니다")
            }

            is PromoCodeValidator.ValidationResult.Error -> {
                showError("오류가 발생했습니다: ${result.message}")
            }
        }
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
        successMessage.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        successMessage.text = message
        successMessage.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
    }

    private fun hideMessages() {
        errorMessage.visibility = View.GONE
        successMessage.visibility = View.GONE
    }
}
