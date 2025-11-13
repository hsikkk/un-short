package com.muuu.affiliate.amazon

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.muuu.affiliate.AffiliateProvider

/**
 * Amazon Associates Affiliate Provider
 * SiteStripe 텍스트 링크 방식 구현
 */
class AmazonAffiliateProvider(
    private val context: Context,
    private val domain: String = "com",  // com, co.uk, co.jp, de, fr, it, es, ca
    private val associateId: String = "muuu-20",
    private val onLinkClick: ((String) -> Unit)? = null
) : AffiliateProvider {

    companion object {
        private const val TAG = "AmazonAffiliateProvider"
    }

    override fun createBannerView(context: Context): View {
        // 고정 크기: 320dp x 50dp
        val density = context.resources.displayMetrics.density
        val widthPx = (320 * density).toInt()
        val heightPx = (50 * density).toInt()
        // 컨테이너 생성 (고지 문구 + 배너)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // FTC 고지 문구 TextView
        val disclosureText = TextView(context).apply {
            text = "As an Amazon Associate, this app earns from qualifying purchases"
            textSize = 10f
            setTextColor(0xFF8A8A8A.toInt())
            gravity = Gravity.CENTER
            val dp4 = (context.resources.displayMetrics.density * 4).toInt()
            setPadding(dp4, 0, dp4, dp4)
        }

        // 배너 TextView (클릭 가능)
        val bannerView = TextView(context).apply {
            text = "Shop on Amazon"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF2D2D2D.toInt())

            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)

            isClickable = true
            isFocusable = true

            // 클릭 리스너
            setOnClickListener {
                val affiliateUrl = "https://www.amazon.$domain?tag=$associateId"

                try {
                    // Analytics 콜백 호출
                    onLinkClick?.invoke(affiliateUrl)

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(affiliateUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Log.d(TAG, "Opening Amazon affiliate link: $affiliateUrl")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open Amazon affiliate link", e)
                }
            }
        }

        // 컨테이너에 추가: 고지 문구 → 배너 순서
        container.addView(disclosureText)
        container.addView(bannerView)

        Log.d(TAG, "Amazon banner created (320x50 dp, domain: $domain, id: $associateId)")

        return container
    }
}
