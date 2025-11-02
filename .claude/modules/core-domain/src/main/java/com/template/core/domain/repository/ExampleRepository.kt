package com.{{packageName}}.core.domain.repository

import com.{{packageName}}.core.domain.model.ExampleModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository 인터페이스 예시
 * Domain 레이어에서는 인터페이스만 정의하고, 실제 구현은 Data 레이어에서 진행
 */
interface ExampleRepository {
    /**
     * Flow를 통한 반응형 데이터 스트림
     */
    fun observeAll(): Flow<List<ExampleModel>>

    /**
     * 단일 데이터 조회
     */
    suspend fun getById(id: Long): ExampleModel?

    /**
     * 데이터 저장 또는 업데이트
     */
    suspend fun save(model: ExampleModel): Long

    /**
     * 데이터 삭제
     */
    suspend fun delete(id: Long)

    /**
     * 전체 데이터 삭제
     */
    suspend fun deleteAll()
}