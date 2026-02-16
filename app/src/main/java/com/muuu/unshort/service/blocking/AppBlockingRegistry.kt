package com.muuu.unshort.service.blocking

import com.muuu.unshort.R

/**
 * 쇼츠 감지를 위한 View ID 감지기 설정
 */
data class ViewIdDetector(
    val viewId: String
)

/**
 * 쇼츠 감지를 위한 텍스트 기반 감지기 설정
 */
data class TextDetector(
    val text: String,
    val requiresSelection: Boolean = false,
    val searchType: SearchType = SearchType.TEXT
)

enum class SearchType {
    TEXT,
    CONTENT_DESCRIPTION
}

/**
 * 차단 종료 시 수행할 액션
 */
enum class ExitAction {
    BACK,  // 백버튼 (기본값, YouTube/Instagram 등)
    HOME   // 홈버튼 (TikTok 등 백버튼이 작동하지 않는 앱)
}

/**
 * 콘텐츠 해시 생성을 위한 설정
 */
data class HashConfig(
    val containerViewId: String?,
    val includedViewIdPatterns: List<String>,
    val excludedViewIdPatterns: List<String>,
    val excludedTextPatterns: List<Regex>,
    val maxDepth: Int = 8,
    val textValidator: (String) -> Boolean,  // 앱별 텍스트 검증 함수
    val excludeBoundsRight: Int? = null,  // 우측 x 좌표 이상인 노드 제외 (null이면 제한 없음)
    val includeContentDescription: Boolean = true,  // contentDescription 포함 여부 (기본값 true)
    val useStableExtractor: Boolean = false  // 안정적인 콘텐츠 추출 사용 여부 (YouTube만 true)
)

/**
 * 앱별 차단 설정
 */
data class AppBlockingConfig(
    val packageName: String,
    val displayName: String,
    val shortName: String,  // 가로 스크롤용 짧은 이름
    val iconResId: Int,  // 아이콘 리소스 ID
    val viewIdDetectors: List<ViewIdDetector>,
    val textDetectors: List<TextDetector>,
    val hashConfig: HashConfig,
    val controlsMedia: Boolean = false,  // 미디어 일시정지/재생 제어 여부 (기본값: false)
    val exitAction: ExitAction = ExitAction.BACK  // 차단 종료 시 수행할 액션 (기본값: BACK)
)

/**
 * 앱별 차단 설정을 중앙에서 관리하는 Registry
 *
 * 새로운 앱을 추가할 때는:
 * 1. 여기에 AppBlockingConfig 상수 추가
 * 2. ALL_CONFIGS 리스트에 추가
 */
object AppBlockingRegistry {

    val YOUTUBE = AppBlockingConfig(
        packageName = "com.google.android.youtube",
        displayName = "YouTube Shorts",
        shortName = "YouTube",
        iconResId = R.drawable.ic_youtube,
        viewIdDetectors = listOf(
            ViewIdDetector("com.google.android.youtube:id/reel_player_page_container")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "Shorts",
                requiresSelection = true,
                searchType = SearchType.TEXT
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.google.android.youtube:id/reel_player_page_container",
            includedViewIdPatterns = listOf(
                "title",
                "channel",
                "author",
                "description",
                "reel_metadata",
                "video_metadata"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "engagement",
                "button",
                "progress",
                "time",
                "duration",
                "seek",
                "player_control"
            ),
            excludedTextPatterns = listOf(
                Regex("\\d+:\\d+"),  // 시간 패턴 (0:15, 1:30 등)
                Regex(".*\\d+명.*좋아함.*"),  // "나 외에 사용자 262명이 이 동영상을 좋아함"
                Regex(".*댓글\\s*\\d+.*"),  // "댓글 91개 보기"
                Regex(".*좋아요\\s*\\d+.*"),  // "좋아요 262개"
                Regex(".*조회수\\s*\\d+.*"),  // "조회수 1.2만회"
                Regex("동영상 재생"),  // Play 버튼
                Regex("동영상 일지중지"),  // Pause 버튼
                Regex("동영상 진행률"),  // 진행률
                Regex("채널로 이동"),  // 채널 버튼
                Regex("더보기"),  // 더보기 버튼
                Regex("라이브"),  // 라이브 뱃지
                Regex("구독"),  // 구독 버튼
                Regex("Play [Vv]ideo", RegexOption.IGNORE_CASE),  // Play
                Regex("Pause [Vv]ideo", RegexOption.IGNORE_CASE),  // Pause
                Regex("Video [Pp]rogress", RegexOption.IGNORE_CASE),  // Progress
                Regex("Go to channel", RegexOption.IGNORE_CASE),  // Channel button
                Regex("Show more", RegexOption.IGNORE_CASE),  // More button
                Regex("Subscribe.*", RegexOption.IGNORE_CASE),  // Subscribe
                Regex("Subscriptions", RegexOption.IGNORE_CASE),  // Tab
                Regex("Shopping", RegexOption.IGNORE_CASE),  // Tab
                Regex("Live", RegexOption.IGNORE_CASE),  // Live
                Regex(".*Affiliate.*", RegexOption.IGNORE_CASE)  // 광고 텍스트
            ),
            maxDepth = 8,
            textValidator = { text ->
                // YouTube: 콜론/슬래시만 있는 텍스트 제외
                text.length > 2 && !text.all { it.isDigit() || it == ':' || it == '/' }
            },
            excludeBoundsRight = 1200,  // 우측 사이드바 제외 (좋아요/댓글/공유 버튼)
            includeContentDescription = false,  // YouTube만 제외 (UI 버튼 변화로 인한 오감지 방지)
            useStableExtractor = true  // 안정적인 콘텐츠 추출 활성화 (UI 변경 오감지 방지)
        ),
        controlsMedia = true  // YouTube만 미디어 제어 활성화
    )

    val INSTAGRAM = AppBlockingConfig(
        packageName = "com.instagram.android",
        displayName = "Instagram Reels",
        shortName = "Instagram",
        iconResId = R.drawable.ic_instagram,
        viewIdDetectors = listOf(
            ViewIdDetector("com.instagram.android:id/clips_viewer_view_pager")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "Reels",
                requiresSelection = true,
                searchType = SearchType.CONTENT_DESCRIPTION
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.instagram.android:id/clips_viewer_view_pager",
            includedViewIdPatterns = listOf(
                "username",
                "caption",
                "description",
                "user_name",
                "text_content",
                "primary_text"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "share",
                "action_bar",
                "button",
                "progress",
                "time",
                "duration",
                "seek",
                "player_control",
                "heart",
                "save"
            ),
            excludedTextPatterns = listOf(
                Regex("^\\d+[KMB]?$"),          // 1K, 10M, 100B 등
                Regex("^\\d+\\.\\d+[KMB]?$")    // 1.2K, 3.5M 등
            ),
            maxDepth = 8,
            textValidator = { text ->
                // Instagram: 콤마/마침표만 있는 텍스트 제외
                text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }
            },
            excludeBoundsRight = 1200  // 우측 사이드바 제외 (좋아요/댓글/공유 버튼)
        )
    )

    val FACEBOOK = AppBlockingConfig(
        packageName = "com.facebook.katana",
        displayName = "Facebook Reels",
        shortName = "Facebook",
        iconResId = R.drawable.ic_facebook,
        viewIdDetectors = listOf(
            // video_feed_container 제거: 피드 전체에서 사용되어 오감지 발생
            // reels_viewer_fragment_container만 사용: 전체화면 릴스 전용
            ViewIdDetector("com.facebook.katana:id/reels_viewer_fragment_container")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "Reels",
                requiresSelection = true,
                searchType = SearchType.TEXT
            ),
            TextDetector(
                text = "릴스",
                requiresSelection = true,  // 릴스 탭이 선택된 상태에서만 감지
                searchType = SearchType.TEXT
            ),
            TextDetector(
                text = "릴스 상세 정보",
                requiresSelection = false,  // 전체화면 릴스 감지 (한국어)
                searchType = SearchType.CONTENT_DESCRIPTION
            ),
            TextDetector(
                text = "Reel details",
                requiresSelection = false,  // 전체화면 릴스 감지 (영어)
                searchType = SearchType.CONTENT_DESCRIPTION
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.facebook.katana:id/video_feed_container",
            includedViewIdPatterns = listOf(
                "username",
                "caption",
                "description",
                "user_name",
                "text_content",
                "primary_text",
                "title"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "share",
                "action_bar",
                "button",
                "progress",
                "time",
                "duration",
                "seek",
                "player_control",
                "reaction"
            ),
            excludedTextPatterns = listOf(
                Regex("^\\d+[KMB]?$"),          // 1K, 10M, 100B 등
                Regex("^\\d+\\.\\d+[KMB]?$")    // 1.2K, 3.5M 등
            ),
            maxDepth = 8,
            textValidator = { text ->
                text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }
            }
        )
    )

    val NAVER = AppBlockingConfig(
        packageName = "com.nhn.android.search",
        displayName = "Naver Shorts",
        shortName = "Naver",
        iconResId = R.drawable.ic_naver,
        viewIdDetectors = listOf(
            ViewIdDetector("com.nhn.android.search:id/pager"),
            ViewIdDetector("com.nhn.android.search:id/player_view"),
            ViewIdDetector("com.nhn.android.search:id/horizontal_scroll_view")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "#네이버클립",
                requiresSelection = false,
                searchType = SearchType.TEXT
            ),
            TextDetector(
                text = "#네이버숏폼",
                requiresSelection = false,
                searchType = SearchType.TEXT
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.nhn.android.search:id/pager",
            includedViewIdPatterns = listOf(
                "title",
                "username",
                "caption",
                "description",
                "text_content",
                "author"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "share",
                "action",
                "button",
                "progress",
                "time",
                "duration",
                "seek",
                "player_control"
            ),
            excludedTextPatterns = listOf(
                Regex("^\\d+[만천억]?$"),        // 1만, 10천 등
                Regex("^\\d+\\.\\d+[만천억]?$")  // 1.2만 등
            ),
            maxDepth = 8,
            textValidator = { text ->
                text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }
            }
        )
    )

    val TIKTOK = AppBlockingConfig(
        packageName = "com.zhiliaoapp.musically",
        displayName = "TikTok Shorts",
        shortName = "TikTok",
        iconResId = R.drawable.ic_tiktok,
        viewIdDetectors = listOf(
            ViewIdDetector("com.zhiliaoapp.musically:id/video_view_pager"),
            ViewIdDetector("com.zhiliaoapp.musically:id/viewpager")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "For You",
                requiresSelection = true,
                searchType = SearchType.TEXT
            ),
            TextDetector(
                text = "Following",
                requiresSelection = false,
                searchType = SearchType.TEXT
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.zhiliaoapp.musically:id/video_view_pager",
            includedViewIdPatterns = listOf(
                "username",
                "title",
                "caption",
                "description",
                "text_content",
                "author"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "share",
                "button",
                "progress",
                "time",
                "duration",
                "player_control",
                "action"
            ),
            excludedTextPatterns = listOf(
                Regex("^\\d+[KMB]?$"),          // 1K, 10M, 100B
                Regex("^\\d+\\.\\d+[KMB]?$")    // 1.2K, 3.5M
            ),
            maxDepth = 8,
            textValidator = { text ->
                text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }
            }
        ),
        exitAction = ExitAction.HOME  // TikTok은 백버튼으로 나가지지 않으므로 홈버튼 사용
    )

    val TIKTOK_TRILL = AppBlockingConfig(
        packageName = "com.ss.android.ugc.trill",
        displayName = "TikTok Shorts",
        shortName = "TikTok",
        iconResId = R.drawable.ic_tiktok,
        viewIdDetectors = listOf(
            ViewIdDetector("com.ss.android.ugc.trill:id/video_view_pager"),
            ViewIdDetector("com.ss.android.ugc.trill:id/viewpager")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "For You",
                requiresSelection = true,
                searchType = SearchType.TEXT
            ),
            TextDetector(
                text = "Following",
                requiresSelection = false,
                searchType = SearchType.TEXT
            )
        ),
        hashConfig = HashConfig(
            containerViewId = "com.ss.android.ugc.trill:id/video_view_pager",
            includedViewIdPatterns = listOf(
                "username",
                "title",
                "caption",
                "description",
                "text_content",
                "author"
            ),
            excludedViewIdPatterns = listOf(
                "comment",
                "like",
                "share",
                "button",
                "progress",
                "time",
                "duration",
                "player_control",
                "action"
            ),
            excludedTextPatterns = listOf(
                Regex("^\\d+[KMB]?$"),          // 1K, 10M, 100B
                Regex("^\\d+\\.\\d+[KMB]?$")    // 1.2K, 3.5M
            ),
            maxDepth = 8,
            textValidator = { text ->
                text.length > 2 && !text.all { it.isDigit() || it == ',' || it == '.' }
            }
        ),
        exitAction = ExitAction.HOME  // TikTok은 백버튼으로 나가지지 않으므로 홈버튼 사용
    )

    /**
     * 모든 앱 설정 리스트
     */
    val ALL_CONFIGS = listOf(
        YOUTUBE,
        INSTAGRAM,
        FACEBOOK,
        NAVER,
        TIKTOK,
        TIKTOK_TRILL
    )

    /**
     * 패키지명으로 설정 조회
     */
    fun getConfigByPackageName(packageName: String): AppBlockingConfig? {
        return ALL_CONFIGS.find { it.packageName == packageName }
    }

    /**
     * 모든 대상 앱 패키지명 Set
     */
    val TARGET_PACKAGES: Set<String> = ALL_CONFIGS.map { it.packageName }.toSet()
}
