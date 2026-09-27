package com.doitsh.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.doitsh.app.data.model.ProjectEntity
import com.doitsh.app.data.model.TaskEntity

@Database(
    entities = [TaskEntity::class, ProjectEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DoitshDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: DoitshDatabase? = null

        fun getDatabase(context: Context): DoitshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoitshDatabase::class.java,
                    "doitsh_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
