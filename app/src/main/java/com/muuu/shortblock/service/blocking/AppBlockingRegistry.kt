package com.muuu.shortblock.service.blocking

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
 * 콘텐츠 해시 생성을 위한 설정
 */
data class HashConfig(
    val containerViewId: String?,
    val includedViewIdPatterns: List<String>,
    val excludedViewIdPatterns: List<String>,
    val excludedTextPatterns: List<Regex>,
    val maxDepth: Int = 8,
    val textValidator: (String) -> Boolean  // 앱별 텍스트 검증 함수
)

/**
 * 앱별 차단 설정
 */
data class AppBlockingConfig(
    val packageName: String,
    val displayName: String,
    val viewIdDetectors: List<ViewIdDetector>,
    val textDetectors: List<TextDetector>,
    val hashConfig: HashConfig
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
                Regex("\\d+:\\d+")  // 시간 패턴만 (0:15, 1:30 등)
            ),
            maxDepth = 8,
            textValidator = { text ->
                // YouTube: 콜론/슬래시만 있는 텍스트 제외
                text.length > 2 && !text.all { it.isDigit() || it == ':' || it == '/' }
            }
        )
    )

    val INSTAGRAM = AppBlockingConfig(
        packageName = "com.instagram.android",
        displayName = "Instagram Reels",
        viewIdDetectors = listOf(
            ViewIdDetector("com.instagram.android:id/clips_viewer_view_pager")
        ),
        textDetectors = listOf(
            TextDetector(
                text = "Reels",
                requiresSelection = false,
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
            }
        )
    )

    // TikTok은 추후 추가 예정
    // val TIKTOK = AppBlockingConfig(...)

    /**
     * 모든 앱 설정 리스트
     */
    val ALL_CONFIGS = listOf(
        YOUTUBE,
        INSTAGRAM
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
