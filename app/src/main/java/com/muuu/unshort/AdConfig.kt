package com.muuu.unshort

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
    const val BANNER_HOME_BOTTOM = "ca-app-pub-3940256099942544/6300978111"

    /**
     * 타이머 화면 하단 배너 광고
     */
    const val BANNER_TIMER_BOTTOM = "ca-app-pub-3940256099942544/6300978111"

    /**
     * 타이머 화면 상단 네이티브 광고
     */
    const val NATIVE_TIMER_TOP = "ca-app-pub-3940256099942544/2247696110"

    // 향후 다른 광고 단위 ID 추가 예시:
    // const val BANNER_SETTINGS = "ca-app-pub-xxxxx/xxxxx"
    // const val INTERSTITIAL_AFTER_BLOCK = "ca-app-pub-xxxxx/xxxxx"
    // const val REWARDED_PREMIUM_UNLOCK = "ca-app-pub-xxxxx/xxxxx"
}
