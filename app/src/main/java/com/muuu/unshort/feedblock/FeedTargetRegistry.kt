package com.muuu.unshort.feedblock

/**
 * 피드 차단 대상 앱 레지스트리
 */
object FeedTargetRegistry {

    val INSTAGRAM = FeedTarget(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        feedContainerViewIds = listOf(
            "com.instagram.android:id/swipeable_nav_view_pager_inner_recycler_view",
            "com.instagram.android:id/sticky_header_list",
            "com.instagram.android:id/refreshable_container"
        ),
        feedItemSignatureViewIds = listOf(
            "com.instagram.android:id/row_feed_view_group_social_ufi_buttons",
            "com.instagram.android:id/row_feed_view_group_social_ufi_container",
            "com.instagram.android:id/row_feed_photo_imageview",
            "com.instagram.android:id/row_feed_profile_header",
            "com.instagram.android:id/row_feed_view_group_buttons"
        ),
        homeTabViewId = "com.instagram.android:id/feed_tab",
        excludeViewIds = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/reels_tray_container"
        )
    )

    val YOUTUBE = FeedTarget(
        packageName = "com.google.android.youtube",
        displayName = "YouTube",
        feedContainerViewIds = listOf(
            "com.google.android.youtube:id/results"
        ),
        feedItemSignatureViewIds = emptyList(),
        homeTabViewId = "com.google.android.youtube:id/pivot_bar",
        excludeViewIds = listOf(
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/watch_player",
            "com.google.android.youtube:id/player_fragment"
        )
    )

    val THREADS = FeedTarget(
        packageName = "com.instagram.barcelona",
        displayName = "Threads",
        feedContainerViewIds = listOf("MainFeedScreen"),
        feedItemSignatureViewIds = listOf(
            "FeedPostRow",
            "feed_post_header",
            "feed_post_ufi_like_button",
            "feed_post_ufi_reply_button"
        ),
        homeTabViewId = "barcelona_tab_main_feed",
        // 다른 탭의 ViewID는 메인피드 화면에서도 visible(하단 탭바)이라 exclude 부적합.
        // MainFeedScreen container + 게시물 시그니처가 동시 가시일 때만 메인피드로 판정.
        excludeViewIds = emptyList()
    )

    val FACEBOOK = FeedTarget(
        packageName = "com.facebook.katana",
        displayName = "Facebook",
        feedContainerViewIds = emptyList(),
        feedItemSignatureViewIds = emptyList(),
        homeTabViewId = "",
        excludeViewIds = emptyList(),
        feedItemSignatureContentDescriptions = listOf(
            "프로필 사진",
            "게시물 옵션 더 보기",
            "공감 ",
            "댓글 "
        ),
        homeTabContentDescription = "홈, 탭"
    )

    val ALL = listOf(INSTAGRAM, YOUTUBE, THREADS, FACEBOOK)

    val TARGET_PACKAGES: Set<String> = ALL.map { it.packageName }.toSet()

    fun getByPackage(packageName: String): FeedTarget? =
        ALL.find { it.packageName == packageName }
}
