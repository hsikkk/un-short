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
 * Confirmation dialog for disabling features
 * Requires user to type exact phrase to confirm the action
 * Can be used for blocking disable, device admin disable, etc.
 */
class DisableConfirmDialog(
    context: Context,
    private val titleResId: Int = R.string.disable_dialog_title,
    private val messageResId: Int = R.string.disable_dialog_message,
    private val requiredPhraseResId: Int = R.string.disable_dialog_phrase,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private val requiredPhrase = context.getString(requiredPhraseResId)

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
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val warningMessage = view.findViewById<TextView>(R.id.warningMessage)
        val phraseToType = view.findViewById<TextView>(R.id.phraseToType)
        val confirmInput = view.findViewById<TextInputEditText>(R.id.confirmInput)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        // Set dynamic texts
        dialogTitle.setText(titleResId)
        warningMessage.setText(messageResId)
        phraseToType.setText(requiredPhraseResId)

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
