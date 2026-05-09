package com.muuu.unshort.changelog

import androidx.annotation.StringRes
import com.muuu.unshort.R

data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    @StringRes val changes: List<Int>
)

/**
 * 버전별 changelog를 관리하는 레지스트리
 *
 * 새 버전 출시 시 changelogs 리스트 상단에 항목을 추가하면 된다.
 * changes에는 string resource ID 리스트를 넣어 다국어를 자동 지원한다.
 */
object ChangelogRegistry {

    private val changelogs = listOf<ChangelogEntry>(
        ChangelogEntry(
            versionCode = 30,
            versionName = "1.8.0",
            changes = listOf(
                R.string.changelog_v1_8_0_item1,
                R.string.changelog_v1_8_0_item2,
                R.string.changelog_v1_8_0_item3
            )
        ),
        ChangelogEntry(
            versionCode = 29,
            versionName = "1.7.0",
            changes = listOf(
                R.string.changelog_v1_7_0_item1,
                R.string.changelog_v1_7_0_item2
            )
        ),
        ChangelogEntry(
            versionCode = 28,
            versionName = "1.6.1",
            changes = listOf(
                R.string.changelog_v1_6_1_item1
            )
        ),
        ChangelogEntry(
            versionCode = 27,
            versionName = "1.6.0",
            changes = listOf(
                R.string.changelog_v1_6_0_item1,
                R.string.changelog_v1_6_0_item2
            )
        ),
        ChangelogEntry(
            versionCode = 26,
            versionName = "1.5.1",
            changes = listOf(
                R.string.changelog_v1_5_1_item1,
                R.string.changelog_v1_5_1_item2
            )
        ),
        ChangelogEntry(
            versionCode = 25,
            versionName = "1.5.0",
            changes = listOf(
                R.string.changelog_v1_5_0_item1,
                R.string.changelog_v1_5_0_item2,
                R.string.changelog_v1_5_0_item3
            )
        )
    )

    fun getChangelogForVersion(versionCode: Int): ChangelogEntry? {
        return changelogs.find { it.versionCode == versionCode }
    }

    fun getLatestChangelog(): ChangelogEntry? {
        return changelogs.firstOrNull()
    }
}
