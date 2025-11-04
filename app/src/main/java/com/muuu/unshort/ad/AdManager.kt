package com.muuu.unshort.ad

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.view.MuuuNativeAdView
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuNativeAdUnit
import com.muuu.ad.core.model.MuuuNativeAdBinder
import com.muuu.ad.core.model.nativetemplate.MuuuNativeAdTemplate
import com.muuu.unshort.premium.PremiumManager

/**
 * 광고 관리자
 *
 * 앱 전체의 광고 로딩 및 프리미엄 상태에 따른 광고 제거를 담당
 * Activity에서 광고를 직접 관리하지 않고 AdManager를 통해 처리
 */
object AdManager {

    /**
     * 배너 광고 설정
     *
     * 프리미엄 사용자인 경우 광고를 표시하지 않음
     * 프리미엄 상태 변경 시 자동으로 광고 제거 (Callback 리스너)
     *
     * @param activity Activity 컨텍스트
     * @param container 광고를 추가할 ViewGroup
     * @param adUnit 광고 단위 정보
     * @param keepContainerSpace 프리미엄 시 container 영역 유지 여부 (기본값: true)
     * @return 생성된 MuuuBannerAdView (프리미엄이면 null)
     */
    fun setupBannerAd(
        activity: Activity,
        container: ViewGroup,
        adUnit: MuuuBannerAdUnit,
        keepContainerSpace: Boolean = true
    ): MuuuBannerAdView? {
        // 이미 프리미엄이면 광고 표시 안함
        if (PremiumManager.isPremium()) {
            if (keepContainerSpace) {
                container.visibility = View.INVISIBLE
            } else {
                container.visibility = View.GONE
            }
            return null
        }

        // 광고 생성 및 로드
        val bannerAdView = MuuuBannerAdView(activity, adUnit)
        container.addView(bannerAdView)
        bannerAdView.loadAd()

        // 프리미엄 변경 리스너 등록
        // 프리미엄 구매 시 자동으로 광고 제거
        val premiumChangeListener = {
            if (PremiumManager.isPremium()) {
                bannerAdView.destroy()
                if (keepContainerSpace) {
                    container.visibility = View.INVISIBLE
                } else {
                    container.visibility = View.GONE
                }
            }
        }
        PremiumManager.addPremiumChangeListener(premiumChangeListener)

        // Activity destroy 시 리스너 제거 (메모리 누수 방지)
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PremiumManager.removePremiumChangeListener(premiumChangeListener)
                    super.onDestroy(owner)
                }
            })
        }

        return bannerAdView
    }

    /**
     * 네이티브 광고 설정
     *
     * 프리미엄 사용자인 경우 광고를 표시하지 않음
     * 프리미엄 상태 변경 시 자동으로 광고 제거 (Callback 리스너)
     *
     * @param activity Activity 컨텍스트
     * @param container 광고를 추가할 ViewGroup
     * @param adUnit 광고 단위 정보
     * @param template 광고 템플릿 (옵션)
     * @param keepContainerSpace 프리미엄 시 container 영역 유지 여부 (기본값: true)
     * @return 생성된 MuuuNativeAdView (프리미엄이면 null)
     */
    fun setupNativeAd(
        activity: Activity,
        container: ViewGroup,
        adUnit: MuuuNativeAdUnit,
        template: MuuuNativeAdTemplate? = null,
        keepContainerSpace: Boolean = true
    ): MuuuNativeAdView? {
        // 이미 프리미엄이면 광고 표시 안함
        if (PremiumManager.isPremium()) {
            if (keepContainerSpace) {
                container.visibility = View.INVISIBLE
            } else {
                container.visibility = View.GONE
            }
            return null
        }

        // 광고 생성
        val nativeAdView = MuuuNativeAdView(activity, adUnit)

        // 템플릿 설정
        if (template != null) {
            nativeAdView.setAdBinder(
                MuuuNativeAdBinder.fromTemplate(activity, template)
            )
        }

        container.addView(nativeAdView)
        nativeAdView.loadAd()

        // 프리미엄 변경 리스너 등록
        // 프리미엄 구매 시 자동으로 광고 제거
        val premiumChangeListener = {
            if (PremiumManager.isPremium()) {
                nativeAdView.destroy()
                if (keepContainerSpace) {
                    container.visibility = View.INVISIBLE
                } else {
                    container.visibility = View.GONE
                }
            }
        }
        PremiumManager.addPremiumChangeListener(premiumChangeListener)

        // Activity destroy 시 리스너 제거 (메모리 누수 방지)
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PremiumManager.removePremiumChangeListener(premiumChangeListener)
                    super.onDestroy(owner)
                }
            })
        }

        return nativeAdView
    }
}
