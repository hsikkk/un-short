package com.muuu.unshort

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView

/**
 * 간단한 경고 다이얼로그
 *
 * un:short 디자인 시스템을 따르는 경고/확인 다이얼로그
 * 입력 필드가 필요 없는 간단한 확인용
 *
 * @param context Context
 * @param titleResId 제목 문자열 리소스 ID
 * @param messageResId 메시지 문자열 리소스 ID
 * @param positiveTextResId 확인 버튼 텍스트 리소스 ID
 * @param negativeTextResId 취소 버튼 텍스트 리소스 ID (null이면 버튼 숨김)
 * @param canceledOnTouchOutside 바깥 영역 터치 시 닫기 여부 (기본값: false)
 * @param onPositive 확인 버튼 클릭 콜백
 * @param onNegative 취소 버튼 클릭 콜백
 */
class WarningDialog(
    context: Context,
    private val titleResId: Int,
    private val messageResId: Int,
    private val positiveTextResId: Int,
    private val negativeTextResId: Int? = null,
    private val canceledOnTouchOutside: Boolean = false,
    private val onPositive: () -> Unit,
    private val onNegative: () -> Unit = {}
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_warning, null)

        setContentView(view)

        // Set dialog size: 90% width, wrap content height
        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Enable dimmed background
        window?.setDimAmount(0.5f)

        // Transparent background for rounded corners
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Allow dismissal by back button
        setCancelable(true)
        setCanceledOnTouchOutside(canceledOnTouchOutside)

        // Call onNegative when dismissed by back button
        setOnCancelListener {
            onNegative()
        }

        // Setup views
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnNegative = view.findViewById<Button>(R.id.btnNegative)
        val btnPositive = view.findViewById<Button>(R.id.btnPositive)

        // Set texts
        dialogTitle.setText(titleResId)
        dialogMessage.setText(messageResId)
        btnPositive.setText(positiveTextResId)

        // Negative button (optional)
        if (negativeTextResId != null) {
            btnNegative.setText(negativeTextResId)
            btnNegative.visibility = android.view.View.VISIBLE
            btnNegative.setOnClickListener {
                onNegative()
                dismiss()
            }
        } else {
            btnNegative.visibility = android.view.View.GONE
        }

        // Positive button
        btnPositive.setOnClickListener {
            onPositive()
            dismiss()
        }
    }
}
