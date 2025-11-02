package com.{{packageName}}.core.domain.usecase

import com.{{packageName}}.core.domain.model.ExampleModel
import com.{{packageName}}.core.domain.repository.ExampleRepository

/**
 * UseCase 예시 - 데이터 저장
 * 비즈니스 규칙 검증 로직을 포함할 수 있음
 */
class SaveExampleUseCase(
    private val repository: ExampleRepository
) {
    suspend operator fun invoke(model: ExampleModel): Result<Long> {
        return try {
            // 비즈니스 규칙 검증
            if (model.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Name cannot be empty"))
            }

            // 저장 실행
            val id = repository.save(model)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}