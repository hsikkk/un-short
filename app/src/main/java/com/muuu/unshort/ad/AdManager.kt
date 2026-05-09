package com.muuu.unshort.ad

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.muuu.ad.view.MuuuBannerAdView
import com.muuu.ad.view.MuuuNativeAdView
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.adunit.MuuuNativeAdUnit
import com.muuu.ad.core.adunit.MuuuRewardedAdUnit
import com.muuu.ad.core.listener.MuuuRewardedAdListener
import com.muuu.ad.core.listener.MuuuRewardedAdLoaderListener
import com.muuu.ad.core.model.MuuuAdDisplayFailError
import com.muuu.ad.core.model.MuuuAdInfo
import com.muuu.ad.core.model.MuuuAdLoadError
import com.muuu.ad.core.model.MuuuNativeAdBinder
import com.muuu.ad.core.model.MuuuRewardedAd
import com.muuu.ad.core.model.nativetemplate.MuuuNativeAdTemplate
import com.muuu.ad.loader.MuuuRewardedAdLoader
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
     * @param context 컨텍스트
     * @param container 광고를 추가할 ViewGroup
     * @param adUnit 광고 단위 정보
     * @param keepContainerSpace 프리미엄 시 container 영역 유지 여부 (기본값: true)
     * @return 생성된 MuuuBannerAdView (프리미엄이면 null)
     */
    fun setupBannerAd(
        context: Context,
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
        val bannerAdView = MuuuBannerAdView(context, adUnit)
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
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {
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
     * @param context 컨텍스트
     * @param container 광고를 추가할 ViewGroup
     * @param adUnit 광고 단위 정보
     * @param template 광고 템플릿 (옵션)
     * @param keepContainerSpace 프리미엄 시 container 영역 유지 여부 (기본값: true)
     * @return 생성된 MuuuNativeAdView (프리미엄이면 null)
     */
    fun setupNativeAd(
        context: Context,
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
        val nativeAdView = MuuuNativeAdView(context, adUnit)

        // 템플릿 설정
        if (template != null) {
            nativeAdView.setAdBinder(
                MuuuNativeAdBinder.fromTemplate(context, template)
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
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PremiumManager.removePremiumChangeListener(premiumChangeListener)
                    super.onDestroy(owner)
                }
            })
        }

        return nativeAdView
    }

    /**
     * 리워드 광고 로드 및 표시
     *
     * 차단 화면 하단 "광고 보고 +N회" 버튼에서 호출.
     * 프리미엄 사용자는 즉시 NotApplicable 반환 (광고 없음).
     *
     * 콜백 시점:
     *  - Earned: 사용자가 광고 시청 완료, 보상 수령 (한도 충전 트리거)
     *  - Failed: 로드 또는 노출 실패 → 토스트 안내 후 사용자 재시도 가능
     *  - Cancelled: 시청 도중 dismiss → 한도 충전 X
     *
     * @param activity Activity 컨텍스트
     * @param adUnit 광고 단위
     * @param onResult 결과 콜백
     */
    fun setupRewardedAd(
        activity: Activity,
        adUnit: MuuuRewardedAdUnit,
        onResult: (RewardResult) -> Unit
    ) {
        if (PremiumManager.isPremium()) {
            onResult(RewardResult.NotApplicable)
            return
        }

        var earnedReward = false

        MuuuRewardedAdLoader(
            context = activity,
            adUnit = adUnit
        ).apply {
            setListener(
                object : MuuuRewardedAdLoaderListener {
                    override fun onAdLoadFail(err: MuuuAdLoadError) {
                        onResult(RewardResult.Failed(loadError = err))
                    }

                    override fun onAdLoadSuccess(
                        ad: MuuuRewardedAd,
                        adInfo: MuuuAdInfo
                    ) {
                        ad.setListener(object : MuuuRewardedAdListener {
                            override fun onShown(adInfo: MuuuAdInfo) {}

                            override fun onDismiss(adInfo: MuuuAdInfo) {
                                if (!earnedReward) {
                                    onResult(RewardResult.Cancelled)
                                }
                            }

                            override fun onFailedToShow(
                                adInfo: MuuuAdInfo,
                                error: MuuuAdDisplayFailError
                            ) {
                                onResult(RewardResult.Failed(displayError = error))
                            }

                            override fun onEarnedReward(
                                adInfo: MuuuAdInfo,
                                reward: MuuuRewardedAd.MuuuRewardedItem
                            ) {
                                earnedReward = true
                                onResult(RewardResult.Earned)
                            }
                        })
                        ad.show(activity)
                    }
                }
            )
            load()
        }
    }
}

/**
 * 리워드 광고 결과
 */
sealed interface RewardResult {
    data object Earned : RewardResult
    data class Failed(
        val loadError: MuuuAdLoadError? = null,
        val displayError: MuuuAdDisplayFailError? = null
    ) : RewardResult
    data object Cancelled : RewardResult
    data object NotApplicable : RewardResult
}
