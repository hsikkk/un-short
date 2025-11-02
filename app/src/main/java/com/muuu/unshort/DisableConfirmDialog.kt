package com.muuu.unshort

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

/**
 * Confirmation dialog for disabling shorts blocking feature
 * Requires user to type exact phrase to confirm the action
 */
class DisableConfirmDialog(
    context: Context,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private val requiredPhrase = context.getString(R.string.disable_dialog_phrase)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_disable_confirm, null)

        setContentView(view)

        // Set dialog size: 90% width, wrap content height
        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Enable dimmed background
        window?.setDimAmount(0.5f)

        // Allow dismissal by back button
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        // Call onCancel when dismissed by back button
        setOnCancelListener {
            onCancel()
        }

        // Setup views
        val warningMessage = view.findViewById<TextView>(R.id.warningMessage)
        val confirmInput = view.findViewById<TextInputEditText>(R.id.confirmInput)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        // Set warning message (simple)
        warningMessage.text = context.getString(R.string.disable_dialog_message)

        // Initially disable confirm button with visual feedback
        btnConfirm.isEnabled = false
        btnConfirm.alpha = 0.4f

        // Real-time validation with TextWatcher
        confirmInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
            }

            override fun afterTextChanged(s: Editable?) {
                val inputText = s?.toString() ?: ""
                // Enable button only if input exactly matches the required phrase (case-sensitive)
                val isMatch = inputText == requiredPhrase
                btnConfirm.isEnabled = isMatch

                // Visual feedback: change button appearance
                btnConfirm.alpha = if (isMatch) 1.0f else 0.4f
            }
        })

        // Cancel button
        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        // Confirm button
        btnConfirm.setOnClickListener {
            onConfirm()
            dismiss()
        }
    }
}
