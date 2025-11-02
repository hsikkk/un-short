package com.{{packageName}}.core.domain.usecase

import com.{{packageName}}.core.domain.model.ExampleModel
import com.{{packageName}}.core.domain.repository.ExampleRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase 예시 - 리스트 조회
 * 단일 책임 원칙에 따라 하나의 비즈니스 로직만 담당
 */
class GetExampleListUseCase(
    private val repository: ExampleRepository
) {
    /**
     * invoke operator를 사용하여 함수처럼 호출 가능
     * 예: useCase() 또는 useCase.invoke()
     */
    operator fun invoke(): Flow<List<ExampleModel>> {
        return repository.observeAll()
    }
}