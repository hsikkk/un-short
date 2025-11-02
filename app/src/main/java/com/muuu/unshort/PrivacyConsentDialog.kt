package com.muuu.unshort

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Privacy consent dialog for accessibility service usage
 * Displays detailed information about data collection and usage
 */
class PrivacyConsentDialog(
    context: Context,
    private val onAgree: (() -> Unit)? = null,
    private val onExit: () -> Unit,
    private val exitButtonText: String? = null,
    private val readOnlyMode: Boolean = false
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_privacy_consent, null)

        setContentView(view)

        // Set dialog size: 90% width, 80% height
        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            (displayMetrics.heightPixels * 0.8).toInt()
        )

        // Enable dimmed background
        window?.setDimAmount(0.5f)

        // Allow dismissal by back button
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        // Call onExit when dismissed by back button
        setOnCancelListener {
            onExit()
        }

        // Setup buttons
        val agreeButton = view.findViewById<Button>(R.id.btnAgree)
        val exitButton = view.findViewById<Button>(R.id.btnExit)

        if (readOnlyMode) {
            // 읽기 전용 모드: Agree 버튼 숨김
            agreeButton.visibility = View.GONE
            exitButton.text = "Close"
        } else {
            // 일반 모드: 동의/거부 버튼 표시
            agreeButton.setOnClickListener {
                onAgree?.invoke()
                dismiss()
            }

            // 커스텀 버튼 텍스트가 제공되면 사용
            exitButtonText?.let {
                exitButton.text = it
            }
        }

        exitButton.setOnClickListener {
            onExit()
            dismiss()
        }
    }
}
