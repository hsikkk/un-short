package com.muuu.affiliate

import android.content.Context
import android.view.View

/**
 * Affiliate 배너 뷰를 제공하는 추상 인터페이스
 * 각 플랫폼(쿠팡, Amazon 등)별로 구현체를 제공
 *
 * 배너 크기: 320dp x 50dp (고정)
 */
interface AffiliateProvider {

    /**
     * Affiliate 배너 뷰를 생성합니다
     * 크기는 320dp x 50dp로 고정됩니다
     *
     * @param context Android Context
     * @return 배너 뷰 (WebView, 커스텀 뷰 등)
     */
    fun createBannerView(context: Context): View
}
