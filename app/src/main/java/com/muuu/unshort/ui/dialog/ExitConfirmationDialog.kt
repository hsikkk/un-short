package com.muuu.unshort.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.ui.dialog.ExitConfirmationDialog
import com.muuu.unshort.config.AdConfig
import com.muuu.unshort.R

/**
 * 앱 종료 확인 다이얼로그
 *
 * 가로 버튼 레이아웃과 커스터마이징 가능한 UI를 제공하는 종료 확인 다이얼로그
 */
class ExitConfirmationDialog(
    context: Context,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) : Dialog(context) {

    private lateinit var dialogTitle: TextView
    private lateinit var dialogMessage: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton
    private lateinit var adContainer: FrameLayout
    private var bannerAdView: MuuuBannerAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_exit_confirmation)

        // 다이얼로그 너비를 화면의 90%로 설정
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // View 초기화
        dialogTitle = findViewById(R.id.dialogTitle)
        dialogMessage = findViewById(R.id.dialogMessage)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
        adContainer = findViewById(R.id.adContainer)

        // 광고 설정
        setupAd()

        // 외부 터치로 닫기 허용
        setCanceledOnTouchOutside(true)

        // 취소 버튼
        btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        // 확인 버튼
        btnConfirm.setOnClickListener {
            onConfirm()
            dismiss()
        }
    }

    private fun setupAd() {
          bannerAdView = AdManager.setupBannerAd(
              context = context,
              container = adContainer,
              adUnit = MuuuBannerAdUnit(
                  key = AdConfig.MREC_EXIT,
                  placement = "exit_dialog_mrec",
                  bannerSize = MuuuBannerSize.MREC,
                  refreshInterval = 0 // 다이얼로그이므로 refresh 안함
              ),
              keepContainerSpace = false
          )
    }

    override fun dismiss() {
        // 광고 정리
        bannerAdView?.destroy()
        super.dismiss()
    }
}
