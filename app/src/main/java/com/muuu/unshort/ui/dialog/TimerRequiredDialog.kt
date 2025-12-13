package com.muuu.unshort.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.muuu.unshort.R

/**
 * 타이머 필요 다이얼로그
 *
 * 최근 10분 내 쇼츠 접근 시도가 있을 때 표시
 * 3가지 옵션 제공:
 * 1. 타이머 시작
 * 2. 광고 보고 해제
 * 3. 취소
 *
 * @param context Context
 * @param onWatchAd 광고 보기 버튼 클릭 콜백
 * @param onStartTimer 타이머 시작 버튼 클릭 콜백
 * @param onCancel 취소 버튼 클릭 콜백
 */
class TimerRequiredDialog(
    context: Context,
    private val onWatchAd: () -> Unit,
    private val onStartTimer: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_timer_required, null)

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
        val dialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnStartTimer = view.findViewById<Button>(R.id.btnStartTimer)
        val btnWatchAd = view.findViewById<Button>(R.id.btnWatchAd)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // Set texts
        dialogTitle.setText(R.string.timer_required_title)
        dialogMessage.setText(R.string.timer_required_message)
        btnStartTimer.setText(R.string.timer_required_start_timer)
        btnWatchAd.setText(R.string.timer_required_watch_ad)
        btnCancel.setText(R.string.timer_required_cancel)

        // Button listeners
        btnStartTimer.setOnClickListener {
            onStartTimer()
            dismiss()
        }

        btnWatchAd.setOnClickListener {
            onWatchAd()
            dismiss()
        }

        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }
    }
}
