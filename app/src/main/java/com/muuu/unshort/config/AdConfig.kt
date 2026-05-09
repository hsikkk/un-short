package com.muuu.unshort.config
import com.muuu.unshort.config.AdConfig

/**
 * Muuu Ad SDK 광고 단위 ID 관리
 *
 * 테스트 ID: Google에서 제공하는 테스트용 광고 단위 ID
 * 실제 배포 시에는 실제 광고 단위 ID로 변경 필요
 */
object AdConfig {

    /**
     * 테스트용 배너 광고 단위 ID
     * 실제 운영 시에는 실제 광고 단위 ID로 교체
     */
    const val BANNER_HOME_BOTTOM = "ca-app-pub-4831094849543419/7233881974"

    /**
     * 타이머 화면 하단 배너 광고
     */
    const val BANNER_TIMER_BOTTOM = "ca-app-pub-4831094849543419/2199873682"

    /**
     * 타이머 화면 상단 네이티브 광고
     */
    const val NATIVE_TIMER_TOP = "ca-app-pub-4831094849543419/1719036695"

    /**
     * 리포트 화면 상단 MREC 광고
     */
    const val MREC_REPORT = "ca-app-pub-4831094849543419/1966950687"

    /**
     * Exit Dialog MREC 광고
     */
    const val MREC_EXIT = "ca-app-pub-4831094849543419/3738648618"

    /**
     * 오버레이 화면 상단 네이티브 광고
     */
    const val NATIVE_OVERLAY_TOP = "ca-app-pub-4831094849543419/1907926690"

    /**
     * 오버레이 화면 하단 배너 광고
     */
    const val BANNER_OVERLAY_BOTTOM = "ca-app-pub-4831094849543419/6918271296"

    /**
     * 즉시 차단 인터스티셜
     */
    const val INTERSTITIAL_TURN_OFF = "ca-app-pub-4831094849543419/5244119557"

    /**
     * 즉시 해제 한도 충전 리워드 광고
     *
     * 차단 화면 하단 "광고 보고 +N회" 버튼에서 사용.
     * 시청 완료 시 DailyUnblockQuotaManager.rechargeFromAd로 한도 충전.
     */
    const val REWARDED_UNBLOCK_QUOTA_RECHARGE = "ca-app-pub-4831094849543419/6524697279"
}
