package com.muuu.affiliate.amazon

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import com.muuu.affiliate.AffiliateProvider

/**
 * Amazon Affiliate Provider (스텁 구현)
 * TODO: 실제 Amazon 제휴 위젯 연동 필요
 */
class AmazonAffiliateProvider(
    private val context: Context,
    private val domain: String = "com"  // com, co.uk, co.jp, de, fr, it, es, ca
) : AffiliateProvider {

    companion object {
        private const val TAG = "AmazonAffiliateProvider"
    }

    override fun createBannerView(
        context: Context,
        width: Int,
        height: Int
    ): View {
        Log.w(TAG, "Amazon affiliate provider not yet implemented - showing placeholder")

        // TODO: Amazon 제휴 위젯 연동
        // Amazon Native Shopping Ads API 또는 Amazon Associates 위젯 사용

        // 임시 플레이스홀더
        return TextView(context).apply {
            text = "Amazon Ads (Coming Soon)"
            textSize = 12f
            setTextColor(0xFF8A8A8A.toInt())
        }
    }
}
