package com.muuu.unshort

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Privacy consent dialog for accessibility service usage
 * Displays detailed information about data collection and usage
 */
class PrivacyConsentDialog(
    context: Context,
    private val onAgree: () -> Unit,
    private val onExit: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_privacy_consent, null)

        setContentView(view)

        // Set dialog size: 90% width, max 65% height
        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            (displayMetrics.heightPixels * 0.65).toInt()
        )

        // Prevent dismissal by back button or outside touch
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        // Setup buttons
        view.findViewById<Button>(R.id.btnAgree).setOnClickListener {
            onAgree()
            dismiss()
        }

        view.findViewById<Button>(R.id.btnExit).setOnClickListener {
            onExit()
            dismiss()
        }
    }
}
