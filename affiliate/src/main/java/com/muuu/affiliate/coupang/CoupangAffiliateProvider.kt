package com.muuu.affiliate.coupang

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import com.muuu.affiliate.AffiliateProvider

/**
 * 쿠팡 파트너스 Affiliate Provider 구현체
 * JavaScript 위젯을 이용한 배너 표시
 */
class CoupangAffiliateProvider(
    private val trackingCode: String = "AF3505458",
    private val partnersId: Int = 941325,
    private val onLinkClick: ((String) -> Unit)? = null
) : AffiliateProvider {

    companion object {
        private const val TAG = "CoupangAffiliateProvider"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createBannerView(context: Context): View {
        // 고정 크기: 320dp x 50dp
        val density = context.resources.displayMetrics.density
        val widthPx = (320 * density).toInt()
        val heightPx = (50 * density).toInt()
        // 컨테이너 생성 (고지 문구 + WebView)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 고지 문구 TextView
        val disclosureText = TextView(context).apply {
            text = "이 앱은 쿠팡 파트너스 활동의 일환으로, 이에 따른 일정액의 수수료를 제공받습니다"
            textSize = 10f
            setTextColor(0xFF8A8A8A.toInt())
            gravity = Gravity.START
            val dp4 = (context.resources.displayMetrics.density * 4).toInt()
            setPadding(dp4, 0, dp4, dp4)
        }

        // WebView 생성
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)

            // WebView 설정
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = false
            }

            // WebViewClient 설정 - 링크 클릭 시 외부 브라우저로 열기
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false

                    // 쿠팡 제휴 링크 클릭 시 외부 브라우저로 열기
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        try {
                            // Analytics 콜백 호출
                            onLinkClick?.invoke(url)

                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            Log.d(TAG, "Opening affiliate link in browser: $url")
                            return true
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open affiliate link", e)
                        }
                    }

                    return false
                }
            }

            // 쿠팡 파트너스 HTML
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background-color: transparent;
                        }
                    </style>
                </head>
                <body>
                    <script src="https://ads-partners.coupang.com/g.js"></script>
                    <script>
                        new PartnersCoupang.G({
                            "id": $partnersId,
                            "template": "carousel",
                            "trackingCode": "$trackingCode",
                            "width": "320",
                            "height": "50",
                            "tsource": ""
                        });
                    </script>
                </body>
                </html>
            """.trimIndent()

            // HTML 로드
            loadDataWithBaseURL(
                "https://www.coupang.com",
                html,
                "text/html",
                "UTF-8",
                null
            )

            Log.d(TAG, "Coupang banner WebView created (320x50 dp)")
        }

        // 컨테이너에 추가: 고지 문구 → WebView 순서
        container.addView(disclosureText)
        container.addView(webView)

        return container
    }
}
