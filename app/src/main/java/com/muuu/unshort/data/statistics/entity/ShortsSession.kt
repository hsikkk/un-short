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
    val timerCompleted: Boolean
)
