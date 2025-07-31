package com.lucas.recontrole.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lucas.recontrole.DAO.OccurrenceDao
import com.lucas.recontrole.model.OccurrenceEntity

@Database(entities = [OccurrenceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun occurrenceDao(): OccurrenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
