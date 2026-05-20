package com.muuu.unshort.service.blocking

import com.muuu.unshort.premium.PremiumManager

/**
 * 차단 오버레이 카운트다운 정책
 *
 * 무료 사용자는 보조 액션 버튼("그만 볼래요", "바로 보기", "계속 볼래요")에
 * 의도적 마찰 카운트다운이 적용되지만, 프리미엄 사용자는 0초로 즉시 활성화된다.
 */
object OverlayCountdownPolicy {

    fun resolveDelaySeconds(defaultSeconds: Int): Int =
        if (PremiumManager.isPremium()) 0 else defaultSeconds
}
