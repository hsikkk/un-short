package com.{{packageName}}.core.domain.model

/**
 * Domain 모델 예시
 * 비즈니스 로직에 필요한 데이터를 표현하는 순수한 데이터 클래스
 */
data class ExampleModel(
    val id: Long,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)