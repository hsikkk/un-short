package com.muuu.unshort.data.statistics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.muuu.unshort.data.statistics.dao.ShortsSessionDao
import com.muuu.unshort.data.statistics.entity.ShortsSession

/**
 * Room Database
 *
 * 쇼츠 차단 통계 데이터베이스
 */
@Database(
    entities = [ShortsSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shortsSessionDao(): ShortsSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 싱글톤 Database 인스턴스 반환
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unshort_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
