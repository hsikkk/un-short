package com.muuu.affiliate

import android.content.Context
import android.view.View

/**
 * Affiliate 배너 뷰를 제공하는 추상 인터페이스
 * 각 플랫폼(쿠팡, Amazon 등)별로 구현체를 제공
 */
interface AffiliateProvider {

    /**
     * Affiliate 배너 뷰를 생성합니다
     *
     * @param context Android Context
     * @param width 배너 너비 (dp)
     * @param height 배너 높이 (dp)
     * @return 배너 뷰 (WebView, 커스텀 뷰 등)
     */
    fun createBannerView(
        context: Context,
        width: Int = 320,
        height: Int = 50
    ): View
}
