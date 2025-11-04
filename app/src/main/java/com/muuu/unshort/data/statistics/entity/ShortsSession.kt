package com.muuu.unshort.data.statistics.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 쇼츠 세션 Entity
 *
 * 하나의 쇼츠 차단 시도 = 하나의 세션
 * 세션 종료 시점에 기록됨
 */
@Entity(
    tableName = "shorts_sessions",
    indices = [Index(value = ["timestamp"])]
)
data class ShortsSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 세션 종료 시각 (밀리초)
     */
    val timestamp: Long,

    /**
     * 차단 대상 앱 패키지명
     * 예: "com.google.android.youtube"
     */
    val packageName: String,

    /**
     * 시청 여부
     * - true: "볼래요" 선택하여 시청함
     * - false: "안볼래요" 선택 또는 앱 나감
     */
    val didWatch: Boolean,

    /**
     * 타이머 완료 여부
     * - true: 30초 타이머 완료 후 선택
     * - false: 타이머 완료 전 종료 (Skip 또는 앱 나감)
     */
    val timerCompleted: Boolean,

    /**
     * 스크롤 세션 여부
     * - true: 스크롤 후 재진입 세션 (이미 한 번 시청 허용된 상태에서 다시 차단됨)
     * - false: 첫 진입 세션 (처음 앱 실행 후 차단됨)
     */
    val isScrollSession: Boolean = false,

    /**
     * 최초 시청 시작 시각 (밀리초)
     * "볼래요" 선택 시의 시각
     */
    val watchStartTime: Long? = null,

    /**
     * 최종 시청 종료 시각 (밀리초)
     * 세션 종료 시의 시각
     */
    val watchEndTime: Long? = null,

    /**
     * 누적 시청 시간 (밀리초)
     * 포그라운드에서 실제 시청한 시간만 포함 (백그라운드 제외)
     */
    val watchDurationMs: Long? = null
)
