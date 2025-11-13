package com.muuu.affiliate

import android.content.Context
import com.muuu.affiliate.amazon.AmazonAffiliateProvider
import com.muuu.affiliate.coupang.CoupangAffiliateProvider
import java.util.Locale

/**
 * 국가별 Affiliate Provider 팩토리
 */
object AffiliateProviderFactory {

    /**
     * 현재 로케일에 따라 적절한 Affiliate Provider를 생성합니다
     *
     * @param context Android Context
     * @param coupangTrackingCode 쿠팡 파트너스 Tracking Code (한국만 해당)
     * @param coupangPartnersId 쿠팡 파트너스 ID (한국만 해당)
     * @param onLinkClick 링크 클릭 시 콜백 (Analytics 등)
     * @return 해당 국가의 Affiliate Provider
     */
    fun create(
        context: Context,
        coupangTrackingCode: String = "AF3505458",
        coupangPartnersId: Int = 941325,
        onLinkClick: ((String) -> Unit)? = null
    ): AffiliateProvider {
        val locale = Locale.getDefault()
        val country = locale.country

        return when (country) {
            "KR" -> {
                // 한국: 쿠팡 파트너스 (JavaScript 위젯)
                CoupangAffiliateProvider(
                    trackingCode = coupangTrackingCode,
                    partnersId = coupangPartnersId,
                    onLinkClick = onLinkClick
                )
            }

            "US", "CA", "UK", "DE", "FR", "JP", "IT", "ES" -> {
                // Amazon 지원 국가
                AmazonAffiliateProvider(
                    context = context,
                    domain = getAmazonDomain(country),
                    associateId = "muuu-20",  // TODO: 각 국가별 Associate ID 등록 필요
                    onLinkClick = onLinkClick
                )
            }

            else -> {
                // 기본값: Amazon.com
                AmazonAffiliateProvider(
                    context = context,
                    domain = "com",
                    associateId = "muuu-20",
                    onLinkClick = onLinkClick
                )
            }
        }
    }

    /**
     * 국가 코드에 따른 Amazon 도메인 반환
     */
    private fun getAmazonDomain(country: String): String {
        return when (country) {
            "UK" -> "co.uk"
            "JP" -> "co.jp"
            "DE" -> "de"
            "FR" -> "fr"
            "IT" -> "it"
            "ES" -> "es"
            "CA" -> "ca"
            else -> "com"
        }
    }
}
