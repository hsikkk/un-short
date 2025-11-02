package com.{{packageName}}.core.di

import android.content.Context
import androidx.room.Room
import com.{{packageName}}.core.data.repository.ExampleRepositoryImpl
import com.{{packageName}}.core.database.AppDatabase
import com.{{packageName}}.core.datastore.UserPreferencesDataStore
import com.{{packageName}}.core.domain.repository.ExampleRepository
import com.{{packageName}}.core.domain.usecase.GetExampleListUseCase
import com.{{packageName}}.core.domain.usecase.SaveExampleUseCase

/**
 * Manual Dependency Injection Container
 *
 * 순환 참조 방지를 위한 설계 원칙:
 * 1. core:di는 domain, data, database, datastore에만 의존
 * 2. feature 모듈의 ViewModel은 app 모듈에서 생성
 * 3. service 모듈은 app 모듈에서 직접 관리
 *
 * 이 구조를 통해 모듈 간 명확한 의존성 방향을 유지합니다.
 */
class AppContainer(private val context: Context) {

    // Database
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    // DataStore
    val userPreferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    // DAOs
    private val exampleDao by lazy { database.exampleDao() }

    // Repositories
    val exampleRepository: ExampleRepository by lazy {
        ExampleRepositoryImpl(exampleDao)
    }

    // Use Cases
    val getExampleListUseCase: GetExampleListUseCase by lazy {
        GetExampleListUseCase(exampleRepository)
    }

    val saveExampleUseCase: SaveExampleUseCase by lazy {
        SaveExampleUseCase(exampleRepository)
    }

    /**
     * Feature 모듈에서 필요한 의존성을 제공하는 팩토리 메서드
     * ViewModel 생성은 app 모듈에서 담당
     */
    fun provideExampleDependencies() = ExampleDependencies(
        getExampleListUseCase = getExampleListUseCase,
        saveExampleUseCase = saveExampleUseCase
    )

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Feature 모듈에서 필요한 의존성을 묶은 데이터 클래스
 * 순환 참조를 방지하면서 필요한 의존성만 전달
 */
data class ExampleDependencies(
    val getExampleListUseCase: GetExampleListUseCase,
    val saveExampleUseCase: SaveExampleUseCase
)