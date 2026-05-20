package com.muuu.unshort.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.R

/**
 * Privacy consent dialog for accessibility service usage
 * Displays detailed information about data collection and usage
 *
 * @param consentContext Analytics용 컨텍스트 (예: "accessibility_setup")
 */
class PrivacyConsentDialog(
    context: Context,
    private val onAgree: (() -> Unit)? = null,
    private val onExit: () -> Unit,
    private val exitButtonTextResId: Int? = null,
    private val consentContext: String = CONTEXT_ACCESSIBILITY_SETUP
) : Dialog(context) {

    companion object {
        const val CONTEXT_ACCESSIBILITY_SETUP = "accessibility_setup"
    }

    private var resolved: Boolean = false

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

        // Track shown
        AnalyticsManager.trackEvent(
            context,
            AnalyticsEvent.PRIVACY_CONSENT_DIALOG_SHOWN,
            mapOf("context" to consentContext)
        )

        // Call onExit when dismissed by back button
        setOnCancelListener {
            if (!resolved) {
                resolved = true
                AnalyticsManager.trackEvent(
                    context,
                    AnalyticsEvent.PRIVACY_CONSENT_DECLINED,
                    mapOf(
                        "context" to consentContext,
                        "via" to "back_button"
                    )
                )
            }
            onExit()
        }

        // Setup buttons
        val agreeButton = view.findViewById<Button>(R.id.btnAgree)
        val exitButton = view.findViewById<Button>(R.id.btnExit)

        agreeButton.setOnClickListener {
            resolved = true
            AnalyticsManager.trackEvent(
                context,
                AnalyticsEvent.PRIVACY_CONSENT_ACCEPTED,
                mapOf("context" to consentContext)
            )
            onAgree?.invoke()
            dismiss()
        }

        // 커스텀 버튼 텍스트 리소스 ID가 제공되면 사용
        exitButtonTextResId?.let {
            exitButton.setText(it)
        }

        exitButton.setOnClickListener {
            resolved = true
            AnalyticsManager.trackEvent(
                context,
                AnalyticsEvent.PRIVACY_CONSENT_DECLINED,
                mapOf(
                    "context" to consentContext,
                    "via" to "exit_button"
                )
            )
            onExit()
            dismiss()
        }
    }
}
