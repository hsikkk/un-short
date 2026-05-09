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
 * 일일 즉시 해제 한도 도달 다이얼로그
 *
 * "바로 보기" 클릭 시 한도가 0이면 노출. 3가지 옵션:
 * 1. 광고 보고 +N회 추가 (rewarded ad → 한도 충전)
 * 2. 프리미엄으로 무제한 (PremiumUpgradeActivity)
 * 3. 그냥 닫기
 */
class QuotaExhaustedDialog(
    context: Context,
    private val rechargeAmount: Int,
    private val onWatchAd: () -> Unit,
    private val onPremium: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_quota_exhausted, null)

        setContentView(view)

        val displayMetrics = context.resources.displayMetrics
        window?.setLayout(
            (displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        window?.setDimAmount(0.5f)

        setCancelable(true)
        setCanceledOnTouchOutside(false)

        setOnCancelListener {
            onCancel()
        }

        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnWatchAd = view.findViewById<Button>(R.id.btnWatchAd)
        val btnPremium = view.findViewById<Button>(R.id.btnPremium)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        dialogTitle.setText(R.string.quota_exhausted_dialog_title)
        dialogMessage.setText(R.string.quota_exhausted_dialog_message)
        btnWatchAd.text = context.getString(R.string.ad_recharge_button, rechargeAmount)
        btnPremium.setText(R.string.quota_exhausted_dialog_premium)
        btnCancel.setText(R.string.quota_exhausted_dialog_cancel)

        btnWatchAd.setOnClickListener {
            onWatchAd()
            dismiss()
        }

        btnPremium.setOnClickListener {
            onPremium()
            dismiss()
        }

        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }
    }
}
