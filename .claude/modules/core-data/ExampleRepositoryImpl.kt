package com.{{packageName}}.core.data.repository

import com.{{packageName}}.core.database.dao.ExampleDao
import com.{{packageName}}.core.database.entity.ExampleEntity
import com.{{packageName}}.core.domain.model.ExampleModel
import com.{{packageName}}.core.domain.repository.ExampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository 구현체
 * Adapter 패턴을 사용하여 Domain 레이어의 인터페이스를 구현
 *
 * 주요 역할:
 * - Database Entity와 Domain Model 간 변환
 * - 데이터 소스(Database, Network, Cache) 조합
 * - 비즈니스 로직이 아닌 데이터 접근 로직만 포함
 */
class ExampleRepositoryImpl(
    private val exampleDao: ExampleDao
) : ExampleRepository {

    override fun observeAll(): Flow<List<ExampleModel>> {
        return exampleDao.observeAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: Long): ExampleModel? {
        return exampleDao.getById(id)?.toDomainModel()
    }

    override suspend fun save(model: ExampleModel): Long {
        val entity = model.toEntity()
        return if (model.id == 0L) {
            exampleDao.insert(entity)
        } else {
            exampleDao.update(entity)
            model.id
        }
    }

    override suspend fun delete(id: Long) {
        exampleDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        exampleDao.deleteAll()
    }
}

/**
 * Entity를 Domain Model로 변환
 */
private fun ExampleEntity.toDomainModel(): ExampleModel {
    return ExampleModel(
        id = id,
        name = name,
        description = description,
        isActive = isActive
    )
}

/**
 * Domain Model을 Entity로 변환
 */
private fun ExampleModel.toEntity(): ExampleEntity {
    return ExampleEntity(
        id = id,
        name = name,
        description = description,
        isActive = isActive
    )
}