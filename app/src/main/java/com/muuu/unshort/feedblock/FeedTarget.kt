package com.muuu.unshort.feedblock

/**
 * 피드 차단 대상 앱 정의
 *
 * 피드 화면 식별용 ViewID/content-description 후보만 포함. 누적 정책/grace는
 * 모든 앱에 동일하게 적용되므로 FeedSessionManager가 단독 관리한다.
 */
data class FeedTarget(
    val packageName: String,
    val displayName: String,
    val feedContainerViewIds: List<String>,
    val feedItemSignatureViewIds: List<String>,
    val homeTabViewId: String,
    val excludeViewIds: List<String>,
    /**
     * 피드 아이템 시그니처 content-description 패턴 (부분 매칭, 대소문자 무시).
     * Facebook처럼 ViewID가 obfuscated된 앱에서 content-desc 기반 감지에 사용.
     */
    val feedItemSignatureContentDescriptions: List<String> = emptyList(),
    /**
     * 홈탭 content-description 패턴 (부분 매칭).
     * 매칭 노드의 isSelected가 true이면 홈탭 활성으로 판정.
     */
    val homeTabContentDescription: String? = null
)
